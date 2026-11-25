package com.lopi.client.modules;

import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Özelleştirilebilir artı imleç. Açıkken oyunun kendi imleci yerine bu çizilir (HudMixin).
 * Stil, boyut, boşluk, kalınlık, renk, opaklık ve kenarlık menüden ayarlanır.
 */
public final class CrosshairModule extends LopiModule {
	public static final int STYLE_PLUS = 0;
	public static final int STYLE_DOT = 1;
	public static final int STYLE_PLUS_DOT = 2;
	public static final int STYLE_CROSS = 3;

	private static CrosshairModule instance;

	private final Setting<Integer> style = setting("style", Integer.class, STYLE_PLUS).range(0, 3, 1);
	private final Setting<Integer> size = setting("size", Integer.class, 5).range(1, 12, 1);
	private final Setting<Integer> gap = setting("gap", Integer.class, 2).range(0, 8, 1);
	private final Setting<Integer> thickness = setting("thickness", Integer.class, 1).range(1, 4, 1);
	private final Setting<Integer> hue = setting("hue", Integer.class, 270).range(0, 359, 5);
	private final Setting<Integer> saturation = setting("saturation", Integer.class, 0).range(0, 100, 5);
	private final Setting<Integer> opacity = setting("opacity", Integer.class, 100).range(20, 100, 5);
	private final Setting<Boolean> outline = setting("outline", Boolean.class, true);

	public CrosshairModule() {
		super("crosshair", Category.INTERFACE, false);
		instance = this;
	}

	public static CrosshairModule get() {
		return instance;
	}

	/** HudMixin için: true ise oyunun kendi imleci çizilmez. */
	public static boolean replacesVanilla() {
		return instance != null && instance.isEnabled();
	}

	/** HudMixin tarafından çağrılır; imleci ekranın ortasına çizer. */
	public static void renderHud(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (instance == null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		instance.draw(g, g.guiWidth() / 2, g.guiHeight() / 2);
	}

	/** Verilen merkeze imleci çizer (menü önizlemesi de bunu kullanır). */
	public void draw(GuiGraphicsExtractor g, int cx, int cy) {
		int a = Math.round(255 * opacity.get() / 100f);
		int color = (a << 24) | (java.awt.Color.HSBtoRGB(hue.get() / 360f, saturation.get() / 100f, 1f) & 0xFFFFFF);
		int t = thickness.get();
		int s = size.get();
		int gp = gap.get();
		int half = t / 2;
		int st = style.get();
		boolean ol = outline.get();
		int olc = (a << 24);

		if (st == STYLE_PLUS || st == STYLE_PLUS_DOT) {
			bar(g, cx - gp - s, cy - half, s, t, color, ol, olc);
			bar(g, cx + gp, cy - half, s, t, color, ol, olc);
			bar(g, cx - half, cy - gp - s, t, s, color, ol, olc);
			bar(g, cx - half, cy + gp, t, s, color, ol, olc);
		}
		if (st == STYLE_DOT || st == STYLE_PLUS_DOT) {
			bar(g, cx - half, cy - half, t, t, color, ol, olc);
		}
		if (st == STYLE_CROSS) {
			for (int i = gp; i < gp + s; i++) {
				for (int sx = -1; sx <= 1; sx += 2) {
					for (int sy = -1; sy <= 1; sy += 2) {
						bar(g, cx + sx * i - (sx < 0 ? t - 1 : 0), cy + sy * i - (sy < 0 ? t - 1 : 0), t, t, color, false, olc);
					}
				}
			}
		}
	}

	private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, boolean outline, int outlineColor) {
		if (outline) {
			g.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor);
		}
		g.fill(x, y, x + w, y + h, color);
	}
}
