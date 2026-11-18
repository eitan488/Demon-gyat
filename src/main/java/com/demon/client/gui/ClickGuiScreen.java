package com.demon.client.gui;

import com.demon.client.DemonClient;
import com.demon.client.config.Config;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.setting.Setting;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** The main mod menu. Immediate-mode style: layout and hit regions are rebuilt every frame. */
public class ClickGuiScreen extends Screen {
	private static final int SIDEBAR = 112;
	private static final int CARD_H = 36;
	private static Category lastCategory = Category.HUNTING;

	private final Screen parent;
	private float openStart = -1, closeStart = -1;
	private boolean closing;
	private Category selected = lastCategory;
	private final Anim.Value tabY = new Anim.Value(-1, 16);
	private final Anim.Value scroll = new Anim.Value(0, 18);
	private float scrollTarget;
	private float maxScroll;
	private final Map<Object, Anim.Value> anims = new HashMap<>();
	private final Set<Module> expanded = new HashSet<>();
	private Module binding;
	private String search = "";
	private boolean searchFocused;
	private NumberSetting dragging;
	private float dragX, dragW;
	private Object lastHover;
	private String hint;

	// transform used for the open/close zoom (for inverse-mapping the mouse)
	private float zoom = 1, zoomCx, zoomCy;
	private final List<Region> regions = new ArrayList<>();
	private final List<float[]> ripples = new ArrayList<>();

	private interface Click {
		void click(int button, double mx, double my);
	}

	private record Region(float x, float y, float w, float h, Click click) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public ClickGuiScreen(Screen parent) {
		super(Component.literal("Demon"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (openStart < 0) {
			openStart = Anim.time();
			Sounds.open();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private Anim.Value anim(Object key, float rate) {
		return anims.computeIfAbsent(key, k -> new Anim.Value(0, rate));
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float open = openAmount();
		if (minecraft.level == null) {
			MenuFx.background(g, width, height, 1f);
			Render2D.rect(g, 0, 0, width, height, Theme.alpha(0x99000000, open));
		} else {
			if (ModuleManager.ui.blur.on()) renderBlurredBackground(g);
			Render2D.vGradient(g, 0, 0, width, height, Theme.alpha(0x99000000, open), Theme.alpha(0xCC05030A, open));
		}
	}

	private float openAmount() {
		float t = Anim.time();
		if (closing) return 1 - Anim.easeInCubic(Anim.progress(t, closeStart, closeStart + 0.2f / Anim.speed));
		return Anim.easeOutCubic(Anim.progress(t, openStart, openStart + 0.35f / Anim.speed));
	}

	@Override
	public void render(GuiGraphics g, int mouseXi, int mouseYi, float delta) {
		float t = Anim.time();
		if (closing && t > closeStart + 0.2f / Anim.speed) {
			minecraft.setScreen(parent);
			return;
		}
		regions.clear();
		hint = null;
		float open = openAmount();
		float pop = closing ? Anim.lerp(0.94f, 1f, open) : Anim.lerp(0.9f, 1f, Anim.easeOutBack(Anim.progress(t, openStart, openStart + 0.45f / Anim.speed)));

		int pw = Math.min(480, width - 24), ph = Math.min(300, height - 24);
		int px = (width - pw) / 2, py = (height - ph) / 2;
		zoom = pop;
		zoomCx = width / 2f;
		zoomCy = height / 2f;
		double mx = toLocalX(mouseXi), my = toLocalY(mouseYi);

		g.pose().pushMatrix();
		g.pose().translate(zoomCx, zoomCy + (1 - open) * 14);
		g.pose().scale(pop, pop);
		g.pose().translate(-zoomCx, -zoomCy);

		// panel
		float glowPulse = 0.55f + 0.15f * (float) Math.sin(t * 2.0);
		Render2D.shadow(g, px - 6, py - 6, pw + 12, ph + 12, Theme.alpha(Theme.wave(0.3f), 0.32f * glowPulse * open));
		Render2D.shadow(g, px, py + 4, pw, ph, Theme.alpha(0xFF000000, 0.6f * open));
		Render2D.round(g, px, py, pw, ph, 12, Theme.alpha(0xF4100D18, open));
		Render2D.round(g, px, py, SIDEBAR, ph, 12, Theme.alpha(0xFF0B0912, open));
		Render2D.rect(g, px + SIDEBAR - 12, py, 12, ph, Theme.alpha(0xFF0B0912, open));
		Render2D.rect(g, px + SIDEBAR, py + 10, 1, ph - 20, Theme.alpha(0x14FFFFFF, open));
		g.enableScissor(px, py, px + pw, py + ph);
		MenuFx.particles(g, px, py, pw, ph, open);
		g.disableScissor();
		Render2D.outline(g, px, py, pw, ph, 12, Theme.alpha(0x26FFFFFF, open));
		// gradient top edge highlight
		Render2D.roundGradient(g, px + 30, py, pw - 60, 1, 1, Theme.alpha(Theme.accent1(), 0), Theme.alpha(Theme.accent2(), 0.8f * open));

		drawSidebar(g, px, py, ph, mx, my, open);
		drawContent(g, px + SIDEBAR + 10, py, pw - SIDEBAR - 20, ph, mx, my, open);

		// ripples
		for (int i = ripples.size() - 1; i >= 0; i--) {
			float[] r = ripples.get(i);
			float age = (t - r[2]) / 0.55f;
			if (age >= 1) {
				ripples.remove(i);
				continue;
			}
			float e = Anim.easeOutCubic(age);
			Render2D.ring(g, r[0], r[1], 3 + 26 * e, Theme.alpha(Theme.wave(0.5f), (1 - age) * 0.7f));
		}
		g.pose().popMatrix();

		Object hover = currentHoverKey(mx, my);
		if (hover != null && hover != lastHover && !closing) Sounds.hover();
		lastHover = hover;
	}

	private double toLocalX(double x) {
		return zoomCx + (x - zoomCx) / zoom;
	}

	private double toLocalY(double y) {
		return zoomCy + (y - zoomCy) / zoom;
	}

	private Object currentHoverKey(double mx, double my) {
		for (int i = regions.size() - 1; i >= 0; i--) if (regions.get(i).contains(mx, my)) return regions.get(i);
		return null;
	}

	private void drawSidebar(GuiGraphics g, int px, int py, int ph, double mx, double my, float open) {
		// brand
		Render2D.glow(g, px + 21, py + 21, 16, Theme.alpha(Theme.wave(0f), 0.35f * open));
		Render2D.sprite(g, Render2D.LOGO_ICON, px + 12, py + 12, 18, 18, Theme.alpha(0xFFFFFFFF, open));
		Render2D.logoSmall(g, px + 35, py + 13, 7.5f, open);
		Render2D.text(g, "v" + DemonClient.VERSION, px + 35, py + 24, Theme.alpha(Theme.MUTED, open));

		// category tabs
		Category[] cats = Category.values();
		int tabX = px + 8, tabW = SIDEBAR - 16, tabH = 22, top = py + 50;
		int selIndex = searchActive() ? -1 : selected.ordinal();
		if (selIndex >= 0) {
			float target = top + selIndex * (tabH + 4);
			if (tabY.peek() < 0) tabY.snap(target);
			float ty = tabY.target(target).get();
			Render2D.roundGradient(g, tabX, ty, tabW, tabH, 6, Theme.alpha(Theme.accent1(), 0.30f * open), Theme.alpha(Theme.accent2(), 0.12f * open));
			Render2D.round(g, tabX, ty + 5, 2.5f, tabH - 10, 1, Theme.alpha(Theme.wave(0.2f), open));
		}
		Render2D.text(g, "MODULES", tabX + 4, top - 11, Theme.alpha(Theme.MUTED, open));
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			float y = top + i * (tabH + 4);
			boolean hovered = mx >= tabX && mx < tabX + tabW && my >= y && my < y + tabH;
			Anim.Value hv = anim("tab" + c, 14).target(hovered || i == selIndex ? 1 : 0);
			float h = hv.get();
			if (i != selIndex && h > 0.01f) Render2D.round(g, tabX, y, tabW, tabH, 6, Theme.alpha(0x14FFFFFF, h * open));
			int color = Theme.lerp(Theme.SUBTEXT, Theme.TEXT, h);
			Render2D.icon(g, c.icon, tabX + 12 + h * 1.5f, y + 11, 9, Theme.alpha(i == selIndex ? Theme.wave(0.4f) : color, open));
			Render2D.text(g, c.title, tabX + 22 + h * 1.5f, y + 7, Theme.alpha(color, open));
			int enabled = 0;
			for (Module m : ModuleManager.byCategory(c)) if (m.isEnabled()) enabled++;
			if (enabled > 0) {
				String n = String.valueOf(enabled);
				int nw = Render2D.width(n) + 7;
				Render2D.round(g, tabX + tabW - nw - 5, y + 6, nw, 10, 3, Theme.alpha(Theme.accent1(), 0.35f * open));
				Render2D.text(g, n, tabX + tabW - nw - 1.5f, y + 7, Theme.alpha(Theme.TEXT, open));
			}
			final Category cat = c;
			regions.add(new Region(tabX, y, tabW, tabH, (b, x, yy) -> {
				if (selected != cat || searchActive()) {
					selected = cat;
					lastCategory = cat;
					search = "";
					scrollTarget = 0;
					Sounds.tab();
				}
			}));
		}

		// user card
		String name = minecraft.getUser().getName();
		int uy = py + ph - 38;
		Render2D.round(g, px + 8, uy, SIDEBAR - 16, 30, 6, Theme.alpha(0x10FFFFFF, open));
		Render2D.circle(g, px + 22, uy + 15, 9, Theme.alpha(Theme.wave(0f), open));
		Render2D.circle(g, px + 22, uy + 15, 7.5f, Theme.alpha(0xFF15111F, open));
		Render2D.textCentered(g, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), px + 22.5f, uy + 11, Theme.alpha(Theme.TEXT, open));
		Render2D.text(g, Render2D.trim(name, SIDEBAR - 52), px + 36, uy + 6, Theme.alpha(Theme.TEXT, open));
		boolean inWorld = minecraft.level != null;
		Render2D.circle(g, px + 38, uy + 20, 2, Theme.alpha(inWorld ? Theme.SUCCESS : Theme.accent2(), open));
		Render2D.text(g, inWorld ? (minecraft.isSingleplayer() ? "Singleplayer" : "Online") : "Main menu", px + 43, uy + 16, Theme.alpha(Theme.MUTED, open));
	}

	private boolean searchActive() {
		return !search.isEmpty();
	}

	private void drawContent(GuiGraphics g, int cx, int py, int cw, int ph, double mx, double my, float open) {
		// header
		String title = searchActive() ? "Search" : selected.title;
		Render2D.scaledText(g, title, cx + 2, py + 13, 1.5f, Theme.alpha(Theme.TEXT, open), false);
		List<Module> mods = filtered();
		Render2D.text(g, mods.size() + (mods.size() == 1 ? " module" : " modules"), cx + 3 + Render2D.width(title) * 1.5f + 6, py + 18, Theme.alpha(Theme.MUTED, open));

		// search box
		int sw = 118, sh = 17, sx = cx + cw - sw, sy = py + 11;
		Anim.Value focus = anim("searchFocus", 14).target(searchFocused ? 1 : 0);
		float f = focus.get();
		Render2D.round(g, sx, sy, sw, sh, 6, Theme.alpha(Theme.lerp(0xFF16131F, 0xFF1D1829, f), open));
		Render2D.outline(g, sx, sy, sw, sh, 6, Theme.alpha(Theme.lerp(0x18FFFFFF, Theme.accent1(), f), open));
		Render2D.ring(g, sx + 10, sy + 8, 3.5f, Theme.alpha(Theme.lerp(Theme.MUTED, Theme.accent1(), f), open));
		Render2D.line(g, sx + 12.5f, sy + 10.5f, sx + 15, sy + 13, Theme.alpha(Theme.lerp(Theme.MUTED, Theme.accent1(), f), open));
		boolean caret = searchFocused && (Anim.time() * 2 % 2) < 1.2f;
		if (search.isEmpty() && !searchFocused) Render2D.text(g, "Search modules…", sx + 19, sy + 5, Theme.alpha(Theme.MUTED, open));
		else Render2D.text(g, Render2D.trim(search, sw - 28) + (caret ? "_" : ""), sx + 19, sy + 5, Theme.alpha(Theme.TEXT, open));
		regions.add(new Region(sx, sy, sw, sh, (b, x, y) -> {
			searchFocused = true;
			Sounds.click();
		}));

		// list area
		int listTop = py + 38, listBottom = py + ph - 22;
		int areaH = listBottom - listTop;
		float sc = scroll.target(scrollTarget).get();
		g.enableScissor(cx - 4, listTop, cx + cw + 4, listBottom);
		float y = listTop + 2 - sc;
		float contentH = 0;
		for (Module m : mods) {
			float h = drawModule(g, m, cx, y, cw, mx, my, open, listTop, listBottom);
			y += h + 6;
			contentH += h + 6;
		}
		if (mods.isEmpty()) {
			Render2D.textCentered(g, "No modules match \"" + search + "\"", cx + cw / 2f, listTop + 30, Theme.alpha(Theme.MUTED, open));
		}
		g.disableScissor();
		maxScroll = Math.max(0, contentH - areaH + 4);
		scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);

		// scrollbar
		if (maxScroll > 0) {
			float barH = Math.max(18, areaH * areaH / (contentH + 4));
			float barY = listTop + (areaH - barH) * (sc / maxScroll);
			Render2D.round(g, cx + cw + 2, barY, 2.5f, barH, 1, Theme.alpha(Theme.wave(0.5f), 0.6f * open));
		}
		// fade edges of list
		Render2D.vGradient(g, cx - 4, listTop, cw + 8, 6, Theme.alpha(0xF4100D18, open), 0);

		// footer hint
		String foot = binding != null ? "Press a key to bind " + binding.name + " · ESC cancel · DEL unbind"
			: hint != null ? hint : "LMB toggle · RMB settings · MMB bind · RShift close";
		Render2D.text(g, Render2D.trim(foot, cw), cx + 2, py + ph - 15, Theme.alpha(binding != null ? Theme.wave(0.5f) : Theme.MUTED, open));
	}

	private List<Module> filtered() {
		List<Module> out = new ArrayList<>();
		String q = search.toLowerCase(Locale.ROOT).trim();
		for (Module m : ModuleManager.all()) {
			if (q.isEmpty() ? m.category == selected : (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q))) out.add(m);
		}
		return out;
	}

	private float settingsHeight(Module m) {
		float h = 4;
		for (Setting<?> s : m.settings()) {
			if (!s.isVisible()) continue;
			h += rowHeight(s);
		}
		return h + 4;
	}

	private static float rowHeight(Setting<?> s) {
		return s instanceof NumberSetting ? 25 : 18;
	}

	private float drawModule(GuiGraphics g, Module m, float x, float y, float w, double mx, double my, float open, int clipTop, int clipBottom) {
		Anim.Value exp = anim("exp" + m.name, 13).target(expanded.contains(m) ? 1 : 0);
		float e = exp.get();
		float sh = settingsHeight(m);
		float h = CARD_H + sh * e;
		if (y > clipBottom || y + h < clipTop) return h;

		boolean inClip = my >= clipTop && my < clipBottom;
		boolean hovered = inClip && mx >= x && mx < x + w && my >= y && my < y + CARD_H;
		float hv = anim("hov" + m.name, 16).target(hovered ? 1 : 0).get();
		float on = m.guiToggle.target(m.isEnabled() || !m.toggleable() ? 1 : 0).get();
		if (!m.toggleable()) on = 0.35f;

		int bg = Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hv);
		if (hv > 0.01f) Render2D.shadow(g, x, y, w, h, Theme.alpha(Theme.accent1(), 0.12f * hv * open));
		Render2D.round(g, x, y, w, h, 6, Theme.alpha(bg, open));
		if (on > 0.01f) Render2D.roundGradient(g, x, y, w, CARD_H, 6, Theme.alpha(Theme.accent1(), 0.16f * on * open), Theme.alpha(Theme.accent2(), 0.0f));
		Render2D.outline(g, x, y, w, h, 6, Theme.alpha(Theme.lerp(0x14FFFFFF, Theme.accent1(), on * 0.5f), open));
		if (on > 0.01f) Render2D.round(g, x + 1, y + 9, 2.5f, CARD_H - 18, 1, Theme.alpha(Theme.wave(0.1f), on * open));

		// icon tile
		float ix = x + 9, iy = y + 7;
		if (on > 0.01f) {
			Render2D.glow(g, ix + 11, iy + 11, 16, Theme.alpha(Theme.accent1(), 0.35f * on * open));
			Render2D.roundGradient(g, ix, iy, 22, 22, 6, Theme.alpha(Theme.accent1(), on * open), Theme.alpha(Theme.accent2(), on * open));
		}
		if (on < 0.99f) Render2D.round(g, ix, iy, 22, 22, 6, Theme.alpha(0x16FFFFFF, (1 - on) * open));
		Render2D.icon(g, m.icon(), ix + 11, iy + 11, 12, Theme.alpha(Theme.lerp(Theme.SUBTEXT, 0xFFFFFFFF, Math.max(on, hv * 0.5f)), open));

		// texts
		Render2D.text(g, m.name, x + 38 + hv * 1.5f, y + 8, Theme.alpha(Theme.TEXT, open));
		Render2D.text(g, Render2D.trim(m.description, (int) w - 150), x + 38 + hv * 1.5f, y + 21, Theme.alpha(Theme.SUBTEXT, open));

		// chevron
		float chevX = x + w - 13, chevY = y + CARD_H / 2f;
		g.pose().pushMatrix();
		g.pose().translate(chevX, chevY);
		g.pose().rotate(e * (float) Math.PI / 2f);
		Render2D.text(g, "›", -2, -4.5f, Theme.alpha(Theme.lerp(Theme.MUTED, Theme.TEXT, Math.max(hv, e)), open));
		g.pose().popMatrix();

		// toggle switch
		float tx = x + w - 52, ty = y + 11;
		if (m.toggleable()) {
			drawSwitch(g, tx, ty, 26, 14, on, open);
		} else {
			Render2D.round(g, tx - 14, ty + 1, 40, 12, 3, Theme.alpha(0x14FFFFFF, open));
			Render2D.textCentered(g, "always", tx + 6, ty + 3, Theme.alpha(Theme.MUTED, open));
		}

		// keybind chip
		boolean isBinding = binding == m;
		String key = isBinding ? "•••" : keyName(m.getKey());
		int kw = Math.max(18, Render2D.width(key) + 8);
		float kx = tx - (m.toggleable() ? 8 : 22) - kw;
		float pulse = isBinding ? 0.5f + 0.5f * (float) Math.sin(Anim.time() * 8) : 0;
		Render2D.round(g, kx, ty + 1, kw, 12, 3, Theme.alpha(isBinding ? Theme.lerp(0xFF2A2540, Theme.accent1(), pulse) : 0x12FFFFFF, open));
		Render2D.textCentered(g, key, kx + kw / 2f, ty + 3, Theme.alpha(isBinding ? Theme.TEXT : Theme.MUTED, open));

		if (inClip) {
			regions.add(new Region(kx, ty + 1, kw, 12, (b, mxx, myy) -> startBinding(m)));
			if (m.toggleable()) regions.add(new Region(tx - 2, ty - 2, 30, 18, (b, mxx, myy) -> {
				if (b == 0) toggle(m);
				else if (b == 1) expand(m);
			}));
			regions.add(new Region(x, y, w, CARD_H, (b, mxx, myy) -> {
				if (b == 0) {
					if (m.toggleable()) toggle(m);
					else expand(m);
				} else if (b == 1) expand(m);
				else if (b == 2) startBinding(m);
			}));
			if (hovered) hint = m.description;
		}

		// settings
		if (e > 0.01f) {
			int sy0 = (int) Math.max(clipTop, y + CARD_H);
			int sy1 = (int) Math.min(clipBottom, y + h);
			if (sy1 > sy0) {
				g.enableScissor((int) x, sy0, (int) (x + w), sy1);
				Render2D.rect(g, (int) x + 10, (int) (y + CARD_H), (int) w - 20, 1, Theme.alpha(0x12FFFFFF, open * e));
				float ry = y + CARD_H + 4;
				for (Setting<?> s : m.settings()) {
					if (!s.isVisible()) continue;
					float rh = rowHeight(s);
					boolean visible = ry + rh > sy0 && ry < sy1;
					drawSetting(g, s, x + 12, ry, w - 24, rh, mx, my, open * e, visible && e > 0.95f && inClip);
					ry += rh;
				}
				g.disableScissor();
			}
		}
		return h;
	}

	private void drawSwitch(GuiGraphics g, float x, float y, float w, float h, float on, float alpha) {
		int track = Theme.lerp(Theme.TRACK, Theme.accent1(), on);
		if (on > 0.5f) Render2D.roundGradient(g, x, y, w, h, 6, Theme.alpha(Theme.accent1(), alpha), Theme.alpha(Theme.accent2(), alpha));
		else Render2D.round(g, x, y, w, h, 6, Theme.alpha(track, alpha));
		float knobX = x + h / 2f + (w - h) * Anim.easeOutBack(on);
		if (on > 0.05f) Render2D.glow(g, knobX, y + h / 2f, h * 0.9f, Theme.alpha(0xFFFFFFFF, 0.25f * on * alpha));
		Render2D.circle(g, knobX, y + h / 2f, h / 2f - 2, Theme.alpha(0xFFFFFFFF, alpha));
	}

	private void drawSetting(GuiGraphics g, Setting<?> s, float x, float y, float w, float h, double mx, double my, float alpha, boolean interactive) {
		boolean hovered = interactive && mx >= x - 4 && mx < x + w + 4 && my >= y && my < y + h;
		float hv = anim(s, 16).target(hovered ? 1 : 0).get();
		if (hv > 0.01f) Render2D.round(g, x - 5, y + 1, w + 10, h - 2, 3, Theme.alpha(0x0CFFFFFF, hv * alpha));
		Render2D.text(g, s.name, x + hv, y + 5, Theme.alpha(Theme.lerp(Theme.SUBTEXT, Theme.TEXT, hv), alpha));
		if (hovered) hint = s.description;

		if (s instanceof BoolSetting b) {
			float on = anim(b.name + "#" + System.identityHashCode(b), 16).target(b.on() ? 1 : 0).get();
			drawSwitch(g, x + w - 20, y + 4, 20, 10, on, alpha);
			if (interactive) regions.add(new Region(x - 4, y, w + 8, h, (btn, mxx, myy) -> {
				b.toggle();
				Sounds.toggle(b.on());
			}));
		} else if (s instanceof NumberSetting n) {
			String val = n.display();
			Render2D.textRight(g, val, x + w, y + 5, Theme.alpha(Theme.wave(n.fraction()), alpha));
			float trackY = y + 16;
			float frac = anim(n.name + "#v" + System.identityHashCode(n), 20).target(n.fraction()).get();
			Render2D.round(g, x, trackY, w, 3, 1, Theme.alpha(Theme.TRACK, alpha));
			Render2D.roundGradient(g, x, trackY, Math.max(3, w * frac), 3, 1, Theme.alpha(Theme.accent1(), alpha), Theme.alpha(Theme.accent2(), alpha));
			float kx = x + w * frac;
			boolean active = dragging == n;
			Render2D.glow(g, kx, trackY + 1.5f, active ? 9 : 6, Theme.alpha(Theme.accent2(), (active ? 0.6f : 0.3f) * alpha));
			Render2D.circle(g, kx, trackY + 1.5f, active ? 4f : 3.5f, Theme.alpha(0xFFFFFFFF, alpha));
			if (interactive) regions.add(new Region(x - 4, y, w + 8, h, (btn, mxx, myy) -> {
				dragging = n;
				dragX = x;
				dragW = w;
				updateDrag(mxx);
			}));
		} else if (s instanceof EnumSetting<?> en) {
			String val = en.get().toString();
			int vw = Render2D.width(val);
			float pillW = vw + 22;
			float pillX = x + w - pillW;
			Render2D.round(g, pillX, y + 3, pillW, 12, 3, Theme.alpha(Theme.lerp(0x16FFFFFF, Theme.accent1(), 0.25f + 0.2f * hv), alpha));
			Render2D.text(g, "‹", pillX + 4, y + 5, Theme.alpha(Theme.MUTED, alpha));
			Render2D.text(g, "›", pillX + pillW - 7, y + 5, Theme.alpha(Theme.MUTED, alpha));
			Render2D.text(g, val, pillX + 11, y + 5, Theme.alpha(Theme.TEXT, alpha));
			if (interactive) regions.add(new Region(x - 4, y, w + 8, h, (btn, mxx, myy) -> {
				en.cycle(btn == 1 ? -1 : 1);
				Sounds.tick(1.2f + 0.1f * en.get().ordinal());
			}));
		}
	}

	private static String keyName(int key) {
		if (key <= 0) return "—";
		String n = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
		return n.replace("Right ", "R").replace("Left ", "L");
	}

	// ------------------------------------------------------------------ actions

	private void toggle(Module m) {
		m.toggle();
		Sounds.toggle(m.isEnabled());
	}

	private void expand(Module m) {
		if (m.settings().isEmpty()) return;
		if (!expanded.remove(m)) expanded.add(m);
		Sounds.play(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, expanded.contains(m) ? 1.6f : 1.2f, 0.3f);
	}

	private void startBinding(Module m) {
		binding = binding == m ? null : m;
		searchFocused = false;
		Sounds.click();
	}

	private void updateDrag(double mx) {
		if (dragging == null) return;
		double frac = Anim.clamp01((float) ((mx - dragX) / dragW));
		double before = dragging.get();
		dragging.set(dragging.min + (dragging.max - dragging.min) * frac);
		if (dragging.get() != before) Sounds.tick(0.9f + dragging.fraction() * 1.1f);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		double mx = toLocalX(event.x()), my = toLocalY(event.y());
		ripples.add(new float[] {(float) mx, (float) my, Anim.time()});
		boolean wasSearch = searchFocused;
		searchFocused = false;
		for (int i = regions.size() - 1; i >= 0; i--) {
			Region r = regions.get(i);
			if (r.contains(mx, my)) {
				r.click.click(event.button(), mx, my);
				return true;
			}
		}
		if (wasSearch) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			updateDrag(toLocalX(event.x()));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			Config.markDirty();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		scrollTarget = Anim.clamp(scrollTarget - (float) sy * 28, 0, maxScroll);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (binding != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				binding = null;
			} else if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
				binding.setKey(-1);
				Notifications.push(binding.name, "Keybind removed", Theme.SUBTEXT, 1.5f);
				binding = null;
			} else {
				binding.setKey(key);
				Notifications.push(binding.name, "Bound to " + keyName(key), Theme.accent1(), 1.5f);
				binding = null;
			}
			Sounds.click();
			return true;
		}
		if (searchFocused) {
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
				scrollTarget = 0;
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
				searchFocused = false;
				return true;
			}
			return true;
		}
		// The key that opened this screen is also forwarded to it by vanilla, so ignore it right after opening.
		if (key == ModuleManager.ui.getKey() && key > 0 && Anim.time() - openStart > 0.25f) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (searchFocused && event.isAllowedChatCharacter() && search.length() < 24) {
			search += event.codepointAsString();
			scrollTarget = 0;
			Sounds.tick(1.6f + (search.length() % 5) * 0.08f);
			return true;
		}
		if (!searchFocused && binding == null && event.isAllowedChatCharacter() && !Character.isWhitespace(event.codepoint())) {
			// typing anywhere starts a search
			searchFocused = true;
			search += event.codepointAsString();
			scrollTarget = 0;
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public void onClose() {
		if (closing) return;
		closing = true;
		closeStart = Anim.time();
		binding = null;
		Sounds.close();
		Config.save();
	}
}
