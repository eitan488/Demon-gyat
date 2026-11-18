package com.demon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final E[] values;

	public EnumSetting(String name, String description, E def) {
		super(name, description, def);
		this.values = def.getDeclaringClass().getEnumConstants();
	}

	public E[] values() {
		return values;
	}

	public void cycle(int dir) {
		int i = (value.ordinal() + dir + values.length) % values.length;
		set(values[i]);
	}

	public boolean is(E e) {
		return value == e;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value.name());
	}

	@Override
	public void fromJson(JsonElement e) {
		if (!e.isJsonPrimitive()) return;
		for (E v : values) if (v.name().equals(e.getAsString())) value = v;
	}
}
