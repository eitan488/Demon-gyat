package com.demon.client.scan;

import com.demon.client.module.ModuleManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Scans loaded chunks for interesting blocks. Work is spread over ticks with a small time budget
 * so even huge render distances never cause a lag spike.
 */
public final class ChunkScanner {
	private ChunkScanner() {}

	private static final Minecraft mc = Minecraft.getInstance();
	private static final Long2ObjectOpenHashMap<ChunkData> DATA = new Long2ObjectOpenHashMap<>();
	private static final LongArrayFIFOQueue QUEUE = new LongArrayFIFOQueue();
	private static final LongOpenHashSet QUEUED = new LongOpenHashSet();
	private static ClientLevel lastLevel;
	private static boolean wasActive;
	private static int ticks;
	private static int scannedTotal;

	public static void init() {
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (active()) enqueue(chunk.getPos().toLong());
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			long key = chunk.getPos().toLong();
			DATA.remove(key);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChunkScanner::clear));
	}

	public static boolean active() {
		return ModuleManager.chunkFinder.isEnabled() || ModuleManager.esp.isEnabled() || ModuleManager.scanNeeded();
	}

	public static void clear() {
		DATA.clear();
		QUEUE.clear();
		QUEUED.clear();
	}

	public static Collection<ChunkData> chunks() {
		return DATA.values();
	}

	public static ChunkData get(int cx, int cz) {
		return DATA.get(ChunkPos.asLong(cx, cz));
	}

	public static int queued() {
		return QUEUE.size();
	}

	public static int scannedTotal() {
		return scannedTotal;
	}

	private static void enqueue(long key) {
		if (QUEUED.add(key)) QUEUE.enqueue(key);
	}

	/** Queues every loaded chunk around the player, nearest first. */
	public static void rescanAll() {
		queueAround(false);
	}

	private static void queueAround(boolean onlyMissing) {
		if (mc.level == null || mc.player == null) return;
		int pcx = SectionPos.blockToSectionCoord(mc.player.getBlockX());
		int pcz = SectionPos.blockToSectionCoord(mc.player.getBlockZ());
		int r = Math.max(mc.options.getEffectiveRenderDistance(), 2) + 3;
		List<long[]> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				list.add(new long[] {ChunkPos.asLong(pcx + dx, pcz + dz), (long) dx * dx + (long) dz * dz});
			}
		}
		list.sort((a, b) -> Long.compare(a[1], b[1]));
		for (long[] e : list) {
			if (onlyMissing && DATA.containsKey(e[0])) continue;
			if (mc.level.getChunkSource().getChunk(ChunkPos.getX(e[0]), ChunkPos.getZ(e[0]), ChunkStatus.FULL, false) == null) continue;
			enqueue(e[0]);
		}
	}

	public static void tick() {
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			clear();
			lastLevel = level;
			wasActive = false;
		}
		boolean isActive = active();
		if (isActive && !wasActive) rescanAll();
		if (!isActive && wasActive) clear();
		wasActive = isActive;
		if (!isActive || level == null || mc.player == null) return;

		ticks++;
		// Re-check nearby chunks every two seconds to pick up placed / broken blocks.
		if (ticks % 40 == 0) {
			int pcx = SectionPos.blockToSectionCoord(mc.player.getBlockX());
			int pcz = SectionPos.blockToSectionCoord(mc.player.getBlockZ());
			for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) enqueue(ChunkPos.asLong(pcx + dx, pcz + dz));
		}
		// Catch chunks that loaded before the scanner was ready (only ones not scanned yet).
		if (ticks % 200 == 0) queueAround(true);

		long deadline = System.nanoTime() + (QUEUE.size() > 200 ? 6_000_000L : 3_000_000L);
		while (!QUEUE.isEmpty() && System.nanoTime() < deadline) {
			long key = QUEUE.dequeueLong();
			QUEUED.remove(key);
			int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
			LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
			if (chunk == null) {
				DATA.remove(key);
				continue;
			}
			scan(chunk, key);
		}

		if (ticks % 10 == 0) updateEntities(level);
		// drop chunks the client no longer has (teleports don't always fire unload events)
		if (ticks % 20 == 0) {
			DATA.long2ObjectEntrySet().removeIf(e -> level.getChunkSource().getChunk(ChunkPos.getX(e.getLongKey()), ChunkPos.getZ(e.getLongKey()), ChunkStatus.FULL, false) == null);
		}
	}

	private static void scan(LevelChunk chunk, long key) {
		ChunkPos pos = chunk.getPos();
		ChunkData data = new ChunkData(pos.x, pos.z);
		int baseX = pos.getMinBlockX(), baseZ = pos.getMinBlockZ();
		LevelChunkSection[] sections = chunk.getSections();
		int minSection = chunk.getMinSectionY();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir()) continue;
			if (!section.maybeHas(s -> Target.of(s) != null)) continue;
			int baseY = (minSection + i) << 4;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						Target t = Target.of(state);
						if (t == null) continue;
						if (t == Target.BED && state.getValue(BedBlock.PART) != BedPart.HEAD) continue;
						data.add(baseX + x, baseY + y, baseZ + z, t);
					}
				}
			}
		}
		ChunkData old = DATA.get(key);
		data.inherit(old);
		data.recompute();
		scannedTotal++;
		if (data.total() == 0 && old == null) {
			// keep an empty marker so we know the chunk was scanned, but avoid storing hit arrays
			DATA.put(key, data);
			return;
		}
		DATA.put(key, data);
	}

	private static void updateEntities(ClientLevel level) {
		for (ChunkData d : DATA.values()) java.util.Arrays.fill(d.entityCounts, 0);
		for (Entity e : level.entitiesForRendering()) {
			Target t = Target.of(e);
			if (t == null) continue;
			ChunkData d = DATA.get(ChunkPos.asLong(SectionPos.blockToSectionCoord(e.getBlockX()), SectionPos.blockToSectionCoord(e.getBlockZ())));
			if (d != null) d.entityCounts[t.ordinal()]++;
		}
		for (ChunkData d : DATA.values()) d.recompute();
	}
}
