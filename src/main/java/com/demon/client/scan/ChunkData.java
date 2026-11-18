package com.demon.client.scan;

import com.demon.client.util.Anim;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Scan result for one chunk: counts per target plus the position of every interesting block. */
public final class ChunkData {
	public static final int MAX_HITS = 8192;

	public final int x, z;
	public final int[] blockCounts = new int[Target.VALUES.length];
	public final int[] entityCounts = new int[Target.VALUES.length];

	private long[] positions = new long[64];
	private byte[] kinds = new byte[64];
	private int size;

	public int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
	public double stashScore, farmScore;

	/** Persistent between rescans of the same chunk. */
	public long firstSeen = System.currentTimeMillis();
	public boolean announced;
	public final Anim.Value appear = new Anim.Value(0, 5);

	public ChunkData(int x, int z) {
		this.x = x;
		this.z = z;
	}

	void add(int bx, int by, int bz, Target t) {
		blockCounts[t.ordinal()]++;
		if (by < minY) minY = by;
		if (by > maxY) maxY = by;
		if (size >= MAX_HITS) return;
		if (size == positions.length) {
			positions = java.util.Arrays.copyOf(positions, size * 2);
			kinds = java.util.Arrays.copyOf(kinds, size * 2);
		}
		positions[size] = BlockPos.asLong(bx, by, bz);
		kinds[size] = (byte) t.ordinal();
		size++;
	}

	public int hits() {
		return size;
	}

	public long pos(int i) {
		return positions[i];
	}

	public Target kind(int i) {
		return Target.VALUES[kinds[i]];
	}

	public int count(Target t) {
		return blockCounts[t.ordinal()] + entityCounts[t.ordinal()];
	}

	public int total() {
		int n = 0;
		for (Target t : Target.VALUES) n += count(t);
		return n;
	}

	public void recompute() {
		double s = 0, f = 0;
		for (Target t : Target.VALUES) {
			int c = count(t);
			if (c == 0) continue;
			s += c * t.stashWeight;
			f += c * t.farmWeight;
		}
		stashScore = s;
		farmScore = f;
	}

	/** Targets present in this chunk, most numerous first. */
	public List<Target> sortedTargets() {
		List<Target> list = new ArrayList<>();
		for (Target t : Target.VALUES) if (count(t) > 0) list.add(t);
		list.sort((a, b) -> Integer.compare(count(b), count(a)));
		return list;
	}

	public double centerX() {
		return (x << 4) + 8;
	}

	public double centerZ() {
		return (z << 4) + 8;
	}

	public double distanceSq(double px, double pz) {
		double dx = centerX() - px, dz = centerZ() - pz;
		return dx * dx + dz * dz;
	}

	/** Copies "sticky" state (first seen / announced) from the previous scan of this chunk. */
	void inherit(ChunkData old) {
		if (old == null) return;
		firstSeen = old.firstSeen;
		announced = old.announced;
		appear.snap(old.appear.peek());
		System.arraycopy(old.entityCounts, 0, entityCounts, 0, entityCounts.length);
	}
}
