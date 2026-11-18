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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Lit nether portals mean players were here. Highlights every portal in loaded chunks. */
public class PortalFinderModule extends Module {
	public final BoolSetting notify = add(new BoolSetting("Notify", "Toast when a new portal is found", true));
	private static final int COLOR = 0xFFB46CFF;
	private final List<AABB> portals = new ArrayList<>();
	private final LongOpenHashSet announced = new LongOpenHashSet();
	private int timer;

	public PortalFinderModule() {
		super("Portal Finder", "Highlights lit nether portals (player activity)", Category.HUNTING, -1, "portal");
	}

	@Override
	public void onTick() {
		if (timer++ % 10 != 0) return;
		portals.clear();
		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.count(Target.PORTAL) == 0) continue;
			int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
			for (int i = 0; i < d.hits(); i++) {
				if (d.kind(i) != Target.PORTAL) continue;
				long p = d.pos(i);
				minX = Math.min(minX, BlockPos.getX(p));
				minY = Math.min(minY, BlockPos.getY(p));
				minZ = Math.min(minZ, BlockPos.getZ(p));
				maxX = Math.max(maxX, BlockPos.getX(p));
				maxY = Math.max(maxY, BlockPos.getY(p));
				maxZ = Math.max(maxZ, BlockPos.getZ(p));
			}
			AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(0.05);
			portals.add(box);
			if (announced.add(ChunkPos.asLong(d.x, d.z)) && notify.on()) {
				Notifications.push("Portal found", (int) box.getCenter().x + ", " + (int) box.getCenter().z, COLOR, 3f);
				com.demon.client.util.Finds.add("PORTAL", COLOR, (int) box.getCenter().x, (int) box.minY, (int) box.getCenter().z, d.count(Target.PORTAL), "Lit nether portal");
				if (com.demon.client.module.ModuleManager.autoScreenshot != null) com.demon.client.module.ModuleManager.autoScreenshot.request();
			}
		}
	}

	@Override
	public String info() {
		return String.valueOf(portals.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		for (AABB b : portals) {
			r.filledBox(b, 0x40B46CFF);
			r.outlineBox(b, 0xE6B46CFF, 2.5f);
			Vec3 c = b.getCenter();
			r.line(c.x, b.maxY, c.z, c.x, b.maxY + 100, c.z, 0xCCB46CFF, 0x00B46CFF, 3f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		Vec3 cam = Render3D.cameraPos();
		for (AABB b : portals) {
			Vec3 c = b.getCenter();
			WorldLabel.draw(g, new Vec3(c.x, b.maxY + 0.8, c.z), "Nether Portal", WorldLabel.distance(c, cam) + "  ·  " + (int) c.x + ", " + (int) c.z, COLOR, 1f);
		}
	}
}
