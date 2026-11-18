package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client-side kill celebration: lightning, particle burst, sound and an animated banner. */
public class KillEffectsModule extends Module {
	public final BoolSetting lightning = add(new BoolSetting("Lightning", "Visual-only lightning strike", true));
	public final BoolSetting particles = add(new BoolSetting("Particles", "Particle burst", true));
	public final BoolSetting banner = add(new BoolSetting("Banner", "Animated ELIMINATED banner", true));
	public final BoolSetting onlyPlayers = add(new BoolSetting("Only Players", "Ignore mob kills", false));

	private String name = "";
	private long time;
	private int streak;

	public KillEffectsModule() {
		super("Kill Effects", "Lightning, particles and a banner when you get a kill", Category.PVP, -1, "bolt");
		CombatFx.KILL_LISTENERS.add(this::onKill);
	}

	private void onKill(LivingEntity e) {
		if (!isEnabled() || mc.level == null) return;
		if (onlyPlayers.on() && !(e instanceof Player)) return;
		Vec3 p = e.position();
		if (lightning.on()) {
			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, mc.level);
			bolt.setVisualOnly(true);
			bolt.setPos(p.x, p.y, p.z);
			mc.level.addEntity(bolt);
		}
		if (particles.on()) {
			for (int i = 0; i < 60; i++) {
				double a = i / 60.0 * Math.PI * 2;
				mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y + 1, p.z, Math.cos(a) * 0.4, 0.25 + (i % 5) * 0.08, Math.sin(a) * 0.4);
			}
			mc.level.addParticle(ParticleTypes.EXPLOSION, p.x, p.y + 1, p.z, 0, 0, 0);
		}
		Sounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1f);
		Sounds.play(SoundEvents.NOTE_BLOCK_BELL, 1.5f, 0.8f);
		name = e.getName().getString();
		streak = CombatFx.streak;
		time = System.currentTimeMillis();
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (!banner.on() || time == 0) return;
		float t = (System.currentTimeMillis() - time) / 1000f;
		if (t > 2.4f) return;
		float in = Anim.easeOutBack(Anim.progress(t, 0, 0.35f));
		float out = Anim.easeInCubic(Anim.progress(t, 1.9f, 2.4f));
		float a = Anim.clamp01(in) * (1 - out);
		float cx = g.guiWidth() / 2f, cy = g.guiHeight() * 0.28f;
		float scale = Anim.lerp(1.6f, 1f, in);
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		String title = "ELIMINATED";
		float tw = HdFont.BOLD.width(title, 13f);
		Render2D.glow(g, 0, 4, tw * 0.9f, Theme.alpha(0xFFF87171, 0.35f * a));
		Render2D.gradientText(g, HdFont.BOLD, title, -tw / 2f, -6, 13f, a);
		float lineW = (tw + 30) * Anim.easeOutCubic(Anim.progress(t, 0.15f, 0.6f));
		Render2D.hGradient(g, -lineW / 2f, 10, lineW / 2f, 0.6f, Theme.alpha(Theme.accent1(), 0), Theme.alpha(Theme.accent2(), a));
		Render2D.hGradient(g, 0, 10, lineW / 2f, 0.6f, Theme.alpha(Theme.accent2(), a), Theme.alpha(Theme.accent1(), 0));
		String sub = name + (streak > 1 ? "   •   " + streak + " kill streak" : "");
		Render2D.textCentered(g, HdFont.SEMIBOLD, sub, 0, 14, 7f, Theme.alpha(Theme.TEXT, a));
		g.pose().popMatrix();
	}
}
