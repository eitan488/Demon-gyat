package com.demon.client.gui;

import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** HD pill labels anchored to world positions (drawn in the overlay pass). */
public final class WorldLabel {
	private WorldLabel() {}

	/** Draws a label centred above pos; returns false if it is behind the camera. */
	public static boolean draw(GuiGraphics g, Vec3 pos, String title, String sub, int color, float alpha) {
		float[] s = Render3D.toScreen(pos, g.guiWidth(), g.guiHeight());
		if (s == null) return false;
		float ts = 6f, ss = 5.2f;
		float tw = HdFont.SEMIBOLD.width(title, ts);
		float sw = sub == null ? 0 : HdFont.MEDIUM.width(sub, ss);
		float w = Math.max(tw, sw) + 14;
		float h = sub == null ? 10 : 17;
		float x = s[0] - w / 2f, y = s[1] - h - 3;
		Render2D.round(g, x, y, w, h, 4, Theme.alpha(0xE6071021, alpha));
		Render2D.outline(g, x, y, w, h, 4, Theme.alpha(color, 0.55f * alpha));
		Render2D.circle(g, x + 5, y + 5, 1.7f, Theme.alpha(color, alpha));
		HdFont.SEMIBOLD.draw(g, title, x + 9, y + (10 - HdFont.SEMIBOLD.capHeight(ts)) / 2f, ts, Theme.alpha(Theme.TEXT, alpha));
		if (sub != null) HdFont.MEDIUM.draw(g, sub, x + 9, y + 10.5f, ss, Theme.alpha(Theme.SUBTEXT, alpha));
		Render2D.circle(g, s[0], s[1], 1.2f, Theme.alpha(color, alpha));
		return true;
	}

	public static String distance(Vec3 a, Vec3 b) {
		double d = a.distanceTo(b);
		return d >= 1000 ? String.format("%.1fkm", d / 1000) : (int) d + "m";
	}
}
