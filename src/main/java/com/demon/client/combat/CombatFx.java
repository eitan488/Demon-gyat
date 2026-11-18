package com.demon.client.combat;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Central combat state: attacks, health changes, kills, totem pops, clicks and server TPS.
 * Visual modules (damage numbers, hit markers, kill effects, target HUD) read from here.
 */
public final class CombatFx {
	private CombatFx() {}

	private static final Minecraft mc = Minecraft.getInstance();

	public record Damage(Vec3 pos, float amount, long time, boolean byMe, boolean heal) {}

	public static final List<Damage> DAMAGES = new ArrayList<>();
	public static final List<Consumer<LivingEntity>> KILL_LISTENERS = new ArrayList<>();
	public static final List<Consumer<Player>> POP_LISTENERS = new ArrayList<>();

	public static LivingEntity lastTarget;
	/** Session totals for the combat stats panel. */
	public static float damageDealt;
	public static int kills, deaths, streak, bestStreak;
	private static boolean wasDead;
	public static long lastAttackTime;
	public static long lastHitTime;
	public static boolean lastHitKill;
	public static final Int2IntOpenHashMap POPS = new Int2IntOpenHashMap();

	private static final Int2FloatOpenHashMap HEALTH = new Int2FloatOpenHashMap();
	private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

	// TPS estimation from world time packets
	private static long lastTimePacket = -1, lastGameTime = -1;
	private static float tps = 20f;

	public static void onAttack(Entity target) {
		if (target instanceof LivingEntity le) {
			lastTarget = le;
			lastAttackTime = System.currentTimeMillis();
		}
	}

	public static void onClick(int button) {
		long now = System.currentTimeMillis();
		(button == 0 ? LEFT_CLICKS : RIGHT_CLICKS).addLast(now);
	}

	public static int cps(boolean left) {
		Deque<Long> q = left ? LEFT_CLICKS : RIGHT_CLICKS;
		long now = System.currentTimeMillis();
		while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
		return q.size();
	}

	public static void onTimePacket(long gameTime) {
		long now = System.nanoTime();
		if (lastTimePacket > 0 && gameTime > lastGameTime) {
			float seconds = (now - lastTimePacket) / 1e9f;
			if (seconds > 0.2f) {
				float measured = (gameTime - lastGameTime) / seconds;
				tps = Math.max(0, Math.min(20, tps * 0.6f + measured * 0.4f));
			}
		}
		lastTimePacket = now;
		lastGameTime = gameTime;
	}

	public static float tps() {
		return tps;
	}

	public static void onTotemPop(Entity e) {
		if (!(e instanceof Player p)) return;
		POPS.addTo(p.getId(), 1);
		for (Consumer<Player> l : POP_LISTENERS) l.accept(p);
	}

	public static boolean recentlyAttacked(Entity e, long ms) {
		return lastTarget == e && System.currentTimeMillis() - lastAttackTime < ms;
	}

	public static void tick() {
		if (mc.level == null || mc.player == null) {
			HEALTH.clear();
			DAMAGES.clear();
			lastTarget = null;
			return;
		}
		long now = System.currentTimeMillis();
		DAMAGES.removeIf(d -> now - d.time > 1400);
		Int2FloatOpenHashMap seen = new Int2FloatOpenHashMap();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || e.distanceToSqr(mc.player) > 48 * 48) continue;
			float hp = le.getHealth() + le.getAbsorptionAmount();
			seen.put(e.getId(), hp);
			if (!HEALTH.containsKey(e.getId())) continue;
			float before = HEALTH.get(e.getId());
			float diff = before - hp;
			if (Math.abs(diff) < 0.05f || e == mc.player && diff < 0) continue;
			boolean byMe = recentlyAttacked(e, 900);
			if (DAMAGES.size() < 64) DAMAGES.add(new Damage(e.position().add(0, e.getBbHeight() + 0.25, 0), Math.abs(diff), now, byMe, diff < 0));
			if (diff > 0 && byMe) {
				damageDealt += diff;
				lastHitTime = now;
				lastHitKill = le.getHealth() <= 0;
			}
			if (diff > 0 && le.getHealth() <= 0 && recentlyAttacked(e, 4000)) fireKill(le);
		}
		// entities that vanished right after we hit them at low health count as kills too
		for (var entry : HEALTH.int2FloatEntrySet()) {
			if (!seen.containsKey(entry.getIntKey()) && lastTarget != null && lastTarget.getId() == entry.getIntKey()
				&& !lastTarget.isAlive() && recentlyAttacked(lastTarget, 4000) && entry.getFloatValue() <= 8f) {
				fireKill(lastTarget);
			}
		}
		HEALTH.clear();
		HEALTH.putAll(seen);
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead) {
			deaths++;
			streak = 0;
			POPS.clear();
		}
		wasDead = dead;
	}

	private static LivingEntity lastKill;

	private static void fireKill(LivingEntity e) {
		if (lastKill == e) return;
		lastKill = e;
		lastHitKill = true;
		if (e instanceof Player) {
			kills++;
			streak++;
			bestStreak = Math.max(bestStreak, streak);
		}
		for (Consumer<LivingEntity> l : KILL_LISTENERS) l.accept(e);
	}
}
