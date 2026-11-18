package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Replaces vanilla player name tags with HD tags: name, health, ping, distance and gear. */
public class NametagsModule extends Module {
	public final BoolSetting gear = add(new BoolSetting("Gear", "Show armor and held items", true));
	public final BoolSetting ping = add(new BoolSetting("Ping", "Show ping", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance", 64, 8, 128, 8, "m"));

	public NametagsModule() {
		super("Nametags", "HD player name tags with health, ping and gear", Category.RENDER, -1, "player");
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (mc.level == null) return;
		float pt = delta.getGameTimeDeltaPartialTick(true);
		double r2 = range.get() * range.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof Player p) || p == mc.player || p.isInvisible() || e.distanceToSqr(mc.player) > r2) continue;
			// like vanilla: sneaking players only show their tag when you can see them
			if (p.isDiscrete() && !mc.player.hasLineOfSight(p)) continue;
			float[] s = Render3D.toScreen(p.getPosition(pt).add(0, p.getBbHeight() + 0.45, 0), g.guiWidth(), g.guiHeight());
			if (s == null) continue;
			String name = p.getGameProfile().name();
			float hp = (p.getHealth() + p.getAbsorptionAmount()) / 2f;
			String hpText = String.format("%.1f", hp);
			int hc = hp > 12 ? 0xFF34D399 : hp > 6 ? 0xFFFBBF24 : 0xFFF87171;
			String extra = (int) p.distanceTo(mc.player) + "m";
			if (ping.on() && mc.getConnection() != null) {
				PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
				if (info != null) extra += "  " + info.getLatency() + "ms";
			}
			float size = 6.4f, small = 5.2f;
			float w = HdFont.SEMIBOLD.width(name, size) + 6 + HdFont.BOLD.width(hpText, size) + 6 + HdFont.MEDIUM.width(extra, small) + 12;
			float h = 11;
			float x = s[0] - w / 2f, y = s[1] - h;
			Render2D.round(g, x, y, w, h, 3.5f, 0xE6071021);
			Render2D.outline(g, x, y, w, h, 3.5f, Theme.alpha(hc, 0.6f));
			float tx = x + 6;
			tx += Render2D.text(g, HdFont.SEMIBOLD, name, tx, y + (h - HdFont.SEMIBOLD.capHeight(size)) / 2f, size, Theme.TEXT) + 6;
			tx += Render2D.text(g, HdFont.BOLD, hpText, tx, y + (h - HdFont.BOLD.capHeight(size)) / 2f, size, hc) + 6;
			Render2D.text(g, HdFont.MEDIUM, extra, tx, y + (h - HdFont.MEDIUM.capHeight(small)) / 2f, small, Theme.SUBTEXT);
			if (gear.on()) {
				ItemStack[] items = {p.getMainHandItem(), p.getItemBySlot(EquipmentSlot.HEAD), p.getItemBySlot(EquipmentSlot.CHEST),
					p.getItemBySlot(EquipmentSlot.LEGS), p.getItemBySlot(EquipmentSlot.FEET), p.getOffhandItem()};
				int n = 0;
				for (ItemStack it : items) if (!it.isEmpty()) n++;
				float ix = s[0] - n * 9 / 2f;
				for (ItemStack it : items) {
					if (it.isEmpty()) continue;
					g.pose().pushMatrix();
					g.pose().translate(ix, y - 10);
					g.pose().scale(0.55f, 0.55f);
					g.renderItem(it, 0, 0);
					g.renderItemDecorations(mc.font, it, 0, 0);
					g.pose().popMatrix();
					ix += 9;
				}
			}
		}
	}
}
