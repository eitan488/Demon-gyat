package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.module.ModuleManager;
import com.demon.client.module.modules.ChunkFinderModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.scan.ChunkData;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Rotating radar with sweep animation: players, sus chunks and waypoints around you. */
public class RadarModule extends HudPanelModule {
	public final NumberSetting range = add(new NumberSetting("Range", "Blocks shown from the centre to the edge", 128, 32, 512, 16, "m"));
	public final NumberSetting size = add(new NumberSetting("Size", "Radar size", 70, 50, 120, 5));

	public RadarModule() {
		super("Radar", "Rotating radar of players, sus chunks and waypoints", "radar", HudLayout.Anchor.TOP_RIGHT);
	}

	@Override
	protected boolean background() {
		return false;
	}

	@Override
	protected float[] size() {
		return new float[] {size.asFloat(), size.asFloat()};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		float r = w / 2f, cx = x + r, cy = y + r;
		Render2D.shadow(g, x, y, w, h, 0x40000000);
		Render2D.circle(g, cx, cy, r, Theme.HUD_BG);
		Render2D.ring(g, cx, cy, r, 0.4f, 0x408CB8FF);
		Render2D.ring(g, cx, cy, r * 0.66f, 0.3f, 0x268CB8FF);
		Render2D.ring(g, cx, cy, r * 0.33f, 0.3f, 0x268CB8FF);
		Render2D.line(g, cx - r + 3, cy, cx + r - 3, cy, 0x1A8CB8FF);
		Render2D.line(g, cx, cy - r + 3, cx, cy + r - 3, 0x1A8CB8FF);
		// sweep animation
		double sweep = Anim.time() * 2.2;
		for (int i = 0; i < 18; i++) {
			double a = sweep - i * 0.035;
			Render2D.line(g, cx, cy, cx + (float) Math.cos(a) * (r - 1.5f), cy + (float) Math.sin(a) * (r - 1.5f), Theme.alpha(Theme.accent2(), 0.35f * (1 - i / 18f)));
		}
		float yaw = mc.player.getYRot();
		double px = mc.player.getX(), pz = mc.player.getZ();
		double scale = (r - 4) / range.get();
		if (ModuleManager.waypoints.isEnabled()) {
			for (Waypoints.Waypoint wp : Waypoints.here()) blip(g, cx, cy, r, wp.x + 0.5 - px, wp.z + 0.5 - pz, yaw, scale, wp.color, 1.8f, true);
		}
		if (ModuleManager.chunkFinder.isEnabled()) {
			for (ChunkData d : ModuleManager.chunkFinder.susChunks()) {
				ChunkFinderModule.Kind k = ModuleManager.chunkFinder.kind(d);
				blip(g, cx, cy, r, d.centerX() - px, d.centerZ() - pz, yaw, scale, k.color, 2.2f, true);
			}
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof Player p && p != mc.player) blip(g, cx, cy, r, p.getX() - px, p.getZ() - pz, yaw, scale, 0xFFF87171, 1.8f, false);
		}
		// you
		Render2D.circle(g, cx, cy, 2, 0xFFFFFFFF);
		double yr = Math.toRadians(yaw);
		Render2D.textCentered(g, HdFont.BOLD, "N", cx + (float) Math.sin(yr) * (r - 6), cy + (float) Math.cos(yr) * (r - 6) - 2, 5f, Theme.accent2());
	}

	private static void blip(GuiGraphics g, float cx, float cy, float r, double dx, double dz, float yaw, double scale, int color, float size, boolean clampEdge) {
		double rad = Math.toRadians(yaw);
		// rotate world offset so that your facing direction points up
		double rx = -(dx * Math.cos(rad) + dz * Math.sin(rad));
		double ry = -(-dx * Math.sin(rad) + dz * Math.cos(rad));
		double sx = rx * scale, sy = ry * scale;
		double len = Math.sqrt(sx * sx + sy * sy);
		if (len > r - 3) {
			if (!clampEdge) return;
			sx *= (r - 3) / len;
			sy *= (r - 3) / len;
		}
		Render2D.glow(g, cx + (float) sx, cy + (float) sy, size * 3, Theme.alpha(color, 0.35f));
		Render2D.circle(g, cx + (float) sx, cy + (float) sy, size, color);
	}
}
