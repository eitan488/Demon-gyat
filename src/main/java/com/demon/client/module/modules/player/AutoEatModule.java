package com.demon.client.module.modules.player;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Eater;
import com.demon.client.util.InvUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Eats food from your hotbar when you get hungry. */
public class AutoEatModule extends Module {
	public final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat at or below this food level", 14, 1, 19, 1));
	public final BoolSetting golden = add(new BoolSetting("Golden Apples", "Allow eating golden apples", false));

	public AutoEatModule() {
		super("Auto Eat", "Eats from your hotbar when you are hungry", Category.PLAYER, -1, "heart");
	}

	private boolean edible(ItemStack s) {
		if (s.isEmpty() || s.get(DataComponents.FOOD) == null) return false;
		if (!golden.on() && (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE))) return false;
		return !s.is(Items.ROTTEN_FLESH) && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO) && !s.is(Items.PUFFERFISH) && !s.is(Items.CHORUS_FRUIT);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		int food = mc.player.getFoodData().getFoodLevel();
		if (Eater.ownedByThis(this)) {
			if (food >= 20 || !edible(mc.player.getMainHandItem()) || mc.screen != null) Eater.stop(this);
			return;
		}
		if (food > hunger.asInt() || Eater.busy() || mc.screen != null) return;
		int slot = InvUtil.findHotbar(this::edible);
		if (slot >= 0) Eater.start(this, slot);
	}

	@Override
	protected void onDisable() {
		Eater.stop(this);
	}
}
