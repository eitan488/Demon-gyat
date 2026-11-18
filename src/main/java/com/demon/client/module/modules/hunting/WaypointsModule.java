package com.demon.client.module.modules.hunting;

import com.demon.client.gui.Notifications;
import com.demon.client.gui.WorldLabel;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Waypoints with sky beams and labels. Press the keybind (B) to drop one where you stand. */
public class WaypointsModule extends Module {
	public final BoolSetting beams = add(new BoolSetting("Beams", "Sky beam at every waypoint", true));
	public final BoolSetting labels = add(new BoolSetting("Labels", "Name and distance labels", true));
	public final BoolSetting autoFinds = add(new BoolSetting("Auto From Finds", "Add a waypoint for every Chunk Finder find", true));
	public final BoolSetting deaths = add(new BoolSetting("Death Points", "Add a waypoint where you die", true));
	public final BoolSetting portalLink = add(new BoolSetting("Portal Linking", "Show overworld waypoints in the nether at x/8 (and nether ones in the overworld at x8)", true));
	public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Hide waypoints further than this (0 = never)", 0, 0, 20000, 500, "m"));

	private int counter;

	public WaypointsModule() {
		super("Waypoints", "Beams and labels for saved spots; press the key to add one", Category.HUNTING, GLFW.GLFW_KEY_B, "flag");
	}

	@Override
	public void onKeyPressed() {
		if (mc.player == null) return;
		if (!isEnabled()) setEnabled(true);
		int n = ++counter;
		var w = Waypoints.add("Waypoint " + Waypoints.all().size(), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), Theme.accent2(), "manual" + n);
		Notifications.push("Waypoint added", w.name + " • " + w.x + ", " + w.y + ", " + w.z, Theme.accent2(), 2.5f);
		Sounds.play(SoundEvents.NOTE_BLOCK_CHIME, 1.6f, 0.6f);
	}

	@Override
	public String info() {
		return String.valueOf(Waypoints.here().size());
	}

	private boolean visible(Waypoints.Waypoint w) {
		if (maxDistance.get() <= 0) return true;
		double dx = w.x - mc.player.getX(), dz = w.z - mc.player.getZ();
		return dx * dx + dz * dz < maxDistance.get() * maxDistance.get();
	}

	private java.util.List<Waypoints.Waypoint> shown() {
		java.util.List<Waypoints.Waypoint> list = new java.util.ArrayList<>(Waypoints.here());
		if (portalLink.on()) list.addAll(Waypoints.linked());
		return list;
	}

	@Override
	public void onRender3D(Render3D r) {
		if (!beams.on() || mc.player == null) return;
		for (Waypoints.Waypoint w : shown()) {
			if (!visible(w)) continue;
			double x = w.x + 0.5, z = w.z + 0.5;
			r.line(x, w.y, z, x, w.y + 200, z, Theme.withAlpha(w.color, 220), Theme.withAlpha(w.color, 0), 3.5f);
			r.outlineBox(new net.minecraft.world.phys.AABB(w.x, w.y, w.z, w.x + 1, w.y + 1, w.z + 1), Theme.withAlpha(w.color, 200), 2f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (!labels.on() || mc.player == null) return;
		Vec3 cam = Render3D.cameraPos();
		for (Waypoints.Waypoint w : shown()) {
			if (!visible(w)) continue;
			Vec3 p = new Vec3(w.x + 0.5, w.y + 2.2, w.z + 0.5);
			double d = p.distanceTo(cam);
			// far waypoints are pinned at a comfortable height in front of the camera
			Vec3 shown = d > 120 ? cam.add(p.subtract(cam).normalize().scale(120)) : p;
			WorldLabel.draw(g, shown, w.name, WorldLabel.distance(p, cam) + "  ·  " + w.x + ", " + w.z, w.color, 1f);
		}
	}
}
