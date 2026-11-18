package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.hud.HudLayout;
import com.demon.client.module.Category;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Session kills, deaths, K/D, streaks and damage dealt. */
public class CombatStatsModule extends HudPanelModule {
	public CombatStatsModule() {
		super("Combat Stats", "Kills, deaths, K/D and streaks this session", Category.PVP, "chart", HudLayout.Anchor.LEFT_CENTER);
	}

	@Override
	protected float[] size() {
		return new float[] {92, 47};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		Render2D.text(g, HdFont.BOLD, "COMBAT", x + 6, y + 5.5f, 5.8f, Theme.TEXT);
		float kd = CombatFx.deaths == 0 ? CombatFx.kills : CombatFx.kills / (float) CombatFx.deaths;
		row(g, x, y + 15, w, "Kills / Deaths", CombatFx.kills + " / " + CombatFx.deaths, Theme.TEXT);
		row(g, x, y + 23, w, "K/D", String.format("%.2f", kd), Theme.accent2());
		row(g, x, y + 31, w, "Streak (best)", CombatFx.streak + " (" + CombatFx.bestStreak + ")", 0xFFFBBF24);
		row(g, x, y + 39, w, "Damage dealt", String.format("%.0f", CombatFx.damageDealt / 2f) + " HP", 0xFFF87171);
	}

	private static void row(GuiGraphics g, float x, float y, float w, String k, String v, int c) {
		Render2D.text(g, HdFont.MEDIUM, k, x + 6, y, 5.4f, Theme.SUBTEXT);
		Render2D.textRight(g, HdFont.BOLD, v, x + w - 6, y, 5.4f, c);
	}
}
