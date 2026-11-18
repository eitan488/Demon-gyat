package com.demon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	public final double min, max, step;
	public final String suffix;

	public NumberSetting(String name, String description, double def, double min, double max, double step, String suffix) {
		super(name, description, def);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	public NumberSetting(String name, String description, double def, double min, double max, double step) {
		this(name, description, def, min, max, step, "");
	}

	@Override
	public void set(Double v) {
		double clamped = Math.max(min, Math.min(max, v));
		if (step > 0) clamped = Math.round(clamped / step) * step;
		clamped = Math.round(clamped * 1000.0) / 1000.0;
		super.set(clamped);
	}

	public int asInt() {
		return (int) Math.round(value);
	}

	public float asFloat() {
		return value.floatValue();
	}

	/** Position of the value inside [min,max] as 0..1. */
	public float fraction() {
		return (float) ((value - min) / (max - min));
	}

	public String display() {
		boolean whole = step >= 1 && Math.abs(value - Math.rint(value)) < 1e-6;
		String s = whole ? String.valueOf((long) Math.rint(value)) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
		return s + suffix;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement e) {
		if (e.isJsonPrimitive()) value = Math.max(min, Math.min(max, e.getAsDouble()));
	}
}
