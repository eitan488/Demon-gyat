package com.demon.client.render;

import com.demon.client.DemonClient;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Modern 2D drawing helpers built on top of anti-aliased GUI sprites
 * (rounded rectangles, soft shadows, circles, glows) shipped with the mod.
 */
public final class Render2D {
	private Render2D() {}

	public static final Identifier ROUND_S = DemonClient.id("round_s");
	public static final Identifier ROUND_M = DemonClient.id("round_m");
	public static final Identifier ROUND_L = DemonClient.id("round_l");
	public static final Identifier ROUND_S_OUT = DemonClient.id("round_s_outline");
	public static final Identifier ROUND_M_OUT = DemonClient.id("round_m_outline");
	public static final Identifier ROUND_L_OUT = DemonClient.id("round_l_outline");
	public static final Identifier SHADOW = DemonClient.id("shadow");
	public static final Identifier CIRCLE = DemonClient.id("circle");
	public static final Identifier RING = DemonClient.id("ring");
	public static final Identifier GLOW = DemonClient.id("glow");
	public static final Identifier SWEEP = DemonClient.id("sweep");
	public static final Identifier FADE_V = DemonClient.id("fade_v");
	public static final Identifier LOGO_ICON = DemonClient.id("logo_icon");
	public static final Identifier LOGO_SMALL = DemonClient.id("logo_small");
	public static final Identifier LOGO_SMALL_GLOW = DemonClient.id("logo_small_glow");
	public static final Identifier LOGO_CLIENT = DemonClient.id("logo_client");

	/** width/height of the "logo_small" sprite and the padding fraction baked into it. */
	public static final float LOGO_SMALL_ASPECT = 3.83333f, LOGO_SMALL_PAD = 0.16667f;
	public static final float LOGO_CLIENT_ASPECT = 4.8913f, LOGO_CLIENT_PAD = 0.17391f;

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	// ------------------------------------------------------------------ shapes

	public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		g.fill(x, y, x + w, y + h, color);
	}

	public static void sprite(GuiGraphics g, Identifier id, float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0 || w < 0.5f || h < 0.5f) return;
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
		float fx = x - ix, fy = y - iy;
		if (fx == 0 && fy == 0) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, id, ix, iy, Math.round(w), Math.round(h), color);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, 0, 0, Math.round(w), Math.round(h), color);
		g.pose().popMatrix();
	}

	private static Identifier roundFor(float radius) {
		return radius <= 3.5f ? ROUND_S : radius <= 7f ? ROUND_M : ROUND_L;
	}

	private static Identifier outlineFor(float radius) {
		return radius <= 3.5f ? ROUND_S_OUT : radius <= 7f ? ROUND_M_OUT : ROUND_L_OUT;
	}

	/** Filled rounded rectangle. Radius is snapped to 3, 6 or 12. */
	public static void round(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		if (w <= 4.5f || h <= 4.5f) {
			capsule(g, x, y, w, h, color);
			return;
		}
		sprite(g, roundFor(radius), x, y, w, h, color);
	}

	/** A pill shape (fully rounded ends) for thin bars, tracks and progress lines. */
	public static void capsule(GuiGraphics g, float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		// work in a 64-unit space so the circle sprite halves line up exactly (no overlapping alpha)
		if (h >= w) {
			g.pose().scale(w / 64f, w / 64f);
			int len = Math.round((h - w) * 64f / w);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, CIRCLE, 64, 64, 0, 0, 0, 0, 64, 32, color);
			if (len > 0) g.fill(0, 32, 64, 32 + len, color);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, CIRCLE, 64, 64, 0, 32, 0, 32 + len, 64, 32, color);
		} else {
			g.pose().scale(h / 64f, h / 64f);
			int len = Math.round((w - h) * 64f / h);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, CIRCLE, 64, 64, 0, 0, 0, 0, 32, 64, color);
			if (len > 0) g.fill(32, 0, 32 + len, 64, color);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, CIRCLE, 64, 64, 32, 0, 32 + len, 0, 32, 64, color);
		}
		g.pose().popMatrix();
	}

	/** 1px rounded outline. */
	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		sprite(g, outlineFor(radius), x, y, w, h, color);
	}

	/** Rounded rectangle filled with a horizontal gradient. */
	public static void roundGradient(GuiGraphics g, float x, float y, float w, float h, float radius, int left, int right) {
		if (w < 1 || h < 1) return;
		Identifier id = roundFor(radius);
		int start = Math.round(x), end = Math.round(x + w);
		int slices = Math.max(1, Math.min(32, (end - start) / 3));
		for (int i = 0; i < slices; i++) {
			// non-overlapping integer columns so translucent slices never double up
			int x0 = start + (end - start) * i / slices;
			int x1 = start + (end - start) * (i + 1) / slices;
			if (x1 <= x0) continue;
			g.enableScissor(x0, (int) Math.floor(y) - 1, x1, (int) Math.ceil(y + h) + 1);
			sprite(g, id, x, y, w, h, Theme.lerp(left, right, (x0 + x1) / 2f - x < 0 ? 0 : ((x0 + x1) / 2f - x) / w));
			g.disableScissor();
		}
	}

	/** Soft drop shadow / glow around a rectangle. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, int color) {
		sprite(g, SHADOW, x - 16, y - 16, w + 32, h + 32, color);
	}

	/** Draws a square icon sprite centred on (cx, cy). */
	public static void icon(GuiGraphics g, Identifier icon, float cx, float cy, float size, int color) {
		sprite(g, icon, cx - size / 2f, cy - size / 2f, size, size, color);
	}

	public static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
		sprite(g, CIRCLE, cx - r, cy - r, r * 2, r * 2, color);
	}

	public static void ring(GuiGraphics g, float cx, float cy, float r, int color) {
		sprite(g, RING, cx - r, cy - r, r * 2, r * 2, color);
	}

	public static void glow(GuiGraphics g, float cx, float cy, float r, int color) {
		sprite(g, GLOW, cx - r, cy - r, r * 2, r * 2, color);
	}

	public static void hGradient(GuiGraphics g, int x, int y, int w, int h, int left, int right) {
		if (w <= 0 || h <= 0) return;
		int slices = Math.max(1, Math.min(48, w / 2));
		for (int i = 0; i < slices; i++) {
			int x0 = x + w * i / slices, x1 = x + w * (i + 1) / slices;
			g.fill(x0, y, x1, y + h, Theme.lerp(left, right, (i + 0.5f) / slices));
		}
	}

	public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
		g.fillGradient(x, y, x + w, y + h, top, bottom);
	}

	/** A 1px line between two points (any angle). */
	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.5f) return;
		g.pose().pushMatrix();
		g.pose().translate(x1, y1);
		g.pose().rotate((float) Math.atan2(dy, dx));
		g.fill(0, 0, Math.round(len), 1, color);
		g.pose().popMatrix();
	}

	/** A line that fades in from transparent at the start to the colour at the end. */
	public static void fadeLine(GuiGraphics g, float x1, float y1, float x2, float y2, int from, int to) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.5f) return;
		g.pose().pushMatrix();
		g.pose().translate(x1, y1);
		g.pose().rotate((float) Math.atan2(dy, dx) - (float) Math.PI / 2f);
		g.fillGradient(0, 0, 1, Math.round(len), from, to);
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ text

	public static int width(String s) {
		return font().width(s);
	}

	public static void text(GuiGraphics g, String s, float x, float y, int color) {
		text(g, s, x, y, color, false);
	}

	public static void text(GuiGraphics g, String s, float x, float y, int color, boolean shadow) {
		if ((color >>> 24) < 8) return;
		int ix = Math.round(x), iy = Math.round(y);
		g.drawString(font(), s, ix, iy, color, shadow);
	}

	public static void textCentered(GuiGraphics g, String s, float cx, float y, int color) {
		text(g, s, cx - width(s) / 2f, y, color, false);
	}

	public static void textRight(GuiGraphics g, String s, float right, float y, int color) {
		text(g, s, right - width(s), y, color, false);
	}

	public static void scaledText(GuiGraphics g, String s, float x, float y, float scale, int color, boolean shadow) {
		if ((color >>> 24) < 8) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.drawString(font(), s, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	/** Text where every character takes a colour from the animated accent gradient. */
	public static void gradientText(GuiGraphics g, String s, float x, float y, float alpha, boolean shadow) {
		float cx = x;
		int total = Math.max(1, s.length() - 1);
		for (int i = 0; i < s.length(); i++) {
			String c = String.valueOf(s.charAt(i));
			int color = Theme.alpha(Theme.wave(i / (float) total), alpha);
			text(g, c, cx, y, color, shadow);
			cx += width(c);
		}
	}

	/** Trims a string to fit the width, adding an ellipsis. */
	public static String trim(String s, int maxWidth) {
		if (width(s) <= maxWidth) return s;
		String dots = "…";
		int dw = width(dots);
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (width(sb.toString() + c) + dw > maxWidth) break;
			sb.append(c);
		}
		return sb + dots;
	}

	// ------------------------------------------------------------------ branding

	/** Draws the smooth "DEMON" wordmark with a gradient tint and glow. capHeight in GUI px. */
	public static float logoSmall(GuiGraphics g, float x, float y, float capHeight, float alpha) {
		float h = capHeight / (1 - 2 * LOGO_SMALL_PAD);
		float w = h * LOGO_SMALL_ASPECT;
		float pad = h * LOGO_SMALL_PAD;
		float glowPulse = 0.35f + 0.15f * (float) Math.sin(Anim.time() * 2.2);
		sprite(g, LOGO_SMALL_GLOW, x - pad, y - pad, w, h, Theme.alpha(Theme.wave(0.5f), alpha * glowPulse));
		// gradient by drawing clipped vertical slices
		int start = (int) Math.floor(x - pad), end = (int) Math.ceil(x - pad + w);
		int slices = Math.max(1, Math.min(12, (end - start) / 3));
		for (int i = 0; i < slices; i++) {
			int x0 = start + (end - start) * i / slices;
			int x1 = start + (end - start) * (i + 1) / slices;
			if (x1 <= x0) continue;
			g.enableScissor(x0, (int) (y - pad) - 1, x1, (int) (y - pad + h) + 1);
			sprite(g, LOGO_SMALL, x - pad, y - pad, w, h, Theme.alpha(Theme.wave(i / (float) Math.max(1, slices - 1)), alpha));
			g.disableScissor();
		}
		return w - pad * 2;
	}
}
