package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import net.minecraft.util.Mth;

/** Snaps your facing to the nearest angle step so you travel in perfectly straight lines. */
public class YawLockModule extends Module {
	public final NumberSetting step = add(new NumberSetting("Step", "Snap angle", 45, 15, 90, 15, "°"));

	public YawLockModule() {
		super("Yaw Lock", "Locks your direction to 45° steps for straight travel", Category.MOVEMENT, -1, "compass");
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		float s = step.asFloat();
		float target = Math.round(mc.player.getYRot() / s) * s;
		mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(target - mc.player.getYRot()) * 0.5f);
	}
}
