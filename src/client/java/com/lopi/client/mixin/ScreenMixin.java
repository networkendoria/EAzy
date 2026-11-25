package com.lopi.client.mixin;

import com.lopi.client.modules.UiModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Envanter ekranlarında oyun karartmasını ve bulanıklığı kaldırır (arka plan şeffaf görünür). */
@Mixin(Screen.class)
public abstract class ScreenMixin {

	@Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void lopi$noDim(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (UiModule.transparentInventory() && (Object) this instanceof AbstractContainerScreen<?>) {
			ci.cancel();
		}
	}

	@Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void lopi$noBlur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (UiModule.transparentInventory() && (Object) this instanceof AbstractContainerScreen<?>) {
			ci.cancel();
		}
	}
}
