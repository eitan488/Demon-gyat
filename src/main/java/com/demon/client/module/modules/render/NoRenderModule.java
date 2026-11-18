package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;

/** Removes distracting effects. */
public class NoRenderModule extends Module {
	public final BoolSetting hurtCam = add(new BoolSetting("Hurt Camera", "No screen shake when hurt", true));
	public final BoolSetting fire = add(new BoolSetting("Fire Overlay", "No fire covering your screen", true));
	public final BoolSetting weather = add(new BoolSetting("Weather", "No rain or thunder", true));
	public final BoolSetting totem = add(new BoolSetting("Totem Animation", "No big totem pop animation", false));

	public NoRenderModule() {
		super("No Render", "Removes hurt cam, fire overlay, weather and more", Category.RENDER, -1, "render");
	}
}
