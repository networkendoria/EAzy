package com.eazy.client.modules;

import com.eazy.client.EAzyClient;
import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Tek tuşla FPS profili: Düşük / Orta / Yüksek (varsayılan tuş F7, Kontroller'den değiştirilebilir).
 * Sadece Minecraft'ın kendi ayarlarını değiştirir; Sodium gibi modların ayar dosyalarına dokunmaz.
 */
public final class FpsProfileModule extends EAzyModule {

	/** Algılanıp ayar ekranında gösterilen bilinen FPS modları (mod id'leri). */
	private static final String[] KNOWN_FPS_MODS = {
			"sodium", "lithium", "iris", "ferritecore", "entityculling",
			"immediatelyfast", "moreculling", "modernfix", "krypton", "c2me", "nvidium"
	};

	private record Profile(String id, int renderDistance, int simulationDistance, double entityScale,
						   CloudStatus clouds, boolean entityShadows, int biomeBlend,
						   boolean ambientOcclusion, boolean cutoutLeaves) {
	}

	private static final Profile[] PROFILES = {
			new Profile("low", 4, 5, 0.5, CloudStatus.OFF, false, 0, false, false),
			new Profile("medium", 8, 6, 0.75, CloudStatus.OFF, false, 1, false, false),
			new Profile("high", 12, 8, 1.0, CloudStatus.FAST, true, 2, true, true)
	};

	private final KeyMapping key = bindKey("key.eazy.fps_profile", GLFW.GLFW_KEY_F7);

	/** Son uygulanan profil (-1: henüz hiçbiri). Ayar ekranında görünmez. */
	private final Setting<Integer> current = setting("current", Integer.class, -1).hidden();

	public FpsProfileModule() {
		super("fps_profile", Category.PERFORMANCE, true);
	}

	/** Yüklü bilinen FPS modlarının id listesi. */
	public static List<String> detectedMods() {
		List<String> found = new ArrayList<>();
		for (String id : KNOWN_FPS_MODS) {
			if (FabricLoader.getInstance().isModLoaded(id)) {
				found.add(id);
			}
		}
		return found;
	}

	@Override
	protected void onEnable() {
		while (key.consumeClick()) {
			// Modül kapalıyken biriken tıklamaları at.
		}
		List<String> found = detectedMods();
		if (!found.isEmpty()) {
			EAzyClient.LOGGER.info("Algılanan FPS modları: {}", String.join(", ", found));
		}
	}

	@Override
	protected void onTick(Minecraft client) {
		while (key.consumeClick()) {
			int next = (current.get() + 1) % PROFILES.length;
			if (next < 0) {
				next = 0;
			}
			apply(client, PROFILES[next]);
			current.set(next);
			com.eazy.client.core.ModuleManager.saveAsync();
			if (client.player != null) {
				client.player.displayClientMessage(Component.translatable("eazy.fps.profile",
						Component.translatable("eazy.fps.profile." + PROFILES[next].id())), true);
			}
		}
	}

	private static void apply(Minecraft client, Profile p) {
		Options o = client.options;
		o.renderDistance().set(p.renderDistance());
		o.simulationDistance().set(p.simulationDistance());
		o.entityDistanceScaling().set(p.entityScale());
		o.cloudStatus().set(p.clouds());
		o.entityShadows().set(p.entityShadows());
		o.biomeBlendRadius().set(p.biomeBlend());
		o.ambientOcclusion().set(p.ambientOcclusion());
		o.cutoutLeaves().set(p.cutoutLeaves());
		o.save();
		EAzyClient.LOGGER.info("FPS profili uygulandı: {}", p.id());
	}
}
