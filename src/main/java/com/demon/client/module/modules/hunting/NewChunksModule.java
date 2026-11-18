package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marks freshly generated chunks. New terrain sends flowing-liquid updates right after it loads,
 * old chunks (already visited by players) don't.
 */
public class NewChunksModule extends Module {
	public final NumberSetting height = add(new NumberSetting("Height", "Render height relative to your feet", -1, -64, 64, 1));
	public final NumberSetting opacity = add(new NumberSetting("Opacity", "Fill opacity", 30, 5, 90, 1, "%"));
	private final LongOpenHashSet chunks = new LongOpenHashSet();
	private Object level;

	public NewChunksModule() {
		super("New Chunks", "Marks newly generated chunks (red) so you can follow old trails", Category.HUNTING, -1, "chunk");
	}

	public void onBlockUpdate(BlockPos pos, BlockState state) {
		if (state.getFluidState().isEmpty() || state.getFluidState().isSource()) return;
		chunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
	}

	@Override
	public void onTick() {
		if (mc.level != level) {
			level = mc.level;
			chunks.clear();
		}
	}

	@Override
	public String info() {
		return String.valueOf(chunks.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.player == null) return;
		double y = Math.floor(mc.player.getY()) + height.get();
		int view = (mc.options.getEffectiveRenderDistance() + 1) * 16;
		int fill = Theme.withAlpha(0xFFF87171, (int) (opacity.get() * 2.55));
		int edge = Theme.withAlpha(0xFFF87171, 200);
		for (long key : chunks) {
			int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
			double x1 = cx << 4, z1 = cz << 4;
			if (Math.abs(x1 + 8 - mc.player.getX()) > view || Math.abs(z1 + 8 - mc.player.getZ()) > view) continue;
			r.plane(x1 + 0.3, z1 + 0.3, x1 + 15.7, z1 + 15.7, y, fill);
			r.line(x1 + 0.3, y, z1 + 0.3, x1 + 15.7, y, z1 + 0.3, edge, 1.5f);
			r.line(x1 + 15.7, y, z1 + 0.3, x1 + 15.7, y, z1 + 15.7, edge, 1.5f);
			r.line(x1 + 15.7, y, z1 + 15.7, x1 + 0.3, y, z1 + 15.7, edge, 1.5f);
			r.line(x1 + 0.3, y, z1 + 15.7, x1 + 0.3, y, z1 + 0.3, edge, 1.5f);
		}
	}
}
