package com.demon.client.module.modules.movement;

import com.demon.client.module.Category;
import com.demon.client.module.Module;

/** Stops you from walking off edges (like sneaking, without slowing down). */
public class SafeWalkModule extends Module {
	public SafeWalkModule() {
		super("Safe Walk", "Prevents walking off block edges", Category.MOVEMENT, -1, "shield");
	}
}
