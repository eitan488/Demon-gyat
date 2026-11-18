package com.demon.client.module;

import com.demon.client.DemonClient;
import net.minecraft.resources.Identifier;

public enum Category {
	HUNTING("Hunting", "hunting"),
	PVP("PvP", "combat"),
	RENDER("Render", "render"),
	MOVEMENT("Movement", "movement"),
	PLAYER("Player", "player"),
	HUD("HUD", "hud"),
	CLIENT("Client", "client");

	public final String title;
	public final Identifier icon;

	Category(String title, String icon) {
		this.title = title;
		this.icon = DemonClient.id("icon_" + icon);
	}
}
