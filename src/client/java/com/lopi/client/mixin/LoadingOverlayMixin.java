package com.lopi.client.mixin;

import com.lopi.client.gui.LoadingScreenRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mojang yükleme ekranının üstüne Lopi yükleme ekranını çizer. Oyunun kendi çizimi (kararma/geçiş ve
 * ekranın kapanma mantığı) bozulmaz; sadece sonuna eklenir ve aynı geçiş değerleriyle görünür/kaybolur.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	@Shadow
	private float currentProgress;
	@Shadow
	private long fadeOutStart;
	@Shadow
	private long fadeInStart;
	@Shadow
	@Final
	private boolean fadeIn;

	@Unique
	private boolean lopi$delayed;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void lopi$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		long now = Util.getMillis();
		// Oyun yüklemeyi bitirince kapanışı 1,5 sn geciktir: Lopi ekranı biraz daha kalır, arkadaki Minecraft ekranı görünmez.
		if (fadeOutStart > 0L && !lopi$delayed) {
			lopi$delayed = true;
			fadeOutStart += 1500L;
			currentProgress = 1f;
		}
		float out = fadeOutStart > 0L ? (now - fadeOutStart) / 1000f : -1f;
		float in = fadeInStart > 0L ? (now - fadeInStart) / 500f : -1f;
		float alpha = 1f;
		if (out >= 1f) {
			alpha = 1f - Math.max(0f, Math.min(1f, out - 1f));
		} else if (fadeIn) {
			alpha = Math.max(0.15f, Math.min(1f, in));
		}
		try {
			LoadingScreenRenderer.render(graphics, alpha, currentProgress);
		} catch (RuntimeException e) {
			// Yükleme ekranı çizimi oyunu asla durdurmamalı.
		}
	}
}
