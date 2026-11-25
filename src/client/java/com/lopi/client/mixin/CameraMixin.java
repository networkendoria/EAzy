package com.lopi.client.mixin;

import com.lopi.client.modules.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: dünya görüş açısını (FOV) zoom çarpanıyla değiştirir. El FOV'una dokunmaz. */
@Mixin(Camera.class)
public abstract class CameraMixin {

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void lopi$zoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ZoomModule.applyFov(cir.getReturnValue()));
	}
}
