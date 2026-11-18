package com.demon.client.module.modules.hunting;

import com.demon.client.gui.FindsScreen;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.util.Finds;
import org.lwjgl.glfw.GLFW;

/** Opens the Finds browser (key J or click in the menu). */
public class FindsModule extends Module {
	public FindsModule() {
		super("Finds", "Browse every stash, base, farm and build found on this server (J)", Category.HUNTING, GLFW.GLFW_KEY_J, "flag");
	}

	@Override
	public boolean toggleable() {
		return false;
	}

	@Override
	public boolean hasAction() {
		return true;
	}

	@Override
	public void action() {
		mc.setScreen(new FindsScreen(mc.screen));
	}

	@Override
	public void onKeyPressed() {
		action();
	}

	@Override
	public String info() {
		return mc.player == null ? null : String.valueOf(Finds.all().size());
	}
}
