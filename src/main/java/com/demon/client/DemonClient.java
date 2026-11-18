package com.demon.client;

import com.demon.client.config.Config;
import com.demon.client.gui.ClickGuiScreen;
import com.demon.client.gui.DemonTitleScreen;
import com.demon.client.gui.HudRenderer;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemonClient implements ClientModInitializer {
	public static final String MOD_ID = "demon";
	public static final String VERSION = "1.3.0";
	public static final Logger LOG = LoggerFactory.getLogger("Demon");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		ModuleManager.defaults();
		Config.load();
		ModuleManager.ui.apply();

		ChunkScanner.init();
		Render3D.init();
		HudRenderer.init();
		// HD crosshair replaces the vanilla one while its module is on
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
			net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CROSSHAIR, vanilla -> (g, delta) -> {
				if (ModuleManager.crosshair.isEnabled()) ModuleManager.crosshair.draw(g);
				else vanilla.render(g, delta);
			});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ChunkScanner.tick();
			com.demon.client.combat.CombatFx.tick();
			ModuleManager.tick();
			Config.tick();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());

		// A way back to the custom menu from the vanilla title screen.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("\u2726 Demon"), b -> client.setScreen(new ClickGuiScreen(screen)))
					.bounds(4, 4, 64, 20).build());
			}
			if (screen instanceof TitleScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("✦ Demon Menu"), b -> {
					ModuleManager.ui.customMenu.set(true);
					client.setScreen(new DemonTitleScreen());
				}).bounds(4, 4, 92, 20).build());
			}
		});
		LOG.info("Demon Client {} loaded", VERSION);
	}
}
