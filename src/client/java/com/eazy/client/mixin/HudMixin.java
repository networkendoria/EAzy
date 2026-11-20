package com.eazy.client.mixin;

import com.eazy.client.modules.AutoGgModule;
import com.eazy.client.modules.EasyPvpModule;
import com.eazy.client.modules.ItemOutlineModule;
import com.eazy.client.modules.UiModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar: slot çerçevesi (eşyanın altına) ve slot numaraları. */
@Mixin(Gui.class)
public abstract class HudMixin {

	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void eazy$outline(GuiGraphics graphics, int x, int y, DeltaTracker tracker,
							  Player player, ItemStack stack, int seed, CallbackInfo ci) {
		if (!stack.isEmpty() && ItemOutlineModule.hotbarEnabled()) {
			graphics.renderOutline(x - 1, y - 1, 18, 18, ItemOutlineModule.colorFor(stack));
		}
	}

	@Inject(method = "renderItemHotbar", at = @At("TAIL"))
	private void eazy$numbers(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		AutoGgModule.renderTargetTotems(graphics);
		EasyPvpModule.renderTotemCounter(graphics);
		if (!UiModule.hotbarNumbers()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int baseX = graphics.guiWidth() / 2 - 88;
		int y = graphics.guiHeight() - 19;
		for (int i = 0; i < 9; i++) {
			graphics.drawString(mc.font, Integer.toString(i + 1), baseX + i * 20 + 1, y, 0xFFE0E0E0, true);
		}
	}
}
