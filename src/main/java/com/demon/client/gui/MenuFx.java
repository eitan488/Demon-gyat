package com.demon.client.gui;

import com.demon.client.module.modules.InterfaceModule;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;

/** Animated background effects shared by the main menu and the click GUI. */
public final class MenuFx {
	private MenuFx() {}

	private static final int STAR_COUNT = 140;
	private static final float[][] STARS = new float[STAR_COUNT][5];
	private static final int PARTICLE_COUNT = 46;
	private static final float[][] PARTICLES = new float[PARTICLE_COUNT][5];

	static {
		Random r = new Random(1337);
		for (float[] s : STARS) {
			s[0] = r.nextFloat(); // x
			s[1] = r.nextFloat() * 0.62f; // y (sky only)
			s[2] = 0.5f + r.nextFloat() * 1.1f; // size
			s[3] = r.nextFloat() * 6.28f; // phase
			s[4] = 0.6f + r.nextFloat() * 2.2f; // twinkle speed
		}
		for (float[] p : PARTICLES) {
			p[0] = r.nextFloat();
			p[1] = r.nextFloat();
			p[2] = 0.02f + r.nextFloat() * 0.05f; // rise speed (fraction of height / s)
			p[3] = 1.5f + r.nextFloat() * 3.5f; // radius
			p[4] = r.nextFloat() * 6.28f;
		}
	}

	/** Full-screen animated background: aurora blobs, stars, synthwave grid and vignette. */
	public static void background(GuiGraphics g, int w, int h, float alpha) {
		float t = Anim.time();
		Render2D.vGradient(g, 0, 0, w, h, Theme.alpha(0xFF07050D, alpha), Theme.alpha(0xFF120B1F, alpha));
		int a1 = Theme.accent1(), a2 = Theme.accent2();
		// aurora
		Render2D.glow(g, w * (0.22f + 0.08f * (float) Math.sin(t * 0.21)), h * (0.32f + 0.06f * (float) Math.cos(t * 0.17)), w * 0.55f, Theme.alpha(a1, 0.26f * alpha));
		Render2D.glow(g, w * (0.78f + 0.07f * (float) Math.cos(t * 0.19)), h * (0.28f + 0.07f * (float) Math.sin(t * 0.23)), w * 0.5f, Theme.alpha(a2, 0.22f * alpha));
		Render2D.glow(g, w * (0.5f + 0.12f * (float) Math.sin(t * 0.13)), h * 0.62f, w * 0.45f, Theme.alpha(Theme.lerp(a1, a2, 0.5f), 0.18f * alpha));
		// stars
		for (float[] s : STARS) {
			float tw = 0.25f + 0.75f * (0.5f + 0.5f * (float) Math.sin(t * s[4] + s[3]));
			float x = (s[0] * w + t * 1.5f * s[2]) % w;
			float y = s[1] * h;
			int c = Theme.alpha(0xFFFFFFFF, tw * 0.75f * alpha);
			if (s[2] > 1.3f) Render2D.glow(g, x, y, s[2] * 2.6f, Theme.alpha(Theme.lerp(0xFFFFFFFF, a2, 0.4f), tw * 0.5f * alpha));
			Render2D.rect(g, (int) x, (int) y, 1, 1, c);
		}
		grid(g, w, h, alpha);
		// vignette
		Render2D.sprite(g, Render2D.FADE_V, 0, 0, w, h * 0.22f, Theme.alpha(0xFF000000, 0.65f * alpha));
		g.pose().pushMatrix();
		g.pose().translate(w, h);
		g.pose().rotate((float) Math.PI);
		Render2D.sprite(g, Render2D.FADE_V, 0, 0, w, h * 0.18f, Theme.alpha(0xFF000000, 0.55f * alpha));
		g.pose().popMatrix();
	}

	/** Perspective "synthwave" grid that scrolls towards the viewer. */
	private static void grid(GuiGraphics g, int w, int h, float alpha) {
		float t = Anim.time();
		float horizon = h * 0.64f;
		int a1 = Theme.accent1(), a2 = Theme.accent2();
		// floor tint
		Render2D.vGradient(g, 0, (int) horizon, w, h - (int) horizon, Theme.alpha(a2, 0.10f * alpha), Theme.alpha(a1, 0.02f * alpha));
		// horizontal lines
		float scroll = (t * 0.32f) % 1f;
		int lines = 16;
		for (int i = 0; i < lines; i++) {
			float p = (i + scroll) / lines;
			float yy = horizon + (h - horizon) * p * p;
			int col = Theme.alpha(Theme.lerp(a2, a1, p), (0.12f + 0.55f * p) * alpha);
			Render2D.rect(g, 0, (int) yy, w, 1, col);
		}
		// converging verticals
		float vx = w / 2f;
		int spokes = 14;
		for (int k = -spokes; k <= spokes; k++) {
			float bottomX = vx + k * (w / 9f);
			Render2D.fadeLine(g, vx + k * 3f, horizon, bottomX, h + 4, Theme.alpha(a1, 0), Theme.alpha(a1, 0.5f * alpha));
		}
		// horizon glow
		Render2D.glow(g, vx, horizon, w * 0.42f, Theme.alpha(a2, 0.20f * alpha));
		Render2D.sprite(g, Render2D.SWEEP, 0, horizon - 1, w, 2, Theme.alpha(Theme.brighten(a2, 0.3f), 0.95f * alpha));
	}

	/** Floating glow particles rising inside a rectangle. */
	public static void particles(GuiGraphics g, float x, float y, float w, float h, float alpha) {
		if (!InterfaceModule.particlesEnabled()) return;
		float t = Anim.time();
		for (float[] p : PARTICLES) {
			float py = ((p[1] - t * p[2]) % 1f + 1f) % 1f;
			float px = p[0] + 0.015f * (float) Math.sin(t * 0.8f + p[4]);
			float fade = Math.min(1f, Math.min(py * 4f, (1 - py) * 4f));
			float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.7f + p[4]);
			int c = Theme.alpha(Theme.wave(p[0]), (0.10f + 0.22f * tw) * fade * alpha);
			Render2D.glow(g, x + px * w, y + py * h, p[3] * 2.2f, c);
		}
	}
}
