package com.demon.client.mixin;

import com.demon.client.combat.CombatFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"), require = 0)
	private void demon$click(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == 1 && Minecraft.getInstance().screen == null) CombatFx.onClick(info.button());
	}
}
