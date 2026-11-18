package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;

/** Full brightness everywhere (caves, nights, the nether). Works through the night-vision lightmap path. */
public class FullbrightModule extends Module {
	public FullbrightModule() {
		super("Fullbright", "See clearly in the dark", Category.RENDER, -1, "sun");
	}
}
