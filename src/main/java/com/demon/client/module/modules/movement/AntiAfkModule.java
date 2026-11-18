package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import net.minecraft.world.InteractionHand;

/** Small random actions so servers don't kick you for being idle while you wait for scans. */
public class AntiAfkModule extends Module {
	public final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between actions", 30, 5, 300, 5, "s"));
	private int timer;

	public AntiAfkModule() {
		super("Anti AFK", "Swings and turns a little so you are not kicked for idling", Category.MOVEMENT, -1, "timer");
	}

	@Override
	public void onTick() {
		if (mc.player == null || timer++ < interval.asFloat() * 20) return;
		timer = 0;
		mc.player.swing(InteractionHand.MAIN_HAND);
		mc.player.setYRot(mc.player.getYRot() + (float) (Math.random() * 30 - 15));
		if (mc.player.onGround()) mc.player.jumpFromGround();
	}
}
