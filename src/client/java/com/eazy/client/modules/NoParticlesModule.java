package com.eazy.client.modules;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;

/**
 * Tüm parçacıkları kapatır (varsayılan AÇIK). Asıl iş ParticleEngineMixin'de yapılır;
 * bu modül sadece "engelle" bayrağını yönetir. Modül kapanınca (ya da hata verirse) parçacıklar geri gelir.
 */
public final class NoParticlesModule extends EAzyModule {
	private static volatile boolean blocking;

	public NoParticlesModule() {
		super("no_particles", Category.PERFORMANCE, true);
	}

	public static boolean isBlocking() {
		return blocking;
	}

	@Override
	protected void onEnable() {
		blocking = true;
	}

	@Override
	protected void onDisable() {
		blocking = false;
	}
}
