package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.hud.HudLayout;
import com.demon.client.module.Category;
import com.demon.client.module.HudPanelModule;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Animated card for the entity you are fighting: face, health with damage trail, armor and distance. */
public class TargetHudModule extends HudPanelModule {
	public final BoolSetting lookedAt = add(new BoolSetting("Looked At", "Also show the entity under your crosshair", true));

	private LivingEntity shown;
	private final Anim.Value visible = new Anim.Value(0, 10);
	private final Anim.Value health = new Anim.Value(1, 12);
	private final Anim.Value trail = new Anim.Value(1, 2.5f);
	private long lastSeen;

	public TargetHudModule() {
		super("Target HUD", "Animated card for your current opponent", Category.PVP, "heart", HudLayout.Anchor.BOTTOM_CENTER);
	}

	private LivingEntity current() {
		LivingEntity t = CombatFx.lastTarget;
		if (t != null && t.isAlive() && CombatFx.recentlyAttacked(t, 6000) && t.distanceTo(mc.player) < 32) return t;
		if (lookedAt.on() && mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) return le;
		return null;
	}

	@Override
	protected boolean background() {
		return false;
	}

	@Override
	protected float[] size() {
		LivingEntity t = current();
		if (t != null) {
			if (shown != t) {
				shown = t;
				health.snap(frac(t));
				trail.snap(frac(t));
			}
			lastSeen = System.currentTimeMillis();
		}
		visible.target(t != null || System.currentTimeMillis() - lastSeen < 1200 ? 1 : 0);
		if (visible.get() < 0.01f || shown == null) return null;
		return new float[] {132, 42};
	}

	private static float frac(LivingEntity e) {
		return Anim.clamp01((e.getHealth() + e.getAbsorptionAmount()) / Math.max(1, e.getMaxHealth()));
	}

	@Override
	protected void draw(GuiGraphics g, float x, float y, float w, float h) {
		LivingEntity t = shown;
		float a = visible.peek();
		float pop = Anim.lerp(0.9f, 1f, Anim.easeOutBack(a));
		g.pose().pushMatrix();
		g.pose().translate(x + w / 2f, y + h / 2f + (1 - a) * 8);
		g.pose().scale(pop, pop);
		g.pose().translate(-w / 2f, -h / 2f);
		float f = frac(t);
		float hp = health.target(f).get();
		if (trail.peek() < hp) trail.snap(hp);
		float tr = trail.target(f).get();
		int hpColor = f > 0.6f ? 0xFF34D399 : f > 0.3f ? 0xFFFBBF24 : 0xFFF87171;

		Render2D.shadow(g, 0, 0, w, h, Theme.alpha(hpColor, 0.25f * a));
		Render2D.round(g, 0, 0, w, h, 6, Theme.alpha(0xEE071021, a));
		Render2D.outline(g, 0, 0, w, h, 6, Theme.alpha(Theme.lerp(0x2A8CB8FF, hpColor, 0.5f), a));
		// avatar
		if (t instanceof AbstractClientPlayer p) {
			Render2D.round(g, 5, 5, 32, 32, 6, Theme.alpha(0xFF0C1528, a));
			PlayerFaceRenderer.draw(g, p.getSkin(), 7, 7, 28);
		} else {
			Render2D.roundGradient(g, 5, 5, 32, 32, 6, Theme.alpha(Theme.accent1(), a), Theme.alpha(Theme.accent2(), a));
			String n = t.getName().getString().replaceAll("[^\\p{L}\\p{N}]", "");
			Render2D.textCentered(g, HdFont.BOLD, n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase(), 21, 21 - HdFont.BOLD.capHeight(11f) / 2f, 11f, Theme.alpha(0xFFFFFFFF, a));
		}
		float tx = 42;
		Render2D.text(g, HdFont.SEMIBOLD, Render2D.trim(HdFont.SEMIBOLD, t.getName().getString(), 7f, w - tx - 30), tx, 6, 7f, Theme.alpha(Theme.TEXT, a));
		Render2D.textRight(g, HdFont.MEDIUM, (int) t.distanceTo(mc.player) + "m", w - 6, 6.6f, 5.6f, Theme.alpha(Theme.SUBTEXT, a));
		// health bar with delayed damage trail
		float bw = w - tx - 6, by = 16;
		Render2D.capsule(g, tx, by, bw, 4, Theme.alpha(Theme.TRACK, a));
		Render2D.capsule(g, tx, by, Math.max(4, bw * tr), 4, Theme.alpha(0xFFFFFFFF, 0.55f * a));
		Render2D.roundGradient(g, tx, by, Math.max(4, bw * hp), 4, 2, Theme.alpha(hpColor, a), Theme.alpha(Theme.brighten(hpColor, 0.3f), a));
		String hpText = String.format("%.1f", (t.getHealth() + t.getAbsorptionAmount()) / 2f) + " HP";
		Render2D.text(g, HdFont.BOLD, hpText, tx, 25, 6f, Theme.alpha(hpColor, a));
		float mine = frac(mc.player);
		String verdict = mine > f + 0.1f ? "WINNING" : mine < f - 0.1f ? "LOSING" : "EVEN";
		int vc = mine > f + 0.1f ? 0xFF34D399 : mine < f - 0.1f ? 0xFFF87171 : Theme.SUBTEXT;
		Render2D.textRight(g, HdFont.BOLD, verdict, w - 6, 25, 5.2f, Theme.alpha(vc, a));
		// armor
		float ix = tx;
		for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
			ItemStack st = t.getItemBySlot(s);
			if (st.isEmpty()) continue;
			g.pose().pushMatrix();
			g.pose().translate(ix, 31.5f);
			g.pose().scale(0.5f, 0.5f);
			g.renderItem(st, 0, 0);
			g.pose().popMatrix();
			ix += 9;
		}
		g.pose().popMatrix();
	}
}
