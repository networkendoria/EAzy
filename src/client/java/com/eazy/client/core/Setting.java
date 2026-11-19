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

	// Sayısal ayarlar için aralık (step > 0 ise sayısal ayar sayılır)
	private double min;
	private double max;
	private double step;
	private boolean hidden;

	public Setting(String key, Class<T> type, T defaultValue) {
		this.key = key;
		this.type = type;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	/** Sayısal ayara aralık ve adım verir (ayar ekranındaki - / + düğmeleri için). */
	public Setting<T> range(double min, double max, double step) {
		if (type != Integer.class && type != Double.class) {
			throw new IllegalStateException("range() sadece Integer/Double ayarlarda kullanılır: " + key);
		}
		this.min = min;
		this.max = max;
		this.step = step;
		return this;
	}

	/** Ayar ekranında gösterilmez (sadece kaydedilir). */
	public Setting<T> hidden() {
		this.hidden = true;
		return this;
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

	public boolean isHidden() {
		return hidden;
	}

	public boolean isBoolean() {
		return type == Boolean.class;
	}

	public boolean isNumber() {
		return step > 0;
	}

	public double step() {
		return step;
	}

	public double asDouble() {
		return ((Number) value).doubleValue();
	}

	/** Sayısal ayarı aralığa sıkıştırarak ayarlar. */
	@SuppressWarnings("unchecked")
	public void setFromNumber(double v) {
		double clamped = Math.max(min, Math.min(max, v));
		if (type == Integer.class) {
			value = (T) Integer.valueOf((int) Math.round(clamped));
		} else if (type == Double.class) {
			value = (T) Double.valueOf(Math.round(clamped * 100.0) / 100.0);
		}
	}

	/** Boolean ayarı tersine çevirir. */
	@SuppressWarnings("unchecked")
	public void toggle() {
		if (value instanceof Boolean b) {
			value = (T) Boolean.valueOf(!b);
		}
	}

	JsonElement toJson() {
		return ConfigManager.GSON.toJsonTree(value, type);
	}

	void fromJson(JsonElement element) {
		try {
			T parsed = ConfigManager.GSON.fromJson(element, type);
			if (parsed == null) {
				return;
			}
			this.value = parsed;
			if (isNumber() && parsed instanceof Number n) {
				setFromNumber(n.doubleValue()); // aralık dışı değeri düzelt
			}
		} catch (JsonParseException | IllegalStateException | ClassCastException e) {
			// Bozuk değer: varsayılan kalsın, oyun çökmesin.
			EAzyClient.LOGGER.warn("Ayar '{}' okunamadı, varsayılan kullanılacak.", key, e);
			this.value = defaultValue;
		}
	}
}
