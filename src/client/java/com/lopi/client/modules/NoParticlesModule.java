package com.lopi.client.modules;

import com.lopi.client.core.Category;
import com.lopi.client.core.LopiModule;

/**
 * Tüm parçacıkları kapatır (varsayılan AÇIK). Asıl iş ParticleEngineMixin'de yapılır;
 * bu modül sadece "engelle" bayrağını yönetir. Modül kapanınca (ya da hata verirse) parçacıklar geri gelir.
 */
public final class NoParticlesModule extends LopiModule {
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
