package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionHudModule extends HudPanelModule {
	public PotionHudModule() {
		super("Potion HUD", "Active effects with time left", "sparkle", HudLayout.Anchor.RIGHT_CENTER);
	}

	private List<MobEffectInstance> effects() {
		return new ArrayList<>(mc.player.getActiveEffects());
	}

	@Override
	protected float[] size() {
		List<MobEffectInstance> list = effects();
		if (list.isEmpty()) return null;
		return new float[] {100, 6 + list.size() * 10};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		float ry = y + 4;
		for (MobEffectInstance e : effects()) {
			int color = 0xFF000000 | e.getEffect().value().getColor();
			String name = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
			String time;
			if (e.isInfiniteDuration()) time = "∞";
			else {
				int s = e.getDuration() / 20;
				time = s / 60 + ":" + String.format("%02d", s % 60);
			}
			Render2D.circle(g, x + 7, ry + 3.5f, 2, color);
			Render2D.text(g, HdFont.MEDIUM, Render2D.trim(HdFont.MEDIUM, name, 5.6f, w - 42), x + 12, ry + 1.5f, 5.6f, Theme.TEXT);
			Render2D.textRight(g, HdFont.BOLD, time, x + w - 6, ry + 1.5f, 5.6f, e.getDuration() < 200 && !e.isInfiniteDuration() ? 0xFFF87171 : Theme.SUBTEXT);
			ry += 10;
		}
	}
}
