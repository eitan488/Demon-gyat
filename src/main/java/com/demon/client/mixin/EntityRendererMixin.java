package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$hideVanillaNametag(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player && ModuleManager.nametags != null && ModuleManager.nametags.isEnabled()) cir.setReturnValue(false);
	}
}
