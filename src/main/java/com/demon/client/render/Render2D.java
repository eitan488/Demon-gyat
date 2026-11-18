package com.demon.client.render;

import com.demon.client.DemonClient;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Modern 2D drawing API. Text, rounded shapes and icons are all rendered in physical screen pixels through
 * {@link Hd} and {@link HdFont}, so everything stays crisp ("HD") at every GUI scale and resolution.
 */
public final class Render2D {
	private Render2D() {}

	// soft effects that are blurry by nature stay as GUI-atlas sprites
	public static final Identifier SHADOW = DemonClient.id("shadow");
	public static final Identifier GLOW = DemonClient.id("glow");
	public static final Identifier SWEEP = DemonClient.id("sweep");
	public static final Identifier FADE_V = DemonClient.id("fade_v");

	/** HD image names (textures/hd/*.png). */
	public static final String LOGO_ICON = "logo_icon";
	public static final String LOGO_SMALL = "logo_small";
	public static final String LOGO_SMALL_GLOW = "logo_small_glow";
	public static final String LOGO_CLIENT = "logo_client";

	public static final float LOGO_SMALL_ASPECT = 3.82083f, LOGO_SMALL_PAD = 0.16667f;
	public static final float LOGO_CLIENT_ASPECT = 4.91848f, LOGO_CLIENT_PAD = 0.17391f;

	/** Default text size (em, in GUI units). */
	public static final float TEXT = 8f;

	// ------------------------------------------------------------------ shapes

	public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		g.fill(0, 0, Math.max(1, f.dx(w)), Math.max(1, f.dy(h)), color);
		Hd.end(g);
	}

	/** A GUI-atlas sprite (used for soft shadows, glows and light sweeps). */
	public static void sprite(GuiGraphics g, Identifier id, float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0 || w < 0.5f || h < 0.5f) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		int iw = Math.max(1, Math.round(w)), ih = Math.max(1, Math.round(h));
		g.pose().scale(w / iw, h / ih);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, 0, 0, iw, ih, color);
		g.pose().popMatrix();
	}

	/** Filled rounded rectangle with a pixel-exact radius. */
	public static void round(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		Hd.roundRect(g, 0, 0, f.dx(w), f.dy(h), Math.round(radius * f.pixelScale), color);
		Hd.end(g);
	}

	/** Rounded outline, one physical pixel (or more on large scales) thick. */
	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		outline(g, x, y, w, h, radius, 0.4f, color);
	}

	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, float stroke, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		int s = Math.max(1, Math.round(stroke * f.pixelScale));
		Hd.roundOutline(g, 0, 0, f.dx(w), f.dy(h), Math.round(radius * f.pixelScale), s, color);
		Hd.end(g);
	}

	public static void roundGradient(GuiGraphics g, float x, float y, float w, float h, float radius, int left, int right) {
		if (w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		Hd.roundGradient(g, 0, 0, f.dx(w), f.dy(h), Math.round(radius * f.pixelScale), left, right);
		Hd.end(g);
	}

	/** Pill shape (fully rounded ends). */
	public static void capsule(GuiGraphics g, float x, float y, float w, float h, int color) {
		round(g, x, y, w, h, Math.min(w, h) / 2f, color);
	}

	public static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
		if ((color >>> 24) == 0 || r <= 0) return;
		Hd.Frame f = Hd.frame(g, cx, cy);
		Hd.disk(g, 0, 0, Math.max(1, Math.round(r * f.pixelScale)), color);
		Hd.end(g);
	}

	public static void ring(GuiGraphics g, float cx, float cy, float r, int color) {
		ring(g, cx, cy, r, 0.8f, color);
	}

	public static void ring(GuiGraphics g, float cx, float cy, float r, float stroke, int color) {
		if ((color >>> 24) == 0 || r <= 0) return;
		Hd.Frame f = Hd.frame(g, cx, cy);
		Hd.ring(g, 0, 0, Math.max(1, Math.round(r * f.pixelScale)), Math.max(1, Math.round(stroke * f.pixelScale)), color);
		Hd.end(g);
	}

	/** Soft drop shadow / glow around a rectangle. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, int color) {
		sprite(g, SHADOW, x - 16, y - 16, w + 32, h + 32, color);
	}

	public static void glow(GuiGraphics g, float cx, float cy, float r, int color) {
		sprite(g, GLOW, cx - r, cy - r, r * 2, r * 2, color);
	}

	public static void hGradient(GuiGraphics g, float x, float y, float w, float h, int left, int right) {
		if (w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		Hd.hGradient(g, 0, 0, f.dx(w), f.dy(h), left, right);
		Hd.end(g);
	}

	public static void vGradient(GuiGraphics g, float x, float y, float w, float h, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		Hd.Frame f = Hd.frame(g, x, y);
		g.fillGradient(0, 0, f.dx(w), f.dy(h), top, bottom);
		Hd.end(g);
	}

	/** A thin line between two points (any angle), one physical pixel wide. */
	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.3f || (color >>> 24) == 0) return;
		g.pose().pushMatrix();
		g.pose().translate(x1, y1);
		g.pose().rotate((float) Math.atan2(dy, dx));
		Hd.Frame f = Hd.frame(g, 0, 0);
		int w = Math.max(1, Math.round(f.pixelScale / 3f));
		g.fill(0, -w / 2, Math.round(len * f.pixelScale), -w / 2 + w, color);
		Hd.end(g);
		g.pose().popMatrix();
	}

	/** A line that fades from one colour to another along its length. */
	public static void fadeLine(GuiGraphics g, float x1, float y1, float x2, float y2, int from, int to) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.5f) return;
		g.pose().pushMatrix();
		g.pose().translate(x1, y1);
		g.pose().rotate((float) Math.atan2(dy, dx) - (float) Math.PI / 2f);
		Hd.Frame f = Hd.frame(g, 0, 0);
		g.fillGradient(0, 0, Math.max(1, Math.round(f.pixelScale / 3f)), Math.round(len * f.pixelScale), from, to);
		Hd.end(g);
		g.pose().popMatrix();
	}

	/** Draws an HD icon (textures/hd/icon_*.png or any HD image identifier) centred on (cx, cy). */
	public static void icon(GuiGraphics g, Identifier icon, float cx, float cy, float size, int color) {
		Hd.image(g, icon.getPath(), cx - size / 2f, cy - size / 2f, size, size, color);
	}

	public static void icon(GuiGraphics g, String name, float cx, float cy, float size, int color) {
		Hd.image(g, name, cx - size / 2f, cy - size / 2f, size, size, color);
	}

	public static void image(GuiGraphics g, String name, float x, float y, float w, float h, int color) {
		Hd.image(g, name, x, y, w, h, color);
	}

	// ------------------------------------------------------------------ text

	public static float width(String s) {
		return HdFont.MEDIUM.width(s, TEXT);
	}

	public static float width(String s, float size) {
		return HdFont.MEDIUM.width(s, size);
	}

	public static float width(HdFont font, String s, float size) {
		return font.width(s, size);
	}

	/** Offset that centres a cap-height block where the 7px-tall vanilla glyphs used to sit. */
	private static float capOffset(HdFont font, float size) {
		return (7f - font.capHeight(size)) / 2f;
	}

	public static void text(GuiGraphics g, String s, float x, float y, int color) {
		HdFont.MEDIUM.draw(g, s, x, y + capOffset(HdFont.MEDIUM, TEXT), TEXT, color);
	}

	public static void text(GuiGraphics g, String s, float x, float y, int color, boolean shadow) {
		if (shadow) HdFont.MEDIUM.draw(g, s, x + 0.35f, y + capOffset(HdFont.MEDIUM, TEXT) + 0.35f, TEXT, Theme.alpha(0xFF000000, (color >>> 24) / 255f * 0.6f));
		text(g, s, x, y, color);
	}

	/** Text with explicit font and size; (x, y) is the top of the capital letters. */
	public static float text(GuiGraphics g, HdFont font, String s, float x, float y, float size, int color) {
		return font.draw(g, s, x, y, size, color);
	}

	public static void textCentered(GuiGraphics g, String s, float cx, float y, int color) {
		text(g, s, cx - width(s) / 2f, y, color);
	}

	public static void textCentered(GuiGraphics g, HdFont font, String s, float cx, float y, float size, int color) {
		font.draw(g, s, cx - font.width(s, size) / 2f, y, size, color);
	}

	public static void textRight(GuiGraphics g, String s, float right, float y, int color) {
		text(g, s, right - width(s), y, color);
	}

	public static void textRight(GuiGraphics g, HdFont font, String s, float right, float y, float size, int color) {
		font.draw(g, s, right - font.width(s, size), y, size, color);
	}

	/** Text vertically centred inside a box of height h starting at y. */
	public static void textMid(GuiGraphics g, HdFont font, String s, float x, float y, float h, float size, int color) {
		font.draw(g, s, x, y + (h - font.capHeight(size)) / 2f, size, color);
	}

	public static void scaledText(GuiGraphics g, String s, float x, float y, float scale, int color, boolean shadow) {
		HdFont.SEMIBOLD.draw(g, s, x, y + capOffset(HdFont.SEMIBOLD, TEXT) * scale, TEXT * scale, color);
	}

	/** Text where every character takes a colour from the animated accent gradient. */
	public static void gradientText(GuiGraphics g, HdFont font, String s, float x, float y, float size, float alpha) {
		float cx = x;
		int total = Math.max(1, s.length() - 1);
		for (int i = 0; i < s.length(); i++) {
			String c = String.valueOf(s.charAt(i));
			font.draw(g, c, cx, y, size, Theme.alpha(Theme.wave(i / (float) total), alpha));
			cx += font.width(c, size);
		}
	}

	/** Trims a string to fit the width, adding an ellipsis. */
	public static String trim(String s, float maxWidth) {
		return trim(HdFont.MEDIUM, s, TEXT, maxWidth);
	}

	public static String trim(HdFont font, String s, float size, float maxWidth) {
		if (font.width(s, size) <= maxWidth) return s;
		String dots = "…";
		float dw = font.width(dots, size);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (font.width(sb.toString() + s.charAt(i), size) + dw > maxWidth) break;
			sb.append(s.charAt(i));
		}
		return sb + dots;
	}

	// ------------------------------------------------------------------ branding

	/** Draws the smooth "DEMON" wordmark with a gradient tint and glow. capHeight in GUI px. Returns the cap width. */
	public static float logoSmall(GuiGraphics g, float x, float y, float capHeight, float alpha) {
		float h = capHeight / (1 - 2 * LOGO_SMALL_PAD);
		float w = h * LOGO_SMALL_ASPECT;
		float pad = h * LOGO_SMALL_PAD;
		float glowPulse = 0.35f + 0.15f * (float) Math.sin(Anim.time() * 2.2);
		Hd.image(g, LOGO_SMALL_GLOW, x - pad, y - pad, w, h, Theme.alpha(Theme.wave(0.5f), alpha * glowPulse));
		// gradient: clip vertical slices and tint each one
		Hd.Frame f = Hd.frame(g, x - pad, y - pad);
		int pw = f.dx(w), ph = f.dy(h);
		Hd.end(g);
		int slices = 10;
		for (int i = 0; i < slices; i++) {
			int sx0 = Math.round(x - pad + w * i / slices), sx1 = Math.round(x - pad + w * (i + 1) / slices);
			if (sx1 <= sx0) continue;
			g.enableScissor(sx0, (int) Math.floor(y - pad), sx1, (int) Math.ceil(y - pad + h));
			Hd.image(g, LOGO_SMALL, x - pad, y - pad, w, h, Theme.alpha(Theme.wave(i / (float) (slices - 1)), alpha));
			g.disableScissor();
		}
		return pw > 0 && ph > 0 ? w - pad * 2 : 0;
	}
}
