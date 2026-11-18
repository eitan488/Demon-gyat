package com.demon.client.render;

import com.demon.client.DemonClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/**
 * Pixel-exact drawing. Shapes and images are generated at the physical pixel size they are displayed at
 * (anti-aliased corner masks, high quality downscaled images), so the GUI is sharp on any resolution.
 */
public final class Hd {
	private Hd() {}

	private static final Minecraft mc = Minecraft.getInstance();

	public static float guiScale() {
		return (float) mc.getWindow().getGuiScale();
	}

	// ------------------------------------------------------------------ pixel frames

	public static final class Frame {
		/** physical pixels per local GUI unit (gui scale x pose scale) */
		public float pixelScale;
		/** true when the pixel scale is a whole number (not mid-animation) */
		public boolean stableScale;
		float originX, originY;
		int snapX, snapY;
		boolean rotated;

		/** Converts a local offset to a pixel offset from the snapped origin (keeps edges consistent). */
		public int dx(float local) {
			return rotated ? Math.round(local * pixelScale) : Math.round(originX + local * pixelScale) - snapX;
		}

		public int dy(float local) {
			return rotated ? Math.round(local * pixelScale) : Math.round(originY + local * pixelScale) - snapY;
		}
	}

	private static final Frame FRAME = new Frame();

	/** Enters a frame whose origin is (x, y) in the current pose and whose unit is one physical pixel. Call {@link #end}. */
	public static Frame frame(GuiGraphics g, float x, float y) {
		Matrix3x2f m = g.pose();
		float ax = m.m00() * x + m.m10() * y + m.m20();
		float ay = m.m01() * x + m.m11() * y + m.m21();
		float ps = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01());
		float rot = (float) Math.atan2(m.m01(), m.m00());
		float gs = guiScale();
		Frame f = FRAME;
		f.pixelScale = ps * gs;
		f.stableScale = Math.abs(f.pixelScale - Math.round(f.pixelScale)) < 0.01f;
		f.rotated = Math.abs(rot) > 1e-4f;
		f.originX = ax * gs;
		f.originY = ay * gs;
		f.snapX = Math.round(f.originX);
		f.snapY = Math.round(f.originY);
		g.pose().pushMatrix();
		g.pose().identity();
		g.pose().scale(1f / gs, 1f / gs);
		g.pose().translate(f.snapX, f.snapY);
		if (f.rotated) g.pose().rotate(rot);
		return f;
	}

	public static void end(GuiGraphics g) {
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ generated masks

	private static final Map<Long, Identifier> MASKS = new HashMap<>();

	/** A white anti-aliased disk (stroke 0) or ring (stroke > 0) of radius r pixels in a 2r x 2r texture. */
	private static Identifier mask(int r, int stroke) {
		long key = (long) r << 16 | stroke;
		Identifier id = MASKS.get(key);
		if (id != null) return id;
		int size = Math.max(1, r * 2);
		NativeImage img = new NativeImage(size, size, true);
		float c = r;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				// 4x4 supersampling for clean edges
				float cov = 0;
				for (int sy = 0; sy < 4; sy++) {
					for (int sx = 0; sx < 4; sx++) {
						float px = x + (sx + 0.5f) / 4f - c, py = y + (sy + 0.5f) / 4f - c;
						float d = (float) Math.sqrt(px * px + py * py);
						boolean in = d <= r && (stroke <= 0 || d >= r - stroke);
						if (in) cov++;
					}
				}
				int a = Math.round(cov / 16f * 255);
				if (a > 0) img.setPixel(x, y, a << 24 | 0xFFFFFF);
			}
		}
		id = DemonClient.id("hdmask/" + r + "_" + stroke);
		mc.getTextureManager().register(id, new DynamicTexture(() -> "Demon mask", img));
		MASKS.put(key, id);
		return id;
	}

	private static void quarter(GuiGraphics g, Identifier tex, int r, int qx, int qy, int x, int y, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, qx * r, qy * r, r, r, r * 2, r * 2, color);
	}

	/** Filled rounded rectangle in pixel coordinates of the current frame. */
	public static void roundRect(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int color) {
		int w = x1 - x0, h = y1 - y0;
		if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) {
			g.fill(x0, y0, x1, y1, color);
			return;
		}
		g.fill(x0 + r, y0, x1 - r, y1, color);
		g.fill(x0, y0 + r, x0 + r, y1 - r, color);
		g.fill(x1 - r, y0 + r, x1, y1 - r, color);
		Identifier m = mask(r, 0);
		quarter(g, m, r, 0, 0, x0, y0, color);
		quarter(g, m, r, 1, 0, x1 - r, y0, color);
		quarter(g, m, r, 0, 1, x0, y1 - r, color);
		quarter(g, m, r, 1, 1, x1 - r, y1 - r, color);
	}

	/** Rounded rectangle outline with the given stroke width in pixels. */
	public static void roundOutline(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int stroke, int color) {
		int w = x1 - x0, h = y1 - y0;
		if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
		r = Math.max(stroke, Math.min(r, Math.min(w, h) / 2));
		g.fill(x0 + r, y0, x1 - r, y0 + stroke, color);
		g.fill(x0 + r, y1 - stroke, x1 - r, y1, color);
		g.fill(x0, y0 + r, x0 + stroke, y1 - r, color);
		g.fill(x1 - stroke, y0 + r, x1, y1 - r, color);
		Identifier m = mask(r, stroke);
		quarter(g, m, r, 0, 0, x0, y0, color);
		quarter(g, m, r, 1, 0, x1 - r, y0, color);
		quarter(g, m, r, 0, 1, x0, y1 - r, color);
		quarter(g, m, r, 1, 1, x1 - r, y1 - r, color);
	}

	/** Rounded rectangle with a horizontal gradient. */
	public static void roundGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int left, int right) {
		int w = x1 - x0, h = y1 - y0;
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		float t0 = r / (float) w, t1 = 1 - t0;
		int cl = com.demon.client.util.Theme.lerp(left, right, t0);
		int cr = com.demon.client.util.Theme.lerp(left, right, t1);
		// left & right columns (solid), centre gradient
		if (r > 0) {
			Identifier m = mask(r, 0);
			g.fill(x0, y0 + r, x0 + r, y1 - r, cl);
			g.fill(x1 - r, y0 + r, x1, y1 - r, cr);
			quarter(g, m, r, 0, 0, x0, y0, cl);
			quarter(g, m, r, 0, 1, x0, y1 - r, cl);
			quarter(g, m, r, 1, 0, x1 - r, y0, cr);
			quarter(g, m, r, 1, 1, x1 - r, y1 - r, cr);
		}
		hGradient(g, x0 + r, y0, x1 - r, y1, cl, cr);
	}

	/** Horizontal gradient rectangle in pixels (rotated vertical gradient, one quad). */
	public static void hGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int left, int right) {
		if (x1 <= x0 || y1 <= y0) return;
		g.pose().pushMatrix();
		g.pose().translate(x0, y1);
		g.pose().rotate((float) (-Math.PI / 2));
		g.fillGradient(0, 0, y1 - y0, x1 - x0, left, right);
		g.pose().popMatrix();
	}

	public static void disk(GuiGraphics g, int cx, int cy, int r, int color) {
		if (r <= 0) return;
		g.blit(RenderPipelines.GUI_TEXTURED, mask(r, 0), cx - r, cy - r, 0, 0, r * 2, r * 2, r * 2, r * 2, color);
	}

	public static void ring(GuiGraphics g, int cx, int cy, int r, int stroke, int color) {
		if (r <= 0) return;
		g.blit(RenderPipelines.GUI_TEXTURED, mask(r, Math.min(stroke, r)), cx - r, cy - r, 0, 0, r * 2, r * 2, r * 2, r * 2, color);
	}

	// ------------------------------------------------------------------ images

	private static final class LinearTexture extends DynamicTexture {
		LinearTexture(NativeImage image) {
			super(() -> "Demon HD image", image);
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}

	private static final Map<String, NativeImage> SOURCES = new HashMap<>();
	private static final Map<String, Identifier> LINEAR = new HashMap<>();
	private static final Map<String, Identifier> RESIZED = new LinkedHashMap<>(64, 0.75f, true);
	private static int resizesThisFrame;
	private static long resizeFrame;

	private static NativeImage source(String name) {
		if (SOURCES.containsKey(name)) return SOURCES.get(name);
		NativeImage img = null;
		try (InputStream in = mc.getResourceManager().open(DemonClient.id("textures/hd/" + name + ".png"))) {
			img = NativeImage.read(in);
			premultiply(img);
		} catch (Exception e) {
			DemonClient.LOG.warn("Missing HD image {}", name);
		}
		SOURCES.put(name, img);
		return img;
	}

	private static void premultiply(NativeImage img) {
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int c = img.getPixel(x, y);
				int a = c >>> 24;
				if (a == 255) continue;
				int r = (c >> 16 & 255) * a / 255, gg = (c >> 8 & 255) * a / 255, b = (c & 255) * a / 255;
				img.setPixel(x, y, a << 24 | r << 16 | gg << 8 | b);
			}
		}
	}

	private static void unpremultiply(NativeImage img) {
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int c = img.getPixel(x, y);
				int a = c >>> 24;
				if (a == 0 || a == 255) continue;
				int r = Math.min(255, (c >> 16 & 255) * 255 / a), gg = Math.min(255, (c >> 8 & 255) * 255 / a), b = Math.min(255, (c & 255) * 255 / a);
				img.setPixel(x, y, a << 24 | r << 16 | gg << 8 | b);
			}
		}
	}

	private static Identifier linear(String name) {
		Identifier id = LINEAR.get(name);
		if (id != null) return id;
		NativeImage src = source(name);
		if (src == null) return null;
		NativeImage copy = new NativeImage(src.getWidth(), src.getHeight(), false);
		src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), copy);
		unpremultiply(copy);
		id = DemonClient.id("hdimg/" + name + "_linear");
		mc.getTextureManager().register(id, new LinearTexture(copy));
		LINEAR.put(name, id);
		return id;
	}

	private static Identifier resized(String name, int w, int h, boolean allowBake) {
		String key = name + "@" + w + "x" + h;
		Identifier id = RESIZED.get(key);
		if (id != null) return id;
		long frame = System.nanoTime() / 16_000_000L;
		if (frame != resizeFrame) {
			resizeFrame = frame;
			resizesThisFrame = 0;
		}
		if (!allowBake && resizesThisFrame >= 3) return null;
		NativeImage src = source(name);
		if (src == null) return null;
		resizesThisFrame++;
		NativeImage dst = new NativeImage(w, h, true);
		src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), dst);
		unpremultiply(dst);
		id = DemonClient.id("hdimg/" + name.replace('/', '_') + "_" + w + "x" + h);
		mc.getTextureManager().register(id, new DynamicTexture(() -> "Demon HD image", dst));
		RESIZED.put(key, id);
		if (RESIZED.size() > 400) {
			var it = RESIZED.entrySet().iterator();
			Map.Entry<String, Identifier> eldest = it.next();
			mc.getTextureManager().release(eldest.getValue());
			it.remove();
		}
		return id;
	}

	/** Draws an image from textures/hd at (x, y, w, h) in the current pose, tinted with colour. */
	public static void image(GuiGraphics g, String name, float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
		Frame f = frame(g, x, y);
		int pw = f.dx(w), ph = f.dy(h);
		if (pw > 0 && ph > 0) {
			Identifier id = f.rotated ? null : resized(name, pw, ph, f.stableScale);
			if (id != null) {
				g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, pw, ph, pw, ph, color);
			} else {
				Identifier lin = linear(name);
				NativeImage src = SOURCES.get(name);
				if (lin != null && src != null) {
					g.pose().pushMatrix();
					g.pose().scale(pw / (float) src.getWidth(), ph / (float) src.getHeight());
					g.blit(RenderPipelines.GUI_TEXTURED, lin, 0, 0, 0, 0, src.getWidth(), src.getHeight(), src.getWidth(), src.getHeight(), color);
					g.pose().popMatrix();
				}
			}
		}
		end(g);
	}
}
