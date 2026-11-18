package com.demon.client.gui;

import com.demon.client.DemonClient;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

/** Custom animated main menu with an intro cutscene. */
public class DemonTitleScreen extends Screen {
	private static boolean introPlayed;

	private static final String[] LETTERS = {"d", "e", "m", "o", "n"};
	private static final float[] ASPECT = {0.9375f, 0.85417f, 1.1125f, 0.98333f, 0.94167f};
	private static final float PAD = 0.16667f;
	private static final Identifier[] SPRITE = new Identifier[5];
	private static final Identifier[] SPRITE_GLOW = new Identifier[5];

	static {
		for (int i = 0; i < 5; i++) {
			SPRITE[i] = DemonClient.id("logo_" + LETTERS[i]);
			SPRITE_GLOW[i] = DemonClient.id("logo_" + LETTERS[i] + "_glow");
		}
	}

	/** Timeline of the intro (seconds). */
	private static final float T_LETTERS = 0.75f, T_SUB = 1.75f, T_MOVE = 2.35f, T_UI = 2.6f, T_END = 3.3f;

	private float start = -1;
	private float exitStart = -1;
	private Runnable exitAction;
	private final Set<Integer> cues = new HashSet<>();
	private final List<MenuButton> buttons = new ArrayList<>();
	private final List<ServerData> servers = new ArrayList<>();
	private final List<Region> regions = new ArrayList<>();
	private final List<float[]> ripples = new ArrayList<>();
	private final Random random = new Random();
	private Object lastHover;
	private float nextGlitch = 4f, glitchUntil = -1;

	private record Region(float x, float y, float w, float h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private static final class MenuButton {
		final Identifier icon;
		final String label;
		final Runnable action;
		final boolean primary;
		final Anim.Value hover = new Anim.Value(0, 16);

		MenuButton(String icon, String label, boolean primary, Runnable action) {
			this.icon = DemonClient.id("icon_" + icon);
			this.label = label;
			this.primary = primary;
			this.action = action;
		}
	}

	public DemonTitleScreen() {
		super(Component.literal("Demon Client"));
	}

	@Override
	protected void init() {
		buttons.clear();
		buttons.add(new MenuButton("play", "Singleplayer", false, () -> minecraft.setScreen(new SelectWorldScreen(this))));
		buttons.add(new MenuButton("multiplayer", "Multiplayer", true, () -> minecraft.setScreen(new JoinMultiplayerScreen(this))));
		buttons.add(new MenuButton("sparkle", "Demon Menu", false, () -> minecraft.setScreen(new ClickGuiScreen(this))));
		buttons.add(new MenuButton("settings", "Settings", false, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		buttons.add(new MenuButton("power", "Quit Game", false, () -> minecraft.stop()));
		servers.clear();
		try {
			ServerList list = new ServerList(minecraft);
			list.load();
			for (int i = 0; i < list.size() && servers.size() < 4; i++) servers.add(list.get(i));
		} catch (Exception e) {
			DemonClient.LOG.warn("Could not load server list", e);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private float elapsed() {
		return start < 0 ? 0 : Anim.time() - start;
	}

	private void cue(int id, float at, Runnable sound) {
		if (elapsed() >= at && cues.add(id)) sound.run();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// drawn in render()
	}

	@Override
	public void render(GuiGraphics g, int mouseXi, int mouseYi, float delta) {
		regions.clear();
		if (start < 0) {
			Render2D.rect(g, 0, 0, width, height, 0xFF000000);
			if (minecraft.getOverlay() != null) return;
			boolean playIntro = !introPlayed && (ModuleManager.ui == null || ModuleManager.ui.intro.on());
			introPlayed = true;
			start = Anim.time() - (playIntro ? 0 : T_MOVE + 0.25f);
			if (!playIntro) {
				cues.add(0);
				cues.add(1);
				for (int i = 0; i < 5; i++) cues.add(10 + i);
				cues.add(2);
				cues.add(3);
			}
		}
		float t = elapsed();
		float now = Anim.time();
		double mx = mouseXi, my = mouseYi;

		// ---- sound cues
		cue(0, 0.05f, () -> Sounds.play(SoundEvents.BEACON_ACTIVATE, 1.4f, 0.7f));
		for (int i = 0; i < 5; i++) {
			final int idx = i;
			cue(10 + i, T_LETTERS + i * 0.13f, () -> Sounds.play(SoundEvents.NOTE_BLOCK_BIT, 0.7f + idx * 0.16f, 0.45f));
		}
		cue(2, T_SUB, () -> {
			Sounds.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1f);
			Sounds.play(SoundEvents.NOTE_BLOCK_CHIME, 1.5f, 0.4f);
		});
		cue(3, T_MOVE, () -> Sounds.play(SoundEvents.BEACON_POWER_SELECT, 1.6f, 0.5f));

		MenuFx.background(g, width, height, 1f);

		// ---- logo (center during intro, moves to the left)
		float move = Anim.easeInOutCubic(Anim.progress(t, T_MOVE, T_MOVE + 0.75f));
		float capCenter = Anim.clamp(height * 0.15f, 28, 72);
		float capFinal = Anim.clamp(height * 0.105f, 20, 50);
		float cap = Anim.lerp(capCenter, capFinal, move);
		float logoW = logoWidth(cap);
		float leftX = Math.max(26, width * 0.07f);
		float finalY = Math.max(22, height * 0.16f);
		float lx = Anim.lerp(width / 2f - logoW / 2f, leftX, move);
		float ly = Anim.lerp(height / 2f - capCenter / 2f - 12, finalY, move);
		if (now > nextGlitch + start && t > T_END) {
			glitchUntil = now + 0.16f;
			nextGlitch = (now - start) + 3.5f + random.nextFloat() * 4f;
		}
		boolean glitch = now < glitchUntil;
		drawLogo(g, lx, ly, cap, t, glitch);

		// subtitle "CLIENT"
		float subCap = cap * 0.26f;
		float subH = subCap / (1 - 2 * Render2D.LOGO_CLIENT_PAD);
		float subW = subH * Render2D.LOGO_CLIENT_ASPECT;
		float subPad = subH * Render2D.LOGO_CLIENT_PAD;
		float subY = ly + cap + cap * 0.22f;
		float subX = Anim.lerp(lx + logoW / 2f - (subW - 2 * subPad) / 2f, lx + 2, move);
		float reveal = Anim.easeOutCubic(Anim.progress(t, T_SUB, T_SUB + 0.5f));
		if (reveal > 0) {
			g.enableScissor((int) (subX - subPad), (int) (subY - subPad) - 1, (int) (subX - subPad + subW * reveal) + 1, (int) (subY - subPad + subH) + 1);
			Render2D.sprite(g, Render2D.LOGO_CLIENT, subX - subPad, subY - subPad, subW, subH, Theme.alpha(Theme.lerp(0xFFFFFFFF, Theme.accent2(), 0.35f), 0.85f));
			g.disableScissor();
		}
		// light sweep over the logo
		float sweep = Anim.progress(t, T_SUB - 0.1f, T_SUB + 0.6f);
		if (sweep > 0 && sweep < 1) {
			float sx = lx - 60 + (logoW + 120) * Anim.easeInOutCubic(sweep);
			g.enableScissor((int) lx - 4, (int) ly - 6, (int) (lx + logoW) + 4, (int) (ly + cap) + 6);
			g.pose().pushMatrix();
			g.pose().translate(sx, ly + cap / 2f);
			g.pose().rotate(0.35f);
			Render2D.sprite(g, Render2D.SWEEP, -20, -cap, 40, cap * 2, 0x88FFFFFF);
			g.pose().popMatrix();
			g.disableScissor();
		}

		// tagline
		float ui = Anim.easeOutCubic(Anim.progress(t, T_UI, T_UI + 0.5f));
		float tagY = subY + subCap + 9;
		Render2D.text(g, "Base hunting utility  ·  Fabric 1.21.11", leftX + 1 - 20 * (1 - ui), tagY, Theme.alpha(Theme.SUBTEXT, ui));

		// ---- buttons
		int bw = 172, bh = 24, gap = 6;
		float by = tagY + 22;
		int available = (int) (height - 30 - by);
		if (available < buttons.size() * (bh + gap)) {
			bh = Math.max(18, available / buttons.size() - 4);
			gap = 4;
		}
		for (int i = 0; i < buttons.size(); i++) {
			float slide = Anim.easeOutCubic(Anim.progress(t, T_UI + i * 0.07f, T_UI + 0.55f + i * 0.07f));
			drawButton(g, buttons.get(i), leftX - 40 * (1 - slide), by + i * (bh + gap), bw, bh, mx, my, slide);
		}

		// ---- servers panel
		float rightEdge = width - Math.max(16, width * 0.05f);
		int panelW = (int) Math.min(210, rightEdge - (leftX + bw + 22));
		if (panelW >= 150) {
			float slide = Anim.easeOutCubic(Anim.progress(t, T_UI + 0.2f, T_UI + 0.8f));
			drawServers(g, rightEdge - panelW + 40 * (1 - slide), by, panelW, mx, my, slide);
		}

		// ---- footer
		float foot = Anim.easeOutCubic(Anim.progress(t, T_UI + 0.4f, T_UI + 0.9f));
		Render2D.text(g, "Copyright Mojang AB. Do not distribute!", 8, height - 12, Theme.alpha(Theme.MUTED, foot * 0.8f));
		String vanilla = "Vanilla menu ›";
		float vx = 8, vy = height - 24;
		boolean vh = mx >= vx && mx < vx + Render2D.width(vanilla) && my >= vy - 2 && my < vy + 10;
		Render2D.text(g, vanilla, vx, vy, Theme.alpha(vh ? Theme.TEXT : Theme.SUBTEXT, foot));
		regions.add(new Region(vx, vy - 2, Render2D.width(vanilla), 12, () -> {
			if (ModuleManager.ui != null) ModuleManager.ui.customMenu.set(false);
			minecraft.setScreen(new TitleScreen());
		}));
		String clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
		String user = minecraft.getUser().getName();
		Render2D.textRight(g, clock, width - 8, height - 12, Theme.alpha(Theme.TEXT, foot));
		Render2D.textRight(g, user, width - 8 - Render2D.width(clock) - 10, height - 12, Theme.alpha(Theme.SUBTEXT, foot));
		Render2D.circle(g, width - 8 - Render2D.width(clock) - 10 - Render2D.width(user) - 6, height - 8.5f, 2, Theme.alpha(Theme.SUCCESS, foot));

		// ---- intro overlays: black + letterbox + light line
		if (t < T_UI + 0.6f) {
			float lineP = Anim.easeOutExpo(Anim.progress(t, 0, 0.55f));
			float open = Anim.easeInOutCubic(Anim.progress(t, 0.45f, 1.15f));
			float retract = Anim.easeInOutCubic(Anim.progress(t, T_MOVE, T_MOVE + 0.7f));
			float barH = height / 2f * (1 - 0.8f * open) * (1 - retract);
			Render2D.rect(g, 0, 0, width, (int) Math.ceil(barH), 0xFF000000);
			Render2D.rect(g, 0, (int) (height - barH), width, (int) Math.ceil(barH) + 1, 0xFF000000);
			float lineA = 1 - Anim.progress(t, 0.6f, 1.1f);
			if (lineA > 0) {
				float lw = width * lineP;
				int c = Theme.alpha(Theme.wave(0.5f), lineA);
				Render2D.glow(g, width / 2f, height / 2f, lw * 0.35f, Theme.alpha(Theme.accent2(), 0.4f * lineA));
				Render2D.sprite(g, Render2D.SWEEP, width / 2f - lw / 2f, height / 2f - 1, lw, 2, c);
			}
			// edge lines on the letterbox bars
			if (barH > 1 && open > 0) {
				int ec = Theme.alpha(Theme.wave(0.3f), 0.6f * (1 - retract));
				Render2D.sprite(g, Render2D.SWEEP, 0, barH - 1, width, 1, ec);
				Render2D.sprite(g, Render2D.SWEEP, 0, height - barH, width, 1, ec);
			}
			if (t < T_MOVE) {
				Render2D.textCentered(g, "click to skip", width / 2f, height - 14, Theme.alpha(Theme.MUTED, 0.6f * Anim.progress(t, 1.2f, 1.6f)));
			}
		}

		// ripples
		for (int i = ripples.size() - 1; i >= 0; i--) {
			float[] r = ripples.get(i);
			float age = (now - r[2]) / 0.55f;
			if (age >= 1) {
				ripples.remove(i);
				continue;
			}
			Render2D.ring(g, r[0], r[1], 3 + 26 * Anim.easeOutCubic(age), Theme.alpha(Theme.wave(0.5f), (1 - age) * 0.7f));
		}

		// exit fade
		if (exitStart >= 0) {
			float p = Anim.progress(now, exitStart, exitStart + 0.22f);
			Render2D.rect(g, 0, 0, width, height, Theme.alpha(0xFF05030A, Anim.easeInCubic(p)));
			if (p >= 1 && exitAction != null) {
				Runnable a = exitAction;
				exitAction = null;
				exitStart = -1;
				a.run();
				return;
			}
		}

		Object hover = null;
		for (Region r : regions) if (r.contains(mx, my)) hover = r;
		if (hover != null && hover != lastHover && t > T_UI) Sounds.hover();
		lastHover = hover;
	}

	private float logoWidth(float cap) {
		float h = cap / (1 - 2 * PAD);
		float w = 0;
		for (int i = 0; i < 5; i++) w += h * ASPECT[i] - 2 * PAD * h;
		return w + cap * 0.06f * 4;
	}

	private void drawLogo(GuiGraphics g, float x, float y, float cap, float t, boolean glitch) {
		float h = cap / (1 - 2 * PAD);
		float pad = PAD * h;
		float cx = x;
		float pulse = 0.55f + 0.2f * (float) Math.sin(Anim.time() * 2.0);
		for (int i = 0; i < 5; i++) {
			float w = h * ASPECT[i];
			float p = Anim.progress(t, T_LETTERS + i * 0.13f, T_LETTERS + i * 0.13f + 0.5f);
			float pop = Anim.easeOutBack(p);
			float alpha = Anim.easeOutCubic(Math.min(1, p * 1.6f));
			if (alpha > 0.01f) {
				float scale = Anim.lerp(1.8f, 1f, pop);
				float dy = -14 * (1 - Anim.easeOutCubic(p));
				int color = Theme.wave(i / 4f);
				float lcx = cx + (w - 2 * pad) / 2f, lcy = y + cap / 2f + dy;
				g.pose().pushMatrix();
				g.pose().translate(lcx, lcy);
				g.pose().scale(scale, scale);
				float dx0 = -(w - 2 * pad) / 2f - pad, dy0 = -cap / 2f - pad;
				Render2D.sprite(g, SPRITE_GLOW[i], dx0, dy0, w, h, Theme.alpha(color, pulse * alpha));
				float glitchAmt = (1 - p) * 6 + (glitch ? 2.5f + random.nextFloat() * 3 : 0);
				if (glitchAmt > 0.3f) {
					Render2D.sprite(g, SPRITE[i], dx0 - glitchAmt, dy0, w, h, Theme.alpha(0xFF00E5FF, 0.55f * alpha));
					Render2D.sprite(g, SPRITE[i], dx0 + glitchAmt, dy0, w, h, Theme.alpha(0xFFFF2D75, 0.55f * alpha));
				}
				Render2D.sprite(g, SPRITE[i], dx0, dy0, w, h, Theme.alpha(Theme.lerp(color, 0xFFFFFFFF, 0.12f), alpha));
				g.pose().popMatrix();
			}
			cx += w - 2 * pad + cap * 0.06f;
		}
	}

	private void drawButton(GuiGraphics g, MenuButton b, float x, float y, int w, int h, double mx, double my, float alpha) {
		if (alpha <= 0.01f) return;
		boolean hovered = exitStart < 0 && mx >= x && mx < x + w && my >= y && my < y + h;
		float hv = b.hover.target(hovered ? 1 : 0).get();
		float ox = hv * 4;
		if (hv > 0.01f) Render2D.shadow(g, x + ox, y, w, h, Theme.alpha(Theme.accent1(), 0.25f * hv * alpha));
		if (b.primary) {
			Render2D.roundGradient(g, x + ox, y, w, h, 6, Theme.alpha(Theme.accent1(), (0.55f + 0.25f * hv) * alpha), Theme.alpha(Theme.accent2(), (0.30f + 0.25f * hv) * alpha));
		} else {
			Render2D.round(g, x + ox, y, w, h, 6, Theme.alpha(Theme.lerp(0xB0120E1C, 0xE0201A33, hv), alpha));
		}
		Render2D.outline(g, x + ox, y, w, h, 6, Theme.alpha(Theme.lerp(0x1CFFFFFF, Theme.accent1(), hv * 0.7f), alpha));
		float barH = 6 + (h - 12) * hv;
		Render2D.round(g, x + ox + 1, y + h / 2f - barH / 2f, 2.5f, barH, 1, Theme.alpha(Theme.wave(0.2f), Math.max(hv, b.primary ? 0.8f : 0) * alpha));
		float ty = y + h / 2f - 4;
		Render2D.icon(g, b.icon, x + ox + 14, y + h / 2f, 9, Theme.alpha(b.primary ? Theme.TEXT : Theme.lerp(Theme.accent1(), Theme.TEXT, hv * 0.4f), alpha));
		Render2D.text(g, b.label, x + ox + 24 + hv * 2, ty, Theme.alpha(Theme.TEXT, alpha));
		if (hv > 0.02f) Render2D.text(g, "→", x + ox + w - 16 - 6 * (1 - hv), ty, Theme.alpha(Theme.TEXT, hv * alpha));
		regions.add(new Region(x, y, w, h, () -> runWithTransition(b.action, b.label.startsWith("Quit"))));
	}

	private void drawServers(GuiGraphics g, float x, float y, int w, double mx, double my, float alpha) {
		if (alpha <= 0.01f) return;
		int cardH = 30;
		int rows = Math.max(1, servers.size());
		float h = 26 + rows * (cardH + 5) + 4;
		Render2D.shadow(g, x, y, w, h, Theme.alpha(0xFF000000, 0.4f * alpha));
		Render2D.round(g, x, y, w, h, 12, Theme.alpha(0xC00E0B16, alpha));
		Render2D.outline(g, x, y, w, h, 12, Theme.alpha(0x1CFFFFFF, alpha));
		Render2D.round(g, x + 10, y + 8, 2.5f, 9, 1, Theme.alpha(Theme.wave(0.4f), alpha));
		Render2D.text(g, "SERVERS", x + 17, y + 9, Theme.alpha(Theme.TEXT, alpha));
		String manage = "Manage ›";
		float mxs = x + w - 10 - Render2D.width(manage);
		boolean mh = mx >= mxs && mx < x + w - 6 && my >= y + 6 && my < y + 20;
		Render2D.text(g, manage, mxs, y + 9, Theme.alpha(mh ? Theme.accent2() : Theme.SUBTEXT, alpha));
		regions.add(new Region(mxs - 2, y + 6, Render2D.width(manage) + 6, 14, () -> runWithTransition(() -> minecraft.setScreen(new JoinMultiplayerScreen(this)), false)));

		float cy = y + 26;
		if (servers.isEmpty()) {
			Render2D.round(g, x + 8, cy, w - 16, cardH, 6, Theme.alpha(0x10FFFFFF, alpha));
			Render2D.text(g, "No saved servers yet", x + 16, cy + 6, Theme.alpha(Theme.SUBTEXT, alpha));
			Render2D.text(g, "Add one with Manage ›", x + 16, cy + 17, Theme.alpha(Theme.MUTED, alpha));
			return;
		}
		for (int i = 0; i < servers.size(); i++) {
			ServerData sd = servers.get(i);
			boolean hovered = exitStart < 0 && mx >= x + 8 && mx < x + w - 8 && my >= cy && my < cy + cardH;
			Anim.Value hvA = hoverValue("srv" + i);
			float hv = hvA.target(hovered ? 1 : 0).get();
			Render2D.round(g, x + 8, cy, w - 16, cardH, 6, Theme.alpha(Theme.lerp(0x10FFFFFF, 0x22FFFFFF, hv), alpha));
			if (hv > 0.01f) Render2D.outline(g, x + 8, cy, w - 16, cardH, 6, Theme.alpha(Theme.accent1(), 0.6f * hv * alpha));
			// avatar circle with initial
			int col = Theme.wave(i / 3f);
			Render2D.circle(g, x + 22, cy + 15, 8, Theme.alpha(col, 0.9f * alpha));
			String initial = sd.name == null || sd.name.isEmpty() ? "?" : sd.name.substring(0, 1).toUpperCase();
			Render2D.textCentered(g, initial, x + 22.5f, cy + 11, Theme.alpha(0xFFFFFFFF, alpha));
			Render2D.text(g, Render2D.trim(sd.name, w - 90), x + 36, cy + 6, Theme.alpha(Theme.TEXT, alpha));
			Render2D.text(g, Render2D.trim(sd.ip, w - 90), x + 36, cy + 17, Theme.alpha(Theme.MUTED, alpha));
			float jw = 34, jx = x + w - 16 - jw;
			Render2D.roundGradient(g, jx, cy + 8, jw, 14, 3, Theme.alpha(Theme.accent1(), (0.35f + 0.5f * hv) * alpha), Theme.alpha(Theme.accent2(), (0.35f + 0.5f * hv) * alpha));
			Render2D.textCentered(g, "JOIN", jx + jw / 2f, cy + 11, Theme.alpha(Theme.TEXT, alpha));
			final ServerData target = sd;
			regions.add(new Region(x + 8, cy, w - 16, cardH, () -> runWithTransition(() -> {
				ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(target.ip), target, false, null);
			}, false)));
			cy += cardH + 5;
		}
	}

	private final java.util.Map<String, Anim.Value> hoverValues = new java.util.HashMap<>();

	private Anim.Value hoverValue(String key) {
		return hoverValues.computeIfAbsent(key, k -> new Anim.Value(0, 16));
	}

	private void runWithTransition(Runnable action, boolean quit) {
		if (exitStart >= 0) return;
		Sounds.click();
		Sounds.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f, 0.5f);
		if (quit) {
			action.run();
			return;
		}
		exitStart = Anim.time();
		exitAction = action;
	}

	private boolean skipIntro() {
		if (elapsed() < T_MOVE) {
			start = Anim.time() - T_MOVE;
			for (int i = 0; i < 5; i++) cues.add(10 + i);
			cues.add(0);
			cues.add(2);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		ripples.add(new float[] {(float) event.x(), (float) event.y(), Anim.time()});
		if (skipIntro()) return true;
		if (elapsed() < T_UI) return true;
		for (int i = regions.size() - 1; i >= 0; i--) {
			if (regions.get(i).contains(event.x(), event.y())) {
				regions.get(i).action.run();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (skipIntro()) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return super.charTyped(event);
	}
}
