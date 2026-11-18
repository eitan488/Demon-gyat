package com.demon.client.mixin;

import com.demon.client.render.Render3D;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	/** Remember the real view/projection matrices so HUD cards can be placed over world positions. */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void demon$capture(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera,
			Matrix4f viewMatrix, Matrix4f projectionMatrix, Matrix4f cullProjection, GpuBufferSlice fog, Vector4f fogColor,
			boolean renderSky, CallbackInfo ci) {
		Render3D.capture(camera, viewMatrix, projectionMatrix);
	}
}
