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
		com.demon.client.hud.HudLayout.reset(g.guiWidth(), g.guiHeight());
		// world-anchored labels first, so HUD panels stay on top
		for (Module m : ModuleManager.all()) {
			if (m.isEnabled()) {
				try {
					m.onRenderOverlay(g, delta);
				} catch (Exception e) {
					DemonClient.LOG.error("Overlay render failed for {}", m.name, e);
				}
			}
		}
		// the main HUD (watermark, module list) claims its corners first
		if (ModuleManager.hud.isEnabled()) safe(ModuleManager.hud, g, delta);
		for (Module m : ModuleManager.all()) {
			if (m.isEnabled() && m != ModuleManager.hud) safe(m, g, delta);
		}
		Notifications.render(g);
	}

	private static void safe(Module m, GuiGraphics g, DeltaTracker delta) {
		try {
			m.onRenderHud(g, delta);
		} catch (Exception e) {
			DemonClient.LOG.error("HUD render failed for {}", m.name, e);
		}
	}
}
