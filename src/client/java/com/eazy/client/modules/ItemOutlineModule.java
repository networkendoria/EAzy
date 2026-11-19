package com.eazy.client.modules;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.Setting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hotbar ve envanterdeki eşyaların slotuna parlak, belirgin bir çerçeve çizer.
 * Renk eşyanın kimliğinden üretilir (her eşya hep aynı canlı renk), önbellekte tutulur.
 */
public final class ItemOutlineModule extends EAzyModule {
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
			return Color.HSBtoRGB(hue, 0.85f, 1.0f) | 0xFF000000;
		});
	}

	@Override
	protected void onDisable() {
		CACHE.clear();
	}
}
