package com.demon.client.module.modules.player;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Switches to the fastest hotbar tool while you mine. */
public class AutoToolModule extends Module {
	public AutoToolModule() {
		super("Auto Tool", "Picks the best hotbar tool for the block you mine", Category.PLAYER, -1, "settings");
	}

	@Override
	public void onTick() {
		if (mc.player == null || !mc.options.keyAttack.isDown()) return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		int best = -1;
		float bestSpeed = mc.player.getMainHandItem().getDestroySpeed(state);
		for (int i = 0; i < 9; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			float speed = s.getDestroySpeed(state);
			if (speed > bestSpeed + 0.01f) {
				bestSpeed = speed;
				best = i;
			}
		}
		if (best >= 0) mc.player.getInventory().setSelectedSlot(best);
	}
}
