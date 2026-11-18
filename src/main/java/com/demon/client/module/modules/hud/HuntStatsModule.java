package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Base hunting session stats: time, chunks scanned, finds, distance travelled. */
public class HuntStatsModule extends HudPanelModule {
	private final long start = System.currentTimeMillis();
	private double travelled;
	private Vec3 last;
	private int finds;
	private int lastSus;

	public HuntStatsModule() {
		super("Hunt Stats", "Session time, chunks scanned, finds and distance", "chart", HudLayout.Anchor.TOP_LEFT);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		Vec3 p = mc.player.position();
		if (last != null) {
			double d = p.distanceTo(last);
			if (d < 50) travelled += d; // ignore teleports
		}
		last = p;
		int sus = ModuleManager.chunkFinder.susChunks().size();
		if (sus > lastSus) finds += sus - lastSus;
		lastSus = sus;
	}

	@Override
	protected float[] size() {
		return new float[] {100, 47};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		long m = (System.currentTimeMillis() - start) / 60000;
		Render2D.text(g, HdFont.BOLD, "HUNT", x + 6, y + 5.5f, 5.8f, Theme.TEXT);
		Render2D.textRight(g, HdFont.MEDIUM, (m / 60) + "h " + (m % 60) + "m", x + w - 6, y + 5.5f, 5.4f, Theme.SUBTEXT);
		row(g, x, y + 15, w, "Chunks scanned", String.valueOf(ChunkScanner.scannedTotal()));
		row(g, x, y + 23, w, "Finds", String.valueOf(finds));
		row(g, x, y + 31, w, "Distance", travelled > 1000 ? String.format("%.1f km", travelled / 1000) : (int) travelled + " m");
		row(g, x, y + 39, w, "Waypoints", String.valueOf(com.demon.client.util.Waypoints.here().size()));
	}

	private static void row(GuiGraphics g, float x, float y, float w, String k, String v) {
		Render2D.text(g, HdFont.MEDIUM, k, x + 6, y, 5.4f, Theme.SUBTEXT);
		Render2D.textRight(g, HdFont.BOLD, v, x + w - 6, y, 5.4f, Theme.accent2());
	}
}
