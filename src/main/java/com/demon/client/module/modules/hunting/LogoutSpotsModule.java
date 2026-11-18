package com.demon.client.module.modules.hunting;

import com.demon.client.gui.Notifications;
import com.demon.client.gui.WorldLabel;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shows a ghost where nearby players logged out; a base is often right there. */
public class LogoutSpotsModule extends Module {
	public final BoolSetting notify = add(new BoolSetting("Notify", "Toast when a nearby player logs out", true));

	private record Seen(String name, AABB box, Vec3 pos) {}

	private record Spot(UUID id, String name, AABB box, Vec3 pos, long time) {}

	private final Map<UUID, Seen> seen = new HashMap<>();
	private final List<Spot> spots = new ArrayList<>();
	private Object level;

	public LogoutSpotsModule() {
		super("Logout Spots", "Ghost markers where nearby players disconnected", Category.HUNTING, -1, "player");
	}

	@Override
	public void onTick() {
		if (mc.level != level) {
			level = mc.level;
			spots.clear();
			seen.clear();
		}
		if (mc.level == null) return;
		seen.clear();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof Player p && p != mc.player) {
				seen.put(p.getUUID(), new Seen(p.getGameProfile().name(), p.getBoundingBox(), p.position()));
				spots.removeIf(s -> s.id.equals(p.getUUID()));
			}
		}
	}

	public void onPlayersLeft(List<UUID> ids, List<String> names) {
		for (int i = 0; i < ids.size(); i++) {
			Seen s = seen.get(ids.get(i));
			if (s == null) continue;
			UUID id = ids.get(i);
			spots.removeIf(sp -> sp.id.equals(id));
			spots.add(new Spot(id, s.name, s.box, s.pos, System.currentTimeMillis()));
			if (notify.on()) {
				Notifications.push("Logout spot", s.name + " logged out at " + (int) s.pos.x + ", " + (int) s.pos.z, Theme.WARNING, 4f);
			}
		}
	}

	@Override
	public String info() {
		return String.valueOf(spots.size());
	}

	@Override
	public void onRender3D(Render3D r) {
		for (Spot s : spots) {
			r.filledBox(s.box, Theme.withAlpha(Theme.WARNING, 40));
			r.outlineBox(s.box, Theme.withAlpha(Theme.WARNING, 220), 2f);
		}
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		Vec3 cam = Render3D.cameraPos();
		for (Spot s : spots) {
			long mins = (System.currentTimeMillis() - s.time) / 60000;
			WorldLabel.draw(g, s.pos.add(0, s.box.getYsize() + 0.6, 0), s.name, "logged out " + (mins == 0 ? "just now" : mins + "m ago") + "  ·  " + WorldLabel.distance(s.pos, cam), Theme.WARNING, 1f);
		}
	}
}
