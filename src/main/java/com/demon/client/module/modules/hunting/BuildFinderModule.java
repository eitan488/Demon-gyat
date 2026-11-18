package com.demon.client.module.modules.hunting;

import com.demon.client.gui.Notifications;
import com.demon.client.gui.WorldLabel;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.scan.Target;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Finds;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Finds player builds by unnatural blocks: concrete, glass, wool, quartz and valuable blocks. */
public class BuildFinderModule extends Module {
	public final NumberSetting threshold = add(new NumberSetting("Sensitivity", "Build points needed to mark a chunk (lower = more)", 24, 4, 300, 4, " pts"));
	public final BoolSetting notify = add(new BoolSetting("Notify", "Toast and save to Finds when a build is found", true));
	private static final int COLOR = 0xFFFB923C;
	private final List<ChunkData> builds = new ArrayList<>();
	private final LongOpenHashSet announced = new LongOpenHashSet();
	private Object level;
	private int timer;

	public BuildFinderModule() {
		super("Build Finder", "Finds builds made of concrete, glass, wool, quartz and gem blocks", Category.HUNTING, -1, "cube");
	}

	@Override
	public void onTick() {
		if (timer++ % 10 != 0) return;
		if (mc.level != level) {
			level = mc.level;
			announced.clear();
		}
		builds.clear();
		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.buildScore() < threshold.get()) continue;
			builds.add(d);
			if (announced.add(ChunkPos.asLong(d.x, d.z)) && notify.on()) {
				String s = describe(d);
				Notifications.push("Build found", s + " • " + (int) d.centerX() + ", " + (int) d.centerZ(), COLOR, 3.5f);
				Finds.add("BUILD", COLOR, (int) d.centerX(), Math.max(0, d.maxY), (int) d.centerZ(), (int) d.buildScore(), s);
			}
		}
	}

	private static String describe(ChunkData d) {
		StringBuilder sb = new StringBuilder();
		for (Target t : new Target[] {Target.CONCRETE, Target.GLASS, Target.WOOL, Target.QUARTZ, Target.VALUABLE}) {
			if (d.count(t) == 0) continue;
			if (sb.length() > 0) sb.append(", ");
			sb.append(d.count(t)).append(' ').append(t == Target.VALUABLE ? "gem blocks" : t.title.toLowerCase());
		}
		return sb.toString();
	}

	@Override
	public String info() {
		return String.valueOf(builds.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		for (ChunkData d : builds) {
			double x1 = d.x << 4, z1 = d.z << 4;
			double y1 = mc.player.getY() - 1, y2 = y1 + 6;
			r.wall(x1, z1, x1 + 16, z1, y1, y2, 0x60FB923C, 0x00FB923C);
			r.wall(x1 + 16, z1, x1 + 16, z1 + 16, y1, y2, 0x60FB923C, 0x00FB923C);
			r.wall(x1 + 16, z1 + 16, x1, z1 + 16, y1, y2, 0x60FB923C, 0x00FB923C);
			r.wall(x1, z1 + 16, x1, z1, y1, y2, 0x60FB923C, 0x00FB923C);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		Vec3 cam = Render3D.cameraPos();
		for (ChunkData d : builds) {
			Vec3 c = new Vec3(d.centerX(), mc.player.getY() + 3, d.centerZ());
			WorldLabel.draw(g, c, "BUILD · " + (int) d.buildScore() + " pts", describe(d) + "  ·  " + WorldLabel.distance(c, cam), COLOR, 1f);
		}
	}
}
