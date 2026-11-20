package com.eazy.client.gui;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.modules.FpsProfileModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kategorili ana menü: solda kategori listesi, sağda seçili kategorinin modülleri
 * (aç/kapat + ayar düğmesi), üstte arama kutusu. Seçili kategori oyun kapanana kadar hatırlanır.
 */
public class SettingsScreen extends Screen {
	private static final int ROW = 24;
	private static final int PANEL_W = 330;
	private static final int SIDE_W = 96;
	private static final int TOP = 44;

	private static Category selected = Category.PERFORMANCE;
	private static String query = "";

	private final Screen parent;
	private int left;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("screen.eazy.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		left = (this.width - PANEL_W) / 2;
		int contentX = left + SIDE_W + 8;
		int contentW = PANEL_W - SIDE_W - 8;

		EditBox search = new EditBox(this.font, left, 20, PANEL_W, 18, Component.translatable("eazy.menu.search"));
		search.setMaxLength(32);
		search.setValue(query);
		search.setResponder(text -> {
			query = text;
			this.rebuildWidgets();
		});
		this.addRenderableWidget(search);

		boolean searching = !query.isBlank();
		int y = TOP;
		for (Category category : Category.values()) {
			if (modulesOf(category, false).isEmpty()) {
				continue;
			}
			Category c = category;
			Button b = Button.builder(categoryName(c), btn -> {
				selected = c;
				query = "";
				this.rebuildWidgets();
			}).bounds(left, y, SIDE_W, 20).build();
			b.active = searching || c != selected;
			this.addRenderableWidget(b);
			y += ROW;
		}

		int ry = TOP;
		for (EAzyModule module : modulesOf(selected, searching)) {
			if (ry + 20 > this.height - 34) {
				break;
			}
			boolean hasSettings = module.settings().stream().anyMatch(s -> !s.isHidden())
					|| module.createSettingsScreen(this) != null;
			int toggleW = hasSettings ? contentW - 30 : contentW;
			this.addRenderableWidget(Button.builder(label(module), btn -> {
				ModuleManager.toggle(module);
				btn.setMessage(label(module));
			}).bounds(contentX, ry, toggleW, 20).build());
			if (hasSettings) {
				this.addRenderableWidget(Button.builder(Component.translatable("eazy.button.settings"), btn -> {
					Screen custom = module.createSettingsScreen(this);
					this.minecraft.gui.setScreen(custom != null ? custom : new ModuleSettingsScreen(this, module));
				}).bounds(contentX + toggleW + 4, ry, 26, 20).build());
			}
			ry += ROW;
		}

		this.addRenderableWidget(Button.builder(Component.translatable("eazy.button.done"), btn -> this.onClose())
				.bounds(this.width / 2 - 100, this.height - 26, 200, 20).build());
	}

	/** Arama varsa tüm modüllerde ada göre süzer; yoksa sadece seçili kategoriyi verir. */
	private List<EAzyModule> modulesOf(Category category, boolean searching) {
		List<EAzyModule> list = new ArrayList<>();
		String q = query.toLowerCase(Locale.ROOT).trim();
		for (EAzyModule m : ModuleManager.all()) {
			if (searching) {
				String name = Component.translatable(m.translationKey()).getString().toLowerCase(Locale.ROOT);
				if (name.contains(q)) {
					list.add(m);
				}
			} else if (m.category() == category) {
				list.add(m);
			}
		}
		return list;
	}

	private static Component categoryName(Category c) {
		return Component.translatable("category.eazy." + c.name().toLowerCase(Locale.ROOT));
	}

	private static Component label(EAzyModule module) {
		String stateKey = module.isFailed() ? "eazy.state.error"
				: module.isEnabled() ? "eazy.state.on" : "eazy.state.off";
		return Component.translatable(module.translationKey())
				.append(Component.literal(": "))
				.append(Component.translatable(stateKey));
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Yarı saydam panel arkası (Minecraft'ın koyu menü stiline uygun)
		graphics.fill(left - 6, 14, left + PANEL_W + 6, this.height - 32, 0x99000000);
		graphics.fill(left + SIDE_W + 3, TOP, left + SIDE_W + 4, this.height - 36, 0x55FFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		List<String> found = FpsProfileModule.detectedMods();
		if (!found.isEmpty()) {
			graphics.centeredText(this.font,
					Component.translatable("eazy.fps.detected", String.join(", ", found)),
					this.width / 2, this.height - 42 + 0, 0xFFAAAAAA);
		}
	}
}
