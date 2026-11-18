package com.demon.client.gui;

import com.demon.client.DemonClient;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Hooks the client overlay into the vanilla HUD. */
public final class HudRenderer {
	private HudRenderer() {}

	public static void init() {
		HudElementRegistry.addLast(DemonClient.id("overlay"), HudRenderer::render);
	}

	private static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null) return;
		for (Module m : ModuleManager.all()) {
			if (m.isEnabled()) {
				try {
					m.onRenderHud(g, delta);
				} catch (Exception e) {
					DemonClient.LOG.error("HUD render failed for {}", m.name, e);
				}
			}
		}
		Notifications.render(g);
	}
}
