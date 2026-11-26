package com.lopi.client.modules;

import com.lopi.client.LopiClient;
import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.core.Setting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Performans modları: ECO / NM / UPS. Seçim menüden veya F7 ile (Kontroller'den değiştirilebilir) yapılır.
 * Sadece Minecraft'ın kendi ayarlarını değiştirir; Sodium gibi modların dosyalarına dokunmaz.
 */
public final class PerformanceModule extends LopiModule {
	public static final int ECO = 0;
	public static final int NM = 1;
	public static final int UPS = 2;
	public static final String[] IDS = {"eco", "nm", "ups"};

	private record Profile(int render, int simulation, int maxFps, double entityScale, CloudStatus clouds,
						   boolean shadows, int biomeBlend, boolean ao, boolean leaves, int mipmap) {
	}

	/** ECO: en düşük değerler; NM: varsayılanın biraz altı; UPS: yüksek değerler. Son alan: mipmap seviyesi. */
	private static final Profile[] PROFILES = {
			new Profile(2, 5, 60, 0.5, CloudStatus.OFF, false, 0, false, false, 0),
			new Profile(8, 6, 120, 0.75, CloudStatus.OFF, false, 1, false, false, 0),
			new Profile(16, 10, 120, 1.0, CloudStatus.FAST, true, 2, true, true, 1)
	};

	private static PerformanceModule instance;

	private final KeyMapping key = bindKey("key.lopi.perf_mode", GLFW.GLFW_KEY_F7);
	/** Son uygulanan mod (-1: henüz hiçbiri). */
	private final Setting<Integer> mode = setting("mode", Integer.class, -1).hidden();

	public PerformanceModule() {
		super("perf_mode", Category.PERFORMANCE, true);
		instance = this;
	}

	public static PerformanceModule get() {
		return instance;
	}

	public int mode() {
		return mode.get();
	}

	/** Verilen modu uygular ve kaydeder. */
	public void select(Minecraft client, int index) {
		if (index < 0 || index >= PROFILES.length) {
			return;
		}
		apply(client, PROFILES[index], IDS[index]);
		mode.set(index);
		ModuleManager.saveAsync();
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.translatable("lopi.perf.applied",
					Component.translatable("lopi.perf.mode." + IDS[index])));
		}
	}

	@Override
	protected void onEnable() {
		while (key.consumeClick()) {
			// Modül kapalıyken biriken tıklamaları at.
		}
	}

	@Override
	protected void onTick(Minecraft client) {
		while (key.consumeClick()) {
			select(client, (Math.max(-1, mode.get()) + 1) % PROFILES.length);
		}
	}

	private static void apply(Minecraft client, Profile p, String id) {
		Options o = client.options;
		o.renderDistance().set(p.render());
		o.simulationDistance().set(p.simulation());
		o.framerateLimit().set(p.maxFps());
		o.entityDistanceScaling().set(p.entityScale());
		o.cloudStatus().set(p.clouds());
		o.entityShadows().set(p.shadows());
		o.biomeBlendRadius().set(p.biomeBlend());
		o.ambientOcclusion().set(p.ao());
		o.cutoutLeaves().set(p.leaves());
		o.mipmapLevels().set(p.mipmap());
		o.save();
		LopiClient.LOGGER.info("Performans modu uygulandı: {}", id);
	}
}
