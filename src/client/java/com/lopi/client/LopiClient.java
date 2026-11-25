package com.lopi.client;

import com.lopi.client.core.ConfigManager;
import com.lopi.client.core.ModuleManager;
import com.lopi.client.gui.LopiMenuScreen;
import com.lopi.client.input.LopiKeys;
import com.lopi.client.modules.CrosshairModule;
import com.lopi.client.modules.PerformanceModule;
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
		ModuleManager.register(new PerformanceModule());
		ModuleManager.register(new ZoomModule());
		ModuleManager.register(new ShortcutsModule());
		ModuleManager.register(new UiModule());
		ModuleManager.register(new ItemOutlineModule());
		ModuleManager.register(new AutoGgModule());
		ModuleManager.register(new EasyPvpModule());
		ModuleManager.register(new CrosshairModule());

		// 2) Kayıtlı ayarları oku ve modüllere uygula
		ConfigManager.load();
		ModuleManager.applyConfig(ConfigManager.root());

		// 3) Menü kısayolu: Sağ Shift + F8 (KeyMapping gerekmez; zoom Sağ Shift basılıyken kapalıdır)
		boolean[] comboHeld = {false};

		// 4) Her tick: menü kısayolu + açık modüllerin tick'i
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean combo = client.getWindow() != null && LopiKeys.rightShiftDown() && LopiKeys.isDown(GLFW.GLFW_KEY_F8);
			if (combo && !comboHeld[0] && client.gui.screen() == null) {
				client.gui.setScreen(new LopiMenuScreen(null));
			}
			comboHeld[0] = combo;
			ModuleManager.tick(client);
		});

		// 5) Oyundan çıkarken ayarları kesin olarak kaydet
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveNow());

		LOGGER.info("Lopi Client yüklendi ({} modül).", ModuleManager.all().size());
	}
}
