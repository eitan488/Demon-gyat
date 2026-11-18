package com.demon.client.module.modules.hunting;

import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Alerts you when players enter or leave your render distance. */
public class VisualRangeModule extends Module {
	public final BoolSetting leave = add(new BoolSetting("Leave Alerts", "Also alert when players leave", true));
	public final BoolSetting sound = add(new BoolSetting("Sound", "Play an alert sound", true));
	private final Map<UUID, String> current = new HashMap<>();
	private Object level;

	public VisualRangeModule() {
		super("Visual Range", "Alerts when players enter or leave your render distance", Category.HUNTING, -1, "radar");
	}

	@Override
	public void onTick() {
		if (mc.level == null || mc.player == null) return;
		boolean fresh = mc.level != level;
		level = mc.level;
		Set<UUID> now = new HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof Player p) || p == mc.player) continue;
			now.add(p.getUUID());
			if (!current.containsKey(p.getUUID()) && !fresh) {
				Notifications.push("Player spotted", p.getGameProfile().name() + " • " + (int) p.distanceTo(mc.player) + "m away", Theme.DANGER, 3.5f);
				if (sound.on()) Sounds.play(SoundEvents.NOTE_BLOCK_PLING, 0.7f, 1f);
			}
			current.put(p.getUUID(), p.getGameProfile().name());
		}
		for (var it = current.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			if (now.contains(e.getKey())) continue;
			if (leave.on() && !fresh) Notifications.push("Player left", e.getValue() + " left your range", Theme.SUBTEXT, 2.5f);
			it.remove();
		}
	}

	@Override
	public String info() {
		return String.valueOf(current.size());
	}
}
