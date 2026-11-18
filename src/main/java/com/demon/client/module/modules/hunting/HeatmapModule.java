package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;

/** Colours every scanned chunk by player activity (storage, redstone, trails, builds): blue -> red. */
public class HeatmapModule extends Module {
	public final NumberSetting opacity = add(new NumberSetting("Opacity", "Tile opacity", 35, 5, 90, 5, "%"));
	public final NumberSetting height = add(new NumberSetting("Height", "Render height relative to your feet", -1, -64, 64, 1));
	private static final int[] GRADIENT = {0xFF1D4ED8, 0xFF06B6D4, 0xFF22C55E, 0xFFFACC15, 0xFFF97316, 0xFFEF4444};

	public HeatmapModule() {
		super("Activity Heatmap", "Colours chunks by how much player activity they show", Category.HUNTING, -1, "chart");
	}

	public static double activity(ChunkData d) {
		return d.stashScore + d.farmScore + d.trailCount() * 0.4 + d.buildScore() * 0.25;
	}

	private static int color(float t) {
		t = Math.max(0, Math.min(1, t)) * (GRADIENT.length - 1);
		int i = Math.min(GRADIENT.length - 2, (int) t);
		return Theme.lerp(GRADIENT[i], GRADIENT[i + 1], t - i);
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.player == null) return;
		double y = Math.floor(mc.player.getY()) + height.get();
		int alpha = (int) (opacity.get() * 2.55);
		for (ChunkData d : ChunkScanner.chunks()) {
			double a = activity(d);
			if (a < 1) continue;
			// log scale: 1 point is faint blue, ~200+ points is red
			float t = (float) (Math.log10(a) / Math.log10(200));
			int c = color(t);
			double x1 = d.x << 4, z1 = d.z << 4;
			r.plane(x1 + 0.5, z1 + 0.5, x1 + 15.5, z1 + 15.5, y, Theme.withAlpha(c, (int) (alpha * (0.4f + 0.6f * Math.min(1, t)))));
		}
	}
}
