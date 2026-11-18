package com.demon.client.hud;

import net.minecraft.client.Minecraft;

/** Stacks HUD panels per screen corner so they never overlap. Reset every frame. */
public final class HudLayout {
	private HudLayout() {}

	public enum Anchor {
		TOP_LEFT("Top Left"), TOP_RIGHT("Top Right"), BOTTOM_LEFT("Bottom Left"), BOTTOM_RIGHT("Bottom Right"),
		TOP_CENTER("Top Center"), BOTTOM_CENTER("Bottom Center"), LEFT_CENTER("Left Middle"), RIGHT_CENTER("Right Middle");

		final String title;

		Anchor(String t) {
			title = t;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public static final float MARGIN = 5, GAP = 3;
	private static final float[] USED = new float[Anchor.values().length];
	private static int sw, sh;

	public static void reset(int width, int height) {
		sw = width;
		sh = height;
		java.util.Arrays.fill(USED, MARGIN);
		boolean chat = Minecraft.getInstance().gui.getChat().isChatFocused();
		USED[Anchor.BOTTOM_LEFT.ordinal()] = chat ? 30 : MARGIN;
		USED[Anchor.BOTTOM_CENTER.ordinal()] = 62;
		USED[Anchor.TOP_CENTER.ordinal()] = MARGIN;
		USED[Anchor.LEFT_CENTER.ordinal()] = 0;
		USED[Anchor.RIGHT_CENTER.ordinal()] = 0;
	}

	/** Reserves a w x h box at the anchor and returns its top-left corner. */
	public static float[] place(Anchor a, float w, float h) {
		float used = USED[a.ordinal()];
		float x, y;
		switch (a) {
			case TOP_LEFT -> { x = MARGIN; y = used; }
			case TOP_RIGHT -> { x = sw - MARGIN - w; y = used; }
			case BOTTOM_LEFT -> { x = MARGIN; y = sh - used - h; }
			case BOTTOM_RIGHT -> { x = sw - MARGIN - w; y = sh - used - h; }
			case TOP_CENTER -> { x = (sw - w) / 2f; y = used; }
			case BOTTOM_CENTER -> { x = (sw - w) / 2f; y = sh - used - h; }
			case LEFT_CENTER -> { x = MARGIN; y = sh * 0.38f + used; }
			default -> { x = sw - MARGIN - w; y = sh * 0.38f + used; }
		}
		USED[a.ordinal()] = used + h + GAP;
		return new float[] {x, y};
	}

	/** Reserve space without drawing (e.g. a list whose height is only known after drawing). */
	public static void reserve(Anchor a, float h) {
		USED[a.ordinal()] += h + GAP;
	}

	public static float used(Anchor a) {
		return USED[a.ordinal()];
	}
}
