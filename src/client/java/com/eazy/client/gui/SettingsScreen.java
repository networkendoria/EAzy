package com.eazy.client.gui;

import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.modules.FpsProfileModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Basit ayar ekranı: modül listesi + aç/kapat düğmeleri + (ayarı olan modüller için) "..." düğmesi.
 * Kategorili asıl menü Aşama 5'te yapılacak.
 */
public class SettingsScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW = 24;
	private static final int TOP = 36;
	private static final int BOTTOM_RESERVED = 52;

	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("screen.eazy.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = TOP;

		for (EAzyModule module : ModuleManager.all()) {
			if (y + BUTTON_HEIGHT > this.height - BOTTOM_RESERVED) {
				break; // Ekrana sığmayanlar Aşama 5'teki kategorili menüde gösterilecek.
			}
			boolean hasSettings = module.settings().stream().anyMatch(s -> !s.isHidden())
					|| module.createSettingsScreen(this) != null;
			int toggleWidth = hasSettings ? 170 : 200;

			this.addRenderableWidget(Button.builder(label(module), btn -> {
				ModuleManager.toggle(module);
				btn.setMessage(label(module));
			}).bounds(x, y, toggleWidth, BUTTON_HEIGHT).build());

			if (hasSettings) {
				this.addRenderableWidget(Button.builder(Component.translatable("eazy.button.settings"),
								btn -> {
								net.minecraft.client.gui.screens.Screen custom = module.createSettingsScreen(this);
								this.minecraft.gui.setScreen(custom != null ? custom : new ModuleSettingsScreen(this, module));
							})
						.bounds(x + 174, y, 26, BUTTON_HEIGHT).build());
			}
			y += ROW;
		}

		this.addRenderableWidget(
				Button.builder(Component.translatable("eazy.button.done"), btn -> this.onClose())
						.bounds(x, this.height - 28, 200, BUTTON_HEIGHT)
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
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		String text = this.title.getString();
		graphics.text(this.font, text, (this.width - this.font.width(text)) / 2, 20, 0xFFFFFFFF, true);

		List<String> found = FpsProfileModule.detectedMods();
		if (!found.isEmpty()) {
			graphics.centeredText(this.font,
					Component.translatable("eazy.fps.detected", String.join(", ", found)),
					this.width / 2, this.height - 42, 0xFFAAAAAA);
		}
	}
}
