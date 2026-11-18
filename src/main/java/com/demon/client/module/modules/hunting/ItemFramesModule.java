package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/** Shows what every item frame holds (maps, gear, loot) through walls. */
public class ItemFramesModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance", 48, 8, 128, 8, "m"));

	public ItemFramesModule() {
		super("Item Frames", "Shows the item inside every item frame", Category.HUNTING, -1, "frame");
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (mc.level == null) return;
		double r2 = range.get() * range.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof ItemFrame f) || e.distanceToSqr(mc.player) > r2) continue;
			ItemStack stack = f.getItem();
			if (stack.isEmpty()) continue;
			float[] p = Render3D.toScreen(e.position().add(0, 0.6, 0), g.guiWidth(), g.guiHeight());
			if (p == null) continue;
			String name = stack.getHoverName().getString();
			float tw = HdFont.MEDIUM.width(name, 5.4f);
			float w = Math.max(22, tw + 10), h = 26;
			float x = p[0] - w / 2f, y = p[1] - h;
			Render2D.round(g, x, y, w, h, 4, 0xE0071021);
			Render2D.outline(g, x, y, w, h, 4, 0x80FCD34D);
			g.pose().pushMatrix();
			g.pose().translate(p[0] - 6, y + 2);
			g.pose().scale(0.75f, 0.75f);
			g.renderItem(stack, 0, 0);
			g.pose().popMatrix();
			Render2D.textCentered(g, HdFont.MEDIUM, name, p[0], y + 17.5f, 5.4f, Theme.TEXT);
		}
	}
}
