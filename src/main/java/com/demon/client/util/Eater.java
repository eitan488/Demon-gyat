package com.demon.client.util;

import net.minecraft.client.Minecraft;

/** Holds the use key on a hotbar item until stopped, then restores the previous slot. */
public final class Eater {
	private Eater() {}

	private static final Minecraft mc = Minecraft.getInstance();
	private static int prevSlot = -1;
	private static Object owner;

	public static boolean ownedByThis(Object o) {
		return owner == o;
	}

	public static boolean busy() {
		return owner != null;
	}

	public static void start(Object who, int hotbar) {
		if (owner == null) prevSlot = InvUtil.selected();
		owner = who;
		InvUtil.select(hotbar);
		mc.options.keyUse.setDown(true);
	}

	public static void stop(Object who) {
		if (owner != who) return;
		mc.options.keyUse.setDown(false);
		if (prevSlot >= 0 && mc.player != null) InvUtil.select(prevSlot);
		prevSlot = -1;
		owner = null;
	}
}
