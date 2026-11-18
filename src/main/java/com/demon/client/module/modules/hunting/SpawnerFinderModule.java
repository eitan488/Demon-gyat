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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shows every spawner with the mob it spawns: dungeons and mob farms. */
public class SpawnerFinderModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance", 256, 32, 512, 16, "m"));
	private static final int COLOR = 0xFF818CF8;

	private record Spawner(BlockPos pos, String mob) {}

	private final List<Spawner> spawners = new ArrayList<>();
	private int timer;

	public SpawnerFinderModule() {
		super("Spawner Finder", "Highlights spawners and shows which mob they spawn", Category.HUNTING, -1, "cube");
	}

	@Override
	public void onTick() {
		if (mc.level == null || timer++ % 20 != 0) return;
		spawners.clear();
		double r2 = range.get() * range.get();
		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.count(Target.SPAWNER) == 0) continue;
			for (int i = 0; i < d.hits(); i++) {
				if (d.kind(i) != Target.SPAWNER) continue;
				BlockPos p = BlockPos.of(d.pos(i));
				if (p.distToCenterSqr(mc.player.position()) > r2) continue;
				String mob = "Spawner";
				if (mc.level.getBlockEntity(p) instanceof SpawnerBlockEntity sbe) {
					Entity e = sbe.getSpawner().getOrCreateDisplayEntity(mc.level, p);
					if (e != null) mob = e.getType().getDescription().getString() + " Spawner";
				}
				spawners.add(new Spawner(p, mob));
			}
		}
	}

	@Override
	public String info() {
		return String.valueOf(spawners.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		for (Spawner s : spawners) {
			AABB b = new AABB(s.pos).inflate(0.02);
			r.filledBox(b, 0x40818CF8);
			r.outlineBox(b, 0xE6818CF8, 2.2f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		Vec3 cam = Render3D.cameraPos();
		for (Spawner s : spawners) {
			Vec3 c = Vec3.atCenterOf(s.pos);
			WorldLabel.draw(g, c.add(0, 0.9, 0), s.mob, WorldLabel.distance(c, cam), COLOR, 1f);
		}
	}
}
