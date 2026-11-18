package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Replaces the thin black block outline with an animated HD highlight. */
public class BlockHighlightModule extends Module {
	public final BoolSetting fill = add(new BoolSetting("Fill", "Fill the block", true));
	public final NumberSetting width = add(new NumberSetting("Line Width", "Outline thickness", 2.5, 1, 6, 0.5));
	private final Anim.Value x = new Anim.Value(0, 30), y = new Anim.Value(0, 30), z = new Anim.Value(0, 30);
	private final Anim.Value x2 = new Anim.Value(0, 30), y2 = new Anim.Value(0, 30), z2 = new Anim.Value(0, 30);
	private boolean had;

	public BlockHighlightModule() {
		super("Block Highlight", "Animated HD outline on the block you look at", Category.RENDER, -1, "cube");
		// hide the vanilla outline while this module is on
		net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, state) -> !isEnabled());
	}

	@Override
	public void onRender3D(Render3D r) {
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || mc.level == null) {
			had = false;
			return;
		}
		BlockPos pos = hit.getBlockPos();
		VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
		AABB box = (shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos)).inflate(0.004);
		if (!had) {
			x.snap((float) box.minX); y.snap((float) box.minY); z.snap((float) box.minZ);
			x2.snap((float) box.maxX); y2.snap((float) box.maxY); z2.snap((float) box.maxZ);
			had = true;
		}
		AABB anim = new AABB(x.target((float) box.minX).get(), y.target((float) box.minY).get(), z.target((float) box.minZ).get(),
			x2.target((float) box.maxX).get(), y2.target((float) box.maxY).get(), z2.target((float) box.maxZ).get());
		float pulse = 0.75f + 0.25f * (float) Math.sin(Anim.time() * 4);
		if (fill.on()) r.filledBox(anim, Theme.withAlpha(Theme.accent1(), (int) (40 * pulse)));
		r.outlineBox(anim, Theme.withAlpha(Theme.accent2(), (int) (235 * pulse)), width.asFloat());
	}
}
