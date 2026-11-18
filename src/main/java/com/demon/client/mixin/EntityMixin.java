package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** While freecam is on, mouse movement rotates the free camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void demon$turn(double yaw, double pitch, CallbackInfo ci) {
		if ((Object) this != Minecraft.getInstance().player) return;
		if (ModuleManager.freecam != null && ModuleManager.freecam.isActive()) {
			ModuleManager.freecam.turn(yaw, pitch);
			ci.cancel();
		}
	}
}
