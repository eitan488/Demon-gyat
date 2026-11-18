package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true, require = 0)
	private void demon$safeWalk(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Minecraft.getInstance().player && ModuleManager.safeWalk != null && ModuleManager.safeWalk.isEnabled()) cir.setReturnValue(true);
	}
}
