package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;

/** Moves, rotates and scales your first-person hands and slows down the swing animation. */
public class ViewModelModule extends Module {
	public final NumberSetting x = add(new NumberSetting("X", "Sideways offset", 0.0, -1, 1, 0.05));
	public final NumberSetting y = add(new NumberSetting("Y", "Vertical offset", 0.0, -1, 1, 0.05));
	public final NumberSetting z = add(new NumberSetting("Z", "Depth offset", 0.0, -1, 1, 0.05));
	public final NumberSetting rotY = add(new NumberSetting("Rotation", "Item rotation", 0, -90, 90, 5));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Hand size", 1.0, 0.3, 1.5, 0.05));
	public final NumberSetting swingSpeed = add(new NumberSetting("Swing Duration", "Higher = slower swing animation", 1.0, 0.5, 3.0, 0.1, "x"));

	public ViewModelModule() {
		super("View Model", "Hand position, size and swing animation speed", Category.RENDER, -1, "camera");
	}
}
