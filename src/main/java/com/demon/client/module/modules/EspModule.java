package com.demon.client.module.modules;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.gui.Ui;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.scan.Target;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Highlights storage, redstone and base blocks through walls, with live counts. */
public class EspModule extends Module {
	public enum Style {
		BOTH("Fill + Outline"), OUTLINE("Outline"), FILL("Fill");

		final String title;

		Style(String t) {
			title = t;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public final BoolSetting storage = add(new BoolSetting("Storage", "Chests, shulkers, barrels, ender chests", true));
	public final BoolSetting redstone = add(new BoolSetting("Redstone", "Hoppers, pistons, observers, dust...", true));
	public final BoolSetting baseBlocks = add(new BoolSetting("Base Blocks", "Beds, beacons, anvils, signs, furnaces...", true));
	public final BoolSetting entities = add(new BoolSetting("Entities", "Item frames, armor stands, storage minecarts", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance to highlight", 128, 16, 512, 16, "m"));
	public final EnumSetting<Style> style = add(new EnumSetting<>("Style", "How boxes are drawn", Style.BOTH));
	public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", "Opacity of filled boxes", 18, 2, 80, 1, "%"));
	public final NumberSetting lineWidth = add(new NumberSetting("Line Width", "Outline thickness", 2.0, 1.0, 6.0, 0.5));
	public final BoolSetting tracers = add(new BoolSetting("Tracers", "Draw lines to highlighted blocks", false));
	public final BoolSetting labels = add(new BoolSetting("Labels", "Name tags on nearby highlighted blocks", true));
	public final NumberSetting labelRange = add(new NumberSetting("Label Range", "Distance for name tags", 24, 4, 96, 4, "m"));
	public final BoolSetting counter = add(new BoolSetting("Counter", "HUD panel with totals in range", true));

	private static final int MAX_BOXES = 4000;
	private final int[] inRange = new int[Target.VALUES.length];
	private final int[] shownCounts = new int[Target.VALUES.length];
	private final List<Label> labelList = new ArrayList<>();
	private int lastTotal;

	private static final class Label {
		Vec3 pos;
		final Target target;
		final double distSq;
		int count = 1;

		Label(Vec3 pos, Target target, double distSq) {
			this.pos = pos;
			this.target = target;
			this.distSq = distSq;
		}
	}

	public EspModule() {
		super("ESP", "See storage, redstone and base blocks through walls", Category.RENDER, -1, "cube");
	}

	public boolean groupEnabled(Target.Group g) {
		return switch (g) {
			case STORAGE -> storage.on();
			case REDSTONE -> redstone.on();
			case BASE -> baseBlocks.on();
			case ENTITY -> entities.on();
		};
	}

	@Override
	public String info() {
		return String.valueOf(lastTotal);
	}

	@Override
	public void onRender3D(Render3D r) {
		if (mc.level == null || mc.player == null) return;
		java.util.Arrays.fill(inRange, 0);
		labelList.clear();
		// measure from the camera so ranges and tags follow freecam
		Vec3 eye = r.camera();
		double rangeSq = range.get() * range.get();
		double labelSq = labelRange.get() * labelRange.get();
		int fillAlpha = (int) (fillOpacity.get() / 100.0 * 255);
		float width = lineWidth.asFloat();
		int drawn = 0;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.hits() == 0) continue;
			double cdx = d.centerX() - eye.x, cdz = d.centerZ() - eye.z;
			double reach = range.get() + 12;
			if (cdx * cdx + cdz * cdz > reach * reach) continue;
			for (int i = 0; i < d.hits(); i++) {
				Target t = d.kind(i);
				if (!groupEnabled(t.group)) continue;
				long p = d.pos(i);
				int bx = BlockPos.getX(p), by = BlockPos.getY(p), bz = BlockPos.getZ(p);
				double dx = bx + 0.5 - eye.x, dy = by + 0.5 - eye.y, dz = bz + 0.5 - eye.z;
				double distSq = dx * dx + dy * dy + dz * dz;
				if (distSq > rangeSq) continue;
				inRange[t.ordinal()]++;
				if (drawn >= MAX_BOXES) continue;
				mpos.set(bx, by, bz);
				AABB box = shapeOf(mpos);
				drawBox(r, box, t.color, fillAlpha, width);
				if (tracers.on()) r.tracer(box.getCenter(), t.color, 1.5f);
				if (labels.on() && distSq < labelSq) labelList.add(new Label(new Vec3(bx + 0.5, box.maxY + 0.35, bz + 0.5), t, distSq));
				drawn++;
			}
		}

		if (entities.on()) {
			float pt = r.partialTick();
			for (Entity e : mc.level.entitiesForRendering()) {
				Target t = Target.of(e);
				if (t == null) continue;
				Vec3 lerp = e.getPosition(pt);
				if (lerp.distanceToSqr(eye) > rangeSq) continue;
				inRange[t.ordinal()]++;
				AABB box = e.getBoundingBox().move(lerp.subtract(e.position())).inflate(0.03);
				drawBox(r, box, t.color, fillAlpha, width);
				if (tracers.on()) r.tracer(box.getCenter(), t.color, 1.5f);
				double dsq = lerp.distanceToSqr(eye);
				if (labels.on() && dsq < labelSq) labelList.add(new Label(new Vec3(lerp.x, box.maxY + 0.35, lerp.z), t, dsq));
			}
		}
		System.arraycopy(inRange, 0, shownCounts, 0, inRange.length);
		int total = 0;
		for (int c : inRange) total += c;
		lastTotal = total;
		mergeLabels();
	}

	/** Groups nearby labels of the same type into one tag with a count (e.g. "Shulker Box x12"). */
	private void mergeLabels() {
		labelList.sort((a, b) -> Double.compare(a.distSq, b.distSq));
		List<Label> merged = new ArrayList<>();
		for (Label l : labelList) {
			Label into = null;
			for (Label m : merged) {
				if (m.target == l.target && m.pos.distanceToSqr(l.pos) < 9 * Math.max(1, Math.sqrt(m.count))) {
					into = m;
					break;
				}
			}
			if (into == null) {
				merged.add(l);
			} else {
				into.pos = new Vec3((into.pos.x * into.count + l.pos.x) / (into.count + 1), Math.max(into.pos.y, l.pos.y),
					(into.pos.z * into.count + l.pos.z) / (into.count + 1));
				into.count++;
			}
			if (merged.size() > 48) break;
		}
		merged.sort((a, b) -> Double.compare(b.distSq, a.distSq));
		labelList.clear();
		labelList.addAll(merged);
	}

	private AABB shapeOf(BlockPos pos) {
		BlockState state = mc.level.getBlockState(pos);
		VoxelShape shape = state.getShape(mc.level, pos);
		if (shape.isEmpty()) return new AABB(pos).inflate(0.002);
		AABB b = shape.bounds();
		return b.move(pos.getX(), pos.getY(), pos.getZ()).inflate(0.002);
	}

	void drawBox(Render3D r, AABB box, int color, int fillAlpha, float width) {
		if (style.get() != Style.OUTLINE) r.filledBox(box, Theme.withAlpha(color, fillAlpha));
		if (style.get() != Style.FILL) r.outlineBox(box, Theme.withAlpha(color, 235), width);
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		int w = g.guiWidth(), h = g.guiHeight();
		if (labels.on()) {
			float size = 5.8f;
			for (Label l : labelList) {
				float[] s = Render3D.toScreen(l.pos, w, h);
				if (s == null) continue;
				String name = l.target.title;
				String count = l.count > 1 ? "x" + l.count : null;
				float tw = HdFont.SEMIBOLD.width(name, size) + (count != null ? 3 + HdFont.BOLD.width(count, size) : 0);
				float bw = tw + 11, bh = 9;
				float x = s[0] - bw / 2f, y = s[1] - bh - 2;
				Render2D.round(g, x, y, bw, bh, bh / 2f, 0xE0081020);
				Render2D.outline(g, x, y, bw, bh, bh / 2f, Theme.alpha(l.target.color, 0.55f));
				Render2D.circle(g, x + 4.5f, y + bh / 2f, 1.6f, l.target.color);
				float tx = x + 7.5f;
				tx += Render2D.text(g, HdFont.SEMIBOLD, name, tx, y + (bh - HdFont.SEMIBOLD.capHeight(size)) / 2f, size, Theme.TEXT) + 3;
				if (count != null) Render2D.text(g, HdFont.BOLD, count, tx, y + (bh - HdFont.BOLD.capHeight(size)) / 2f, size, l.target.color);
			}
		}
		if (counter.on()) drawCounter(g);
	}

	private void drawCounter(GuiGraphics g) {
		List<Target> present = new ArrayList<>();
		for (Target t : Target.VALUES) if (shownCounts[t.ordinal()] > 0) present.add(t);
		present.sort((a, b) -> Integer.compare(shownCounts[b.ordinal()], shownCounts[a.ordinal()]));
		if (present.size() > 5) present = present.subList(0, 5);
		float x = 5, y = 23;
		float rowH = 8.5f, size = 6f;
		float w = 96;
		float hgt = 15 + Math.max(1, present.size()) * rowH + 3;
		Ui.hud(g, x, y, w, hgt, 1f);
		Ui.marker(g, x + 4, y + 4, 6.5f, 1f);
		Render2D.text(g, HdFont.BOLD, "ESP", x + 8.5f, y + 5, 6.2f, Theme.TEXT);
		Render2D.textRight(g, HdFont.MEDIUM, lastTotal + " in " + range.display(), x + w - 5, y + 5, 5.6f, Theme.SUBTEXT);
		Render2D.rect(g, x + 5, y + 13, w - 10, 0.35f, 0x1F9CC4FF);
		float ry = y + 16;
		if (present.isEmpty()) {
			Render2D.text(g, HdFont.MEDIUM, "Nothing in range", x + 6, ry + 1, size, Theme.MUTED);
			return;
		}
		for (Target t : present) {
			Render2D.round(g, x + 6, ry + 1, 3.6f, 3.6f, 1.2f, t.color);
			Render2D.text(g, HdFont.MEDIUM, Render2D.trim(HdFont.MEDIUM, t.title, size, 60), x + 12.5f, ry + 0.8f, size, Theme.SUBTEXT);
			Render2D.textRight(g, HdFont.BOLD, String.valueOf(shownCounts[t.ordinal()]), x + w - 5, ry + 0.8f, size, t.color);
			ry += rowH;
		}
	}
}
