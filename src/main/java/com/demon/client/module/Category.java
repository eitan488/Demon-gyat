package com.demon.client.module;

import com.demon.client.DemonClient;
import net.minecraft.resources.Identifier;

public enum Category {
	HUNTING("Hunting", "hunting"),
	RENDER("Render", "render"),
	CLIENT("Client", "client");

	public final String title;
	public final Identifier icon;

	Category(String title, String icon) {
		this.title = title;
		this.icon = DemonClient.id("icon_" + icon);
	}
}
