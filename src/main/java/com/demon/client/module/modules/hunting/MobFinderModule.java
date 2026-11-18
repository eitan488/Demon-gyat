package com.demon.client.module.modules.hunting;

import com.demon.client.gui.Notifications;
import com.demon.client.gui.WorldLabel;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Finds;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Mobs that only exist because a player is nearby: pets, name tags, tamed horses, trading halls, golem farms. */
public class MobFinderModule extends Module {
	public final NumberSetting hallSize = add(new NumberSetting("Trading Hall", "Villagers in one chunk to flag a trading hall", 6, 3, 30, 1));
	public final NumberSetting golemFarm = add(new NumberSetting("Golem Farm", "Iron golems in one chunk to flag a farm", 3, 2, 20, 1));
	public final BoolSetting notify = add(new BoolSetting("Notify", "Toast when something new is found", true));
	private static final int COLOR = 0xFFF472B6;

	private record Hit(Entity entity, String reason) {}

	private record Cluster(double x, double y, double z, String reason, int count) {}

	private final List<Hit> hits = new ArrayList<>();
	private final List<Cluster> clusters = new ArrayList<>();
	private final Set<UUID> announced = new HashSet<>();
	private final Set<Long> announcedClusters = new HashSet<>();
	private Object level;
	private int timer;

	public MobFinderModule() {
		super("Mob Finder", "Pets, name tags, tamed horses, trading halls and golem farms", Category.HUNTING, -1, "heart");
	}

	private static String reason(Entity e) {
		if (e instanceof Player) return null;
		if (e instanceof TamableAnimal t && t.isTame()) return "Tamed " + e.getType().getDescription().getString();
		if (e instanceof AbstractHorse h && h.isTamed()) return "Tamed " + e.getType().getDescription().getString();
		if (e.hasCustomName()) return "Named: " + e.getCustomName().getString();
		if (e instanceof Leashable l && l.isLeashed()) return "Leashed " + e.getType().getDescription().getString();
		return null;
	}

	@Override
	public void onTick() {
		if (mc.level == null || timer++ % 20 != 0) return;
		if (mc.level != level) {
			level = mc.level;
			announced.clear();
			announcedClusters.clear();
		}
		hits.clear();
		clusters.clear();
		Long2IntOpenHashMap villagers = new Long2IntOpenHashMap(), golems = new Long2IntOpenHashMap();
		for (Entity e : mc.level.entitiesForRendering()) {
			String r = reason(e);
			if (r != null) {
				hits.add(new Hit(e, r));
				if (announced.add(e.getUUID()) && notify.on()) Notifications.push("Mob Finder", r + " at " + e.getBlockX() + ", " + e.getBlockZ(), COLOR, 3f);
			}
			long key = ChunkPos.asLong(e.getBlockX() >> 4, e.getBlockZ() >> 4);
			if (e instanceof Villager) villagers.addTo(key, 1);
			if (e instanceof IronGolem) golems.addTo(key, 1);
		}
		cluster(villagers, hallSize.asInt(), "Trading hall?");
		cluster(golems, golemFarm.asInt(), "Iron golem farm?");
	}

	private void cluster(Long2IntOpenHashMap counts, int min, String reason) {
		for (var e : counts.long2IntEntrySet()) {
			if (e.getIntValue() < min) continue;
			int cx = ChunkPos.getX(e.getLongKey()), cz = ChunkPos.getZ(e.getLongKey());
			clusters.add(new Cluster((cx << 4) + 8, mc.player.getY() + 2, (cz << 4) + 8, reason, e.getIntValue()));
			if (announcedClusters.add(e.getLongKey() * 31 + reason.hashCode()) && notify.on()) {
				Notifications.push("Mob Finder", reason + " " + e.getIntValue() + " mobs at " + ((cx << 4) + 8) + ", " + ((cz << 4) + 8), COLOR, 3.5f);
				Finds.add("MOBS", COLOR, (cx << 4) + 8, (int) mc.player.getY(), (cz << 4) + 8, e.getIntValue(), reason + " (" + e.getIntValue() + ")");
			}
		}
	}

	@Override
	public String info() {
		return String.valueOf(hits.size() + clusters.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		float pt = r.partialTick();
		for (Hit h : hits) {
			Entity e = h.entity;
			var box = e.getBoundingBox().move(e.getPosition(pt).subtract(e.position())).inflate(0.05);
			r.filledBox(box, 0x30F472B6);
			r.outlineBox(box, 0xE0F472B6, 2f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		Vec3 cam = Render3D.cameraPos();
		float pt = delta.getGameTimeDeltaPartialTick(true);
		for (Hit h : hits) {
			Vec3 p = h.entity.getPosition(pt).add(0, h.entity.getBbHeight() + 0.5, 0);
			WorldLabel.draw(g, p, h.reason, WorldLabel.distance(p, cam), COLOR, 1f);
		}
		for (Cluster c : clusters) {
			Vec3 p = new Vec3(c.x, c.y, c.z);
			WorldLabel.draw(g, p, c.reason, c.count + " mobs  ·  " + WorldLabel.distance(p, cam), COLOR, 1f);
		}
	}
}
