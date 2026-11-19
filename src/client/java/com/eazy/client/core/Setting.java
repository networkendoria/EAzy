package com.eazy.client.core;

import com.eazy.client.EAzyClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/** Bir modülün tek bir ayarı. Değer JSON olarak kaydedilir/okunur. */
public final class Setting<T> {
	private final String key;
	private final Class<T> type;
	private final T defaultValue;
	private T value;

	public Setting(String key, Class<T> type, T defaultValue) {
		this.key = key;
		this.type = type;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String key() {
		return key;
	}

	public T get() {
		return value;
	}

	public void set(T newValue) {
		this.value = newValue;
	}

	public void reset() {
		this.value = defaultValue;
	}

	JsonElement toJson() {
		return ConfigManager.GSON.toJsonTree(value, type);
	}

	void fromJson(JsonElement element) {
		try {
			T parsed = ConfigManager.GSON.fromJson(element, type);
			if (parsed != null) {
				this.value = parsed;
			}
		} catch (JsonParseException | IllegalStateException | ClassCastException e) {
			// Bozuk değer: varsayılan kalsın, oyun çökmesin.
			EAzyClient.LOGGER.warn("Ayar '{}' okunamadı, varsayılan kullanılacak.", key, e);
			this.value = defaultValue;
		}
	}
}
