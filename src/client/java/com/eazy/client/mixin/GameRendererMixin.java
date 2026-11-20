package com.eazy.client.mixin;

import com.eazy.client.modules.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: dünya görüş açısını (FOV) zoom çarpanıyla değiştirir. El FOV'una (useFovSetting=false) dokunmaz. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void eazy$zoomFov(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		if (useFovSetting) {
			cir.setReturnValue(ZoomModule.applyFov(cir.getReturnValue()));
		}
	}
}
