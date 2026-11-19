package com.eazy.client.modules;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.Setting;
import com.eazy.client.gui.ShortcutsScreen;
import com.eazy.client.input.EAzyKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Komut / yazı kısayolları: her slot bir tuşa atanır (Kontroller > EAzy), metni ayar ekranından yazılır.
 * "/" ile başlayan metin komut olarak, diğerleri sohbet mesajı olarak gönderilir.
 */
public final class ShortcutsModule extends EAzyModule {
	public static final int SLOTS = 6;

	private final List<KeyMapping> keys = new ArrayList<>();
	private final List<Setting<String>> texts = new ArrayList<>();

	public ShortcutsModule() {
		super("shortcuts", Category.SHORTCUTS, true);
		for (int i = 1; i <= SLOTS; i++) {
			keys.add(EAzyKeys.register("key.eazy.shortcut." + i, GLFW.GLFW_KEY_UNKNOWN));
			texts.add(setting("slot" + i, String.class, "").hidden());
		}
	}

	public List<Setting<String>> slotSettings() {
		return texts;
	}

	@Override
	public Screen createSettingsScreen(Screen parent) {
		return new ShortcutsScreen(parent, this);
	}

	@Override
	protected void onEnable() {
		for (KeyMapping key : keys) {
			while (key.consumeClick()) {
				// Modül kapalıyken biriken tıklamaları at.
			}
		}
	}

	@Override
	protected void onTick(Minecraft client) {
		for (int i = 0; i < SLOTS; i++) {
			KeyMapping key = keys.get(i);
			while (key.consumeClick()) {
				send(client, texts.get(i).get());
			}
		}
	}

	private static void send(Minecraft client, String raw) {
		if (client.player == null || raw == null) {
			return;
		}
		String text = raw.strip();
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
}
