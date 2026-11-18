package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;

/** Always sprint when moving forward (like toggle sprint). */
public class SprintModule extends Module {
	public SprintModule() {
		super("Sprint", "Always sprint while walking forward", Category.MOVEMENT, -1, "movement");
	}

	@Override
	public void onTick() {
		if (mc.player != null && mc.options.keyUp.isDown()) mc.options.keySprint.setDown(true);
	}

	@Override
	protected void onDisable() {
		mc.options.keySprint.setDown(false);
	}
}
