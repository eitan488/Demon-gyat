package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** Hide the first-person hand while flying around in freecam. */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void demon$hideHand(float partialTick, boolean sleeping, Matrix4f matrix, CallbackInfo ci) {
		if (ModuleManager.freecam != null && ModuleManager.freecam.isActive()) ci.cancel();
	}
}
