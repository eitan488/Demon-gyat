package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.EnumSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Cosmetic particle trail behind you (only you can see it). */
public class TrailsModule extends Module {
	public enum Kind {
		SPARKS("Sparks"), FLAMES("Soul Flames"), MAGIC("Magic"), HEARTS("Hearts"), END_ROD("Stars");

		final String t;

		Kind(String t) {
			this.t = t;
		}

		@Override
		public String toString() {
			return t;
		}
	}

	public final EnumSetting<Kind> kind = add(new EnumSetting<>("Particle", "Trail particle", Kind.END_ROD));

	public TrailsModule() {
		super("Trails", "Cosmetic particle trail behind you", Category.RENDER, -1, "sparkle");
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001) return;
		ParticleOptions p = switch (kind.get()) {
			case SPARKS -> ParticleTypes.ELECTRIC_SPARK;
			case FLAMES -> ParticleTypes.SOUL_FIRE_FLAME;
			case MAGIC -> ParticleTypes.WITCH;
			case HEARTS -> ParticleTypes.HEART;
			case END_ROD -> ParticleTypes.END_ROD;
		};
		var pos = mc.player.position();
		for (int i = 0; i < 2; i++) {
			mc.level.addParticle(p, pos.x + (Math.random() - 0.5) * 0.4, pos.y + 0.1 + Math.random() * 0.3, pos.z + (Math.random() - 0.5) * 0.4, 0, 0.01, 0);
		}
	}
}
