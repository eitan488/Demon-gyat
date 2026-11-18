package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;

/**
 * Marks overworld chunks generated before 1.17. Copper ore exists since 1.17, so stone terrain between
 * y 0 and 96 without any copper ore is old - and old terrain is where old bases are.
 */
public class OldChunksModule extends Module {
	public final BoolSetting showNew = add(new BoolSetting("Show New", "Also tint newer chunks faintly", false));
	public final NumberSetting height = add(new NumberSetting("Height", "Render height relative to your feet", -1, -64, 64, 1));
	private int oldCount;

	public OldChunksModule() {
		super("Old Chunks", "Marks pre-1.17 terrain (no copper ore) where old bases are", Category.HUNTING, -1, "chunk");
	}

	@Override
	public String info() {
		return String.valueOf(oldCount);
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.player == null) return;
		double y = Math.floor(mc.player.getY()) + height.get();
		int count = 0;
		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.oldGen == 0 || d.oldGen == 2 && !showNew.on()) continue;
			boolean old = d.oldGen == 1;
			if (old) count++;
			int fill = old ? 0x38FBBF24 : 0x1834D399;
			int edge = old ? 0xC0FBBF24 : 0x4034D399;
			double x1 = d.x << 4, z1 = d.z << 4;
			r.plane(x1 + 0.4, z1 + 0.4, x1 + 15.6, z1 + 15.6, y, fill);
			r.line(x1 + 0.4, y, z1 + 0.4, x1 + 15.6, y, z1 + 0.4, edge, 1.3f);
			r.line(x1 + 15.6, y, z1 + 0.4, x1 + 15.6, y, z1 + 15.6, edge, 1.3f);
			r.line(x1 + 15.6, y, z1 + 15.6, x1 + 0.4, y, z1 + 15.6, edge, 1.3f);
			r.line(x1 + 0.4, y, z1 + 15.6, x1 + 0.4, y, z1 + 0.4, edge, 1.3f);
		}
		oldCount = count;
	}
}
