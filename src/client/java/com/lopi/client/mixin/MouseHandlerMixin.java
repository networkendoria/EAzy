package com.lopi.client.mixin;

import com.lopi.client.modules.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Zoom sırasında fare hassasiyetini düşürür (turnPlayer içindeki tek 8.0 çarpanı). */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	@ModifyConstant(method = "turnPlayer", constant = @Constant(doubleValue = 8.0))
	private double lopi$zoomSensitivity(double original) {
		return original * ZoomModule.sensitivityFactor();
	}
}
