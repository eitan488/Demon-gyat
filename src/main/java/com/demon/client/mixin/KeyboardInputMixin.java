package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	/** While freecam is on, the player stands still; WASD drives the camera instead. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void demon$freeze(CallbackInfo ci) {
		if (ModuleManager.freecam != null && ModuleManager.freecam.isActive()) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
		}
	}
}
