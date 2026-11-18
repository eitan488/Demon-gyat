package com.demon.client.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

/** Base class for a configurable module option. */
public abstract class Setting<T> {
	public final String name;
	public final String description;
	protected T value;
	protected final T defaultValue;
	private BooleanSupplier visible = () -> true;
	private Runnable onChange = () -> {};

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T v) {
		if (v == null || v.equals(value)) return;
		value = v;
		onChange.run();
		com.demon.client.config.Config.markDirty();
	}

	public void reset() {
		set(defaultValue);
	}

	public boolean isVisible() {
		return visible.getAsBoolean();
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
		this.visible = supplier;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S onChange(Runnable r) {
		this.onChange = r;
		return (S) this;
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
