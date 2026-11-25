package com.lopi.client.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lopi.client.LopiClient;
import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.core.Setting;
import com.lopi.client.input.LopiKeys;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dinamik komut / yazı kısayolları. Oyuncu istediği kadar kısayol ekler, düzenler, siler.
 * Varsayılan olarak ilk açılışta tek bir "RTP AT" (/rtp) kısayolu eklenir; o da düzenlenebilir veya silinebilir.
 * Tuşlar GLFW ile doğrudan okunur (çalışma anında KeyMapping eklenemediği için).
 * "/" ile başlayan metin komut, diğerleri sohbet mesajı olarak gönderilir.
 */
public final class ShortcutsModule extends LopiModule {
	public static final int MAX_ENTRIES = 24;

	/** Tek bir kısayol. key: GLFW tuş kodu, 0 = atanmamış. */
	public static final class Entry {
		public String id = UUID.randomUUID().toString();
		public String name = "";
		public String command = "";
		public int key = 0;
	}

	private static ShortcutsModule instance;

	private final Setting<String> stored = setting("entries", String.class, "[]").hidden();
	private final Setting<Boolean> seeded = setting("seeded", Boolean.class, false).hidden();

	private final List<Entry> entries = new ArrayList<>();
	private final Map<String, Boolean> wasDown = new HashMap<>();
	private boolean parsed;

	public ShortcutsModule() {
		super("shortcuts", Category.SHORTCUTS, true);
		instance = this;
	}

	public static ShortcutsModule get() {
		return instance;
	}

	/** Kayıtlı kısayollar (ilk çağrıda config'ten okunur, gerekirse RTP eklenir). */
	public List<Entry> entries() {
		ensureParsed();
		return entries;
	}

	public Entry find(String id) {
		for (Entry e : entries()) {
			if (e.id.equals(id)) {
				return e;
			}
		}
		return null;
	}

	/** Değişiklikleri kaydeder. */
	public void commit() {
		JsonArray array = new JsonArray();
		for (Entry e : entries) {
			JsonObject o = new JsonObject();
			o.addProperty("id", e.id);
			o.addProperty("name", e.name);
			o.addProperty("command", e.command);
			o.addProperty("key", e.key);
			array.add(o);
		}
		stored.set(array.toString());
		ModuleManager.saveAsync();
	}

	private void ensureParsed() {
		if (parsed) {
			return;
		}
		parsed = true;
		entries.clear();
		try {
			JsonElement root = JsonParser.parseString(stored.get() == null ? "[]" : stored.get());
			if (root.isJsonArray()) {
				for (JsonElement el : root.getAsJsonArray()) {
					if (!el.isJsonObject() || entries.size() >= MAX_ENTRIES) {
						continue;
					}
					JsonObject o = el.getAsJsonObject();
					Entry e = new Entry();
					if (o.has("id")) {
						e.id = o.get("id").getAsString();
					}
					if (o.has("name")) {
						e.name = o.get("name").getAsString();
					}
					if (o.has("command")) {
						e.command = o.get("command").getAsString();
					}
					if (o.has("key")) {
						e.key = o.get("key").getAsInt();
					}
					entries.add(e);
				}
			}
		} catch (RuntimeException ex) {
			LopiClient.LOGGER.warn("Kısayol listesi okunamadı, boş liste kullanılıyor.", ex);
		}
		if (!seeded.get()) {
			Entry rtp = new Entry();
			rtp.id = "rtp";
			rtp.name = "RTP AT";
			rtp.command = "/rtp";
			entries.add(0, rtp);
			seeded.set(true);
			commit();
		}
	}

	/** Kısayolun komutunu / mesajını gönderir. */
	public static void run(Minecraft client, Entry entry) {
		if (client.player == null || entry == null) {
			return;
		}
		String text = entry.command == null ? "" : entry.command.strip();
		if (text.isEmpty()) {
			return;
		}
		if (text.startsWith("/")) {
			if (text.length() > 1) {
				client.player.connection.sendCommand(text.substring(1));
			}
		} else {
			client.player.connection.sendChat(text);
		}
	}

	@Override
	protected void onEnable() {
		ensureParsed();
		wasDown.clear();
	}

	@Override
	protected void onTick(Minecraft client) {
		ensureParsed();
		boolean free = client.gui.screen() == null && client.player != null;
		for (Entry e : entries) {
			boolean down = free && e.key > 0 && LopiKeys.isDown(e.key);
			boolean before = wasDown.getOrDefault(e.id, Boolean.FALSE);
			if (down && !before) {
				run(client, e);
			}
			wasDown.put(e.id, down);
		}
	}
}
