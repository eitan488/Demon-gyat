package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Live FPS graph and memory usage. */
public class PerformanceModule extends HudPanelModule {
	private final int[] history = new int[60];
	private int idx;
	private long lastSample;

	public PerformanceModule() {
		super("Performance", "FPS graph and memory usage", "chart", HudLayout.Anchor.BOTTOM_RIGHT);
	}

	@Override
	protected float[] size() {
		return new float[] {100, 38};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		long now = System.currentTimeMillis();
		if (now - lastSample > 250) {
			lastSample = now;
			history[idx++ % history.length] = mc.getFps();
		}
		int max = 30;
		for (int v : history) max = Math.max(max, v);
		Runtime rt = Runtime.getRuntime();
		long used = (rt.totalMemory() - rt.freeMemory()) / 1048576, total = rt.maxMemory() / 1048576;
		Render2D.text(g, HdFont.BOLD, mc.getFps() + " FPS", x + 6, y + 5, 5.8f, Theme.TEXT);
		Render2D.textRight(g, HdFont.MEDIUM, used + " / " + total + " MB", x + w - 6, y + 5.2f, 5.2f, Theme.SUBTEXT);
		float gx = x + 6, gy = y + 14, gw = w - 12, gh = 19;
		float bw = gw / history.length;
		for (int i = 0; i < history.length; i++) {
			int v = history[(idx + i) % history.length];
			float bh = gh * v / (float) max;
			Render2D.rect(g, gx + i * bw, gy + gh - bh, Math.max(0.4f, bw - 0.3f), bh, Theme.alpha(Theme.accent(i / (float) history.length), 0.85f));
		}
	}
}
