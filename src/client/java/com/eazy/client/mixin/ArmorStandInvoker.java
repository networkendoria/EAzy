package com.eazy.client.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** ArmorStand.setMarker özel olduğu için buradan çağrılır (marker = vurulamaz, sadece yazı görünür). */
@Mixin(ArmorStand.class)
public interface ArmorStandInvoker {

	@Invoker("setMarker")
	void eazy$setMarker(boolean marker);
}
