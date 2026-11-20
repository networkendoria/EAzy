package com.eazy.client.modules;

import com.eazy.client.core.Category;
import com.eazy.client.core.EAzyModule;
import com.eazy.client.core.Setting;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Zoomify benzeri yakınlaştırma. Tuşa basılı tutulunca yakınlaşır (varsayılan F8, Kontroller'den değiştirilebilir).
 * Mixin'ler: CameraMixin (FOV) ve MouseHandlerMixin (hassasiyet).
 */
public final class ZoomModule extends EAzyModule {
	private static ZoomModule instance;

	private final KeyMapping key = bindKey("key.eazy.zoom", GLFW.GLFW_KEY_F8);
	private final Setting<Double> level = setting("level", Double.class, 4.0).range(1.5, 10.0, 0.5);
	private final Setting<Boolean> smooth = setting("smooth", Boolean.class, true);
	private final Setting<Double> sensitivity = setting("sensitivity", Double.class, 1.0).range(0.1, 1.0, 0.1);

	/** 1.0 = zoom yok, 1/seviye = tam zoom. */
	private double current = 1.0;
	private long lastNanos;

	public ZoomModule() {
		super("zoom", Category.ZOOM, true);
		instance = this;
	}

	/** CameraMixin tarafından her karede çağrılır. */
	public static float applyFov(float fov) {
		ZoomModule z = instance;
		if (z == null || !z.isEnabled()) {
			return fov;
		}
		return (float) (fov * z.update());
	}

	/** MouseHandlerMixin tarafından çağrılır. Zoom yokken 1.0 döner. */
	public static double sensitivityFactor() {
		ZoomModule z = instance;
		if (z == null || !z.isEnabled() || z.current >= 0.999) {
			return 1.0;
		}
		return z.current * z.sensitivity.get();
	}

	private double update() {
		long now = System.nanoTime();
		double dt = lastNanos == 0 ? 0.0 : Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
		lastNanos = now;

		double target = key.isDown() ? 1.0 / level.get() : 1.0;
		if (smooth.get()) {
			current += (target - current) * (1.0 - Math.exp(-dt * 14.0));
			if (Math.abs(target - current) < 0.0005) {
				current = target;
			}
		} else {
			current = target;
		}
		return current;
	}

	@Override
	protected void onEnable() {
		current = 1.0;
		lastNanos = 0;
	}

	@Override
	protected void onDisable() {
		current = 1.0;
	}
}
