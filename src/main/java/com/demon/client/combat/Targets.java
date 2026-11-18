package com.demon.client.combat;

import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Target selection and rotation maths shared by combat modules. */
public final class Targets {
	private Targets() {}

	private static final Minecraft mc = Minecraft.getInstance();

	public enum Priority {
		DISTANCE("Distance"), HEALTH("Lowest Health"), ANGLE("Crosshair");

		final String t;

		Priority(String t) {
			this.t = t;
		}

		@Override
		public String toString() {
			return t;
		}
	}

	public static boolean valid(Entity e, boolean players, boolean hostiles, boolean animals) {
		if (!(e instanceof LivingEntity le) || e == mc.player || !e.isAlive() || le.getHealth() <= 0) return false;
		if (e instanceof ArmorStand) return false;
		if (e instanceof Player p) return players && !p.isSpectator() && !p.isCreative();
		if (e instanceof Enemy) return hostiles;
		if (e instanceof Animal) return animals;
		return false;
	}

	public static LivingEntity find(double range, Priority priority, Predicate<Entity> filter) {
		if (mc.level == null || mc.player == null) return null;
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		Vec3 eye = mc.player.getEyePosition();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!filter.test(e)) continue;
			double d = eye.distanceTo(closest(e));
			if (d > range) continue;
			LivingEntity le = (LivingEntity) e;
			double score = switch (priority) {
				case DISTANCE -> d;
				case HEALTH -> le.getHealth() + le.getAbsorptionAmount();
				case ANGLE -> angleTo(e);
			};
			if (score < bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}

	/** Closest point of the entity's hitbox to the player's eyes. */
	public static Vec3 closest(Entity e) {
		Vec3 eye = mc.player.getEyePosition();
		var b = e.getBoundingBox();
		return new Vec3(Mth.clamp(eye.x, b.minX, b.maxX), Mth.clamp(eye.y, b.minY, b.maxY), Mth.clamp(eye.z, b.minZ, b.maxZ));
	}

	public static float[] rotationTo(Vec3 target) {
		Vec3 eye = mc.player.getEyePosition();
		double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
		double h = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
		return new float[] {yaw, pitch};
	}

	public static double angleTo(Entity e) {
		float[] r = rotationTo(e.getBoundingBox().getCenter());
		float dy = Mth.wrapDegrees(r[0] - mc.player.getYRot());
		float dp = r[1] - mc.player.getXRot();
		return Math.sqrt(dy * dy + dp * dp);
	}

	/** Smoothly turns the player's view toward a point; speed 0..1 (1 = instant). */
	public static void lookAt(Vec3 target, float speed) {
		float[] r = rotationTo(target);
		float yaw = mc.player.getYRot() + Mth.wrapDegrees(r[0] - mc.player.getYRot()) * speed;
		float pitch = Mth.lerp(speed, mc.player.getXRot(), r[1]);
		mc.player.setYRot(yaw);
		mc.player.setXRot(Mth.clamp(pitch, -90, 90));
	}

	public static Comparator<Entity> byDistance() {
		return Comparator.comparingDouble(e -> e.distanceToSqr(mc.player));
	}
}
