package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void demon$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (Boolean.getBoolean("demon.debugKeys")) com.demon.client.DemonClient.LOG.info("key {} action {} screen {}", event.key(), action, mc.screen);
		if (action != GLFW.GLFW_PRESS || window != mc.getWindow().handle()) return;
		if (mc.screen != null || mc.getOverlay() != null) return;
		ModuleManager.onKey(event.key());
	}
}
