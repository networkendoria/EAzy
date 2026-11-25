package com.lopi.client.core;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Yüklü modları listeler (Modlar sekmesi). Mod dosyalarına dokunmaz, sadece okur. */
public final class ModsInfo {
	/** Önerilen FPS/performans modları: id, görünen ad. Paketlenmez; oyuncu kendi kurar. */
	public static final String[][] RECOMMENDED = {
			{"sodium", "Sodium"},
			{"lithium", "Lithium"},
			{"zoomify", "Zoomify"},
			{"ferritecore", "FerriteCore"},
			{"immediatelyfast", "ImmediatelyFast"}
	};

	public record Entry(String id, String name, String version) {
	}

	private ModsInfo() {
	}

	public static boolean isLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	/** Oyuncunun kurduğu modlar (oyun, Java, Fabric iç parçaları ve gömülü kütüphaneler hariç). */
	public static List<Entry> installed() {
		List<Entry> list = new ArrayList<>();
		for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
			String id = c.getMetadata().getId();
			if (c.getContainingMod().isPresent() || "builtin".equals(c.getMetadata().getType())) {
				continue;
			}
			if (id.equals("minecraft") || id.equals("java") || id.equals("fabricloader")
					|| (id.startsWith("fabric-") && !id.equals("fabric-api"))) {
				continue;
			}
			list.add(new Entry(id, c.getMetadata().getName(), c.getMetadata().getVersion().getFriendlyString()));
		}
		list.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
		return list;
	}
}
