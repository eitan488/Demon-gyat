package com.demon.client.render;

import com.demon.client.DemonClient;
import com.demon.client.mixin.GameRendererAccessor;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Draws boxes and lines in the world that are visible through walls, and projects
 * world positions to screen space for the 2D info cards.
 */
public final class Render3D {
	private static final Minecraft mc = Minecraft.getInstance();

	public static final RenderPipeline ESP_LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
		.withLocation(DemonClient.id("pipeline/esp_lines"))
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.withDepthWrite(false)
		.build();

	public static final RenderPipeline ESP_QUADS_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
		.withLocation(DemonClient.id("pipeline/esp_quads"))
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.withCull(false)
		.build();

	public static final RenderType ESP_LINES = RenderType.create("demon_esp_lines",
		RenderSetup.builder(ESP_LINES_PIPELINE).bufferSize(1 << 18).createRenderSetup());

	public static final RenderType ESP_QUADS = RenderType.create("demon_esp_quads",
		RenderSetup.builder(ESP_QUADS_PIPELINE).bufferSize(1 << 18).createRenderSetup());

	private static final Render3D INSTANCE = new Render3D();
	private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 20));

	// Matrices captured at the start of world rendering (used to project to the screen in the HUD).
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static Vec3 cameraPos = Vec3.ZERO;
	private static boolean captured;

	private Vec3 cam = Vec3.ZERO;
	private float partialTick;

	private Render3D() {}

	public static void init() {
		WorldRenderEvents.END_MAIN.register(ctx -> INSTANCE.render());
	}

	/** Called from LevelRendererMixin at the start of renderLevel. */
	public static void capture(Camera camera, Matrix4f view, Matrix4f projection) {
		VIEW.set(view);
		PROJECTION.set(projection);
		cameraPos = camera.position();
		captured = true;
	}

	private void render() {
		if (mc.level == null || mc.player == null) return;
		cam = mc.gameRenderer.getMainCamera().position();
		partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		GpuBufferSlice previousFog = RenderSystem.getShaderFog();
		FogRenderer fog = ((GameRendererAccessor) mc.gameRenderer).demon$getFogRenderer();
		RenderSystem.setShaderFog(fog.getBuffer(FogRenderer.FogMode.NONE));
		try {
			for (Module m : ModuleManager.all()) {
				if (m.isEnabled()) m.onRender3D(this);
			}
			buffers.endBatch(ESP_QUADS);
			buffers.endBatch(ESP_LINES);
		} catch (Exception e) {
			DemonClient.LOG.error("ESP render failed", e);
			buffers.endBatch();
		} finally {
			RenderSystem.setShaderFog(previousFog);
		}
	}

	public float partialTick() {
		return partialTick;
	}

	public Vec3 camera() {
		return cam;
	}

	// ------------------------------------------------------------------ drawing (world coordinates)

	public void filledBox(AABB box, int color) {
		if ((color >>> 24) == 0) return;
		VertexConsumer vc = buffers.getBuffer(ESP_QUADS);
		float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
		float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
		// bottom
		quad(vc, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
		// top
		quad(vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
		// north / south
		quad(vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
		quad(vc, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
		// west / east
		quad(vc, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
		quad(vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
	}

	/** A horizontal plane at height y covering the given x/z rectangle. */
	public void plane(double minX, double minZ, double maxX, double maxZ, double y, int color) {
		if ((color >>> 24) == 0) return;
		VertexConsumer vc = buffers.getBuffer(ESP_QUADS);
		float x1 = (float) (minX - cam.x), z1 = (float) (minZ - cam.z), x2 = (float) (maxX - cam.x), z2 = (float) (maxZ - cam.z);
		float fy = (float) (y - cam.y);
		quad(vc, x1, fy, z1, x2, fy, z1, x2, fy, z2, x1, fy, z2, color);
	}

	/** A vertical gradient quad between two x/z points (used for glowing chunk walls). */
	public void wall(double ax, double az, double bx, double bz, double y1, double y2, int bottom, int top) {
		VertexConsumer vc = buffers.getBuffer(ESP_QUADS);
		float fax = (float) (ax - cam.x), faz = (float) (az - cam.z), fbx = (float) (bx - cam.x), fbz = (float) (bz - cam.z);
		float fy1 = (float) (y1 - cam.y), fy2 = (float) (y2 - cam.y);
		vc.addVertex(fax, fy1, faz).setColor(bottom);
		vc.addVertex(fbx, fy1, fbz).setColor(bottom);
		vc.addVertex(fbx, fy2, fbz).setColor(top);
		vc.addVertex(fax, fy2, faz).setColor(top);
	}

	private static void quad(VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color) {
		vc.addVertex(ax, ay, az).setColor(color);
		vc.addVertex(bx, by, bz).setColor(color);
		vc.addVertex(cx, cy, cz).setColor(color);
		vc.addVertex(dx, dy, dz).setColor(color);
	}

	public void outlineBox(AABB box, int color, float width) {
		if ((color >>> 24) == 0) return;
		double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
		line(x1, y1, z1, x2, y1, z1, color, width);
		line(x2, y1, z1, x2, y1, z2, color, width);
		line(x2, y1, z2, x1, y1, z2, color, width);
		line(x1, y1, z2, x1, y1, z1, color, width);
		line(x1, y2, z1, x2, y2, z1, color, width);
		line(x2, y2, z1, x2, y2, z2, color, width);
		line(x2, y2, z2, x1, y2, z2, color, width);
		line(x1, y2, z2, x1, y2, z1, color, width);
		line(x1, y1, z1, x1, y2, z1, color, width);
		line(x2, y1, z1, x2, y2, z1, color, width);
		line(x2, y1, z2, x2, y2, z2, color, width);
		line(x1, y1, z2, x1, y2, z2, color, width);
	}

	public void line(double ax, double ay, double az, double bx, double by, double bz, int color, float width) {
		line(ax, ay, az, bx, by, bz, color, color, width);
	}

	public void line(double ax, double ay, double az, double bx, double by, double bz, int colorA, int colorB, float width) {
		VertexConsumer vc = buffers.getBuffer(ESP_LINES);
		float x1 = (float) (ax - cam.x), y1 = (float) (ay - cam.y), z1 = (float) (az - cam.z);
		float x2 = (float) (bx - cam.x), y2 = (float) (by - cam.y), z2 = (float) (bz - cam.z);
		Vector3f n = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
		if (n.lengthSquared() < 1e-8f) return;
		n.normalize();
		vc.addVertex(x1, y1, z1).setColor(colorA).setNormal(n.x, n.y, n.z).setLineWidth(width);
		vc.addVertex(x2, y2, z2).setColor(colorB).setNormal(n.x, n.y, n.z).setLineWidth(width);
	}

	/** A tracer line from just in front of the camera to a world position. */
	public void tracer(Vec3 target, int color, float width) {
		Vector3f fwd = new Vector3f(mc.gameRenderer.getMainCamera().forwardVector());
		double sx = cam.x + fwd.x * 1.5, sy = cam.y + fwd.y * 1.5, sz = cam.z + fwd.z * 1.5;
		line(sx, sy, sz, target.x, target.y, target.z, color & 0x00FFFFFF | 0x30000000, color, width);
	}

	// ------------------------------------------------------------------ projection

	/**
	 * Projects a world position to GUI coordinates. Returns {x, y, depth} or null if behind the camera.
	 */
	public static float[] toScreen(Vec3 pos, int guiWidth, int guiHeight) {
		if (!captured) return null;
		Vector4f v = new Vector4f((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z), 1f);
		VIEW.transform(v);
		PROJECTION.transform(v);
		if (v.w <= 0.05f) return null;
		float nx = v.x / v.w, ny = v.y / v.w;
		return new float[] {(nx * 0.5f + 0.5f) * guiWidth, (0.5f - ny * 0.5f) * guiHeight, v.w};
	}

	public static Vec3 cameraPos() {
		return cameraPos;
	}
}
