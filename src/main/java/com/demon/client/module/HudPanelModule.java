package com.demon.client.module;

import com.demon.client.gui.Ui;
import com.demon.client.hud.HudLayout;
import com.demon.client.setting.EnumSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** A HUD element that is placed automatically in its screen corner and drawn on a glass panel. */
public abstract class HudPanelModule extends Module {
	public final EnumSetting<HudLayout.Anchor> anchor;

	protected HudPanelModule(String name, String description, String icon, HudLayout.Anchor defaultAnchor) {
		this(name, description, Category.HUD, icon, defaultAnchor);
	}

	protected HudPanelModule(String name, String description, Category category, String icon, HudLayout.Anchor defaultAnchor) {
		super(name, description, category, -1, icon);
		anchor = add(new EnumSetting<>("Position", "Screen corner this panel stacks in", defaultAnchor));
	}

	/** Returns {width, height} or null to hide this frame. */
	protected abstract float[] size();

	protected abstract void draw(GuiGraphics g, float x, float y, float w, float h);

	protected boolean background() {
		return true;
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		float[] s = size();
		if (s == null) return;
		float[] p = HudLayout.place(anchor.get(), s[0], s[1]);
		if (background()) Ui.hud(g, p[0], p[1], s[0], s[1], 1f);
		draw(g, p[0], p[1], s[0], s[1]);
	}
}
