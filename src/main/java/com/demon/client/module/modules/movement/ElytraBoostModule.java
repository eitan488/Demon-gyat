package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.InvUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Uses a firework from your hotbar every few seconds while flying with an elytra. */
public class ElytraBoostModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds between rockets", 3, 1, 10, 0.5, "s"));
	private int timer;

	public ElytraBoostModule() {
		super("Elytra Boost", "Uses fireworks automatically while gliding", Category.MOVEMENT, -1, "movement");
	}

	@Override
	public void onTick() {
		if (mc.player == null || !mc.player.isFallFlying()) {
			timer = 0;
			return;
		}
		if (timer-- > 0) return;
		int slot = InvUtil.findHotbar(s -> s.is(Items.FIREWORK_ROCKET));
		if (slot < 0) return;
		int prev = InvUtil.selected();
		InvUtil.select(slot);
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		InvUtil.select(prev);
		timer = Math.round(delay.asFloat() * 20);
	}
}
