package com.demon.client.module.modules.hunting;

import com.demon.client.gui.WorldLabel;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.scan.Target;
import com.demon.client.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Highlights chunks with player trail blocks (torches, lanterns, crafting tables, obsidian). */
public class TrailFinderModule extends Module {
	public final NumberSetting threshold = add(new NumberSetting("Min Blocks", "Trail blocks needed to mark a chunk", 4, 1, 64, 1));
	private static final int COLOR = 0xFF4ADE80;
	private final List<ChunkData> trails = new ArrayList<>();
	private int timer;

	public TrailFinderModule() {
		super("Trail Finder", "Marks chunks with torches, lanterns, crafting tables or obsidian", Category.HUNTING, -1, "compass");
	}

	@Override
	public void onTick() {
		if (timer++ % 10 != 0) return;
		trails.clear();
		for (ChunkData d : ChunkScanner.chunks()) if (d.trailCount() >= threshold.asInt()) trails.add(d);
	}

	@Override
	public String info() {
		return String.valueOf(trails.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.player == null) return;
		double y = Math.floor(mc.player.getY()) - 0.9;
		for (ChunkData d : trails) {
			double x1 = d.x << 4, z1 = d.z << 4;
			r.plane(x1 + 1, z1 + 1, x1 + 15, z1 + 15, y, 0x304ADE80);
			r.line(x1 + 1, y, z1 + 1, x1 + 15, y, z1 + 1, 0xC04ADE80, 1.5f);
			r.line(x1 + 15, y, z1 + 1, x1 + 15, y, z1 + 15, 0xC04ADE80, 1.5f);
			r.line(x1 + 15, y, z1 + 15, x1 + 1, y, z1 + 15, 0xC04ADE80, 1.5f);
			r.line(x1 + 1, y, z1 + 15, x1 + 1, y, z1 + 1, 0xC04ADE80, 1.5f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		Vec3 cam = Render3D.cameraPos();
		for (ChunkData d : trails) {
			if (d.distanceSq(cam.x, cam.z) > 96 * 96) continue;
			StringBuilder sb = new StringBuilder();
			for (Target t : new Target[] {Target.TORCH, Target.LANTERN, Target.CRAFTING_TABLE, Target.OBSIDIAN}) {
				if (d.count(t) == 0) continue;
				if (sb.length() > 0) sb.append("  ");
				sb.append(d.count(t)).append(' ').append(t.title);
			}
			WorldLabel.draw(g, new Vec3(d.centerX(), mc.player.getY() + 1.5, d.centerZ()), "TRAIL", sb.toString(), COLOR, 0.9f);
		}
	}
}
