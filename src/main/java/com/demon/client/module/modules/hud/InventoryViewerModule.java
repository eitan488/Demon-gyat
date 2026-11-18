package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class InventoryViewerModule extends HudPanelModule {
	public InventoryViewerModule() {
		super("Inventory Viewer", "Shows your main inventory on screen", "cube", HudLayout.Anchor.BOTTOM_RIGHT);
	}

	@Override
	protected float[] size() {
		return new float[] {9 * 15 + 8, 3 * 15 + 8};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		for (int i = 9; i < 36; i++) {
			int col = (i - 9) % 9, row = (i - 9) / 9;
			float sx = x + 4 + col * 15, sy = y + 4 + row * 15;
			Render2D.round(g, sx, sy, 14, 14, 2.5f, 0x149CC4FF);
			ItemStack s = mc.player.getInventory().getItem(i);
			if (s.isEmpty()) continue;
			g.pose().pushMatrix();
			g.pose().translate(sx + 0.5f, sy + 0.5f);
			g.pose().scale(0.8125f, 0.8125f);
			g.renderItem(s, 0, 0);
			g.renderItemDecorations(mc.font, s, 0, 0);
			g.pose().popMatrix();
		}
	}
}
