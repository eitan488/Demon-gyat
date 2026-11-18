package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;

/** Client-side time of day. */
public class TimeChangerModule extends Module {
	public final NumberSetting time = add(new NumberSetting("Time", "0 = sunrise, 6000 = noon, 13000 = night", 6000, 0, 24000, 500));

	public TimeChangerModule() {
		super("Time Changer", "Set your own time of day (client only)", Category.RENDER, -1, "timer");
	}
}
