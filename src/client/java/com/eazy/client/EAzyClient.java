package com.eazy.client;

import com.eazy.client.core.ConfigManager;
import com.eazy.client.core.ModuleManager;
import com.eazy.client.gui.SettingsScreen;
import com.eazy.client.input.EAzyKeys;
import com.eazy.client.modules.FpsProfileModule;
import com.eazy.client.modules.AutoGgModule;
import com.eazy.client.modules.ItemOutlineModule;
import com.eazy.client.modules.NoParticlesModule;
import com.eazy.client.modules.UiModule;
import com.eazy.client.modules.ShortcutsModule;
import com.eazy.client.modules.ZoomModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EAzy Client giriş noktası.
 * Altyapı + modüller.
 */
public final class EAzyClient implements ClientModInitializer {
	public static final String MOD_ID = "eazy";
	public static final Logger LOGGER = LoggerFactory.getLogger("EAzy");

	@Override
	public void onInitializeClient() {
		// 1) Modülleri kaydet
		ModuleManager.register(new NoParticlesModule());
		ModuleManager.register(new FpsProfileModule());
		ModuleManager.register(new ZoomModule());
		ModuleManager.register(new ShortcutsModule());
		ModuleManager.register(new UiModule());
		ModuleManager.register(new ItemOutlineModule());
		ModuleManager.register(new AutoGgModule());

		// 2) Kayıtlı ayarları oku ve modüllere uygula
		ConfigManager.load();
		ModuleManager.applyConfig(ConfigManager.root());

		// 3) Tuşlar (Minecraft > Kontroller > "EAzy" başlığı altında değiştirilebilir)
		KeyMapping openMenu = EAzyKeys.register("key.eazy.open_menu", GLFW.GLFW_KEY_K);

		// 4) Her tick: menü tuşu + açık modüllerin tick'i
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenu.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new SettingsScreen(null));
				}
			}
			ModuleManager.tick(client);
		});

		// 5) Oyundan çıkarken ayarları kesin olarak kaydet
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveNow());

		LOGGER.info("EAzy Client yüklendi ({} modül).", ModuleManager.all().size());
	}
}
