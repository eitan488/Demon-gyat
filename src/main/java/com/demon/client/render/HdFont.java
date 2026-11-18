package com.demon.client.render;

import com.demon.client.DemonClient;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;

/**
 * High resolution text: glyphs are rasterised with FreeType at the exact physical pixel size they are
 * shown at, packed into an atlas and drawn 1:1 in screen pixels. Text stays razor sharp at every GUI scale.
 */
public final class HdFont {
	public static final HdFont REGULAR = new HdFont("inter-regular");
	public static final HdFont MEDIUM = new HdFont("inter-medium");
	public static final HdFont SEMIBOLD = new HdFont("inter-semibold");
	public static final HdFont BOLD = new HdFont("inter-bold");

	private static final int LOAD_FLAGS = FreeType.FT_LOAD_RENDER | (1 << 16); // FT_LOAD_TARGET_LIGHT
	private static final float CAP_HEIGHT = 0.7275f; // Inter cap height / em
	private static int bakesThisFrame;
	private static long frameMark;

	private final String name;
	private ByteBuffer data;
	private FT_Face face;
	private boolean failed;
	private final Int2ObjectOpenHashMap<Atlas> atlases = new Int2ObjectOpenHashMap<>();

	private HdFont(String name) {
		this.name = name;
	}

	private static final class Glyph {
		int u, v, w, h, left, top;
		float advance;
	}

	/** One atlas per pixel size. Glyphs are added lazily, so any character can be drawn. */
	private final class Atlas {
		final int px;
		final int size;
		final NativeImage image;
		final DynamicTexture texture;
		final Identifier id;
		final Map<Integer, Glyph> glyphs = new HashMap<>();
		int penX = 1, penY = 1, rowH;
		int capPx;
		boolean dirty;

		Atlas(int px) {
			this.px = px;
			this.size = px <= 20 ? 256 : px <= 40 ? 512 : px <= 80 ? 1024 : 2048;
			this.image = new NativeImage(size, size, true);
			this.image.fillRect(0, 0, size, size, 0);
			this.id = DemonClient.id("hdfont/" + name + "_" + px);
			this.texture = new DynamicTexture(() -> "Demon HD font " + name + " " + px, image);
			Minecraft.getInstance().getTextureManager().register(id, texture);
			Glyph h = glyph('H');
			capPx = h != null && h.h > 0 ? h.top : Math.round(px * CAP_HEIGHT);
			// pre-bake printable ASCII
			for (int c = 32; c < 127; c++) glyph(c);
			flush();
		}

		Glyph glyph(int codepoint) {
			Glyph g = glyphs.get(codepoint);
			if (g != null) return g;
			g = new Glyph();
			glyphs.put(codepoint, g);
			if (face == null) return g;
			synchronized (FreeTypeUtil.LIBRARY_LOCK) {
				FreeType.FT_Set_Pixel_Sizes(face, 0, px);
				int index = FreeType.FT_Get_Char_Index(face, codepoint);
				if (index == 0 && codepoint != ' ') {
					Glyph fallback = codepoint == '?' ? g : glyph('?');
					glyphs.put(codepoint, fallback);
					return fallback;
				}
				if (FreeType.FT_Load_Char(face, codepoint, LOAD_FLAGS) != 0) return g;
				FT_GlyphSlot slot = face.glyph();
				FT_Bitmap bmp = slot.bitmap();
				g.advance = slot.advance().x() / 64f;
				g.left = slot.bitmap_left();
				g.top = slot.bitmap_top();
				g.w = bmp.width();
				g.h = bmp.rows();
				if (g.w > 0 && g.h > 0) {
					if (penX + g.w + 1 > size) {
						penX = 1;
						penY += rowH + 1;
						rowH = 0;
					}
					if (penY + g.h + 1 > size) {
						g.w = g.h = 0; // atlas full; extremely unlikely
						return g;
					}
					ByteBuffer buf = bmp.buffer(Math.abs(bmp.pitch()) * g.h);
					int pitch = bmp.pitch();
					for (int y = 0; y < g.h; y++) {
						for (int x = 0; x < g.w; x++) {
							int a = buf.get(y * pitch + x) & 0xFF;
							if (a != 0) image.setPixel(penX + x, penY + y, a << 24 | 0xFFFFFF);
						}
					}
					g.u = penX;
					g.v = penY;
					penX += g.w + 1;
					rowH = Math.max(rowH, g.h);
					dirty = true;
				}
			}
			return g;
		}

		void flush() {
			if (dirty) {
				texture.upload();
				dirty = false;
			}
		}
	}

	private boolean ensureLoaded() {
		if (face != null) return true;
		if (failed) return false;
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(DemonClient.id("fonts/" + name + ".otf"))) {
			byte[] bytes = in.readAllBytes();
			data = MemoryUtil.memAlloc(bytes.length);
			data.put(bytes).flip();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pb = stack.mallocPointer(1);
				synchronized (FreeTypeUtil.LIBRARY_LOCK) {
					int err = FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), data, 0L, pb);
					if (err != 0) throw new IllegalStateException("FreeType error " + err);
					face = FT_Face.create(pb.get(0));
				}
			}
			return true;
		} catch (Exception e) {
			failed = true;
			DemonClient.LOG.error("Could not load HD font {}", name, e);
			return false;
		}
	}

	private Atlas atlas(int px, boolean allowBake) {
		Atlas a = atlases.get(px);
		if (a != null) return a;
		if (!ensureLoaded()) return null;
		long now = System.nanoTime() / 16_000_000L;
		if (now != frameMark) {
			frameMark = now;
			bakesThisFrame = 0;
		}
		if (!allowBake && bakesThisFrame >= 2) {
			// find the closest existing size instead of stalling the frame
			Atlas best = null;
			for (Atlas c : atlases.values()) if (best == null || Math.abs(c.px - px) < Math.abs(best.px - px)) best = c;
			if (best != null) return best;
		}
		bakesThisFrame++;
		a = new Atlas(px);
		atlases.put(px, a);
		return a;
	}

	// ------------------------------------------------------------------ API (sizes are in GUI units)

	/** Width of a string in GUI units at the given size. */
	public float width(String s, float size) {
		float gs = Hd.guiScale();
		int px = Math.max(1, Math.round(size * gs));
		Atlas a = atlas(px, true);
		if (a == null) return Minecraft.getInstance().font.width(s) * size / 9f;
		float w = 0;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			i += Character.charCount(cp);
			w += a.glyph(cp).advance;
		}
		a.flush();
		return w / gs;
	}

	/** Cap height in GUI units (useful for vertical centring). */
	public float capHeight(float size) {
		return size * CAP_HEIGHT;
	}

	/**
	 * Draws text with the top of capital letters at (x, y) in the current pose.
	 * Returns the width drawn in GUI units.
	 */
	public float draw(GuiGraphics g, String s, float x, float y, float size, int color) {
		if (s == null || s.isEmpty() || (color >>> 24) < 4) return 0;
		Hd.Frame f = Hd.frame(g, x, y);
		int px = Math.max(1, Math.round(size * f.pixelScale));
		Atlas a = atlas(px, f.stableScale);
		if (a == null) {
			Hd.end(g);
			g.drawString(Minecraft.getInstance().font, s, Math.round(x), Math.round(y), color, false);
			return Minecraft.getInstance().font.width(s);
		}
		// if a nearby size was substituted, scale it to the requested size
		float fix = px / (float) a.px;
		if (Math.abs(fix - 1f) > 0.001f) g.pose().scale(fix, fix);
		float pen = 0;
		int baseline = a.capPx;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			i += Character.charCount(cp);
			Glyph gl = a.glyph(cp);
			if (gl.w > 0) {
				int gx = Math.round(pen) + gl.left;
				int gy = baseline - gl.top;
				g.blit(RenderPipelines.GUI_TEXTURED, a.id, gx, gy, gl.u, gl.v, gl.w, gl.h, a.size, a.size, color);
			}
			pen += gl.advance;
		}
		a.flush();
		Hd.end(g);
		return pen * fix / f.pixelScale;
	}
}
