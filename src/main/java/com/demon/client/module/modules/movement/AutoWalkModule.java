package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;

/** Keeps walking forward (long highway / hunting trips). */
public class AutoWalkModule extends Module {
	public AutoWalkModule() {
		super("Auto Walk", "Walks forward for you", Category.MOVEMENT, -1, "movement");
	}

	@Override
	public void onTick() {
		if (mc.player != null && mc.screen == null) mc.options.keyUp.setDown(true);
	}

	@Override
	protected void onDisable() {
		mc.options.keyUp.setDown(false);
	}
}
