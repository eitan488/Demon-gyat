package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;

/** HD chunk grid around you; your current chunk glows. */
public class ChunkBordersModule extends Module {
	public final NumberSetting radius = add(new NumberSetting("Radius", "Chunks around you", 2, 1, 6, 1));

	public ChunkBordersModule() {
		super("Chunk Borders", "Shows chunk boundaries around you", Category.HUNTING, -1, "chunk");
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.player == null) return;
		int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
		double y = Math.floor(mc.player.getY()) + 0.02;
		int rad = radius.asInt();
		int grid = Theme.withAlpha(Theme.accent1(), 110);
		for (int dx = -rad; dx <= rad + 1; dx++) {
			double x = (pcx + dx) << 4;
			r.line(x, y, (pcz - rad) << 4, x, y, (pcz + rad + 1) << 4, grid, 1.2f);
		}
		for (int dz = -rad; dz <= rad + 1; dz++) {
			double z = (pcz + dz) << 4;
			r.line((pcx - rad) << 4, y, z, (pcx + rad + 1) << 4, y, z, grid, 1.2f);
		}
		double x1 = pcx << 4, z1 = pcz << 4;
		int glow = Theme.withAlpha(Theme.accent2(), 220);
		r.wall(x1, z1, x1 + 16, z1, y, y + 6, Theme.withAlpha(Theme.accent2(), 70), Theme.withAlpha(Theme.accent2(), 0));
		r.wall(x1 + 16, z1, x1 + 16, z1 + 16, y, y + 6, Theme.withAlpha(Theme.accent2(), 70), Theme.withAlpha(Theme.accent2(), 0));
		r.wall(x1 + 16, z1 + 16, x1, z1 + 16, y, y + 6, Theme.withAlpha(Theme.accent2(), 70), Theme.withAlpha(Theme.accent2(), 0));
		r.wall(x1, z1 + 16, x1, z1, y, y + 6, Theme.withAlpha(Theme.accent2(), 70), Theme.withAlpha(Theme.accent2(), 0));
		for (double[] c : new double[][] {{x1, z1}, {x1 + 16, z1}, {x1, z1 + 16}, {x1 + 16, z1 + 16}}) r.line(c[0], y, c[1], c[0], y + 6, c[1], glow, 2f);
	}
}
