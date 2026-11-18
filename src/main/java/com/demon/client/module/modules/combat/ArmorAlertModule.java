package com.demon.client.module.modules.combat;

import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Warns once when a piece of your armor drops below a durability percentage. */
public class ArmorAlertModule extends Module {
	public final NumberSetting percent = add(new NumberSetting("Durability", "Warn below this durability", 20, 5, 60, 5, "%"));
	private final Set<EquipmentSlot> warned = EnumSet.noneOf(EquipmentSlot.class);

	public ArmorAlertModule() {
		super("Armor Alert", "Toast when your armor is about to break", Category.PVP, -1, "shield");
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack st = mc.player.getItemBySlot(s);
			if (st.isEmpty() || !st.isDamageableItem()) {
				warned.remove(s);
				continue;
			}
			float left = 1f - st.getDamageValue() / (float) st.getMaxDamage();
			if (left * 100 < percent.get()) {
				if (warned.add(s)) {
					Notifications.push("Armor breaking", st.getHoverName().getString() + " at " + Math.round(left * 100) + "%", Theme.DANGER, 4f);
					Sounds.play(SoundEvents.ANVIL_LAND, 1.6f, 0.4f);
				}
			} else {
				warned.remove(s);
			}
		}
	}
}
