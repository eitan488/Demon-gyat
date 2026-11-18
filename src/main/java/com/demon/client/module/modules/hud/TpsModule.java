package com.demon.client.module.modules.hud;

import com.demon.client.combat.CombatFx;
import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class TpsModule extends HudPanelModule {
	public TpsModule() {
		super("TPS", "Estimated server ticks per second (lag meter)", "chart", HudLayout.Anchor.TOP_LEFT);
	}

	@Override
	protected float[] size() {
		return new float[] {56, 11};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		float t = CombatFx.tps();
		int c = t > 18 ? 0xFF34D399 : t > 14 ? 0xFFFBBF24 : 0xFFF87171;
		Render2D.textMid(g, HdFont.MEDIUM, "TPS", x + 7, y, h, 5.8f, Theme.SUBTEXT);
		Render2D.textMid(g, HdFont.BOLD, String.format("%.1f", t), x + 24, y, h, 6f, c);
	}
}
