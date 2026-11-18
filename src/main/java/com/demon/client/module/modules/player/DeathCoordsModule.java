package com.demon.client.module.modules.player;

import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import net.minecraft.network.chat.Component;

/** Remembers where you died: chat message, toast and a waypoint. */
public class DeathCoordsModule extends Module {
	private boolean wasDead;

	public DeathCoordsModule() {
		super("Death Coords", "Saves a waypoint and prints coords where you die", Category.PLAYER, -1, "skull");
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead) {
			int x = mc.player.getBlockX(), y = mc.player.getBlockY(), z = mc.player.getBlockZ();
			mc.player.displayClientMessage(Component.literal("[Demon] ").withColor(0x38BDF8)
				.append(Component.literal("You died at " + x + ", " + y + ", " + z).withColor(0xF87171)), false);
			Notifications.push("You died", x + ", " + y + ", " + z, Theme.DANGER, 5f);
			if (ModuleManager.waypoints.deaths.on()) Waypoints.add("Death", x, y, z, Theme.DANGER, "death" + System.currentTimeMillis());
		}
		wasDead = dead;
	}
}
