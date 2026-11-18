package com.demon.client.module;

import com.demon.client.config.Config;
import com.demon.client.gui.Notifications;
import com.demon.client.render.Render3D;
import com.demon.client.setting.Setting;
import com.demon.client.util.Anim;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A toggleable feature of the client. */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	public final String name;
	public final String description;
	public final Category category;
	private boolean enabled;
	private int key;
	private final List<Setting<?>> settings = new ArrayList<>();

	/** Used by the HUD array list and the click GUI for smooth transitions. */
	public final Anim.Value arrayAnim = new Anim.Value(0, 14);
	public final Anim.Value guiToggle = new Anim.Value(0, 16);
	private final net.minecraft.resources.Identifier icon;

	protected Module(String name, String description, Category category, int defaultKey, String icon) {
		this.icon = com.demon.client.DemonClient.id("icon_" + icon);
		this.name = name;
		this.description = description;
		this.category = category;
		this.key = defaultKey;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public net.minecraft.resources.Identifier icon() {
		return icon;
	}

	public List<Setting<?>> settings() {
		return settings;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Modules that are always active (e.g. Interface) return false. */
	public boolean toggleable() {
		return true;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean value) {
		if (!toggleable() || value == enabled) return;
		enabled = value;
		if (value) onEnable();
		else onDisable();
		Config.markDirty();
	}

	/** Called by the key handler; also shows a toast. */
	public void onKeyPressed() {
		toggle();
		Notifications.moduleToggled(this);
	}

	/** Restores state from config without side effects. */
	public void loadEnabled(boolean value) {
		if (!toggleable()) return;
		if (value && !enabled) {
			enabled = true;
			onEnable();
		} else if (!value && enabled) {
			enabled = false;
			onDisable();
		}
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
		Config.markDirty();
	}

	/** Extra text shown next to the module name in the HUD array list. */
	public String info() {
		return null;
	}

	protected void onEnable() {}

	protected void onDisable() {}

	public void onTick() {}

	public void onRender3D(Render3D r) {}

	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {}
}
