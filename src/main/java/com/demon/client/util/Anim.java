package com.demon.client.util;

/** Frame-rate independent easing and smoothing helpers. */
public final class Anim {
	private Anim() {}

	private static final long START = System.nanoTime();

	/** Seconds since the client started. */
	public static float time() {
		return (System.nanoTime() - START) / 1_000_000_000f;
	}

	public static float clamp01(float t) {
		return t < 0 ? 0 : (t > 1 ? 1 : t);
	}

	public static float clamp(float v, float min, float max) {
		return v < min ? min : (v > max ? max : v);
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	/** Maps v from [a,b] to [0,1] (clamped). */
	public static float progress(float v, float a, float b) {
		return clamp01((v - a) / (b - a));
	}

	public static float easeOutCubic(float t) {
		t = clamp01(t);
		float f = 1 - t;
		return 1 - f * f * f;
	}

	public static float easeInCubic(float t) {
		t = clamp01(t);
		return t * t * t;
	}

	public static float easeInOutCubic(float t) {
		t = clamp01(t);
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	public static float easeOutExpo(float t) {
		t = clamp01(t);
		return t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
	}

	public static float easeOutBack(float t) {
		t = clamp01(t);
		float c1 = 1.70158f, c3 = c1 + 1;
		float f = t - 1;
		return 1 + c3 * f * f * f + c1 * f * f;
	}

	public static float easeOutElastic(float t) {
		t = clamp01(t);
		if (t == 0 || t == 1) return t;
		float c4 = (float) (2 * Math.PI / 3);
		return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
	}

	/** Global animation speed multiplier (set from the Interface module). */
	public static float speed = 1f;

	/** An exponentially smoothed value that chases a target, independent of frame rate. */
	public static final class Value {
		private float value;
		private float target;
		private final float rate;
		private long last = System.nanoTime();

		public Value(float initial, float rate) {
			this.value = initial;
			this.target = initial;
			this.rate = rate;
		}

		public Value target(float t) {
			this.target = t;
			return this;
		}

		public float target() {
			return target;
		}

		public void snap(float v) {
			value = target = v;
			last = System.nanoTime();
		}

		public float get() {
			long now = System.nanoTime();
			float dt = Math.min((now - last) / 1_000_000_000f, 0.1f);
			last = now;
			value += (target - value) * (1 - (float) Math.exp(-rate * speed * dt));
			if (Math.abs(target - value) < 0.0005f) value = target;
			return value;
		}

		public float peek() {
			return value;
		}
	}
}
