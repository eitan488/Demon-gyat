package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
	private boolean demon$client() {
		return ((Level) (Object) this).isClientSide();
	}

	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$noRain(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (demon$client() && ModuleManager.noRender != null && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.weather.on()) cir.setReturnValue(0f);
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$noThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (demon$client() && ModuleManager.noRender != null && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.weather.on()) cir.setReturnValue(0f);
	}

	@Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true, require = 0)
	private void demon$time(CallbackInfoReturnable<Long> cir) {
		if (demon$client() && ModuleManager.timeChanger != null && ModuleManager.timeChanger.isEnabled()) cir.setReturnValue((long) ModuleManager.timeChanger.time.asInt());
	}
}
