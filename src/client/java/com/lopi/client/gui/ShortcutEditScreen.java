package com.lopi.client.gui;

import com.lopi.client.modules.ShortcutsModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Kısayol ekler veya düzenler: ad, komut/mesaj ve tuş. Tüm metinler Türkçe. */
public class ShortcutEditScreen extends Screen {
	private static final int BG = 0xFF0B0A10;
	private static final int BORDER = 0xFF2B2542;
	private static final int SURFACE = 0xFF15131D;
	private static final int LAVENDER = 0xFFB69AFF;
	private static final int MUTED = 0xFF8F8A9E;

	private final Screen parent;
	private final ShortcutsModule.Entry existing;
	private int key;
	private boolean listening;

	private EditBox nameBox;
	private EditBox commandBox;
	private Button keyButton;

	/** entry == null ise yeni kısayol oluşturur. */
	public ShortcutEditScreen(Screen parent, ShortcutsModule.Entry entry) {
		super(Component.translatable(entry == null ? "lopi.shortcuts.add" : "lopi.shortcuts.edit"));
		this.parent = parent;
		this.existing = entry;
		this.key = entry == null ? 0 : entry.key;
	}

	@Override
	protected void init() {
		int w = Math.min(this.width - 24, 280);
		int x = (this.width - w) / 2;
		int y = this.height / 2 - 72;

		nameBox = new EditBox(this.font, x + 8, y + 38, w - 16, 18, Component.translatable("lopi.shortcuts.name"));
		nameBox.setMaxLength(24);
		nameBox.setHint(Component.translatable("lopi.shortcuts.name_hint"));
		nameBox.setValue(existing == null ? "" : existing.name);
		this.addRenderableWidget(nameBox);

		commandBox = new EditBox(this.font, x + 8, y + 76, w - 16, 18, Component.translatable("lopi.shortcuts.command"));
		commandBox.setMaxLength(256);
		commandBox.setHint(Component.translatable("lopi.shortcuts.command_hint"));
		commandBox.setValue(existing == null ? "" : existing.command);
		this.addRenderableWidget(commandBox);

		keyButton = Button.builder(keyLabel(), b -> {
			listening = true;
			b.setMessage(keyLabel());
		}).bounds(x + 8, y + 112, w - 16, 20).build();
		this.addRenderableWidget(keyButton);

		int bw = (w - 16 - 8) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("lopi.shortcuts.save"), b -> save())
				.bounds(x + 8, y + 140, bw, 20).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable(existing == null ? "lopi.shortcuts.cancel" : "lopi.shortcuts.delete"),
				b -> {
					if (existing != null) {
						ShortcutsModule sc = ShortcutsModule.get();
						sc.entries().remove(existing);
						sc.commit();
					}
					this.onClose();
				}).bounds(x + 8 + bw + 8, y + 140, bw, 20).build());
		this.setInitialFocus(nameBox);
	}

	private Component keyLabel() {
		if (listening) {
			return Component.translatable("lopi.gui.press_key");
		}
		String name = key > 0 ? InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString()
				: Component.translatable("lopi.gui.unbound").getString();
		return Component.translatable("lopi.shortcuts.key", name);
	}

	private void save() {
		ShortcutsModule sc = ShortcutsModule.get();
		String name = nameBox.getValue().strip();
		String command = commandBox.getValue().strip();
		if (sc != null && !(name.isEmpty() && command.isEmpty())) {
			ShortcutsModule.Entry e = existing != null ? existing : new ShortcutsModule.Entry();
			e.name = name.isEmpty() ? command : name;
			e.command = command;
			e.key = key;
			if (existing == null) {
				sc.entries().add(e);
			}
			sc.commit();
		}
		this.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening) {
			int k = event.key();
			if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
				key = 0;
			} else if (k != GLFW.GLFW_KEY_ESCAPE) {
				key = k;
			}
			listening = false;
			keyButton.setMessage(keyLabel());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		this.extractTransparentBackground(g);
		int w = Math.min(this.width - 24, 280);
		int x = (this.width - w) / 2;
		int y = this.height / 2 - 72;
		g.fill(x - 1, y - 1, x + w + 1, y + 170, BORDER);
		g.fill(x, y, x + w, y + 169, BG);
		g.fill(x, y, x + w, y + 24, SURFACE);
		g.text(this.font, this.title.getString(), x + 8, y + 8, 0xFFFFFFFF, false);
		g.text(this.font, Component.translatable("lopi.shortcuts.name").getString(), x + 8, y + 28, LAVENDER, false);
		g.text(this.font, Component.translatable("lopi.shortcuts.command").getString(), x + 8, y + 66, LAVENDER, false);
		g.text(this.font, Component.translatable("lopi.shortcuts.hint").getString(), x + 8, y + 98, MUTED, false);
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
