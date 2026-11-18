package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import com.demon.client.module.modules.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Inject(method = "setup", at = @At("TAIL"))
	private void demon$freecam(Level level, Entity entity, boolean thirdPerson, boolean mirror, float partialTick, CallbackInfo ci) {
		FreecamModule fc = ModuleManager.freecam;
		if (fc == null || !fc.isActive()) return;
		this.detached = true;
		this.setRotation(fc.yaw(partialTick), fc.pitch(partialTick));
		this.setPosition(fc.position(partialTick));
	}
}
