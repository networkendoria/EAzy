package com.eazy.client.gui;

import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Aşama 1 için basit ayar ekranı: modül listesi + aç/kapat düğmeleri.
 * Kategorili asıl menü Aşama 5'te yapılacak.
 */
public class SettingsScreen extends Screen {
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int TOP = 40;
	private static final int BOTTOM_RESERVED = 36;

	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("screen.eazy.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - BUTTON_WIDTH / 2;
		int y = TOP;

		for (EAzyModule module : ModuleManager.all()) {
			if (y + BUTTON_HEIGHT > this.height - BOTTOM_RESERVED) {
				break; // Ekrana sığmayanlar Aşama 5'teki kategorili menüde gösterilecek.
			}
			Button button = Button.builder(label(module), btn -> {
				ModuleManager.toggle(module);
				btn.setMessage(label(module));
			}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
			this.addRenderableWidget(button);
			y += BUTTON_HEIGHT + GAP;
		}

		this.addRenderableWidget(
				Button.builder(Component.translatable("eazy.button.done"), btn -> this.onClose())
						.bounds(x, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT)
						.build());
	}

	private static Component label(EAzyModule module) {
		String stateKey = module.isFailed() ? "eazy.state.error"
				: module.isEnabled() ? "eazy.state.on" : "eazy.state.off";
		return Component.translatable(module.translationKey())
				.append(Component.literal(": "))
				.append(Component.translatable(stateKey));
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		String text = this.title.getString();
		graphics.text(this.font, text, (this.width - this.font.width(text)) / 2, 20, 0xFFFFFFFF, true);
	}
}
