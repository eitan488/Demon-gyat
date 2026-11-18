package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$fullbright(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
		if (effect == MobEffects.NIGHT_VISION && (Object) this == Minecraft.getInstance().player
			&& ModuleManager.fullbright != null && ModuleManager.fullbright.isEnabled()) cir.setReturnValue(true);
	}

	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
	private void demon$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this == Minecraft.getInstance().player && ModuleManager.viewModel != null && ModuleManager.viewModel.isEnabled()) {
			cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() * ModuleManager.viewModel.swingSpeed.asFloat())));
		}
	}
}
