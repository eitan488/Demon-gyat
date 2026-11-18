package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import com.demon.client.module.modules.render.ViewModelModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
	private void demon$viewModelPush(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		pose.pushPose();
		ViewModelModule vm = ModuleManager.viewModel;
		if (vm == null || !vm.isEnabled()) return;
		boolean main = hand == InteractionHand.MAIN_HAND;
		float side = main ? 1 : -1;
		pose.translate(vm.x.asFloat() * side, vm.y.asFloat(), vm.z.asFloat());
		pose.mulPose(Axis.YP.rotationDegrees(vm.rotY.asFloat() * side));
		float s = vm.scale.asFloat();
		pose.scale(s, s, s);
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
	private void demon$viewModelPop(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		pose.popPose();
	}
}
