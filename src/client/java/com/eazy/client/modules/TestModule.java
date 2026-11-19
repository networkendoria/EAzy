package com.eazy.client.modules;

import com.eazy.client.EAzyClient;
import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.Setting;

/**
 * GEÇİCİ: Aşama 1 testi için. Aşama sonunda silinecek.
 * Açılıp kapanınca log'a yazar; durumu ve ayarı eazy.json'a kaydedilir.
 */
public final class TestModule extends EAzyModule {
	private final Setting<Boolean> logMessages = setting("log_messages", Boolean.class, true);

	public TestModule() {
		super("test", Category.DEBUG, false);
	}

	@Override
	protected void onEnable() {
		if (logMessages.get()) {
			EAzyClient.LOGGER.info("Test modülü AÇILDI");
		}
	}

	@Override
	protected void onDisable() {
		if (logMessages.get()) {
			EAzyClient.LOGGER.info("Test modülü KAPANDI");
		}
	}
}
