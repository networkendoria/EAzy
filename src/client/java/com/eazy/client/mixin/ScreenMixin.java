package com.eazy.client.mixin;

import com.eazy.client.modules.UiModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Envanter ekranlarında oyun karartmasını ve bulanıklığı kaldırır (arka plan şeffaf görünür). */
@Mixin(Screen.class)
public abstract class ScreenMixin {

	@Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void eazy$noDim(GuiGraphics graphics, CallbackInfo ci) {
		if (UiModule.transparentInventory() && (Object) this instanceof AbstractContainerScreen<?>) {
			ci.cancel();
		}
	}

	@Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void eazy$noBlur(GuiGraphics graphics, CallbackInfo ci) {
		if (UiModule.transparentInventory() && (Object) this instanceof AbstractContainerScreen<?>) {
			ci.cancel();
		}
	}
}
