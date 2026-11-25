package com.lopi.client.modules;

import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.Setting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hotbar ve envanterdeki eşyaların slotuna parlak, belirgin bir çerçeve çizer.
 * Renk eşyanın kimliğinden üretilir (her eşya hep aynı canlı renk), önbellekte tutulur.
 */
public final class ItemOutlineModule extends LopiModule {
	private static ItemOutlineModule instance;
	private static final Map<Item, Integer> CACHE = new ConcurrentHashMap<>();

	private final Setting<Boolean> hotbar = setting("hotbar", Boolean.class, true);
	private final Setting<Boolean> inventory = setting("inventory", Boolean.class, true);

	public ItemOutlineModule() {
		super("item_outline", Category.INTERFACE, true);
		instance = this;
	}

	public static boolean hotbarEnabled() {
		ItemOutlineModule m = instance;
		return m != null && m.isEnabled() && m.hotbar.get();
	}

	public static boolean inventoryEnabled() {
		ItemOutlineModule m = instance;
		return m != null && m.isEnabled() && m.inventory.get();
	}

	/** Eşyaya özel parlak ARGB renk (tam doygunluk, yüksek parlaklık). */
	public static int colorFor(ItemStack stack) {
		return CACHE.computeIfAbsent(stack.getItem(), item -> {
			int hash = BuiltInRegistries.ITEM.getKey(item).toString().hashCode();
			hash ^= hash >>> 16;
			hash *= 0x45d9f3b;
			hash ^= hash >>> 16;
			float hue = (hash & 0xFFFF) / 65536f;
			return hsvToArgb(hue, 0.85f, 1.0f);
		});
	}

	/** java.awt kullanmadan HSV -> ARGB (Android'de java.awt olmayabilir). */
	private static int hsvToArgb(float h, float s, float v) {
		float hh = (h - (float) Math.floor(h)) * 6f;
		int sector = (int) hh;
		float f = hh - sector;
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float t = v * (1f - s * (1f - f));
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	@Override
	protected void onDisable() {
		CACHE.clear();
	}
}
