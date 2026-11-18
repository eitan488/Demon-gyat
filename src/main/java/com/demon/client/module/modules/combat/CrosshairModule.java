package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** HD animated crosshair with hit markers and an attack-cooldown arc. Replaces the vanilla crosshair. */
public class CrosshairModule extends Module {
	public enum Style {
		CROSS("Cross"), DOT("Dot"), CIRCLE("Circle"), CROSS_DOT("Cross + Dot");

		final String t;

		Style(String t) {
			this.t = t;
		}

		@Override
		public String toString() {
			return t;
		}
	}

	public final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Crosshair shape", Style.CROSS_DOT));
	public final NumberSetting size = add(new NumberSetting("Size", "Line length", 4, 1, 10, 0.5));
	public final NumberSetting gap = add(new NumberSetting("Gap", "Gap from the centre", 2, 0, 8, 0.5));
	public final NumberSetting thickness = add(new NumberSetting("Thickness", "Line thickness", 0.8, 0.3, 2, 0.1));
	public final BoolSetting dynamic = add(new BoolSetting("Dynamic", "Spread while moving and attacking", true));
	public final BoolSetting hitMarker = add(new BoolSetting("Hit Marker", "X marker when your hits land", true));
	public final BoolSetting cooldown = add(new BoolSetting("Cooldown Arc", "Attack charge arc under the crosshair", true));

	private final Anim.Value spread = new Anim.Value(0, 14);
	private long lastAttackSeen;

	public CrosshairModule() {
		super("Crosshair", "HD animated crosshair with hit markers", Category.PVP, -1, "crosshair");
	}

	public void draw(GuiGraphics g) {
		if (mc.player == null || mc.options.hideGui) return;
		float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f;
		float moving = (float) Math.min(1, mc.player.getDeltaMovement().horizontalDistance() * 4);
		if (CombatFx.lastAttackTime != lastAttackSeen) {
			lastAttackSeen = CombatFx.lastAttackTime;
			spread.snap(Math.max(spread.peek(), 3));
		}
		float sp = dynamic.on() ? spread.target(moving * 2.5f).get() : 0;
		float gp = gap.asFloat() + sp, len = size.asFloat(), th = thickness.asFloat();
		int c = Theme.alpha(Theme.TEXT, 0.95f);
		int sh = 0x99000000;
		Style s = style.get();
		if (s == Style.CROSS || s == Style.CROSS_DOT) {
			bar(g, cx - th / 2f, cy - gp - len, th, len, c, sh);
			bar(g, cx - th / 2f, cy + gp, th, len, c, sh);
			bar(g, cx - gp - len, cy - th / 2f, len, th, c, sh);
			bar(g, cx + gp, cy - th / 2f, len, th, c, sh);
		}
		if (s == Style.DOT || s == Style.CROSS_DOT) {
			Render2D.circle(g, cx, cy, th * 0.9f + 0.35f, sh);
			Render2D.circle(g, cx, cy, th * 0.9f, Theme.accent2());
		}
		if (s == Style.CIRCLE) {
			Render2D.ring(g, cx, cy, gp + len * 0.6f, th, c);
			Render2D.circle(g, cx, cy, th * 0.7f, Theme.accent2());
		}
		if (cooldown.on()) {
			float charge = mc.player.getAttackStrengthScale(0f);
			if (charge < 1f) {
				float w = 14;
				Render2D.capsule(g, cx - w / 2f, cy + gp + len + 4, w, 1.4f, 0x66000000);
				Render2D.capsule(g, cx - w / 2f, cy + gp + len + 4, Math.max(1.4f, w * charge), 1.4f, Theme.accent2());
			}
		}
		if (hitMarker.on()) {
			float age = (System.currentTimeMillis() - CombatFx.lastHitTime) / 350f;
			if (age < 1) {
				float pop = Anim.easeOutBack(Anim.clamp01(age * 3));
				float a = 1 - Anim.easeInCubic(age);
				int hc = CombatFx.lastHitKill ? 0xFFF87171 : 0xFFFFFFFF;
				float r0 = 3 + 2 * pop, r1 = r0 + 3.5f;
				for (int i = 0; i < 4; i++) {
					double ang = Math.PI / 4 + i * Math.PI / 2;
					float x0 = cx + (float) Math.cos(ang) * r0, y0 = cy + (float) Math.sin(ang) * r0;
					float x1 = cx + (float) Math.cos(ang) * r1, y1 = cy + (float) Math.sin(ang) * r1;
					Render2D.line(g, x0, y0, x1, y1, Theme.alpha(hc, a));
					Render2D.line(g, x0 + 0.3f, y0, x1 + 0.3f, y1, Theme.alpha(hc, a));
				}
			}
		}
	}

	private static void bar(GuiGraphics g, float x, float y, float w, float h, int c, int shadow) {
		Render2D.round(g, x - 0.35f, y - 0.35f, w + 0.7f, h + 0.7f, 0.4f, shadow);
		Render2D.round(g, x, y, w, h, 0.3f, c);
	}
}
