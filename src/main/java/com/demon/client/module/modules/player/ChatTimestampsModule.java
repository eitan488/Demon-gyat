package com.demon.client.module.modules.player;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;

/** Adds the time to every chat message. */
public class ChatTimestampsModule extends Module {
	public final BoolSetting seconds = add(new BoolSetting("Seconds", "Include seconds", false));

	public ChatTimestampsModule() {
		super("Chat Timestamps", "Adds [HH:mm] before every chat message", Category.PLAYER, -1, "timer");
	}
}
