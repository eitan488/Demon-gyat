package com.demon.client.gui;

import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** Slide-in toasts in the bottom-right corner with a progress bar. */
public final class Notifications {
	private Notifications() {}

	private static final class Toast {
		final String title, text;
		final int color;
		final float duration;
		final float start = Anim.time();
		final Anim.Value y = new Anim.Value(-1, 12);

		Toast(String title, String text, int color, float duration) {
			this.title = title;
			this.text = text;
			this.color = color;
			this.duration = duration;
		}
	}

	private static final List<Toast> TOASTS = new ArrayList<>();

	public static void push(String title, String text, int color, float seconds) {
		if (ModuleManager.hud != null && !ModuleManager.hud.notifications.on()) return;
		TOASTS.add(new Toast(title, text, color, seconds));
		while (TOASTS.size() > 6) TOASTS.remove(0);
	}

	public static void moduleToggled(Module m) {
		boolean on = m.isEnabled();
		Sounds.toggle(on);
		push(m.name, on ? "Enabled" : "Disabled", on ? Theme.SUCCESS : Theme.DANGER, 1.6f);
	}

	public static void render(GuiGraphics g) {
		if (TOASTS.isEmpty()) return;
		int sw = g.guiWidth(), sh = g.guiHeight();
		float now = Anim.time();
		float stackY = sh - 8;
		Iterator<Toast> it = TOASTS.iterator();
		List<Toast> live = new ArrayList<>();
		while (it.hasNext()) {
			Toast t = it.next();
			if (now - t.start > t.duration + 0.5f) it.remove();
			else live.add(t);
		}
		for (int i = live.size() - 1; i >= 0; i--) {
			Toast t = live.get(i);
			float age = now - t.start;
			float in = Anim.easeOutBack(Anim.progress(age, 0, 0.45f));
			float out = Anim.easeInCubic(Anim.progress(age, t.duration, t.duration + 0.4f));
			float slide = in * (1 - out);
			int textW = Math.max(Render2D.width(t.title), Render2D.width(t.text));
			int w = Math.min(Math.max(130, textW + 34), sw - 20);
			int h = 30;
			float targetY = stackY - h;
			if (t.y.peek() < 0) t.y.snap(targetY + 10);
			float y = t.y.target(targetY).get();
			float x = sw - 8 - w * slide - 8 * (1 - slide);
			float alpha = Anim.clamp01(slide * 1.4f);
			Render2D.shadow(g, x, y, w, h, Theme.alpha(t.color, 0.25f * alpha));
			Render2D.round(g, x, y, w, h, 6, Theme.alpha(0xEE0E0B16, alpha));
			Render2D.outline(g, x, y, w, h, 6, Theme.alpha(Theme.BORDER, alpha));
			Render2D.circle(g, x + 12, y + 11, 4, Theme.alpha(t.color, alpha));
			Render2D.glow(g, x + 12, y + 11, 9, Theme.alpha(t.color, 0.5f * alpha));
			Render2D.text(g, Render2D.trim(t.title, w - 30), x + 22, y + 6, Theme.alpha(Theme.TEXT, alpha));
			Render2D.text(g, Render2D.trim(t.text, w - 30), x + 22, y + 16, Theme.alpha(Theme.SUBTEXT, alpha));
			float progress = 1 - Anim.clamp01(age / t.duration);
			Render2D.round(g, x + 6, y + h - 3, (w - 12) * progress, 1.5f, 1, Theme.alpha(t.color, 0.9f * alpha));
			stackY -= (h + 5) * slide;
		}
	}
}
