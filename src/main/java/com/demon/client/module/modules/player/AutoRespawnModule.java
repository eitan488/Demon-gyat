package com.demon.client.module.modules.player;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import net.minecraft.client.gui.screens.DeathScreen;

/** Respawns instantly when you die. */
public class AutoRespawnModule extends Module {
	public AutoRespawnModule() {
		super("Auto Respawn", "Respawns instantly after death", Category.PLAYER, -1, "skull");
	}

	@Override
	public void onTick() {
		if (mc.screen instanceof DeathScreen && mc.player != null) {
			mc.player.respawn();
			mc.setScreen(null);
		}
	}
}
