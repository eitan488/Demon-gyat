package com.demon.client.gui;

import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Shared HD components so every screen and HUD panel has the same blue glass look. */
public final class Ui {
	private Ui() {}

	/** Large glass panel: soft shadow, blue ambient glow, gradient body, hairline border and top highlight. */
	public static void glass(GuiGraphics g, float x, float y, float w, float h, float r, float alpha) {
		Render2D.shadow(g, x, y + 3, w, h, Theme.alpha(0xFF000000, 0.55f * alpha));
		Render2D.round(g, x, y, w, h, r, Theme.alpha(0xF2081022, alpha));
		sheen(g, x, y, w, h * 0.55f, r, 0x126EA8FF, alpha);
		Render2D.outline(g, x, y, w, h, r, Theme.alpha(0x2A8CB8FF, alpha));
		Render2D.hGradient(g, x + r, y + 0.35f, w - r * 2, 0.35f, Theme.alpha(0x00FFFFFF, alpha), Theme.alpha(0x40FFFFFF, alpha));
	}

	/** Soft top-down highlight that fades out (glass sheen). */
	public static void sheen(GuiGraphics g, float x, float y, float w, float h, float r, int color, float alpha) {
		Render2D.vGradient(g, x + r, y + 0.4f, w - r * 2, h, Theme.alpha(color, alpha), Theme.alpha(color, 0));
	}

	/** Compact HUD panel. */
	public static void hud(GuiGraphics g, float x, float y, float w, float h, float alpha) {
		Render2D.shadow(g, x, y + 2, w, h, Theme.alpha(0xFF000000, 0.35f * alpha));
		Render2D.round(g, x, y, w, h, 4.5f, Theme.alpha(Theme.HUD_BG, alpha));
		Render2D.outline(g, x, y, w, h, 4.5f, Theme.alpha(0x268CB8FF, alpha));
	}

	/** Small vertical accent pill used as a section marker. */
	public static void marker(GuiGraphics g, float x, float y, float h, float alpha) {
		Render2D.capsule(g, x, y, 1.8f, h, Theme.alpha(Theme.wave(0.3f), alpha));
	}

	/** Animated on/off switch. on = 0..1 animation value. */
	public static void toggle(GuiGraphics g, float x, float y, float w, float h, float on, float alpha) {
		if (on > 0.02f) Render2D.roundGradient(g, x, y, w, h, h / 2f, Theme.alpha(Theme.accent1(), on * alpha), Theme.alpha(Theme.accent2(), on * alpha));
		if (on < 0.98f) Render2D.round(g, x, y, w, h, h / 2f, Theme.alpha(Theme.TRACK, (1 - on) * alpha));
		Render2D.outline(g, x, y, w, h, h / 2f, Theme.alpha(0x1FFFFFFF, alpha));
		float knob = h / 2f - 1.6f;
		float kx = x + h / 2f + (w - h) * Anim.easeOutBack(on);
		if (on > 0.05f) Render2D.glow(g, kx, y + h / 2f, h, Theme.alpha(Theme.accent2(), 0.45f * on * alpha));
		Render2D.circle(g, kx, y + h / 2f + 0.3f, knob, Theme.alpha(0x40000000, alpha));
		Render2D.circle(g, kx, y + h / 2f, knob, Theme.alpha(0xFFFFFFFF, alpha));
	}

	/** Rounded badge with text, returns its width. */
	public static float badge(GuiGraphics g, String text, float x, float y, float size, int bg, int fg) {
		float w = HdFont.SEMIBOLD.width(text, size) + size * 1.1f;
		float h = size * 1.5f;
		Render2D.round(g, x, y, w, h, h / 2f, bg);
		Render2D.textMid(g, HdFont.SEMIBOLD, text, x + size * 0.55f, y, h, size, fg);
		return w;
	}

	/** Uppercase section label. */
	public static void label(GuiGraphics g, String text, float x, float y, float alpha) {
		float cx = x;
		for (char c : text.toCharArray()) {
			String s = String.valueOf(c);
			HdFont.SEMIBOLD.draw(g, s, cx, y, 5.6f, Theme.alpha(Theme.MUTED, alpha));
			cx += HdFont.SEMIBOLD.width(s, 5.6f) + 0.9f;
		}
	}
}
