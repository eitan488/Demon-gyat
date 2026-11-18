package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.InvUtil;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorHudModule extends HudPanelModule {
	public ArmorHudModule() {
		super("Armor HUD", "Armor durability, held items and totem count", "shield", HudLayout.Anchor.BOTTOM_RIGHT);
	}

	@Override
	protected float[] size() {
		return new float[] {7 * 17 + 8, 27};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		ItemStack[] items = {mc.player.getItemBySlot(EquipmentSlot.HEAD), mc.player.getItemBySlot(EquipmentSlot.CHEST), mc.player.getItemBySlot(EquipmentSlot.LEGS),
			mc.player.getItemBySlot(EquipmentSlot.FEET), mc.player.getMainHandItem(), mc.player.getOffhandItem()};
		float ix = x + 5;
		for (ItemStack s : items) {
			Render2D.round(g, ix, y + 4, 15, 15, 3, 0x149CC4FF);
			if (!s.isEmpty()) {
				g.pose().pushMatrix();
				g.pose().translate(ix + 0.5f, y + 4.5f);
				g.pose().scale(0.875f, 0.875f);
				g.renderItem(s, 0, 0);
				g.pose().popMatrix();
				if (s.isDamageableItem()) {
					float f = 1 - s.getDamageValue() / (float) s.getMaxDamage();
					int c = f > 0.5f ? 0xFF34D399 : f > 0.2f ? 0xFFFBBF24 : 0xFFF87171;
					Render2D.capsule(g, ix + 1.5f, y + 21, 12, 1.4f, Theme.TRACK);
					Render2D.capsule(g, ix + 1.5f, y + 21, Math.max(1.4f, 12 * f), 1.4f, c);
				} else if (s.getCount() > 1) {
					Render2D.textCentered(g, HdFont.BOLD, String.valueOf(s.getCount()), ix + 7.5f, y + 20.5f, 4.8f, Theme.TEXT);
				}
			}
			ix += 17;
		}
		int totems = InvUtil.count(s -> s.is(Items.TOTEM_OF_UNDYING));
		Render2D.round(g, ix, y + 4, 15, 15, 3, totems > 0 ? 0x30FBBF24 : 0x149CC4FF);
		g.pose().pushMatrix();
		g.pose().translate(ix + 0.5f, y + 4.5f);
		g.pose().scale(0.875f, 0.875f);
		g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), 0, 0);
		g.pose().popMatrix();
		Render2D.textCentered(g, HdFont.BOLD, String.valueOf(totems), ix + 7.5f, y + 20.5f, 4.8f, totems > 0 ? 0xFFFBBF24 : Theme.MUTED);
	}
}
