package com.eazy.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tüm EAzy modüllerinin temel sınıfı.
 * Her özellik ayrı bir modüldür; açılıp kapatılabilir ve kendi ayarları vardır.
 * Durum değişikliği sadece {@link ModuleManager} üzerinden yapılır.
 */
public abstract class EAzyModule {
	private final String id;
	private final Category category;
	private final boolean defaultEnabled;
	private final List<Setting<?>> settings = new ArrayList<>();

	// ModuleManager tarafından yönetilir (aynı paket)
	boolean enabled;
	boolean failed;

	protected EAzyModule(String id, Category category, boolean defaultEnabled) {
		this.id = id;
		this.category = category;
		this.defaultEnabled = defaultEnabled;
	}

	/** Modüle yeni bir ayar ekler. Constructor içinde çağrılmalı. */
	protected final <T> Setting<T> setting(String key, Class<T> type, T defaultValue) {
		Setting<T> s = new Setting<>(key, type, defaultValue);
		settings.add(s);
		return s;
	}

	/** Modül açıldığında çağrılır. */
	protected void onEnable() {
	}

	/** Modül kapandığında çağrılır. */
	protected void onDisable() {
	}

	/** Modül açıkken her client tick'inde çağrılır. Ağır işleri burada yapma. */
	protected void onTick(Minecraft client) {
	}

	public final String id() {
		return id;
	}

	public final Category category() {
		return category;
	}

	public final boolean defaultEnabled() {
		return defaultEnabled;
	}

	public final boolean isEnabled() {
		return enabled;
	}

	/** Modül hata verip otomatik kapatıldıysa true. */
	public final boolean isFailed() {
		return failed;
	}

	/** Modülün kendine özel ayar ekranı varsa döndürür (yoksa null: standart ayar ekranı kullanılır). */
	public Screen createSettingsScreen(Screen parent) {
		return null;
	}

	public final List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	/** Dil dosyasındaki çeviri anahtarı: module.eazy.&lt;id&gt; */
	public final String translationKey() {
		return "module.eazy." + id;
	}
}
