package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.phys.Vec3;

/** A glowing trail of where you have been, so you never lose your way back. */
public class BreadcrumbsModule extends Module {
	public final NumberSetting length = add(new NumberSetting("Length", "Number of trail points", 600, 50, 3000, 50));
	private final Deque<Vec3> points = new ArrayDeque<>();
	private Object level;

	public BreadcrumbsModule() {
		super("Breadcrumbs", "Glowing trail showing where you walked", Category.RENDER, -1, "compass");
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		if (mc.level != level) {
			level = mc.level;
			points.clear();
		}
		Vec3 p = mc.player.position().add(0, 0.1, 0);
		if (points.isEmpty() || points.peekLast().distanceToSqr(p) > 0.36) points.addLast(p);
		while (points.size() > length.asInt()) points.pollFirst();
	}

	@Override
	public void onRender3D(Render3D r) {
		Vec3 prev = null;
		int i = 0, n = points.size();
		for (Vec3 p : points) {
			if (prev != null && prev.distanceToSqr(p) < 64) {
				float t = i / (float) Math.max(1, n);
				r.line(prev.x, prev.y, prev.z, p.x, p.y, p.z, Theme.withAlpha(Theme.accent(t), (int) (40 + 200 * t)), 2.5f);
			}
			prev = p;
			i++;
		}
	}
}
