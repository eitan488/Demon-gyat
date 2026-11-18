package com.demon.client.module.modules.hud;

import com.demon.client.combat.CombatFx;
import com.demon.client.hud.HudLayout;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

/** Animated WASD / mouse / space keys with CPS. */
public class KeystrokesModule extends HudPanelModule {
	private final Map<String, Anim.Value> anims = new HashMap<>();

	public KeystrokesModule() {
		super("Keystrokes", "Animated key and mouse display with CPS", "keyboard", HudLayout.Anchor.BOTTOM_LEFT);
	}

	@Override
	protected boolean background() {
		return false;
	}

	@Override
	protected float[] size() {
		return new float[] {58, 70};
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		float k = 18, gp = 2;
		key(g, "W", mc.options.keyUp, x + k + gp, y, k, k, null);
		key(g, "A", mc.options.keyLeft, x, y + k + gp, k, k, null);
		key(g, "S", mc.options.keyDown, x + k + gp, y + k + gp, k, k, null);
		key(g, "D", mc.options.keyRight, x + 2 * (k + gp), y + k + gp, k, k, null);
		float mw = (w - gp) / 2f;
		key(g, "LMB", mc.options.keyAttack, x, y + 2 * (k + gp), mw, k, CombatFx.cps(true) + " CPS");
		key(g, "RMB", mc.options.keyUse, x + mw + gp, y + 2 * (k + gp), mw, k, CombatFx.cps(false) + " CPS");
		key(g, "—", mc.options.keyJump, x, y + 3 * (k + gp), w, 10, null);
	}

	private void key(GuiGraphics g, String label, KeyMapping key, float x, float y, float w, float h, String sub) {
		float p = anims.computeIfAbsent(label, l -> new Anim.Value(0, 18)).target(key.isDown() ? 1 : 0).get();
		Render2D.round(g, x, y, w, h, 4, Theme.lerp(Theme.HUD_BG, Theme.alpha(Theme.accent1(), 0.9f), p));
		Render2D.outline(g, x, y, w, h, 4, Theme.lerp(0x268CB8FF, Theme.accent2(), p));
		int c = Theme.lerp(Theme.TEXT, 0xFFFFFFFF, p);
		if (sub == null) {
			Render2D.textCentered(g, HdFont.BOLD, label, x + w / 2f, y + (h - HdFont.BOLD.capHeight(6.4f)) / 2f, 6.4f, c);
		} else {
			Render2D.textCentered(g, HdFont.BOLD, label, x + w / 2f, y + 3.5f, 5.8f, c);
			Render2D.textCentered(g, HdFont.MEDIUM, sub, x + w / 2f, y + 11, 4.6f, Theme.alpha(c, 0.8f));
		}
	}
}
