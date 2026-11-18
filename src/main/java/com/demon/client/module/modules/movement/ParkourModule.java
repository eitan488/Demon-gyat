package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;

/** Jumps automatically at the very edge of a block. */
public class ParkourModule extends Module {
	public ParkourModule() {
		super("Parkour", "Jumps automatically at block edges", Category.MOVEMENT, -1, "movement");
	}

	@Override
	public void onTick() {
		if (mc.player == null || !mc.player.onGround() || mc.player.isShiftKeyDown() || mc.options.keyJump.isDown()) return;
		if (mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001) return;
		var box = mc.player.getBoundingBox().move(0, -0.5, 0).inflate(-0.001, 0, -0.001);
		if (mc.level.noCollision(mc.player, box)) mc.player.jumpFromGround();
	}
}
