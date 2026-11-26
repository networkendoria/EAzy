package com.lopi.client.gui;

import com.lopi.client.core.LopiModule;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.core.Setting;
import com.lopi.client.core.ModsInfo;
import com.lopi.client.input.LopiKeys;
import com.lopi.client.modules.CrosshairModule;
import com.lopi.client.modules.EasyPvpModule;
import com.lopi.client.modules.PerformanceModule;
import com.lopi.client.modules.ShortcutsModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Lopi ClickGUI: koyu, minimal; başlık (logo, sürüm, arama, kapat), kategori kenar çubuğu,
 * modül kartları ve kartların içinde açılan ayarlar (anahtar, kaydırıcı, tuş atama).
 * Tüm metinler Türkçe dil dosyasından gelir. Fare ve dokunmatik (sürükleyerek kaydırma) desteklenir.
 */
public class LopiMenuScreen extends Screen {
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
	private static final int H_TAB = 1;
	private static final int H_TOGGLE = 2;
	private static final int H_EXPAND = 3;
	private static final int H_BOOL = 4;
	private static final int H_SLIDER = 5;
	private static final int H_KEY = 6;
	private static final int H_ADVANCED = 7;
	private static final int H_RUN = 8;
	private static final int H_EDIT = 9;
	private static final int H_ADD = 10;
	private static final int H_MODE = 11;

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

	/** Sol taraftaki sekmeler. */
	private enum Tab {
		GENERAL("general"), CROSSHAIR("crosshair"), MODS("mods"), SETTINGS("settings");

		final String key;

		Tab(String key) {
			this.key = key;
		}
	}

	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("lopi", "textures/gui/logo.png");
	private final ItemStack[] tabIcons = {
			new ItemStack(Items.NETHER_STAR), new ItemStack(Items.SPYGLASS),
			new ItemStack(Items.CHEST), new ItemStack(Items.REPEATER)
	};

	// Oturum boyunca hatırlanan durum
	private static Tab selected = Tab.GENERAL;
	private int hoverMode = -1;
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
	private final List<LopiModule> visible = new ArrayList<>();
	private boolean dirty = true;
	private final Map<LopiModule, Boolean> expanded = new IdentityHashMap<>();
	private final Map<LopiModule, Boolean> hasCustom = new IdentityHashMap<>();
	private final Map<LopiModule, Integer> bodyHeight = new IdentityHashMap<>();
	private final Map<String, String> texts = new HashMap<>();

	// Animasyon (hedefe doğru ilerleyen değerler)
	private final Map<Object, float[]> anim = new HashMap<>();
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
	// Kaydırma çubuğu sürükleme
	private boolean barDragging;
	private float barGrab;
	private int barHitX1, barHitX2, barTrackTop, barTrackH, barThumbH, barThumbY;
	private float barMax;
	private Hit sliderHit;
	private KeyMapping listening;

	public LopiMenuScreen(Screen parent) {
		super(Component.translatable("screen.lopi.settings"));
		this.parent = parent;
		this.version = FabricLoader.getInstance().getModContainer("lopi")
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
		pw = Math.min(this.width - 12, 470);
		ph = Math.min(this.height - 12, 290);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		sideW = pw >= 300 ? 104 : 76;
		cx = px + sideW;
		cy = py + HEADER_H;
		cw = pw - sideW;
		ch = ph - HEADER_H;

		for (LopiModule m : ModuleManager.all()) {
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

		LopiModule crosshairModule = module("crosshair");
		if (crosshairModule != null) {
			expanded.put(crosshairModule, true);
		}

		int sw = Math.min(140, pw / 3);
		int sx = px + pw - 26 - sw;
		search = new EditBox(this.font, sx + 6, py + 11, sw - 12, 10, Component.translatable("lopi.gui.search"));
		search.setBordered(false);
		search.setMaxLength(32);
		search.setHint(Component.translatable("lopi.gui.search"));
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
		if (!q.isEmpty()) {
			for (LopiModule m : ModuleManager.all()) {
				String name = t(m.translationKey()).toLowerCase(Locale.ROOT);
				String desc = t(m.translationKey() + ".desc").toLowerCase(Locale.ROOT);
				if (name.contains(q) || desc.contains(q)) {
					visible.add(m);
				}
			}
		}
		dirty = false;
	}

	private static LopiModule module(String id) {
		for (LopiModule m : ModuleManager.all()) {
			if (m.id().equals(id)) {
				return m;
			}
		}
		return null;
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
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, px + 7, oy + 5, 0f, 0f, 20, 20, 64, 64, 64, 64);
		g.text(this.font, Component.literal("Lopi").withStyle(ChatFormatting.BOLD), px + 32, oy + 6, alpha(TEXT, ga), false);
		int vx = px + 32 + this.font.width("Lopi") + 4;
		g.text(this.font, "v" + version, vx, oy + 6, alpha(MUTED, ga), false);
		// Etkin durum göstergesi
		int active = 0;
		for (LopiModule m : ModuleManager.all()) {
			if (m.isEnabled()) {
				active++;
			}
		}
		g.fill(px + 32, oy + 19, px + 36, oy + 23, alpha(ACCENT, ga));
		g.text(this.font, String.format(t("lopi.gui.active_count"), active), px + 40, oy + 17, alpha(LAVENDER, ga), false);

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
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			Tab tab = tabs[i];
			boolean sel = tab == selected && !searching;
			boolean over = mx >= px + 4 && mx < px + sideW - 5 && my >= y && my < y + 28;
			float sv = approach(tab, sel ? 1f : 0f, 9f, dt);
			float hv = approach(new ExKey(tab), over ? 1f : 0f, 10f, dt);
			int bg = mix(SURFACE, CARD, Math.max(sv, hv * 0.7f));
			g.fill(px + 5, y, px + sideW - 6, y + 28, alpha(bg, ga));
			g.fill(px + 5, y, px + 5 + Math.round(2 * sv), y + 28, alpha(ACCENT, ga));
			g.item(tabIcons[i], px + 10, y + 6);
			g.text(this.font, fit(t("lopi.tab." + tab.key), sideW - 40), px + 30, y + 10,
					alpha(mix(MUTED, TEXT, Math.max(sv, hv)), ga), false);
			hit(px + 4, y, px + sideW - 5, y + 28, H_TAB, tab, null);
			y += 31;
		}
		// Alt bilgi: açma kısayolu
		g.text(this.font, fit(t("lopi.gui.open_hint"), sideW - 12), px + 7, oy + ph - 13, alpha(MUTED, ga), false);
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
		if (searching) {
			y = section(g, t("lopi.gui.results"), cardX, y, fade);
			if (visible.isEmpty()) {
				g.text(this.font, fit(t("lopi.gui.no_results"), cardW), cardX, y + 4, alpha(MUTED, fade), false);
				y += 20;
			}
			for (LopiModule m : visible) {
				y = drawCard(g, m, cardX, y, cardW, top, mx, my, fade, dt) + GAP;
			}
		} else {
			switch (selected) {
				case GENERAL -> y = drawGeneral(g, cardX, y, cardW, top, mx, my, fade, dt);
				case CROSSHAIR -> y = drawCrosshair(g, cardX, y, cardW, top, mx, my, fade, dt);
				case MODS -> y = drawMods(g, cardX, y, cardW, top, fade);
				case SETTINGS -> y = drawSettings(g, cardX, y, cardW, top, mx, my, fade, dt);
			}
		}
		contentHeight = (y + Math.round(scroll)) - (top + 8 - fy) + 4;

		g.disableScissor();

		// Kaydırma çubuğu
		barMax = max;
		barHitX1 = barHitX2 = 0;
		if (max > 0f) {
			int trackH = h - 8;
			int barH = Math.max(14, Math.round(trackH * (h / (contentHeight + 8f))));
			int barY = top + 4 + Math.round((trackH - barH) * (scroll / max));
			boolean hot = barDragging || (mx >= cx + cw - 12 && mx <= cx + cw && my >= top && my <= top + h);
			g.fill(cx + cw - 5, top + 4, cx + cw - 1, top + 4 + trackH, alpha(0xFF1C1926, ga * (hot ? 0.9f : 0.5f)));
			g.fill(cx + cw - 5, barY, cx + cw - 1, barY + barH, alpha(hot ? LAVENDER : ACCENT, ga * 0.9f));
			barHitX1 = cx + cw - 12;
			barHitX2 = cx + cw;
			barTrackTop = top + 4;
			barTrackH = trackH;
			barThumbH = barH;
			barThumbY = barY;
		}
		if (listening != null) {
			String hint = t("lopi.gui.key_hint");
			g.centeredText(this.font, hint, cx + cw / 2, oy + ph - 12, alpha(LAVENDER, ga));
		} else {
			g.centeredText(this.font, fit(t("lopi.gui.hint"), cw - 12), cx + cw / 2, oy + ph - 12, alpha(MUTED, ga * 0.8f));
		}
	}

	// ---------------------------------------------------------------- sekmeler

	private int section(GuiGraphicsExtractor g, String text, int x, int y, float fade) {
		g.text(this.font, text, x, y, alpha(LAVENDER, fade), false);
		g.fill(x, y + 11, x + 18, y + 12, alpha(ACCENT, fade));
		return y + 18;
	}

	private int drawGeneral(GuiGraphicsExtractor g, int x, int y, int w, int top, int mx, int my, float fade, float dt) {
		y = section(g, t("lopi.tab.general"), x, y, fade);
		for (String id : new String[]{"autogg", "easypvp"}) {
			LopiModule m = module(id);
			if (m != null) {
				y = drawCard(g, m, x, y, w, top, mx, my, fade, dt) + GAP;
			}
		}
		ShortcutsModule sc = ShortcutsModule.get();
		y = section(g, t("lopi.shortcuts.title"), x, y + 2, fade);
		if (sc != null) {
			for (ShortcutsModule.Entry e : sc.entries()) {
				y = drawShortcutCard(g, e, x, y, w, top, mx, my, fade, dt) + GAP;
			}
			if (sc.entries().size() < ShortcutsModule.MAX_ENTRIES) {
				y = button(g, x, y, w, 22, "+  " + t("lopi.shortcuts.add"), "add", true, H_ADD, null, top, mx, my, fade, dt) + GAP;
			}
		}
		return y;
	}

	private int drawShortcutCard(GuiGraphicsExtractor g, ShortcutsModule.Entry e, int x, int y, int w, int top,
								 int mx, int my, float fade, float dt) {
		int hgt = 36;
		boolean rtp = "rtp".equals(e.id);
		if (inContent(y, y + hgt)) {
			g.fill(x, y, x + w, y + hgt, alpha(BORDER, fade));
			g.fill(x + 1, y + 1, x + w - 1, y + hgt - 1, alpha(CARD, fade));
			int editW = this.font.width(t("lopi.gui.edit")) + 14;
			String runLabel = e.name.isBlank() ? t("lopi.shortcuts.unnamed") : e.name;
			int runW = Math.min(w / 3, Math.max(56, this.font.width(runLabel) + 16));
			int textW = w - editW - runW - 36;
			String title = rtp ? t("lopi.rtp.title") : (e.name.isBlank() ? t("lopi.shortcuts.unnamed") : e.name);
			g.text(this.font, fit(title, textW), x + 10, y + 8, alpha(TEXT, fade), false);
			String sub = rtp ? t("lopi.rtp.desc") : e.command;
			if (e.key > 0) {
				sub += "  [" + InputConstants.Type.KEYSYM.getOrCreate(e.key).getDisplayName().getString() + "]";
			}
			g.text(this.font, fit(sub, textW), x + 10, y + 21, alpha(MUTED, fade), false);
			int bx = x + w - 8 - editW;
			buttonAt(g, bx, y + 8, editW, 20, t("lopi.gui.edit"), "edit" + e.id, false, H_EDIT, e, top, mx, my, fade, dt);
			int rx = bx - 6 - runW;
			buttonAt(g, rx, y + 8, runW, 20, runLabel, "run" + e.id, true, H_RUN, e, top, mx, my, fade, dt);
		}
		return y + hgt;
	}

	private int button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, Object key, boolean primary,
					   int type, Object data, int top, int mx, int my, float fade, float dt) {
		if (inContent(y, y + h)) {
			buttonAt(g, x, y, w, h, label, key, primary, type, data, top, mx, my, fade, dt);
		}
		return y + h;
	}

	private void buttonAt(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, Object key, boolean primary,
						  int type, Object data, int top, int mx, int my, float fade, float dt) {
		boolean over = mx >= x && mx < x + w && my >= Math.max(y, top) && my < Math.min(y + h, top + ch)
				&& mx >= cx && mx < cx + cw;
		float hv = approach(new ExKey(key), over ? 1f : 0f, 10f, dt);
		int edge = mix(primary ? ACCENT : BORDER, LAVENDER, hv * 0.8f);
		g.fill(x, y, x + w, y + h, alpha(edge, fade));
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(primary ? mix(0xFF2A1F4D, ACCENT, hv * 0.5f) : mix(BG, CARD_HOVER, hv), fade));
		g.centeredText(this.font, fit(label, w - 8), x + w / 2, y + (h - 8) / 2, alpha(TEXT, fade));
		if (over || (my >= top && my < top + ch)) {
			hit(x, Math.max(y, top), x + w, Math.min(y + h, top + ch), type, data, null);
		}
	}

	private int drawCrosshair(GuiGraphicsExtractor g, int x, int y, int w, int top, int mx, int my, float fade, float dt) {
		y = section(g, t("lopi.tab.crosshair"), x, y, fade);
		CrosshairModule c = CrosshairModule.get();
		int boxH = 78;
		if (inContent(y, y + boxH)) {
			g.fill(x, y, x + w, y + boxH, alpha(BORDER, fade));
			g.fill(x + 1, y + 1, x + w - 1, y + boxH - 1, alpha(0xFF2D4A2B, fade));
			for (int i = 0; i < 6; i++) {
				g.fill(x + 1, y + 1 + i * 13, x + w - 1, y + 1 + i * 13 + 13, alpha(i % 2 == 0 ? 0xFF35592F : 0xFF2D4A2B, fade * 0.8f));
			}
			if (c != null) {
				c.draw(g, x + w / 2, y + boxH / 2);
			}
			g.text(this.font, t("lopi.crosshair.preview"), x + 6, y + 5, alpha(0xFFE6E0F5, fade), true);
		}
		y += boxH + GAP;
		if (c != null) {
			y = drawCard(g, c, x, y, w, top, mx, my, fade, dt) + GAP;
		}
		return y;
	}

	private int drawMods(GuiGraphicsExtractor g, int x, int y, int w, int top, float fade) {
		y = section(g, t("lopi.mods.recommended"), x, y, fade);
		for (String[] r : ModsInfo.RECOMMENDED) {
			boolean on = ModsInfo.isLoaded(r[0]);
			y = modRow(g, r[1], null, on ? t("lopi.mods.installed") : t("lopi.mods.missing"), on, x, y, w, top, fade);
		}
		y = section(g, String.format(t("lopi.mods.installed_list"), ModsInfo.installed().size()), x, y + 6, fade);
		for (ModsInfo.Entry e : ModsInfo.installed()) {
			y = modRow(g, e.name(), e.version(), t("lopi.mods.installed"), true, x, y, w, top, fade);
		}
		g.text(this.font, fit(t("lopi.mods.note"), w), x, y + 4, alpha(MUTED, fade), false);
		return y + 18;
	}

	private int modRow(GuiGraphicsExtractor g, String name, String version, String badge, boolean ok,
					   int x, int y, int w, int top, float fade) {
		int hgt = 22;
		if (inContent(y, y + hgt)) {
			g.fill(x, y, x + w, y + hgt, alpha(BORDER, fade));
			g.fill(x + 1, y + 1, x + w - 1, y + hgt - 1, alpha(CARD, fade));
			int bw = this.font.width(badge) + 12;
			g.fill(x + w - 6 - bw, y + 4, x + w - 6, y + hgt - 4, alpha(ok ? 0xFF2A4A3A : 0xFF3A2F45, fade));
			g.centeredText(this.font, badge, x + w - 6 - bw / 2, y + 7, alpha(ok ? 0xFF8CF0B4 : MUTED, fade));
			String left = version == null ? name : name + "  " + version;
			g.text(this.font, fit(left, w - bw - 22), x + 8, y + 7, alpha(ok ? TEXT : TEXT_DIM, fade), false);
		}
		return y + hgt + 3;
	}

	private int drawSettings(GuiGraphicsExtractor g, int x, int y, int w, int top, int mx, int my, float fade, float dt) {
		y = section(g, t("lopi.perf.title"), x, y, fade);
		PerformanceModule perf = PerformanceModule.get();
		int current = perf == null ? -1 : perf.mode();
		int bh = 40;
		int gap = 6;
		int bw = (w - 2 * gap) / 3;
		hoverMode = -1;
		for (int i = 0; i < 3; i++) {
			int bx = x + i * (bw + gap);
			if (!inContent(y, y + bh)) {
				continue;
			}
			boolean over = mx >= bx && mx < bx + bw && my >= Math.max(y, top) && my < Math.min(y + bh, top + ch)
					&& mx >= cx && mx < cx + cw;
			if (over) {
				hoverMode = i;
			}
			float sel = approach(new OnKey(PerformanceModule.IDS[i]), current == i ? 1f : 0f, 9f, dt);
			float hv = approach(new ExKey(PerformanceModule.IDS[i]), over ? 1f : 0f, 10f, dt);
			g.fill(bx, y, bx + bw, y + bh, alpha(mix(BORDER, ACCENT, Math.max(sel, hv * 0.6f)), fade));
			g.fill(bx + 1, y + 1, bx + bw - 1, y + bh - 1, alpha(mix(CARD, 0xFF2A1F4D, sel), fade));
			g.centeredText(this.font, Component.literal(t("lopi.perf.mode." + PerformanceModule.IDS[i]))
					.withStyle(ChatFormatting.BOLD), bx + bw / 2, y + 8, alpha(mix(TEXT_DIM, TEXT, Math.max(sel, hv)), fade));
			g.centeredText(this.font, fit(t("lopi.perf.short." + PerformanceModule.IDS[i]), bw - 8), bx + bw / 2, y + 22,
					alpha(sel > 0.5f ? LAVENDER : MUTED, fade));
			hit(bx, Math.max(y, top), bx + bw, Math.min(y + bh, top + ch), H_MODE, i, null);
		}
		y += bh + GAP;
		int shown = hoverMode >= 0 ? hoverMode : current;
		String info = shown >= 0 ? t("lopi.perf.warn." + PerformanceModule.IDS[shown]) : t("lopi.perf.warn.none");
		List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(info), w - 20);
		int boxH = lines.size() * 10 + 14;
		if (inContent(y, y + boxH)) {
			g.fill(x, y, x + w, y + boxH, alpha(shown == PerformanceModule.UPS || shown == PerformanceModule.ECO ? 0xFF6B4A1E : BORDER, fade));
			g.fill(x + 1, y + 1, x + w - 1, y + boxH - 1, alpha(0xFF1A1620, fade));
			g.text(this.font, "!", x + 6, y + 7, alpha(0xFFFFC857, fade), false);
			for (int i = 0; i < lines.size(); i++) {
				g.text(this.font, lines.get(i), x + 16, y + 7 + i * 10, alpha(TEXT_DIM, fade), false);
			}
		}
		y += boxH + GAP + 4;

		y = section(g, t("lopi.settings.warnings"), x, y, fade);
		List<String> warns = warnings(current);
		if (warns.isEmpty()) {
			g.text(this.font, fit(t("lopi.warn.none"), w), x, y, alpha(MUTED, fade), false);
			y += 14;
		}
		for (String warn : warns) {
			List<net.minecraft.util.FormattedCharSequence> wl = this.font.split(Component.literal(warn), w - 20);
			int wh = wl.size() * 10 + 12;
			if (inContent(y, y + wh)) {
				g.fill(x, y, x + w, y + wh, alpha(0xFF6B4A1E, fade));
				g.fill(x + 1, y + 1, x + w - 1, y + wh - 1, alpha(0xFF1A1620, fade));
				g.text(this.font, "!", x + 6, y + 6, alpha(0xFFFFC857, fade), false);
				for (int i = 0; i < wl.size(); i++) {
					g.text(this.font, wl.get(i), x + 16, y + 6 + i * 10, alpha(TEXT_DIM, fade), false);
				}
			}
			y += wh + 4;
		}
		y += GAP;

		y = section(g, t("lopi.settings.modules"), x, y, fade);
		for (String id : new String[]{"info_hud", "zoom", "no_particles", "ui", "item_outline"}) {
			LopiModule m = module(id);
			if (m != null) {
				y = drawCard(g, m, x, y, w, top, mx, my, fade, dt) + GAP;
			}
		}
		y = section(g, t("lopi.settings.info"), x, y + 2, fade);
		for (String key : new String[]{"lopi.settings.info.menu", "lopi.settings.info.config", "lopi.settings.info.keys"}) {
			for (net.minecraft.util.FormattedCharSequence line : this.font.split(Component.literal(t(key)), w)) {
				if (inContent(y, y + 10)) {
					g.text(this.font, line, x, y, alpha(MUTED, fade), false);
				}
				y += 10;
			}
			y += 3;
		}
		return y;
	}

	/** Oyuncunun görmesi gereken uyarılar (bellek, mod seçimi, Sodium). */
	private List<String> warnings(int perfMode) {
		List<String> list = new ArrayList<>();
		long mb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		if (mb < 2000) {
			list.add(Component.translatable("lopi.warn.ram_low", Long.toString(mb)).getString());
		} else if (perfMode == PerformanceModule.UPS && mb < 3000) {
			list.add(Component.translatable("lopi.warn.ram_ups", Long.toString(mb)).getString());
		}
		if (perfMode < 0) {
			list.add(t("lopi.warn.mode_none"));
		}
		if (!com.lopi.client.core.ModsInfo.isLoaded("sodium")) {
			list.add(t("lopi.warn.no_sodium"));
		}
		return list;
	}

	private int drawCard(GuiGraphicsExtractor g, LopiModule m, int x, int y, int w, int top, int mx, int my,
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
			String desc = m.isFailed() ? t("lopi.gui.failed") : t(m.translationKey() + ".desc");
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

	private void drawBody(GuiGraphicsExtractor g, LopiModule m, int x, int y, int w, int top, int mx, int my,
						  float fade, float dt, boolean interactive) {
		for (Setting<?> s : m.settings()) {
			if (s.isHidden()) {
				continue;
			}
			String name = t("setting.lopi." + m.id() + "." + s.key());
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
			String name = t("lopi.gui.key") + ": " + t(k.getName());
			g.text(this.font, fit(name, w - 80), x, y + 6, alpha(TEXT_DIM, fade), false);
			String keyText = listen ? t("lopi.gui.press_key")
					: (k.isUnbound() ? t("lopi.gui.unbound") : k.getTranslatedKeyMessage().getString());
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
			g.centeredText(this.font, t("lopi.gui.advanced") + "  >", x + w / 2, y + 6, alpha(LAVENDER, fade));
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
		if (barHitX2 > 0 && barMax > 0f && mx >= barHitX1 && mx <= barHitX2
				&& my >= barTrackTop && my <= barTrackTop + barTrackH) {
			barDragging = true;
			if (my >= barThumbY && my <= barThumbY + barThumbH) {
				barGrab = (float) (my - barThumbY);
			} else {
				barGrab = barThumbH / 2f;
				dragBarTo(my);
			}
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

	private void dragBarTo(double my) {
		float range = Math.max(1, barTrackH - barThumbH);
		float t = (float) ((my - barGrab - barTrackTop) / range);
		scrollTarget = Math.max(0f, Math.min(barMax, t * barMax));
		scroll = scrollTarget;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (barDragging) {
			dragBarTo(event.y());
			return true;
		}
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
		if (barDragging) {
			barDragging = false;
			return true;
		}
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
			case H_TAB -> {
				if (selected != h.a || !query.isEmpty()) {
					selected = (Tab) h.a;
					query = "";
					search.setValue("");
					dirty = true;
					scrollTarget = 0;
					scroll = 0;
					catFade = 0f;
				}
			}
			case H_TOGGLE -> {
				LopiModule m = (LopiModule) h.a;
				if (m instanceof EasyPvpModule) {
					EasyPvpModule.setEazz(!m.isEnabled());
				} else {
					ModuleManager.toggle(m);
				}
			}
			case H_RUN -> {
				ShortcutsModule.run(this.minecraft, (ShortcutsModule.Entry) h.a);
				this.onClose();
			}
			case H_EDIT -> this.minecraft.gui.setScreen(new ShortcutEditScreen(this, (ShortcutsModule.Entry) h.a));
			case H_ADD -> this.minecraft.gui.setScreen(new ShortcutEditScreen(this, null));
			case H_MODE -> {
				PerformanceModule p = PerformanceModule.get();
				if (p != null) {
					p.select(this.minecraft, (Integer) h.a);
				}
			}
			case H_EXPAND -> expanded.merge((LopiModule) h.a, true, (o, n) -> !o);
			case H_BOOL -> {
				((Setting<?>) h.a).toggle();
				ModuleManager.saveAsync();
			}
			case H_KEY -> listening = (KeyMapping) h.a;
			case H_ADVANCED -> {
				LopiModule m = (LopiModule) h.a;
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
		if (key == GLFW.GLFW_KEY_F8 && LopiKeys.rightShiftDown()) {
			this.onClose();
			return true;
		}
		switch (key) {
			case GLFW.GLFW_KEY_ESCAPE -> this.onClose();
			case GLFW.GLFW_KEY_DOWN -> scrollTarget += 28;
			case GLFW.GLFW_KEY_UP -> scrollTarget -= 28;
			case GLFW.GLFW_KEY_PAGE_DOWN -> scrollTarget += ch - 20;
			case GLFW.GLFW_KEY_PAGE_UP -> scrollTarget -= ch - 20;
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
				if (query.isBlank()) {
					Tab[] tabs = Tab.values();
					int n = tabs.length;
					selected = tabs[(selected.ordinal() + (key == GLFW.GLFW_KEY_RIGHT ? 1 : n - 1)) % n];
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
