package com.lopi.client.modules;

import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.Setting;

/** Arayüz düzenlemeleri: karartmasız (şeffaf) envanter ve numaralı hotbar. Asıl iş Mixin'lerde. */
public final class UiModule extends LopiModule {
	private static UiModule instance;

	private final Setting<Boolean> transparentInventory = setting("transparent_inventory", Boolean.class, true);
	private final Setting<Boolean> hotbarNumbers = setting("hotbar_numbers", Boolean.class, true);

	public UiModule() {
		super("ui", Category.INTERFACE, true);
		instance = this;
	}

	public static boolean transparentInventory() {
		UiModule u = instance;
		return u != null && u.isEnabled() && u.transparentInventory.get();
	}

	public static boolean hotbarNumbers() {
		UiModule u = instance;
		return u != null && u.isEnabled() && u.hotbarNumbers.get();
	}
}
