package com.lopi.client;

import com.lopi.client.core.ConfigManager;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.gui.ClickGuiScreen;
import com.lopi.client.input.LopiKeys;
import com.lopi.client.modules.FpsProfileModule;
import com.lopi.client.modules.AutoGgModule;
import com.lopi.client.modules.EasyPvpModule;
import com.lopi.client.modules.ItemOutlineModule;
import com.lopi.client.modules.NoParticlesModule;
import com.lopi.client.modules.UiModule;
import com.lopi.client.modules.ShortcutsModule;
import com.lopi.client.modules.ZoomModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lopi Client giriş noktası.
 * Altyapı + modüller.
 */
public final class LopiClient implements ClientModInitializer {
	public static final String MOD_ID = "lopi";
	public static final Logger LOGGER = LoggerFactory.getLogger("Lopi");

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
		ModuleManager.register(new EasyPvpModule());

		// 2) Kayıtlı ayarları oku ve modüllere uygula
		ConfigManager.load();
		ModuleManager.applyConfig(ConfigManager.root());

		// 3) Tuşlar (Minecraft > Kontroller > "Lopi" başlığı altında değiştirilebilir)
		KeyMapping openMenu = LopiKeys.register("key.lopi.open_menu", GLFW.GLFW_KEY_K);

		// 4) Her tick: menü tuşu + açık modüllerin tick'i
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenu.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new ClickGuiScreen(null));
				}
			}
			ModuleManager.tick(client);
		});

		// 5) Oyundan çıkarken ayarları kesin olarak kaydet
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveNow());

		LOGGER.info("Lopi Client yüklendi ({} modül).", ModuleManager.all().size());
	}
}
