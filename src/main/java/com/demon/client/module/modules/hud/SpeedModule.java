package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class SpeedModule extends HudPanelModule {
	private final Anim.Value speed = new Anim.Value(0, 6);

	public SpeedModule() {
		super("Speed", "Your speed in blocks per second and km/h", "movement", HudLayout.Anchor.BOTTOM_LEFT);
	}

	private String text() {
		double bps = mc.player.getDeltaMovement().horizontalDistance() * 20;
		if (mc.player.getVehicle() != null) bps = mc.player.getVehicle().getDeltaMovement().horizontalDistance() * 20;
		float s = speed.target((float) bps).get();
		return String.format("%.1f b/s   %.0f km/h", s, s * 3.6f);
	}

	@Override
	protected float[] size() {
		return new float[] {HdFont.BOLD.width(text(), 6f) + 14, 11};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		Render2D.textMid(g, HdFont.BOLD, text(), x + 7, y, h, 6f, Theme.accent2());
	}
}
