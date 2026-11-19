package com.eazy.client.mixin;

import com.eazy.client.modules.AutoGgModule;
import com.eazy.client.modules.ItemOutlineModule;
import com.eazy.client.modules.UiModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar: slot çerçevesi (eşyanın altına) ve slot numaraları. */
@Mixin(Hud.class)
public abstract class HudMixin {

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void eazy$outline(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker tracker,
							  Player player, ItemStack stack, int seed, CallbackInfo ci) {
		if (!stack.isEmpty() && ItemOutlineModule.hotbarEnabled()) {
			graphics.outline(x - 1, y - 1, 18, 18, ItemOutlineModule.colorFor(stack));
		}
	}

	@Inject(method = "extractItemHotbar", at = @At("TAIL"))
	private void eazy$numbers(GuiGraphicsExtractor graphics, DeltaTracker tracker, CallbackInfo ci) {
		AutoGgModule.renderTargetTotems(graphics);
		if (!UiModule.hotbarNumbers()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int baseX = graphics.guiWidth() / 2 - 88;
		int y = graphics.guiHeight() - 19;
		for (int i = 0; i < 9; i++) {
			graphics.text(mc.font, Integer.toString(i + 1), baseX + i * 20 + 1, y, 0xFFE0E0E0, true);
		}
	}
}
