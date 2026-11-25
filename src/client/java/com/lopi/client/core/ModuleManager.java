package com.lopi.client.core;

import com.lopi.client.LopiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modülleri kaydeder, açar/kapatır, tick'ler ve ayarlarını kaydeder.
 * Bir modül hata verirse sadece o modül kapatılır, oyun çökmez.
 */
public final class ModuleManager {
	private static final Map<String, LopiModule> MODULES = new LinkedHashMap<>();
	private static final List<LopiModule> ORDERED = new ArrayList<>();

	private ModuleManager() {
	}

	public static void register(LopiModule module) {
		if (MODULES.putIfAbsent(module.id(), module) != null) {
			throw new IllegalStateException("Aynı id ile iki modül: " + module.id());
		}
		ORDERED.add(module);
	}

	public static List<LopiModule> all() {
		return Collections.unmodifiableList(ORDERED);
	}

	/** Ayar ağacındaki değerleri modüllere uygular. Kayıt yoksa varsayılanlar kullanılır. */
	public static void applyConfig(JsonObject root) {
		JsonObject modules = root.has("modules") && root.get("modules").isJsonObject()
				? root.getAsJsonObject("modules") : new JsonObject();

		for (LopiModule module : ORDERED) {
			JsonObject section = modules.has(module.id()) && modules.get(module.id()).isJsonObject()
					? modules.getAsJsonObject(module.id()) : new JsonObject();

			JsonObject settingsJson = section.has("settings") && section.get("settings").isJsonObject()
					? section.getAsJsonObject("settings") : new JsonObject();
			for (Setting<?> setting : module.settings()) {
				JsonElement value = settingsJson.get(setting.key());
				if (value != null) {
					setting.fromJson(value);
				}
			}

			boolean wantEnabled = section.has("enabled") && section.get("enabled").isJsonPrimitive()
					? section.get("enabled").getAsBoolean() : module.defaultEnabled();
			applyState(module, wantEnabled);
		}
	}

	/** Modülü açar/kapatır ve kaydeder. Hatalı modül tekrar açılmayı dener. */
	public static void setEnabled(LopiModule module, boolean on) {
		applyState(module, on);
		saveAsync();
	}

	public static void toggle(LopiModule module) {
		// Hatalı modüle tıklanınca yeniden açmayı dener.
		setEnabled(module, module.failed || !module.enabled);
	}

	/** Her client tick'inde açık modüllerin onTick'ini çağırır. */
	public static void tick(Minecraft client) {
		for (int i = 0; i < ORDERED.size(); i++) {
			LopiModule module = ORDERED.get(i);
			if (!module.enabled) {
				continue;
			}
			try {
				module.onTick(client);
			} catch (Throwable t) {
				fail(module, t);
			}
		}
	}

	public static void saveAsync() {
		ConfigManager.saveAsync(toJson());
	}

	public static void saveNow() {
		ConfigManager.saveBlocking(toJson());
	}

	private static void applyState(LopiModule module, boolean on) {
		if (on) {
			try {
				module.failed = false;
				if (!module.enabled) {
					module.enabled = true;
					module.onEnable();
				}
			} catch (Throwable t) {
				fail(module, t);
			}
		} else {
			if (module.enabled) {
				module.enabled = false;
				try {
					module.onDisable();
				} catch (Throwable t) {
					LopiClient.LOGGER.error("Modül '{}' kapatılırken hata verdi.", module.id(), t);
				}
			}
			module.failed = false;
		}
	}

	private static void fail(LopiModule module, Throwable t) {
		LopiClient.LOGGER.error("Modül '{}' hata verdi ve kapatıldı.", module.id(), t);
		module.enabled = false;
		module.failed = true;
		try {
			module.onDisable();
		} catch (Throwable ignored) {
			// Temizlik sırasında hata olursa yok say.
		}
	}

	private static JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		JsonObject modules = new JsonObject();
		for (LopiModule module : ORDERED) {
			JsonObject section = new JsonObject();
			section.addProperty("enabled", module.enabled);
			JsonObject settingsJson = new JsonObject();
			for (Setting<?> setting : module.settings()) {
				settingsJson.add(setting.key(), setting.toJson());
			}
			section.add("settings", settingsJson);
			modules.add(module.id(), section);
		}
		root.add("modules", modules);
		return root;
	}
}
