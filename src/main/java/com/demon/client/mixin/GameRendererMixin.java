package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** Hide the first-person hand while flying around in freecam. */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void demon$hideHand(float partialTick, boolean sleeping, Matrix4f matrix, CallbackInfo ci) {
		if (ModuleManager.freecam != null && ModuleManager.freecam.isActive()) ci.cancel();
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
	private void demon$zoom(Camera camera, float partialTick, boolean useSetting, CallbackInfoReturnable<Float> cir) {
		if (ModuleManager.zoom != null) cir.setReturnValue(cir.getReturnValue() / ModuleManager.zoom.factor());
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$noHurtCam(PoseStack pose, float partialTick, CallbackInfo ci) {
		if (ModuleManager.noRender != null && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hurtCam.on()) ci.cancel();
	}

	@Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$noTotemAnimation(ItemStack stack, CallbackInfo ci) {
		if (ModuleManager.noRender != null && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.totem.on()) ci.cancel();
	}

	@Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true, require = 0)
	private static void demon$fullbright(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
		if (entity == Minecraft.getInstance().player && ModuleManager.fullbright != null && ModuleManager.fullbright.isEnabled()) cir.setReturnValue(1f);
	}
}
