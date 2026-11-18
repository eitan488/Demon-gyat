package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Floating, animated damage and heal numbers above entities. */
public class DamageNumbersModule extends Module {
	public final BoolSetting onlyMine = add(new BoolSetting("Only Mine", "Only show damage you deal", false));
	public final BoolSetting heals = add(new BoolSetting("Heals", "Show healing in green", true));

	public DamageNumbersModule() {
		super("Damage Numbers", "Animated floating damage numbers", Category.PVP, -1, "chart");
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		long now = System.currentTimeMillis();
		for (CombatFx.Damage d : CombatFx.DAMAGES) {
			if (onlyMine.on() && !d.byMe()) continue;
			if (d.heal() && !heals.on()) continue;
			float age = (now - d.time()) / 1400f;
			if (age >= 1) continue;
			// small sideways drift based on spawn time so stacked numbers spread out
			double side = ((d.time() % 7) - 3) * 0.12;
			Vec3 p = d.pos().add(side, Anim.easeOutCubic(age) * 1.1, 0);
			float[] s = Render3D.toScreen(p, g.guiWidth(), g.guiHeight());
			if (s == null) continue;
			float pop = age < 0.15f ? Anim.easeOutBack(age / 0.15f) * 1.25f : Anim.lerp(1.25f, 1f, Anim.progress(age, 0.15f, 0.3f));
			float alpha = 1 - Anim.easeInCubic(Anim.progress(age, 0.6f, 1f));
			boolean big = d.amount() >= 6;
			int color = d.heal() ? 0xFF4ADE80 : d.byMe() ? (big ? 0xFFFFB547 : Theme.accent2()) : 0xFFF87171;
			String text = (d.heal() ? "+" : "") + (d.amount() >= 10 ? String.valueOf(Math.round(d.amount())) : String.format("%.1f", d.amount()));
			float size = (big ? 10f : 8f) * pop;
			float w = HdFont.BOLD.width(text, size);
			float cap = HdFont.BOLD.capHeight(size);
			HdFont.BOLD.draw(g, text, s[0] - w / 2f + 0.5f, s[1] - cap / 2f + 0.5f, size, Theme.alpha(0xFF000000, 0.6f * alpha));
			HdFont.BOLD.draw(g, text, s[0] - w / 2f, s[1] - cap / 2f, size, Theme.alpha(color, alpha));
		}
	}
}
