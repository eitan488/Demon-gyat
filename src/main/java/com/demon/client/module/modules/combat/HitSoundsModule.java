package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Sounds;
import net.minecraft.sounds.SoundEvents;

/** Plays a crisp confirmation sound when your hits land (and a different one on kills). */
public class HitSoundsModule extends Module {
	public enum Sound {
		DING("Ding"), PLING("Pling"), CLICK("Click"), CHIME("Chime");

		final String t;

		Sound(String t) {
			this.t = t;
		}

		@Override
		public String toString() {
			return t;
		}
	}

	public final EnumSetting<Sound> sound = add(new EnumSetting<>("Sound", "Hit sound", Sound.DING));
	public final NumberSetting pitch = add(new NumberSetting("Pitch", "Sound pitch", 1.6, 0.5, 2.0, 0.1));
	private long lastHit;

	public HitSoundsModule() {
		super("Hit Sounds", "Sound feedback when your hits land", Category.PVP, -1, "bolt");
	}

	@Override
	public void onTick() {
		if (CombatFx.lastHitTime == lastHit) return;
		lastHit = CombatFx.lastHitTime;
		float p = pitch.asFloat();
		if (CombatFx.lastHitKill) {
			Sounds.play(SoundEvents.NOTE_BLOCK_CHIME, 2f, 1f);
			return;
		}
		switch (sound.get()) {
			case DING -> Sounds.play(SoundEvents.NOTE_BLOCK_BELL, p, 0.8f);
			case PLING -> Sounds.play(SoundEvents.NOTE_BLOCK_PLING, p, 0.8f);
			case CLICK -> Sounds.play(SoundEvents.NOTE_BLOCK_HAT, p, 1f);
			case CHIME -> Sounds.play(SoundEvents.AMETHYST_BLOCK_CHIME, p, 1f);
		}
	}
}
