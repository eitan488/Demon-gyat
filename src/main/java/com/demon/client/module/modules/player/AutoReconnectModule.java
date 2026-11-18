package com.demon.client.module.modules.player;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/** Reconnects to the last server after a kick or disconnect, with a countdown button. */
public class AutoReconnectModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds before reconnecting", 5, 1, 60, 1, "s"));
	private ServerData last;

	public AutoReconnectModule() {
		super("Auto Reconnect", "Reconnects to the server after you get disconnected", Category.PLAYER, -1, "plug");
		ClientPlayConnectionEvents.JOIN.register((h, s, client) -> {
			if (client.getCurrentServer() != null) last = client.getCurrentServer();
		});
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof DisconnectedScreen) || last == null) return;
			long start = System.currentTimeMillis();
			Button button = Button.builder(Component.literal("Reconnect"), b -> reconnect()).bounds(w / 2 - 100, h - 30, 200, 20).build();
			Screens.getButtons(screen).add(button);
			ScreenEvents.afterTick(screen).register(sc -> {
				long left = Math.round(delay.get() * 1000) - (System.currentTimeMillis() - start);
				button.setMessage(Component.literal(isEnabled() ? "Reconnecting in " + Math.max(0, (left + 999) / 1000) + "s" : "Reconnect"));
				if (isEnabled() && left <= 0) reconnect();
			});
		});
	}

	private void reconnect() {
		if (last == null) return;
		ServerData target = last;
		ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(target.ip), target, false, null);
	}
}
