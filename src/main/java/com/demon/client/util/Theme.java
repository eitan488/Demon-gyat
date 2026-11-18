package com.demon.client.util;

/** Colour palette for the whole client. All colours are ARGB ints. */
public final class Theme {
	private Theme() {}

	public enum Palette {
		DEMON("Demon", 0xFF8B5CF6, 0xFFEC4899),
		INFERNO("Inferno", 0xFFFF6A00, 0xFFEE0979),
		OCEAN("Ocean", 0xFF06B6D4, 0xFF6366F1),
		TOXIC("Toxic", 0xFFA3E635, 0xFF10B981),
		SAKURA("Sakura", 0xFFF9A8D4, 0xFFC084FC),
		FROST("Frost", 0xFFE0F2FE, 0xFF38BDF8),
		GOLD("Gold", 0xFFFDE68A, 0xFFF59E0B);

		public final String title;
		public final int a, b;

		Palette(String title, int a, int b) {
			this.title = title;
			this.a = a;
			this.b = b;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public static Palette palette = Palette.DEMON;

	public static final int BG = 0xFF0B0911;
	public static final int PANEL = 0xFF110E19;
	public static final int SIDEBAR = 0xFF0D0B14;
	public static final int CARD = 0xFF181524;
	public static final int CARD_HOVER = 0xFF221D33;
	public static final int CARD_ACTIVE = 0xFF1F1A2E;
	public static final int BORDER = 0x22FFFFFF;
	public static final int TEXT = 0xFFF4F1FF;
	public static final int SUBTEXT = 0xFFA39CBF;
	public static final int MUTED = 0xFF625B7D;
	public static final int TRACK = 0xFF2A2540;
	public static final int SUCCESS = 0xFF34D399;
	public static final int DANGER = 0xFFF87171;

	public static int accent1() {
		return palette.a;
	}

	public static int accent2() {
		return palette.b;
	}

	/** Gradient between the two accent colours at position t (0..1). */
	public static int accent(float t) {
		return lerp(palette.a, palette.b, Anim.clamp01(t));
	}

	/** Animated gradient that slowly waves over time; offset shifts the phase. */
	public static int wave(float offset) {
		float t = (float) (Math.sin(Anim.time() * 1.6 + offset * 2.4) * 0.5 + 0.5);
		return accent(t);
	}

	public static int lerp(int a, int b, float t) {
		t = Anim.clamp01(t);
		int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
		int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
		return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
	}

	/** Multiplies the alpha channel of a colour by a (0..1). */
	public static int alpha(int color, float a) {
		int al = (int) ((color >>> 24) * Anim.clamp01(a));
		return al << 24 | (color & 0xFFFFFF);
	}

	/** Replaces the alpha channel (0..255). */
	public static int withAlpha(int color, int alpha) {
		return (alpha & 255) << 24 | (color & 0xFFFFFF);
	}

	public static int brighten(int color, float amount) {
		return lerp(color, color | 0xFFFFFF, amount);
	}
}
