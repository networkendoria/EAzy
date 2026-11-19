package com.eazy.client.core;

import com.eazy.client.EAzyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ayar dosyası (config/eazy.json) okuma/yazma.
 * Yazma işlemi ayrı bir thread'de yapılır, oyunun çizim akışını yavaşlatmaz.
 */
public final class ConfigManager {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("eazy.json");
	private static final Object WRITE_LOCK = new Object();
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EAzy-Config-IO");
		thread.setDaemon(true);
		return thread;
	});

	private static JsonObject root = new JsonObject();

	private ConfigManager() {
	}

	/** Dosyayı okur. Dosya yoksa veya bozuksa varsayılanlarla devam edilir. */
	public static void load() {
		if (!Files.exists(FILE)) {
			root = new JsonObject();
			return;
		}
		try {
			String text = Files.readString(FILE, StandardCharsets.UTF_8);
			JsonElement parsed = JsonParser.parseString(text);
			root = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (IOException | RuntimeException e) {
			EAzyClient.LOGGER.error("eazy.json okunamadı, varsayılan ayarlar kullanılacak.", e);
			backupBrokenFile();
			root = new JsonObject();
		}
	}

	/** Son okunan ayar ağacı. */
	public static JsonObject root() {
		return root;
	}

	/** Verilen JSON'u arka planda dosyaya yazar. */
	static void saveAsync(JsonObject snapshot) {
		String text = GSON.toJson(snapshot);
		IO.execute(() -> write(text));
	}

	/** Verilen JSON'u yazar ve bitmesini (en fazla 3 sn) bekler. Oyun kapanırken kullanılır. */
	static void saveBlocking(JsonObject snapshot) {
		String text = GSON.toJson(snapshot);
		try {
			IO.submit(() -> write(text)).get(3, TimeUnit.SECONDS);
		} catch (Exception e) {
			EAzyClient.LOGGER.error("eazy.json kaydedilemedi.", e);
		}
	}

	private static void write(String text) {
		synchronized (WRITE_LOCK) {
			try {
				Files.createDirectories(FILE.getParent());
				Path temp = FILE.resolveSibling("eazy.json.tmp");
				Files.writeString(temp, text, StandardCharsets.UTF_8);
				try {
					Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				EAzyClient.LOGGER.error("eazy.json yazılamadı.", e);
			}
		}
	}

	private static void backupBrokenFile() {
		try {
			Files.move(FILE, FILE.resolveSibling("eazy.json.broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			EAzyClient.LOGGER.warn("Bozuk eazy.json yedeklenemedi.", e);
		}
	}
}
