package com.demon.client.util;

import com.demon.client.module.modules.InterfaceModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** UI sound effects. Every effect respects the "GUI Sounds" setting. */
public final class Sounds {
	private Sounds() {}

	private static long lastHover;

	public static void play(SoundEvent sound, float pitch, float volume) {
		if (!InterfaceModule.soundsEnabled()) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume * InterfaceModule.soundVolume()));
	}

	public static void play(Holder<SoundEvent> sound, float pitch, float volume) {
		play(sound.value(), pitch, volume);
	}

	public static void click() {
		play(SoundEvents.UI_BUTTON_CLICK, 1.35f, 0.35f);
	}

	public static void hover() {
		long now = System.currentTimeMillis();
		if (now - lastHover < 45) return;
		lastHover = now;
		play(SoundEvents.NOTE_BLOCK_HAT, 1.9f, 0.12f);
	}

	public static void toggle(boolean on) {
		if (on) {
			play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.9f);
			play(SoundEvents.NOTE_BLOCK_PLING, 1.6f, 0.25f);
		} else {
			play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.75f, 0.7f);
			play(SoundEvents.NOTE_BLOCK_PLING, 0.8f, 0.18f);
		}
	}

	public static void open() {
		play(SoundEvents.BEACON_POWER_SELECT, 1.9f, 0.35f);
		play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
	}

	public static void close() {
		play(SoundEvents.UI_TOAST_OUT, 1.4f, 0.6f);
	}

	public static void tab() {
		play(SoundEvents.NOTE_BLOCK_BIT, 1.4f, 0.22f);
	}

	public static void tick(float pitch) {
		play(SoundEvents.NOTE_BLOCK_HAT, pitch, 0.18f);
	}

	public static void notify(boolean important) {
		if (important) {
			play(SoundEvents.NOTE_BLOCK_CHIME, 1.2f, 0.7f);
			play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8f, 0.8f);
		} else {
			play(SoundEvents.UI_TOAST_IN, 1.6f, 0.5f);
		}
	}
}
