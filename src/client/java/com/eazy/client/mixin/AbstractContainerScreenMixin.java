package com.eazy.client.mixin;

import com.eazy.client.modules.ItemOutlineModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Envanter/sandık slotlarında eşya çerçevesi. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void eazy$outline(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (ItemOutlineModule.inventoryEnabled() && slot.isActive() && slot.hasItem()) {
			graphics.outline(slot.x - 1, slot.y - 1, 18, 18, ItemOutlineModule.colorFor(slot.getItem()));
		}
	}
}
