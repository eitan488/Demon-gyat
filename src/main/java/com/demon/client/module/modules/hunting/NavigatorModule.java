package com.demon.client.module.modules.hunting;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.Category;
import com.demon.client.module.HudPanelModule;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Finds;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Points you to the nearest (or latest) find or waypoint with an arrow, distance and ETA. */
public class NavigatorModule extends HudPanelModule {
	public enum Mode {
		NEAREST_FIND("Nearest Find"), LATEST_FIND("Latest Find"), NEAREST_WAYPOINT("Nearest Waypoint");

		final String t;

		Mode(String t) {
			this.t = t;
		}

		@Override
		public String toString() {
			return t;
		}
	}

	public final EnumSetting<Mode> mode = add(new EnumSetting<>("Target", "What to navigate to", Mode.NEAREST_FIND));
	public final BoolSetting guide = add(new BoolSetting("Guide Line", "3D line from you to the target", true));

	private String name;
	private int color;
	private Vec3 target;
	private final Anim.Value arrow = new Anim.Value(0, 10);
	private final Anim.Value speed = new Anim.Value(0, 3);

	public NavigatorModule() {
		super("Navigator", "Arrow, distance and ETA to your nearest find or waypoint", Category.HUNTING, "compass", HudLayout.Anchor.TOP_CENTER);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		target = null;
		double best = Double.MAX_VALUE;
		long latest = -1;
		if (mode.get() == Mode.NEAREST_WAYPOINT) {
			for (Waypoints.Waypoint w : Waypoints.here()) {
				double d = mc.player.distanceToSqr(w.x + 0.5, w.y, w.z + 0.5);
				if (d < best) {
					best = d;
					target = new Vec3(w.x + 0.5, w.y, w.z + 0.5);
					name = w.name;
					color = w.color;
				}
			}
			return;
		}
		for (Finds.Find f : Finds.here()) {
			double d = mc.player.distanceToSqr(f.x, f.y, f.z);
			boolean better = mode.get() == Mode.NEAREST_FIND ? d < best : f.time > latest;
			if (!better || d < 16 * 16) continue;
			best = d;
			latest = f.time;
			target = new Vec3(f.x, f.y, f.z);
			name = f.kind + " · " + f.summary;
			color = f.color;
		}
	}

	@Override
	protected float[] size() {
		return target == null ? null : new float[] {150, 26};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		double dx = target.x - mc.player.getX(), dz = target.z - mc.player.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		float ang = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float rel = Mth.wrapDegrees(ang - mc.player.getYRot());
		float shown = arrow.peek() + Mth.wrapDegrees(rel - arrow.peek());
		arrow.target(shown).get();
		// arrow pointing toward the target (up = straight ahead)
		float cx = x + 13, cy = y + h / 2f;
		Render2D.circle(g, cx, cy, 9, Theme.alpha(color, 0.18f));
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate((float) Math.toRadians(arrow.peek()));
		Render2D.line(g, 0, 5, 0, -5.5f, 0xFFFFFFFF);
		Render2D.line(g, 0, -6, -3.5f, -2.5f, 0xFFFFFFFF);
		Render2D.line(g, 0, -6, 3.5f, -2.5f, 0xFFFFFFFF);
		g.pose().popMatrix();
		double bps = speed.target((float) (mc.player.getDeltaMovement().horizontalDistance() * 20)).get();
		String eta = bps > 1 ? formatTime(dist / bps) : "—";
		Render2D.text(g, HdFont.SEMIBOLD, Render2D.trim(HdFont.SEMIBOLD, name, 6f, w - 32), x + 26, y + 5, 6f, Theme.TEXT);
		String d = dist >= 1000 ? String.format("%.1f km", dist / 1000) : (int) dist + " m";
		Render2D.text(g, HdFont.BOLD, d, x + 26, y + 15, 5.6f, color);
		Render2D.textRight(g, HdFont.MEDIUM, "ETA " + eta, x + w - 6, y + 15, 5.4f, Theme.SUBTEXT);
	}

	private static String formatTime(double seconds) {
		int s = (int) seconds;
		return s >= 3600 ? s / 3600 + "h " + (s % 3600) / 60 + "m" : s >= 60 ? s / 60 + "m " + s % 60 + "s" : s + "s";
	}

	@Override
	public void onRender3D(Render3D r) {
		if (!guide.on() || target == null || mc.player == null) return;
		Vec3 feet = mc.player.getPosition(r.partialTick()).add(0, 0.05, 0);
		Vec3 dir = new Vec3(target.x - feet.x, 0, target.z - feet.z);
		double len = Math.min(dir.length(), 40);
		if (len < 1) return;
		Vec3 end = feet.add(dir.normalize().scale(len));
		r.line(feet.x, feet.y, feet.z, end.x, feet.y, end.z, Theme.withAlpha(color, 220), Theme.withAlpha(color, 0), 3f);
	}

	@Override
	public String info() {
		return target == null ? null : WorldLabelDistance.of(target, mc.player.position());
	}

	private static final class WorldLabelDistance {
		static String of(Vec3 a, Vec3 b) {
			double d = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
			return d >= 1000 ? String.format("%.1fkm", d / 1000) : (int) d + "m";
		}
	}
}
