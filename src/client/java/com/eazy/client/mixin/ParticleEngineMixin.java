package com.eazy.client.mixin;

import com.eazy.client.modules.NoParticlesModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Parçacıklar kapalı" modülü için: parçacık oluşturan tüm yolları client tarafında durdurur.
 * Sunucuya hiçbir etkisi yoktur. Modül kapalıysa hiçbir şeye karışmaz.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void eazy$blockCreate(ParticleOptions options, double x, double y, double z,
								  double vx, double vy, double vz,
								  CallbackInfoReturnable<Particle> cir) {
		if (NoParticlesModule.isBlocking()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void eazy$blockAdd(Particle particle, CallbackInfo ci) {
		if (NoParticlesModule.isBlocking()) {
			ci.cancel();
		}
	}

	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
			at = @At("HEAD"), cancellable = true)
	private void eazy$blockEmitter(Entity entity, ParticleOptions options, CallbackInfo ci) {
		if (NoParticlesModule.isBlocking()) {
			ci.cancel();
		}
	}

	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
			at = @At("HEAD"), cancellable = true)
	private void eazy$blockEmitterTimed(Entity entity, ParticleOptions options, int lifetime, CallbackInfo ci) {
		if (NoParticlesModule.isBlocking()) {
			ci.cancel();
		}
	}
}
