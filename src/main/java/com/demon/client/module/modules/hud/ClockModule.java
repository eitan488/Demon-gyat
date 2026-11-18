package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.GuiGraphics;

public class ClockModule extends HudPanelModule {
	private final long start = System.currentTimeMillis();

	public ClockModule() {
		super("Clock", "Real time and session length", "timer", HudLayout.Anchor.TOP_LEFT);
	}

	@Override
	protected float[] size() {
		return new float[] {78, 11};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		long m = (System.currentTimeMillis() - start) / 60000;
		Render2D.textMid(g, HdFont.BOLD, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), x + 7, y, h, 6f, Theme.TEXT);
		Render2D.textRight(g, HdFont.MEDIUM, (m / 60) + "h " + (m % 60) + "m", x + w - 7, y + (h - HdFont.MEDIUM.capHeight(5.4f)) / 2f, 5.4f, Theme.SUBTEXT);
	}
}
