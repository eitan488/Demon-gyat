package com.demon.client.module.modules.combat;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/** Smooth animated health bars above visible entities. */
public class HealthBarsModule extends Module {
	public final BoolSetting mobs = add(new BoolSetting("Mobs", "Also show bars on mobs", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance", 24, 4, 64, 2, "m"));
	private final Int2ObjectOpenHashMap<Anim.Value> anims = new Int2ObjectOpenHashMap<>();

	public HealthBarsModule() {
		super("Health Bars", "Animated health bars above entities you can see", Category.PVP, -1, "heart");
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		if (mc.level == null) return;
		float pt = delta.getGameTimeDeltaPartialTick(true);
		double r2 = range.get() * range.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || e == mc.player || e instanceof ArmorStand || !e.isAlive()) continue;
			if (!(e instanceof Player) && !mobs.on()) continue;
			if (e.distanceToSqr(mc.player) > r2 || !mc.player.hasLineOfSight(e)) continue;
			float f = Anim.clamp01(le.getHealth() / Math.max(1, le.getMaxHealth()));
			Anim.Value v = anims.computeIfAbsent(e.getId(), k -> new Anim.Value(f, 10));
			float shown = v.target(f).get();
			float[] s = Render3D.toScreen(e.getPosition(pt).add(0, e.getBbHeight() + (e instanceof Player ? 0.15 : 0.35), 0), g.guiWidth(), g.guiHeight());
			if (s == null) continue;
			float w = 26, h = 2.6f;
			int c = f > 0.6f ? 0xFF34D399 : f > 0.3f ? 0xFFFBBF24 : 0xFFF87171;
			Render2D.capsule(g, s[0] - w / 2f - 0.6f, s[1] - 0.6f, w + 1.2f, h + 1.2f, 0xB0000000);
			Render2D.roundGradient(g, s[0] - w / 2f, s[1], Math.max(h, w * shown), h, h / 2f, c, Theme.brighten(c, 0.35f));
		}
		if (anims.size() > 256) anims.clear();
	}
}
