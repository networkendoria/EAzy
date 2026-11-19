package com.eazy.client.gui;

import com.eazy.client.core.ModuleManager;
import com.eazy.client.core.Setting;
import com.eazy.client.modules.ShortcutsModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Kısayol slotlarının metinlerini düzenler. Tuşlar Kontroller ekranından atanır. */
public class ShortcutsScreen extends Screen {
	private static final int ROW = 24;
	private static final int TOP = 36;

	private final Screen parent;
	private final ShortcutsModule module;

	public ShortcutsScreen(Screen parent, ShortcutsModule module) {
		super(Component.translatable("screen.eazy.shortcuts"));
		this.parent = parent;
		this.module = module;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 90;
		int y = TOP;
		List<Setting<String>> slots = module.slotSettings();
		for (int i = 0; i < slots.size(); i++) {
			Setting<String> slot = slots.get(i);
			EditBox box = new EditBox(this.font, x, y, 200, 20, Component.translatable("key.eazy.shortcut." + (i + 1)));
			box.setMaxLength(256);
			box.setValue(slot.get());
			box.setResponder(slot::set);
			this.addRenderableWidget(box);
			y += ROW;
		}
		this.addRenderableWidget(
				Button.builder(Component.translatable("eazy.button.done"), btn -> this.onClose())
						.bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
	}

	@Override
	public void onClose() {
		ModuleManager.saveAsync();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		String text = this.title.getString();
		graphics.text(this.font, text, (this.width - this.font.width(text)) / 2, 14, 0xFFFFFFFF, true);
		int y = TOP + 6;
		for (int i = 1; i <= ShortcutsModule.SLOTS; i++) {
			graphics.text(this.font, Integer.toString(i), this.width / 2 - 104, y, 0xFFAAAAAA, true);
			y += ROW;
		}
		graphics.centeredText(this.font, Component.translatable("eazy.shortcuts.hint"),
				this.width / 2, this.height - 42, 0xFFAAAAAA);
	}
}
