package com.demon.client.module.modules.combat;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

/** Pulsing red screen edges and a heartbeat when your health is low. */
public class LowHealthModule extends Module {
	public final NumberSetting threshold = add(new NumberSetting("Hearts", "Warn at or below this many hearts", 4, 1, 10, 0.5));
	public final BoolSetting heartbeat = add(new BoolSetting("Heartbeat", "Heartbeat sound", true));
	private int beat;

	public LowHealthModule() {
		super("Low Health", "Pulsing red warning and heartbeat at low health", Category.PVP, -1, "heart");
	}

	private boolean low() {
		return mc.player != null && !mc.player.isCreative() && mc.player.getHealth() / 2f <= threshold.get();
	}

	@Override
	public void onTick() {
		if (!low() || !heartbeat.on()) return;
		if (beat++ % 18 == 0) Sounds.play(SoundEvents.NOTE_BLOCK_BASEDRUM, 0.6f, 1f);
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (!low()) return;
		float danger = 1 - Anim.clamp01(mc.player.getHealth() / 2f / threshold.asFloat());
		float pulse = 0.5f + 0.5f * (float) Math.sin(Anim.time() * 6.5);
		float a = (0.35f + 0.4f * danger) * (0.6f + 0.4f * pulse);
		int w = g.guiWidth(), h = g.guiHeight();
		int red = 0xFFE11D48;
		float edge = Math.min(w, h) * 0.18f;
		Render2D.vGradient(g, 0, 0, w, edge, Theme.alpha(red, a), Theme.alpha(red, 0));
		Render2D.vGradient(g, 0, h - edge, w, edge, Theme.alpha(red, 0), Theme.alpha(red, a));
		Render2D.hGradient(g, 0, 0, edge, h, Theme.alpha(red, a), Theme.alpha(red, 0));
		Render2D.hGradient(g, w - edge, 0, edge, h, Theme.alpha(red, 0), Theme.alpha(red, a));
		String text = "LOW HP  " + String.format("%.1f", mc.player.getHealth() / 2f) + " HP";
		float tw = HdFont.BOLD.width(text, 7f) + 14;
		float x = w / 2f - tw / 2f, y = h * 0.62f;
		Render2D.round(g, x, y, tw, 13, 6.5f, Theme.alpha(0xE0400010, 0.6f + 0.4f * pulse));
		Render2D.outline(g, x, y, tw, 13, 6.5f, Theme.alpha(red, 0.8f));
		Render2D.textCentered(g, HdFont.BOLD, text, w / 2f, y + (13 - HdFont.BOLD.capHeight(7f)) / 2f, 7f, 0xFFFFE4E6);
	}
}
