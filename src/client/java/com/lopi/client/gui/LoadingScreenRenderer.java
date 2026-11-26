package com.lopi.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/**
 * Lopi yükleme ekranı: Loki logosu (nefes alan ışıma), ilerleme çubuğu (kayan parıltı), yüzde ve "YÜKLENİYOR" yazısı.
 * Sadece dolu kareler çizer; doku ve oyunun yazı tipi gerekmez, o yüzden oyun açılırken güvenle çalışır.
 */
public final class LoadingScreenRenderer {
	private static final int BG = 0x0E0818;
	private static final int ACCENT = 0x8957FF;
	private static final int LAVENDER = 0xB69AFF;
	private static final int TRACK = 0x2A2340;

	private LoadingScreenRenderer() {
	}

	private static int argb(int rgb, float a) {
		int alpha = Math.max(0, Math.min(255, Math.round(a * 255f)));
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	private static int mixRgb(int c1, int c2, float t) {
		int r = Math.round(((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
		int g = Math.round(((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
		int b = Math.round((c1 & 255) * (1 - t) + (c2 & 255) * t);
		return (r << 16) | (g << 8) | b;
	}

	/**
	 * @param alpha    genel görünürlük (0..1), açılış/kapanış geçişi için
	 * @param progress yükleme ilerlemesi (0..1)
	 */
	public static void render(GuiGraphicsExtractor g, float alpha, float progress) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		float time = (Util.getMillis() % 100000L) / 1000f;

		g.nextStratum();
		g.fill(0, 0, w, h, argb(BG, alpha));

		int cell = Math.max(2, Math.min(h * 28 / 100, 120) / LogoPixels.SIZE);
		int size = cell * LogoPixels.SIZE;
		int lx = (w - size) / 2;
		int bob = Math.round((float) Math.sin(time * 2.2f) * cell * 0.6f);
		int ly = h / 2 - size / 2 - cell * 4 + bob;

		// Nefes alan ışıma (logonun arkasında genişleyen yumuşak çerçeveler)
		float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
		for (int i = 4; i >= 1; i--) {
			int pad = cell * (i * 2 + 1);
			float a = alpha * (0.05f + 0.035f * pulse) * (5 - i);
			g.fill(lx - pad, ly - pad, lx + size + pad, ly + size + pad, argb(ACCENT, a));
		}

		for (int y = 0; y < LogoPixels.SIZE; y++) {
			int x = 0;
			while (x < LogoPixels.SIZE) {
				int c = LogoPixels.DATA[y * LogoPixels.SIZE + x];
				if (c == 0) {
					x++;
					continue;
				}
				int run = 1;
				while (x + run < LogoPixels.SIZE && LogoPixels.DATA[y * LogoPixels.SIZE + x + run] == c) {
					run++;
				}
				g.fill(lx + x * cell, ly + y * cell, lx + (x + run) * cell, ly + (y + 1) * cell, argb(c, alpha));
				x += run;
			}
		}

		// Başlık: LOPI CLIENT
		int tc = Math.max(1, cell / 2);
		String title = "LOPI CLIENT";
		int ty = ly + size + cell * 4 + Math.abs(bob) * 0;
		PixelFont.draw(g, title, (w - PixelFont.width(title, tc)) / 2, ty, tc, argb(0xFFFFFF, alpha));

		// İlerleme çubuğu
		int barW = Math.min(w * 55 / 100, 260);
		int barH = Math.max(6, cell * 2);
		int bx = (w - barW) / 2;
		int by = ty + 7 * tc + cell * 4;
		float p = Math.max(0f, Math.min(1f, progress));
		g.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, argb(0x3B2F63, alpha));
		g.fill(bx, by, bx + barW, by + barH, argb(TRACK, alpha));
		int fillW = Math.round(barW * p);
		if (fillW > 0) {
			g.fill(bx, by, bx + fillW, by + barH, argb(mixRgb(ACCENT, LAVENDER, 0.35f), alpha));
			// Kayan parıltı
			int shineW = Math.max(10, barW / 7);
			float sweep = (time * 0.7f) % 1.6f;
			int sx = bx + Math.round((sweep - 0.3f) / 1.0f * barW);
			int x1 = Math.max(bx, sx);
			int x2 = Math.min(bx + fillW, sx + shineW);
			if (x2 > x1) {
				g.fill(x1, by, x2, by + barH, argb(0xFFFFFF, alpha * 0.35f));
			}
			g.fill(bx, by, bx + fillW, by + 1, argb(0xFFFFFF, alpha * 0.25f));
		}

		// Yüzde ve durum yazısı
		int pc = Math.max(1, cell / 2);
		String percent = Math.round(p * 100f) + "%";
		PixelFont.draw(g, percent, (w - PixelFont.width(percent, pc)) / 2, by + barH + pc * 3, pc, argb(0xFFFFFF, alpha));
		int dots = (int) (time * 2.5f) % 4;
		String status = "YÜKLENİYOR" + ".".repeat(dots);
		int sc = Math.max(1, pc - 1);
		int sw = PixelFont.width("YÜKLENİYOR...", sc);
		PixelFont.draw(g, status, (w - sw) / 2, by + barH + pc * 3 + 7 * pc + pc * 3, sc, argb(LAVENDER, alpha * 0.85f));
	}
}
