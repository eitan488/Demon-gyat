package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.scan.ChunkData;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Game-style compass strip with markers for sus chunks and waypoints. */
public class CompassBarModule extends HudPanelModule {
	public CompassBarModule() {
		super("Compass Bar", "Compass strip with markers for finds and waypoints", "compass", HudLayout.Anchor.TOP_CENTER);
	}

	@Override
	protected float[] size() {
		return new float[] {180, 16};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		float yaw = Mth.wrapDegrees(mc.player.getYRot());
		float fov = 140;
		g.enableScissor((int) x + 2, (int) y, (int) (x + w - 2), (int) (y + h));
		String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		for (int deg = -180; deg < 180; deg += 15) {
			float rel = Mth.wrapDegrees(deg - yaw);
			if (Math.abs(rel) > fov / 2 + 10) continue;
			float px = x + w / 2f + rel / fov * w;
			float edge = 1 - Math.abs(rel) / (fov / 2);
			if (deg % 45 == 0) {
				String n = names[((deg + 180) / 45) % 8];
				int c = n.equals("N") ? Theme.accent2() : Theme.TEXT;
				Render2D.textCentered(g, HdFont.BOLD, n, px, y + 4.5f, n.length() > 1 ? 5f : 6.2f, Theme.alpha(c, 0.3f + 0.7f * edge));
			} else {
				Render2D.rect(g, px - 0.2f, y + 5, 0.4f, 4, Theme.alpha(Theme.SUBTEXT, 0.25f + 0.5f * edge));
			}
		}
		if (ModuleManager.chunkFinder.isEnabled()) {
			for (ChunkData d : ModuleManager.chunkFinder.susChunks()) marker(g, x, y, w, h, yaw, fov, d.centerX(), d.centerZ(), ModuleManager.chunkFinder.kind(d).color);
		}
		if (ModuleManager.waypoints.isEnabled()) {
			for (Waypoints.Waypoint wp : Waypoints.here()) marker(g, x, y, w, h, yaw, fov, wp.x + 0.5, wp.z + 0.5, wp.color);
		}
		g.disableScissor();
		Render2D.capsule(g, x + w / 2f - 0.4f, y + h - 3, 0.8f, 3, Theme.accent2());
	}

	private void marker(GuiGraphics g, float x, float y, float w, float h, float yaw, float fov, double tx, double tz, int color) {
		double dx = tx - mc.player.getX(), dz = tz - mc.player.getZ();
		float ang = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float rel = Mth.wrapDegrees(ang - yaw);
		if (Math.abs(rel) > fov / 2) return;
		float px = x + w / 2f + rel / fov * w;
		Render2D.glow(g, px, y + h - 4, 5, Theme.alpha(color, 0.5f));
		Render2D.circle(g, px, y + h - 4, 1.8f, color);
	}
}
