package com.eazy.client.gui;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.core.Setting;
import com.eazy.client.modules.FpsProfileModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EAzy ClickGUI: koyu, minimal; başlık (logo, sürüm, arama, kapat), kategori kenar çubuğu,
 * modül kartları ve kartların içinde açılan ayarlar (anahtar, kaydırıcı, tuş atama).
 * Tüm metinler Türkçe dil dosyasından gelir. Fare ve dokunmatik (sürükleyerek kaydırma) desteklenir.
 */
public class ClickGuiScreen extends Screen {
	// Renk paleti (ARGB)
	private static final int BG = 0xFF0B0A10;
	private static final int SURFACE = 0xFF15131D;
	private static final int CARD = 0xFF1C1926;
	private static final int CARD_HOVER = 0xFF242034;
	private static final int ACCENT = 0xFF8957FF;
	private static final int LAVENDER = 0xFFB69AFF;
	private static final int BORDER = 0xFF2B2542;
	private static final int TRACK_OFF = 0xFF3A3550;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFFC9C5D6;
	private static final int MUTED = 0xFF8F8A9E;
	private static final int DANGER = 0xFFFF5C7A;

	private static final int HEADER_H = 30;
	private static final int CARD_HEAD_H = 32;
	private static final int ROW_H = 20;
	private static final int SLIDER_ROW_H = 28;
	private static final int GAP = 6;

	// Tıklanabilir alan türleri
	private static final int H_CLOSE = 0;
	private static final int H_CATEGORY = 1;
	private static final int H_TOGGLE = 2;
	private static final int H_EXPAND = 3;
	private static final int H_BOOL = 4;
	private static final int H_SLIDER = 5;
	private static final int H_KEY = 6;
	private static final int H_ADVANCED = 7;

	private static final class Hit {
		int x1;
		int y1;
		int x2;
		int y2;
		int type;
		Object a;
		Object b;

		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	// Oturum boyunca hatırlanan durum
	private static Category selected = Category.PERFORMANCE;
	private static String query = "";

	private final Screen parent;
	private final String version;

	// Düzen
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int sideW;
	private int cx;
	private int cy;
	private int cw;
	private int ch;

	// Durum
	private EditBox search;
	private final List<Category> categories = new ArrayList<>();
	private final List<EAzyModule> visible = new ArrayList<>();
	private boolean dirty = true;
	private final Map<EAzyModule, Boolean> expanded = new IdentityHashMap<>();
	private final Map<EAzyModule, Boolean> hasCustom = new IdentityHashMap<>();
	private final Map<EAzyModule, Integer> bodyHeight = new IdentityHashMap<>();
	private final Map<String, String> texts = new HashMap<>();

	// Animasyon (hedefe doğru ilerleyen değerler)
	private final Map<Object, float[]> anim = new IdentityHashMap<>();
	private float openAnim;
	private float catFade;
	private float scroll;
	private float scrollTarget;
	private float contentHeight;
	private long lastNanos;

	// Etkileşim
	private final List<Hit> hits = new ArrayList<>();
	private int hitCount;
	private Hit pressed;
	private double pressY;
	private boolean dragScrolled;
	private Hit sliderHit;
	private KeyMapping listening;

	public ClickGuiScreen(Screen parent) {
		super(Component.translatable("screen.eazy.settings"));
		this.parent = parent;
		this.version = FabricLoader.getInstance().getModContainer("eazy")
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}

	// ---------------------------------------------------------------- yardımcılar

	private String t(String key) {
		return texts.computeIfAbsent(key, k -> Component.translatable(k).getString());
	}

	private static int alpha(int argb, float a) {
		int na = (int) ((argb >>> 24) * Math.max(0f, Math.min(1f, a)));
		return (na << 24) | (argb & 0xFFFFFF);
	}

	private static int mix(int c1, int c2, float t) {
		int a = (int) (((c1 >>> 24) & 255) + ((((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t));
		int r = (int) (((c1 >> 16) & 255) + ((((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t));
		int g = (int) (((c1 >> 8) & 255) + ((((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t));
		int b = (int) ((c1 & 255) + (((c2 & 255) - (c1 & 255)) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** Değeri hedefe doğru saniyede `speed` hızıyla ilerletir. */
	private float approach(Object key, float target, float speed, float dt) {
		float[] v = anim.computeIfAbsent(key, k -> new float[]{target});
		float d = target - v[0];
		float step = speed * dt;
		v[0] = Math.abs(d) <= step ? target : v[0] + Math.signum(d) * step;
		return v[0];
	}

	private String fit(String s, int maxW) {
		if (this.font.width(s) <= maxW) {
			return s;
		}
		return this.font.plainSubstrByWidth(s, Math.max(0, maxW - this.font.width(".."))) + "..";
	}

	private Hit hit(int x1, int y1, int x2, int y2, int type, Object a, Object b) {
		Hit h;
		if (hitCount < hits.size()) {
			h = hits.get(hitCount);
		} else {
			h = new Hit();
			hits.add(h);
		}
		hitCount++;
		h.x1 = x1;
		h.y1 = y1;
		h.x2 = x2;
		h.y2 = y2;
		h.type = type;
		h.a = a;
		h.b = b;
		return h;
	}

	private boolean inContent(int y1, int y2) {
		return y2 > cy && y1 < cy + ch;
	}

	// ---------------------------------------------------------------- başlatma

	@Override
	protected void init() {
		pw = Math.min(this.width - 12, 440);
		ph = Math.min(this.height - 12, 270);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		sideW = pw >= 300 ? 92 : 72;
		cx = px + sideW;
		cy = py + HEADER_H;
		cw = pw - sideW;
		ch = ph - HEADER_H;

		categories.clear();
		for (Category c : Category.values()) {
			for (EAzyModule m : ModuleManager.all()) {
				if (m.category() == c) {
					categories.add(c);
					break;
				}
			}
		}
		if (!categories.contains(selected) && !categories.isEmpty()) {
			selected = categories.get(0);
		}

		for (EAzyModule m : ModuleManager.all()) {
			boolean custom = m.createSettingsScreen(this) != null;
			hasCustom.put(m, custom);
			int h = 0;
			for (Setting<?> s : m.settings()) {
				if (!s.isHidden() && s.isBoolean()) {
					h += ROW_H;
				} else if (!s.isHidden() && s.isNumber()) {
					h += SLIDER_ROW_H;
				}
			}
			h += m.keyBindings().size() * ROW_H;
			if (custom) {
				h += ROW_H;
			}
			bodyHeight.put(m, h > 0 ? h + 8 : 0);
		}

		int sw = Math.min(140, pw / 3);
		int sx = px + pw - 26 - sw;
		search = new EditBox(this.font, sx + 6, py + 11, sw - 12, 10, Component.translatable("eazy.gui.search"));
		search.setBordered(false);
		search.setMaxLength(32);
		search.setHint(Component.translatable("eazy.gui.search"));
		search.setValue(query);
		search.setResponder(text -> {
			query = text;
			dirty = true;
			scrollTarget = 0;
		});
		this.addRenderableWidget(search);

		openAnim = 0f;
		catFade = 0f;
		lastNanos = System.nanoTime();
		dirty = true;
	}

	private void rebuildVisible() {
		visible.clear();
		String q = query.toLowerCase(Locale.ROOT).trim();
		for (EAzyModule m : ModuleManager.all()) {
			if (q.isEmpty()) {
				if (m.category() == selected) {
					visible.add(m);
				}
			} else {
				String name = t(m.translationKey()).toLowerCase(Locale.ROOT);
				String desc = t(m.translationKey() + ".desc").toLowerCase(Locale.ROOT);
				if (name.contains(q) || desc.contains(q)) {
					visible.add(m);
				}
			}
		}
		dirty = false;
	}

	// ---------------------------------------------------------------- çizim

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Oyunun üstüne hafif karartma; bulanıklık yok (performans).
		g.fill(0, 0, this.width, this.height, alpha(0xFF000000, 0.45f * openAnim));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float dt = Math.min((now - lastNanos) / 1_000_000_000f, 0.05f);
		lastNanos = now;
		openAnim = Math.min(1f, openAnim + dt * 6f);
		catFade = Math.min(1f, catFade + dt * 7f);
		if (dirty) {
			rebuildVisible();
		}
		hitCount = 0;

		this.extractBackground(g, mouseX, mouseY, delta);
		float ga = openAnim;
		int slide = Math.round((1f - openAnim) * 8f);
		int oy = py + slide;

		// Ana panel
		g.fill(px - 1, oy - 1, px + pw + 1, oy + ph + 1, alpha(BORDER, ga));
		g.fill(px, oy, px + pw, oy + ph, alpha(BG, ga));

		drawHeader(g, mouseX, mouseY, ga, oy, dt);
		drawSidebar(g, mouseX, mouseY, ga, oy, dt);
		drawContent(g, mouseX, mouseY, ga, oy, dt);

		search.extractRenderState(g, mouseX, mouseY, delta);
	}

	private void drawHeader(GuiGraphicsExtractor g, int mx, int my, float ga, int oy, float dt) {
		g.fill(px, oy, px + pw, oy + HEADER_H, alpha(SURFACE, ga));
		g.fill(px, oy + HEADER_H - 1, px + pw, oy + HEADER_H, alpha(BORDER, ga));
		// Logo
		g.fill(px + 8, oy + 7, px + 24, oy + 23, alpha(ACCENT, ga));
		g.fill(px + 8, oy + 7, px + 24, oy + 8, alpha(LAVENDER, ga * 0.6f));
		g.centeredText(this.font, Component.literal("E").withStyle(ChatFormatting.BOLD), px + 16, oy + 11, alpha(TEXT, ga));
		g.text(this.font, Component.literal("EAzy").withStyle(ChatFormatting.BOLD), px + 30, oy + 6, alpha(TEXT, ga), false);
		int vx = px + 30 + this.font.width("EAzy") + 4;
		g.text(this.font, "v" + version, vx, oy + 6, alpha(MUTED, ga), false);
		// Etkin durum göstergesi
		int active = 0;
		for (EAzyModule m : ModuleManager.all()) {
			if (m.isEnabled()) {
				active++;
			}
		}
		g.fill(px + 30, oy + 19, px + 34, oy + 23, alpha(ACCENT, ga));
		g.text(this.font, String.format(t("eazy.gui.active_count"), active), px + 38, oy + 17, alpha(LAVENDER, ga), false);

		// Arama kutusu zemini
		int sw = Math.min(140, pw / 3);
		int sx = px + pw - 26 - sw;
		boolean focus = search.isFocused();
		float f = approach("search", focus ? 1f : 0f, 8f, dt);
		g.fill(sx - 1, oy + 7, sx + sw + 1, oy + 25, alpha(mix(BORDER, ACCENT, f), ga));
		g.fill(sx, oy + 8, sx + sw, oy + 24, alpha(BG, ga));
		search.setY(oy + 11);

		// Kapat düğmesi
		int bx = px + pw - 22;
		boolean over = mx >= bx && mx < bx + 16 && my >= oy + 7 && my < oy + 23;
		float hv = approach("close", over ? 1f : 0f, 10f, dt);
		g.fill(bx, oy + 7, bx + 16, oy + 23, alpha(mix(CARD, DANGER, hv * 0.8f), ga));
		g.centeredText(this.font, "×", bx + 8, oy + 11, alpha(TEXT, ga));
		hit(bx, oy + 7, bx + 16, oy + 23, H_CLOSE, null, null);
	}

	private void drawSidebar(GuiGraphicsExtractor g, int mx, int my, float ga, int oy, float dt) {
		int top = oy + HEADER_H;
		g.fill(px, top, px + sideW, oy + ph, alpha(SURFACE, ga));
		g.fill(px + sideW - 1, top, px + sideW, oy + ph, alpha(BORDER, ga));
		int y = top + 8;
		boolean searching = !query.isBlank();
		for (Category c : categories) {
			boolean sel = c == selected && !searching;
			boolean over = mx >= px + 4 && mx < px + sideW - 5 && my >= y && my < y + 22;
			float s = approach(c, sel ? 1f : 0f, 9f, dt);
			float hv = approach(new ExKey(c), over ? 1f : 0f, 10f, dt);
			int bg = mix(SURFACE, CARD, Math.max(s, hv * 0.7f));
			g.fill(px + 5, y, px + sideW - 6, y + 22, alpha(bg, ga));
			g.fill(px + 5, y, px + 5 + Math.round(2 * s), y + 22, alpha(ACCENT, ga));
			String name = t("category.eazy." + c.name().toLowerCase(Locale.ROOT));
			g.text(this.font, fit(name, sideW - 36), px + 12, y + 7, alpha(mix(MUTED, TEXT, Math.max(s, hv)), ga), false);
			int total = 0;
			int on = 0;
			for (EAzyModule m : ModuleManager.all()) {
				if (m.category() == c) {
					total++;
					if (m.isEnabled()) {
						on++;
					}
				}
			}
			String cnt = on + "/" + total;
			g.text(this.font, cnt, px + sideW - 9 - this.font.width(cnt), y + 7, alpha(s > 0.5f ? LAVENDER : MUTED, ga), false);
			hit(px + 4, y, px + sideW - 5, y + 22, H_CATEGORY, c, null);
			y += 25;
		}
		// Alt bilgi: algılanan FPS modları
		List<String> found = FpsProfileModule.detectedMods();
		if (!found.isEmpty()) {
			String line = fit(String.format(t("eazy.fps.detected"), String.join(", ", found)), sideW - 12);
			g.text(this.font, line, px + 7, oy + ph - 13, alpha(MUTED, ga), false);
		}
	}

	private void drawContent(GuiGraphicsExtractor g, int mx, int my, float ga, int oy, float dt) {
		int top = oy + HEADER_H;
		int h = ch;
		// Kaydırma sınırı ve yumuşatma
		float max = Math.max(0f, contentHeight - h + 8f);
		scrollTarget = Math.max(0f, Math.min(max, scrollTarget));
		scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
		if (Math.abs(scrollTarget - scroll) < 0.2f) {
			scroll = scrollTarget;
		}

		float fade = ga * catFade;
		int fy = Math.round((1f - catFade) * 6f);
		int cardX = cx + 8;
		int cardW = cw - 16;

		g.enableScissor(cx, top, cx + cw, top + h);
		int y = top + 8 - Math.round(scroll) + fy;

		boolean searching = !query.isBlank();
		String label = searching ? t("eazy.gui.results")
				: t("category.eazy." + selected.name().toLowerCase(Locale.ROOT));
		g.text(this.font, label, cardX, y, alpha(LAVENDER, fade), false);
		g.fill(cardX, y + 11, cardX + 18, y + 12, alpha(ACCENT, fade));
		y += 18;

		if (visible.isEmpty()) {
			g.text(this.font, fit(t("eazy.gui.no_results"), cardW), cardX, y + 4, alpha(MUTED, fade), false);
			y += 20;
		}
		for (EAzyModule m : visible) {
			y = drawCard(g, m, cardX, y, cardW, top, mx, my, fade, dt);
			y += GAP;
		}
		contentHeight = (y + Math.round(scroll)) - (top + 8 - fy) + 4;

		g.disableScissor();

		// Kaydırma çubuğu
		if (max > 0f) {
			int trackH = h - 8;
			int barH = Math.max(14, Math.round(trackH * (h / (contentHeight + 8f))));
			int barY = top + 4 + Math.round((trackH - barH) * (scroll / max));
			g.fill(cx + cw - 4, barY, cx + cw - 2, barY + barH, alpha(ACCENT, ga * 0.7f));
		}
		if (listening != null) {
			String hint = t("eazy.gui.key_hint");
			g.centeredText(this.font, hint, cx + cw / 2, oy + ph - 12, alpha(LAVENDER, ga));
		} else {
			g.centeredText(this.font, fit(t("eazy.gui.hint"), cw - 12), cx + cw / 2, oy + ph - 12, alpha(MUTED, ga * 0.8f));
		}
	}

	private int drawCard(GuiGraphicsExtractor g, EAzyModule m, int x, int y, int w, int top, int mx, int my,
						 float fade, float dt) {
		int bodyFull = bodyHeight.getOrDefault(m, 0);
		boolean open = Boolean.TRUE.equals(expanded.get(m));
		float p = approach(new ExKey(m), open && bodyFull > 0 ? 1f : 0f, 7f, dt);
		int bodyH = Math.round(bodyFull * p);
		int cardH = CARD_HEAD_H + bodyH;
		boolean visibleNow = inContent(y, y + cardH);

		boolean over = mx >= x && mx < x + w && my >= y && my < y + CARD_HEAD_H
				&& my >= top && my < top + ch && mx >= cx && mx < cx + cw;
		float hv = approach(m, over ? 1f : 0f, 9f, dt);
		float on = approach(new OnKey(m), m.isEnabled() ? 1f : 0f, 9f, dt);

		if (visibleNow) {
			int bg = mix(CARD, CARD_HOVER, hv);
			g.fill(x, y, x + w, y + cardH, alpha(mix(BORDER, ACCENT, on * 0.6f), fade));
			g.fill(x + 1, y + 1, x + w - 1, y + cardH - 1, alpha(bg, fade));
			g.fill(x + 1, y + 1, x + 1 + Math.round(2 * on), y + CARD_HEAD_H - 1, alpha(ACCENT, fade));

			int textW = w - 70;
			g.text(this.font, fit(t(m.translationKey()), textW), x + 10, y + 7,
					alpha(mix(TEXT_DIM, TEXT, on), fade), false);
			String desc = m.isFailed() ? t("eazy.gui.failed") : t(m.translationKey() + ".desc");
			g.text(this.font, fit(desc, textW), x + 10, y + 19, alpha(m.isFailed() ? DANGER : MUTED, fade), false);

			// Anahtar
			int sx = x + w - 34;
			int sy = y + 10;
			g.fill(sx, sy, sx + 24, sy + 12, alpha(mix(TRACK_OFF, ACCENT, on), fade));
			int kx = sx + 2 + Math.round(12 * on);
			g.fill(kx, sy + 2, kx + 8, sy + 10, alpha(TEXT, fade));
			// Aç/kapat oku
			if (bodyFull > 0) {
				g.text(this.font, open ? "v" : ">", x + w - 46, y + 12, alpha(MUTED, fade), false);
			}
		}

		if (visibleNow && y + CARD_HEAD_H > top && y < top + ch) {
			int hy1 = Math.max(y, top);
			int hy2 = Math.min(y + CARD_HEAD_H, top + ch);
			if (bodyFull > 0) {
				hit(x, hy1, x + w - 38, hy2, H_EXPAND, m, null);
			} else {
				hit(x, hy1, x + w - 38, hy2, H_TOGGLE, m, null);
			}
			hit(x + w - 38, hy1, x + w, hy2, H_TOGGLE, m, null);
		}

		if (bodyH > 0 && visibleNow) {
			int by = y + CARD_HEAD_H;
			g.enableScissor(Math.max(x, cx), Math.max(by, top), Math.min(x + w, cx + cw), Math.min(by + bodyH, top + ch));
			g.fill(x + 6, by - 1, x + w - 6, by, alpha(BORDER, fade));
			drawBody(g, m, x + 10, by + 6, w - 20, top, mx, my, fade * p, dt, p >= 0.99f);
			g.disableScissor();
		}
		return y + cardH;
	}

	private void drawBody(GuiGraphicsExtractor g, EAzyModule m, int x, int y, int w, int top, int mx, int my,
						  float fade, float dt, boolean interactive) {
		for (Setting<?> s : m.settings()) {
			if (s.isHidden()) {
				continue;
			}
			String name = t("setting.eazy." + m.id() + "." + s.key());
			if (s.isBoolean()) {
				boolean on = Boolean.TRUE.equals(s.get());
				float tv = approach(s, on ? 1f : 0f, 10f, dt);
				g.text(this.font, fit(name, w - 40), x, y + 6, alpha(TEXT_DIM, fade), false);
				int sx = x + w - 24;
				g.fill(sx, y + 4, sx + 24, y + 16, alpha(mix(TRACK_OFF, ACCENT, tv), fade));
				int kx = sx + 2 + Math.round(12 * tv);
				g.fill(kx, y + 6, kx + 8, y + 14, alpha(TEXT, fade));
				if (interactive && inContent(y, y + ROW_H)) {
					hit(x, Math.max(y, top), x + w, Math.min(y + ROW_H, top + ch), H_BOOL, s, null);
				}
				y += ROW_H;
			} else if (s.isNumber()) {
				String val = s.get() instanceof Integer i ? Integer.toString(i)
						: String.format(Locale.ROOT, "%.1f", s.asDouble());
				g.text(this.font, fit(name, w - 40), x, y + 2, alpha(TEXT_DIM, fade), false);
				g.text(this.font, val, x + w - this.font.width(val), y + 2, alpha(LAVENDER, fade), false);
				int ty = y + 17;
				double span = s.max() - s.min();
				float frac = span <= 0 ? 0f : (float) ((s.asDouble() - s.min()) / span);
				float sm = approach(new ExKey(s), frac, 6f, dt);
				g.fill(x, ty, x + w, ty + 4, alpha(TRACK_OFF, fade));
				g.fill(x, ty, x + Math.round(w * sm), ty + 4, alpha(ACCENT, fade));
				int kx = x + Math.round((w - 6) * sm);
				g.fill(kx, ty - 3, kx + 6, ty + 7, alpha(TEXT, fade));
				if (interactive && inContent(y, y + SLIDER_ROW_H)) {
					hit(x, Math.max(y + 10, top), x + w, Math.min(y + SLIDER_ROW_H, top + ch), H_SLIDER, s, null);
				}
				y += SLIDER_ROW_H;
			}
		}
		for (KeyMapping k : m.keyBindings()) {
			boolean listen = listening == k;
			String name = t("eazy.gui.key") + ": " + t(k.getName());
			g.text(this.font, fit(name, w - 80), x, y + 6, alpha(TEXT_DIM, fade), false);
			String keyText = listen ? t("eazy.gui.press_key")
					: (k.isUnbound() ? t("eazy.gui.unbound") : k.getTranslatedKeyMessage().getString());
			int bw = Math.min(w - 8, Math.max(60, this.font.width(keyText) + 12));
			int bx = x + w - bw;
			boolean over = mx >= bx && mx < bx + bw && my >= y + 1 && my < y + 19;
			float hv = approach(new ExKey(k), over || listen ? 1f : 0f, 10f, dt);
			g.fill(bx, y + 1, bx + bw, y + 19, alpha(mix(BORDER, ACCENT, listen ? 1f : hv * 0.6f), fade));
			g.fill(bx + 1, y + 2, bx + bw - 1, y + 18, alpha(BG, fade));
			g.centeredText(this.font, fit(keyText, bw - 6), bx + bw / 2, y + 6, alpha(listen ? LAVENDER : TEXT, fade));
			if (interactive && inContent(y, y + ROW_H)) {
				hit(bx, Math.max(y + 1, top), bx + bw, Math.min(y + 19, top + ch), H_KEY, k, null);
			}
			y += ROW_H;
		}
		if (Boolean.TRUE.equals(hasCustom.get(m))) {
			boolean over = mx >= x && mx < x + w && my >= y + 1 && my < y + 19;
			float hv = approach(new ExKey(m), over ? 1f : 0f, 10f, dt);
			g.fill(x, y + 1, x + w, y + 19, alpha(mix(BORDER, ACCENT, hv), fade));
			g.fill(x + 1, y + 2, x + w - 1, y + 18, alpha(BG, fade));
			g.centeredText(this.font, t("eazy.gui.advanced") + "  >", x + w / 2, y + 6, alpha(LAVENDER, fade));
			if (interactive && inContent(y, y + ROW_H)) {
				hit(x, Math.max(y + 1, top), x + w, Math.min(y + 19, top + ch), H_ADVANCED, m, null);
			}
		}
	}

	/** Animasyon tablosunda aynı nesne için farklı değerleri ayırmak için anahtarlar (eşitlik: nesne + tür). */
	private static final class ExKey {
		private final Object o;

		ExKey(Object o) {
			this.o = o;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ExKey k && k.o == o;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(o) * 31 + 1;
		}
	}

	private static final class OnKey {
		private final Object o;

		OnKey(Object o) {
			this.o = o;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof OnKey k && k.o == o;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(o) * 31 + 2;
		}
	}

	// ---------------------------------------------------------------- girdi

	private Hit hitAt(double x, double y) {
		for (int i = hitCount - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (h.contains(x, y)) {
				return h;
			}
		}
		return null;
	}

	private void updateSlider(Hit h, double mouseX) {
		Setting<?> s = (Setting<?>) h.a;
		double frac = Math.max(0.0, Math.min(1.0, (mouseX - h.x1) / Math.max(1, h.x2 - h.x1)));
		double raw = s.min() + frac * (s.max() - s.min());
		if (s.step() > 0) {
			raw = s.min() + Math.round((raw - s.min()) / s.step()) * s.step();
		}
		s.setFromNumber(raw);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		if (listening != null) {
			listening = null;
			return true;
		}
		boolean inSearch = search.isMouseOver(mx, my);
		if (!inSearch) {
			search.setFocused(false);
		}
		if (inSearch) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() != 0) {
			return true;
		}
		Hit h = hitAt(mx, my);
		pressed = h;
		pressY = my;
		dragScrolled = false;
		sliderHit = null;
		if (h != null && h.type == H_SLIDER) {
			sliderHit = h;
			updateSlider(h, mx);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (sliderHit != null) {
			updateSlider(sliderHit, event.x());
			return true;
		}
		if (pressed != null || dragScrolled) {
			if (dragScrolled || Math.abs(event.y() - pressY) > 5) {
				dragScrolled = true;
				scrollTarget -= (float) dy;
				scroll = scrollTarget;
			}
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		Hit h = pressed;
		pressed = null;
		if (sliderHit != null) {
			sliderHit = null;
			ModuleManager.saveAsync();
			return true;
		}
		if (h != null && !dragScrolled && event.button() == 0 && h.contains(event.x(), event.y())) {
			act(h);
		}
		dragScrolled = false;
		return super.mouseReleased(event);
	}

	private void act(Hit h) {
		switch (h.type) {
			case H_CLOSE -> this.onClose();
			case H_CATEGORY -> {
				if (selected != h.a || !query.isEmpty()) {
					selected = (Category) h.a;
					query = "";
					search.setValue("");
					dirty = true;
					scrollTarget = 0;
					scroll = 0;
					catFade = 0f;
				}
			}
			case H_TOGGLE -> ModuleManager.toggle((EAzyModule) h.a);
			case H_EXPAND -> expanded.merge((EAzyModule) h.a, true, (o, n) -> !o);
			case H_BOOL -> {
				((Setting<?>) h.a).toggle();
				ModuleManager.saveAsync();
			}
			case H_KEY -> listening = (KeyMapping) h.a;
			case H_ADVANCED -> {
				EAzyModule m = (EAzyModule) h.a;
				Screen custom = m.createSettingsScreen(this);
				if (custom != null) {
					this.minecraft.gui.setScreen(custom);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double sx, double sy) {
		scrollTarget -= (float) (sy * 28.0);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (listening != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				listening = null;
			} else {
				if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
					listening.setKey(InputConstants.UNKNOWN);
				} else {
					listening.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
				}
				KeyMapping.resetMapping();
				this.minecraft.options.save();
				listening = null;
			}
			return true;
		}
		if (search.isFocused()) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				search.setFocused(false);
				return true;
			}
			return super.keyPressed(event);
		}
		switch (key) {
			case GLFW.GLFW_KEY_ESCAPE -> this.onClose();
			case GLFW.GLFW_KEY_DOWN -> scrollTarget += 28;
			case GLFW.GLFW_KEY_UP -> scrollTarget -= 28;
			case GLFW.GLFW_KEY_PAGE_DOWN -> scrollTarget += ch - 20;
			case GLFW.GLFW_KEY_PAGE_UP -> scrollTarget -= ch - 20;
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
				int i = categories.indexOf(selected);
				if (i >= 0 && query.isBlank() && !categories.isEmpty()) {
					int n = categories.size();
					selected = categories.get((i + (key == GLFW.GLFW_KEY_RIGHT ? 1 : n - 1)) % n);
					dirty = true;
					scrollTarget = 0;
					scroll = 0;
					catFade = 0f;
				}
			}
			default -> {
				return super.keyPressed(event);
			}
		}
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		ModuleManager.saveAsync();
		this.minecraft.gui.setScreen(this.parent);
	}
}
