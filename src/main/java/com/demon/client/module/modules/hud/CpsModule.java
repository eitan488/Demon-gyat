package com.demon.client.module.modules.hud;

import com.demon.client.combat.CombatFx;
import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class CpsModule extends HudPanelModule {
	public CpsModule() {
		super("CPS", "Clicks per second", "keyboard", HudLayout.Anchor.TOP_LEFT);
	}

	@Override
	protected float[] size() {
		return new float[] {HdFont.BOLD.width(text(), 6f) + 14, 11};
	}

	private String text() {
		return CombatFx.cps(true) + " | " + CombatFx.cps(false) + " CPS";
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		Render2D.textMid(g, HdFont.BOLD, text(), x + 7, y, h, 6f, Theme.TEXT);
	}
}
