package com.eazy.client.gui;

import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.core.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Bir modülün ayarlarını düzenler: Boolean -> aç/kapat düğmesi, sayı -> [-] değer [+].
 * Her değişiklik hemen kaydedilir.
 */
public class ModuleSettingsScreen extends Screen {
	private static final int ROW = 24;
	private static final int TOP = 40;

	private final Screen parent;
	private final EAzyModule module;

	public ModuleSettingsScreen(Screen parent, EAzyModule module) {
		super(Component.translatable(module.translationKey()));
		this.parent = parent;
		this.module = module;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = TOP;

		for (Setting<?> setting : module.settings()) {
			if (setting.isHidden()) {
				continue;
			}
			if (y + 20 > this.height - 36) {
				break;
			}
			if (setting.isBoolean()) {
				this.addRenderableWidget(Button.builder(boolLabel(setting), btn -> {
					setting.toggle();
					ModuleManager.saveAsync();
					btn.setMessage(boolLabel(setting));
				}).bounds(x, y, 200, 20).build());
			} else if (setting.isNumber()) {
				Button middle = Button.builder(numberLabel(setting), btn -> {
				}).bounds(x + 22, y, 156, 20).build();
				this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
					setting.setFromNumber(setting.asDouble() - setting.step());
					ModuleManager.saveAsync();
					middle.setMessage(numberLabel(setting));
				}).bounds(x, y, 20, 20).build());
				this.addRenderableWidget(middle);
				this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
					setting.setFromNumber(setting.asDouble() + setting.step());
					ModuleManager.saveAsync();
					middle.setMessage(numberLabel(setting));
				}).bounds(x + 180, y, 20, 20).build());
			}
			y += ROW;
		}

		this.addRenderableWidget(
				Button.builder(Component.translatable("eazy.button.done"), btn -> this.onClose())
						.bounds(x, this.height - 28, 200, 20).build());
	}

	private Component settingName(Setting<?> setting) {
		return Component.translatable("setting.eazy." + module.id() + "." + setting.key());
	}

	private Component boolLabel(Setting<?> setting) {
		boolean on = Boolean.TRUE.equals(setting.get());
		return settingName(setting).copy()
				.append(Component.literal(": "))
				.append(Component.translatable(on ? "eazy.state.on" : "eazy.state.off"));
	}

	private Component numberLabel(Setting<?> setting) {
		Object v = setting.get();
		String text = v instanceof Integer i ? Integer.toString(i)
				: String.format(Locale.ROOT, "%.1f", setting.asDouble());
		return settingName(setting).copy().append(Component.literal(": " + text));
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
	}
}
