package com.demon.client.module.modules;

import com.demon.client.gui.Notifications;
import com.demon.client.gui.Ui;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Base hunting on autopilot: runs /rtp every few seconds and lets the Chunk Finder scan each new
 * location. Stops as soon as a stash, base or farm is found.
 */
public class AutoRtpModule extends Module {
	public enum Dimension {
		OVERWORLD("Overworld", "overworld"), NETHER("Nether", "nether"), END("End", "end"), CYCLE("Cycle All", null);

		final String title, arg;

		Dimension(String title, String arg) {
			this.title = title;
			this.arg = arg;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public enum Command {
		RTP_DIM("/rtp <world>"), RTP("/rtp"), WILD("/wild"), RANDOMTP("/randomtp");

		final String title;

		Command(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public final EnumSetting<Dimension> dimension = add(new EnumSetting<>("World", "Where to teleport: overworld, nether, end or cycle through all", Dimension.OVERWORLD));
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds between each /rtp", 30, 5, 600, 5, "s"));
	public final EnumSetting<Command> command = add(new EnumSetting<>("Command", "Command format your server uses", Command.RTP_DIM));
	public final BoolSetting waitForScan = add(new BoolSetting("Wait For Scan", "Only count down once all loaded chunks are scanned", true));
	public final BoolSetting stopOnFind = add(new BoolSetting("Stop On Find", "Stop when the Chunk Finder finds a stash, base or farm", true));
	public final BoolSetting stopOnDamage = add(new BoolSetting("Stop On Damage", "Stop if you take damage", true));
	public final NumberSetting maxRtps = add(new NumberSetting("Max RTPs", "Stop after this many teleports (0 = no limit)", 0, 0, 500, 5));
	public final BoolSetting panel = add(new BoolSetting("HUD Panel", "Countdown panel on screen", true));

	private float remaining;
	private int count, finds, cycle;
	private String last = "", status = "";
	private float lastHealth;
	private Vec3 sentFrom;
	private int teleportWait;
	private final Anim.Value pulse = new Anim.Value(0, 6);

	public AutoRtpModule() {
		super("Auto RTP", "Runs /rtp on a timer and hunts each spot with the Chunk Finder", Category.HUNTING, -1, "rtp");
	}

	@Override
	protected void onEnable() {
		count = 0;
		finds = 0;
		cycle = 0;
		remaining = 3f;
		teleportWait = 0;
		sentFrom = null;
		status = "Starting";
		if (mc.player != null) lastHealth = mc.player.getHealth();
		if (!ModuleManager.chunkFinder.isEnabled()) {
			ModuleManager.chunkFinder.setEnabled(true);
			Notifications.push("Auto RTP", "Chunk Finder turned on", Theme.accent1(), 2f);
		}
	}

	@Override
	public String info() {
		return teleportWait > 0 ? "…" : (int) Math.ceil(remaining) + "s";
	}

	private String nextWorld() {
		Dimension d = dimension.get();
		if (d != Dimension.CYCLE) return d.arg;
		return Dimension.values()[cycle % 3].arg;
	}

	private String buildCommand() {
		return switch (command.get()) {
			case RTP_DIM -> "rtp " + nextWorld();
			case RTP -> "rtp";
			case WILD -> "wild";
			case RANDOMTP -> "randomtp";
		};
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.getConnection() == null) {
			status = "Not in a world";
			return;
		}
		if (stopOnDamage.on() && mc.player.getHealth() < lastHealth - 0.01f) {
			stop("You took damage", Theme.DANGER);
			return;
		}
		lastHealth = mc.player.getHealth();

		// wait for the teleport to happen (position jump) after sending the command
		if (teleportWait > 0) {
			teleportWait--;
			if (sentFrom != null && mc.player.position().distanceToSqr(sentFrom) > 48 * 48) {
				teleportWait = 0;
				status = "Landed — scanning";
				ChunkScanner.rescanAll();
			} else if (teleportWait == 0) {
				status = "No teleport (cooldown?)";
			} else {
				status = "Teleporting…";
				return;
			}
		}

		boolean scanning = waitForScan.on() && ChunkScanner.queued() > 0;
		if (scanning) {
			status = "Scanning " + ChunkScanner.queued() + " chunks";
			return;
		}
		remaining -= 0.05f;
		status = "Next " + displayCommand();
		if (remaining > 0) return;

		if (maxRtps.asInt() > 0 && count >= maxRtps.asInt()) {
			stop("Reached " + count + " RTPs", Theme.accent1());
			return;
		}
		String cmd = buildCommand();
		mc.getConnection().sendCommand(cmd);
		last = "/" + cmd;
		count++;
		cycle++;
		sentFrom = mc.player.position();
		teleportWait = 200; // up to 10 s for the server to teleport us
		remaining = delay.asFloat();
		pulse.snap(1);
		Sounds.play(SoundEvents.ENDER_EYE_DEATH, 1.6f, 0.4f);
	}

	private String displayCommand() {
		return "/" + buildCommand();
	}

	/** Called by the Chunk Finder when a new sus chunk is announced. */
	public void onFind(ChunkData d, ChunkFinderModule.Kind kind, String summary) {
		if (!isEnabled()) return;
		finds++;
		if (stopOnFind.on()) {
			stop(kind.title + " found at " + (int) d.centerX() + ", " + (int) d.centerZ(), kind.color);
			Sounds.play(SoundEvents.NOTE_BLOCK_BELL, 1.2f, 1f);
		}
	}

	private void stop(String reason, int color) {
		setEnabled(false);
		Notifications.push("Auto RTP stopped", reason, color, 5f);
		Sounds.notify(true);
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (!panel.on() || mc.player == null) return;
		int sw = g.guiWidth();
		float w = 122, h = 37;
		float[] pos = com.demon.client.hud.HudLayout.place(com.demon.client.hud.HudLayout.Anchor.TOP_RIGHT, w, h);
		float x = pos[0], y = pos[1];
		Ui.hud(g, x, y, w, h, 1f);
		// countdown ring
		float total = Math.max(1f, delay.asFloat());
		float frac = teleportWait > 0 ? 1f : Anim.clamp01(remaining / total);
		float cx = x + 13, cy = y + h / 2f;
		Render2D.ring(g, cx, cy, 8.5f, 1.4f, Theme.TRACK);
		drawArc(g, cx, cy, 8.5f, frac);
		float p = pulse.target(0).get();
		if (p > 0.01f) Render2D.glow(g, cx, cy, 14 + 8 * p, Theme.alpha(Theme.accent2(), 0.6f * p));
		String sec = teleportWait > 0 ? "…" : String.valueOf((int) Math.ceil(remaining));
		Render2D.textCentered(g, HdFont.BOLD, sec, cx, cy - HdFont.BOLD.capHeight(6.4f) / 2f, 6.4f, Theme.TEXT);
		float tx = x + 26;
		Render2D.text(g, HdFont.BOLD, "AUTO RTP", tx, y + 5.5f, 6f, Theme.TEXT);
		Render2D.text(g, HdFont.MEDIUM, Render2D.trim(HdFont.MEDIUM, status, 5.6f, w - 32), tx, y + 15f, 5.6f, Theme.accent2());
		String stats = count + (count == 1 ? " RTP" : " RTPs") + "  ·  " + finds + (finds == 1 ? " find" : " finds");
		Render2D.text(g, HdFont.MEDIUM, stats, tx, y + 24.5f, 5.4f, Theme.SUBTEXT);
	}

	/** Progress arc made of small dots (smooth on every scale). */
	private static void drawArc(GuiGraphics g, float cx, float cy, float r, float frac) {
		int steps = 48;
		int lit = Math.round(steps * frac);
		for (int i = 0; i < lit; i++) {
			double ang = -Math.PI / 2 + i / (double) steps * Math.PI * 2;
			int color = Theme.accent(i / (float) steps);
			Render2D.circle(g, cx + (float) Math.cos(ang) * (r - 0.7f), cy + (float) Math.sin(ang) * (r - 0.7f), 0.85f, color);
		}
	}

	@Override
	public boolean persistEnabled() {
		return false;
	}
}
