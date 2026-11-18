package com.demon.client.module.modules.player;

import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import org.lwjgl.glfw.GLFW;

/** Press the key (default N) to copy your coordinates to the clipboard. */
public class CopyCoordsModule extends Module {
	public CopyCoordsModule() {
		super("Copy Coords", "Press the key to copy your coordinates", Category.PLAYER, GLFW.GLFW_KEY_N, "flag");
	}

	@Override
	public boolean toggleable() {
		return false;
	}

	@Override
	public void onKeyPressed() {
		if (mc.player == null) return;
		String c = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
		mc.keyboardHandler.setClipboard(c);
		Notifications.push("Coordinates copied", c, Theme.accent2(), 2f);
		Sounds.click();
	}
}
