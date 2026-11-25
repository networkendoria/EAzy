package com.lopi.client.gui;

import com.lopi.client.core.LopiModule;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.modules.EasyPvpModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** EasyPVP ayarları: totem sayacı, parçacıklar, kozmetik seçimi. */
public class EasyPvpScreen extends Screen {
	private final Screen parent;
	private final EasyPvpModule module;

	public EasyPvpScreen(Screen parent, EasyPvpModule module) {
		super(Component.translatable("module.lopi.easypvp"));
		this.parent = parent;
		this.module = module;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = 40;
		this.addRenderableWidget(Button.builder(totemLabel(), btn -> {
			module.totemCounterSetting().toggle();
			ModuleManager.saveAsync();
			btn.setMessage(totemLabel());
		}).bounds(x, y, 200, 20).build());
		y += 24;
		this.addRenderableWidget(Button.builder(particleLabel(), btn -> {
			EasyPvpModule.toggleParticles();
			btn.setMessage(particleLabel());
		}).bounds(x, y, 200, 20).build());
		y += 24;
		this.addRenderableWidget(Button.builder(cosmeticLabel(), btn -> {
			String[] ids = EasyPvpModule.COSMETIC_IDS;
			int next = 0;
			for (int i = 0; i < ids.length; i++) {
				if (ids[i].equals(module.cosmetic())) {
					next = (i + 1) % ids.length;
				}
			}
			module.setCosmetic(ids[next]);
			btn.setMessage(cosmeticLabel());
		}).bounds(x, y, 200, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("lopi.button.done"), btn -> this.onClose())
				.bounds(x, this.height - 28, 200, 20).build());
	}

	private Component onOff(boolean on) {
		return Component.translatable(on ? "lopi.state.on" : "lopi.state.off");
	}

	private Component totemLabel() {
		return Component.translatable("setting.lopi.easypvp.totem_counter").append(": ")
				.append(onOff(module.totemCounterSetting().get()));
	}

	private Component particleLabel() {
		LopiModule m = EasyPvpModule.particlesModule();
		boolean particlesOn = m == null || !m.isEnabled();
		return Component.translatable("lopi.easypvp.particles").append(": ").append(onOff(particlesOn));
	}

	private Component cosmeticLabel() {
		Item item = EasyPvpModule.itemFor(module.cosmetic());
		Component value = item == null ? onOff(false) : new ItemStack(item).getHoverName();
		return Component.translatable("setting.lopi.easypvp.cosmetic").append(": ").append(value);
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
