package com.demon.client.module.modules.hud;

import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

public class ServerInfoModule extends HudPanelModule {
	public ServerInfoModule() {
		super("Server Info", "Server address, players online, ping and dimension", "multiplayer", HudLayout.Anchor.TOP_LEFT);
	}

	@Override
	protected float[] size() {
		return new float[] {110, 33};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		String ip = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer";
		int players = mc.getConnection() != null ? mc.getConnection().getOnlinePlayers().size() : 1;
		String ping = "-";
		if (mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) ping = info.getLatency() + "ms";
		}
		String dim = mc.level.dimension().identifier().getPath().replace("the_", "").replace('_', ' ');
		Render2D.text(g, HdFont.SEMIBOLD, Render2D.trim(HdFont.SEMIBOLD, ip, 6f, w - 12), x + 6, y + 5, 6f, Theme.TEXT);
		Render2D.text(g, HdFont.MEDIUM, players + " online  ·  " + ping, x + 6, y + 14, 5.4f, Theme.SUBTEXT);
		Render2D.text(g, HdFont.MEDIUM, dim.substring(0, 1).toUpperCase() + dim.substring(1), x + 6, y + 23, 5.4f, Theme.accent2());
	}
}
