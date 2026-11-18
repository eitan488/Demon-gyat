package com.demon.client.gui;

import com.demon.client.DemonClient;
import com.demon.client.config.Config;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
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

/** The main mod menu (blue HD). Immediate-mode style: layout and hit regions are rebuilt every frame. */
public class ClickGuiScreen extends Screen {
	private static final float SIDEBAR = 116;
	private static final float CARD_H = 34;
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

	private float zoom = 1, zoomCx, zoomCy, zoomDy;
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
			Render2D.rect(g, 0, 0, width, height, Theme.alpha(0x8C02050C, open));
		} else {
			if (ModuleManager.ui.blur.on()) renderBlurredBackground(g);
			Render2D.vGradient(g, 0, 0, width, height, Theme.alpha(0x8C020812, open), Theme.alpha(0xCC01040A, open));
		}
		Render2D.glow(g, width / 2f, height / 2f, Math.max(width, height) * 0.45f, Theme.alpha(Theme.accent1(), 0.10f * open));
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
		float pop = closing ? Anim.lerp(0.95f, 1f, open) : Anim.lerp(0.92f, 1f, Anim.easeOutBack(Anim.progress(t, openStart, openStart + 0.45f / Anim.speed)));

		float pw = Math.min(470, width - 24), ph = Math.min(292, height - 24);
		float px = Math.round((width - pw) / 2f), py = Math.round((height - ph) / 2f);
		zoom = pop;
		zoomCx = width / 2f;
		zoomCy = height / 2f;
		zoomDy = (1 - open) * 12;
		double mx = toLocalX(mouseXi), my = toLocalY(mouseYi);

		g.pose().pushMatrix();
		g.pose().translate(zoomCx, zoomCy + zoomDy);
		g.pose().scale(pop, pop);
		g.pose().translate(-zoomCx, -zoomCy);

		// window
		float glowPulse = 0.5f + 0.15f * (float) Math.sin(t * 2.0);
		Render2D.shadow(g, px - 8, py - 8, pw + 16, ph + 16, Theme.alpha(Theme.accent1(), 0.30f * glowPulse * open));
		Ui.glass(g, px, py, pw, ph, 10, open);
		// sidebar
		Render2D.round(g, px, py, SIDEBAR, ph, 10, Theme.alpha(0x99040914, open));
		Render2D.rect(g, px + SIDEBAR - 10, py + 0.4f, 10, ph - 0.8f, Theme.alpha(0x99040914, open));
		Render2D.rect(g, px + SIDEBAR, py + 10, 0.35f, ph - 20, Theme.alpha(0x2A8CB8FF, open));
		g.enableScissor((int) px, (int) py, (int) (px + pw), (int) (py + ph));
		MenuFx.particles(g, px, py, pw, ph, open * 0.8f);
		g.disableScissor();
		Render2D.hGradient(g, px + 40, py, pw - 80, 0.5f, Theme.alpha(Theme.accent1(), 0), Theme.alpha(Theme.accent2(), 0.9f * open));

		drawSidebar(g, px, py, ph, mx, my, open);
		drawContent(g, px + SIDEBAR + 11, py, pw - SIDEBAR - 22, ph, mx, my, open);

		// click ripples
		for (int i = ripples.size() - 1; i >= 0; i--) {
			float[] r = ripples.get(i);
			float age = (t - r[2]) / 0.55f;
			if (age >= 1) {
				ripples.remove(i);
				continue;
			}
			float e = Anim.easeOutCubic(age);
			Render2D.ring(g, r[0], r[1], 3 + 22 * e, 0.6f, Theme.alpha(Theme.accent2(), (1 - age) * 0.7f));
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
		return zoomCy + (y - zoomCy - zoomDy) / zoom;
	}

	private Object currentHoverKey(double mx, double my) {
		for (int i = regions.size() - 1; i >= 0; i--) if (regions.get(i).contains(mx, my)) return regions.get(i);
		return null;
	}

	private void drawSidebar(GuiGraphics g, float px, float py, float ph, double mx, double my, float open) {
		// brand
		Render2D.glow(g, px + 20, py + 20, 15, Theme.alpha(Theme.accent1(), 0.45f * open));
		Render2D.image(g, Render2D.LOGO_ICON, px + 11, py + 11, 18, 18, Theme.alpha(0xFFFFFFFF, open));
		Render2D.logoSmall(g, px + 34, py + 12.5f, 6.6f, open);
		Render2D.text(g, HdFont.MEDIUM, "v" + DemonClient.VERSION + "  ·  1.21.11", px + 34, py + 23.5f, 5.2f, Theme.alpha(Theme.MUTED, open));

		Category[] cats = Category.values();
		float tabX = px + 8, tabW = SIDEBAR - 16, top = py + 50;
		float tabH = Math.min(20, (ph - 50 - 42) / Category.values().length - 2);
		Ui.label(g, "MODULES", tabX + 3, top - 10, open);
		int selIndex = searchActive() ? -1 : selected.ordinal();
		if (selIndex >= 0) {
			float target = top + selIndex * (tabH + 2);
			if (tabY.peek() < 0) tabY.snap(target);
			float ty = tabY.target(target).get();
			Render2D.roundGradient(g, tabX, ty, tabW, tabH, 6, Theme.alpha(Theme.accent1(), 0.32f * open), Theme.alpha(Theme.accent2(), 0.06f * open));
			Render2D.outline(g, tabX, ty, tabW, tabH, 6, Theme.alpha(Theme.accent1(), 0.35f * open));
			Render2D.capsule(g, tabX - 0.5f, ty + 6, 2, tabH - 12, Theme.alpha(Theme.accent2(), open));
		}
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			float y = top + i * (tabH + 2);
			boolean hovered = mx >= tabX && mx < tabX + tabW && my >= y && my < y + tabH;
			float h = anim("tab" + c, 14).target(hovered || i == selIndex ? 1 : 0).get();
			if (i != selIndex && h > 0.01f) Render2D.round(g, tabX, y, tabW, tabH, 6, Theme.alpha(0x10FFFFFF, h * open));
			int color = Theme.lerp(Theme.SUBTEXT, Theme.TEXT, h);
			Render2D.icon(g, c.icon, tabX + 12 + h, y + tabH / 2f, 9, Theme.alpha(i == selIndex ? Theme.accent2() : color, open));
			Render2D.textMid(g, i == selIndex ? HdFont.SEMIBOLD : HdFont.MEDIUM, c.title, tabX + 23 + h, y, tabH, 7.2f, Theme.alpha(color, open));
			int enabled = 0;
			for (Module m : ModuleManager.byCategory(c)) if (m.isEnabled()) enabled++;
			if (enabled > 0) {
				String n = String.valueOf(enabled);
				float bw = HdFont.SEMIBOLD.width(n, 5.4f) + 6;
				Ui.badge(g, n, tabX + tabW - bw - 5, y + (tabH - 8.1f) / 2f, 5.4f, Theme.alpha(Theme.accent1(), 0.45f * open), Theme.alpha(Theme.TEXT, open));
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
		float uy = py + ph - 36;
		Render2D.round(g, px + 8, uy, SIDEBAR - 16, 28, 7, Theme.alpha(0x0DFFFFFF, open));
		Render2D.outline(g, px + 8, uy, SIDEBAR - 16, 28, 7, Theme.alpha(0x149CC4FF, open));
		float ax = px + 21, ay = uy + 14;
		Render2D.circle(g, ax, ay, 8.5f, Theme.alpha(Theme.accent1(), open));
		Render2D.circle(g, ax, ay, 7.2f, Theme.alpha(0xFF0A1428, open));
		Render2D.textCentered(g, HdFont.BOLD, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), ax, ay - HdFont.BOLD.capHeight(7f) / 2f, 7f, Theme.alpha(Theme.TEXT, open));
		Render2D.text(g, HdFont.SEMIBOLD, Render2D.trim(HdFont.SEMIBOLD, name, 6.6f, SIDEBAR - 50), px + 34, uy + 7.5f, 6.6f, Theme.alpha(Theme.TEXT, open));
		boolean inWorld = minecraft.level != null;
		Render2D.circle(g, px + 36, uy + 19.7f, 1.6f, Theme.alpha(inWorld ? Theme.SUCCESS : Theme.accent2(), open));
		Render2D.text(g, HdFont.MEDIUM, inWorld ? (minecraft.isSingleplayer() ? "Singleplayer" : "Online") : "Main menu", px + 40, uy + 17.6f, 5.4f, Theme.alpha(Theme.MUTED, open));
	}

	private boolean searchActive() {
		return !search.isEmpty();
	}

	private void drawContent(GuiGraphics g, float cx, float py, float cw, float ph, double mx, double my, float open) {
		String title = searchActive() ? "Search" : selected.title;
		Render2D.text(g, HdFont.SEMIBOLD, title, cx + 1, py + 14, 12f, Theme.alpha(Theme.TEXT, open));
		List<Module> mods = filtered();
		float tw = HdFont.SEMIBOLD.width(title, 12f);
		Render2D.text(g, HdFont.MEDIUM, mods.size() + (mods.size() == 1 ? " module" : " modules"), cx + tw + 7, py + 17.6f, 5.8f, Theme.alpha(Theme.MUTED, open));

		// search box
		float sw = 116, sh = 16, sx = cx + cw - sw, sy = py + 11;
		float f = anim("searchFocus", 14).target(searchFocused ? 1 : 0).get();
		Render2D.round(g, sx, sy, sw, sh, 8, Theme.alpha(Theme.lerp(0xFF0C1528, 0xFF101E38, f), open));
		Render2D.outline(g, sx, sy, sw, sh, 8, Theme.alpha(Theme.lerp(0x229CC4FF, Theme.accent1(), f), open));
		Render2D.icon(g, "icon_search", sx + 9, sy + sh / 2f, 7, Theme.alpha(Theme.lerp(Theme.MUTED, Theme.accent2(), f), open));
		boolean caret = searchFocused && (Anim.time() * 2 % 2) < 1.2f;
		if (search.isEmpty() && !searchFocused) {
			Render2D.textMid(g, HdFont.MEDIUM, "Search modules…", sx + 16, sy, sh, 6.2f, Theme.alpha(Theme.MUTED, open));
		} else {
			String shown = Render2D.trim(HdFont.MEDIUM, search, 6.2f, sw - 26);
			Render2D.textMid(g, HdFont.MEDIUM, shown, sx + 16, sy, sh, 6.2f, Theme.alpha(Theme.TEXT, open));
			if (caret) Render2D.rect(g, sx + 16.5f + HdFont.MEDIUM.width(shown, 6.2f), sy + 4.5f, 0.5f, sh - 9, Theme.alpha(Theme.accent2(), open));
		}
		regions.add(new Region(sx, sy, sw, sh, (b, x, y) -> {
			searchFocused = true;
			Sounds.click();
		}));

		// list
		float listTop = py + 36, listBottom = py + ph - 20;
		float areaH = listBottom - listTop;
		float sc = scroll.target(scrollTarget).get();
		g.enableScissor((int) (cx - 6), (int) listTop, (int) (cx + cw + 6), (int) listBottom);
		float y = listTop + 2 - sc;
		float contentH = 0;
		for (Module m : mods) {
			float h = drawModule(g, m, cx, y, cw, mx, my, open, listTop, listBottom);
			y += h + 5;
			contentH += h + 5;
		}
		if (mods.isEmpty()) {
			Render2D.textCentered(g, HdFont.MEDIUM, "No modules match \"" + search + "\"", cx + cw / 2f, listTop + 30, 6.6f, Theme.alpha(Theme.MUTED, open));
		}
		g.disableScissor();
		maxScroll = Math.max(0, contentH - areaH + 4);
		scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
		if (maxScroll > 0) {
			float barH = Math.max(18, areaH * areaH / (contentH + 4));
			float barY = listTop + (areaH - barH) * (sc / maxScroll);
			Render2D.capsule(g, cx + cw + 3.5f, barY, 1.8f, barH, Theme.alpha(Theme.accent1(), 0.7f * open));
		}

		// footer hint
		Render2D.rect(g, cx, py + ph - 18, cw, 0.35f, Theme.alpha(0x1F9CC4FF, open));
		String foot = binding != null ? "Press a key to bind " + binding.name + "  ·  ESC cancel  ·  DEL unbind"
			: hint != null ? hint : "Left click toggle  ·  Right click settings  ·  Middle click bind  ·  RShift close";
		Render2D.text(g, HdFont.MEDIUM, Render2D.trim(HdFont.MEDIUM, foot, 5.8f, cw), cx + 1, py + ph - 11.5f, 5.8f,
			Theme.alpha(binding != null ? Theme.accent2() : Theme.MUTED, open));
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
		for (Setting<?> s : m.settings()) if (s.isVisible()) h += rowHeight(s);
		return h + 4;
	}

	private static float rowHeight(Setting<?> s) {
		return s instanceof NumberSetting ? 21 : 15;
	}

	private float drawModule(GuiGraphics g, Module m, float x, float y, float w, double mx, double my, float open, float clipTop, float clipBottom) {
		float e = anim("exp" + m.name, 13).target(expanded.contains(m) ? 1 : 0).get();
		float sh = settingsHeight(m);
		float h = CARD_H + sh * e;
		if (y > clipBottom || y + h < clipTop) return h;

		boolean inClip = my >= clipTop && my < clipBottom;
		boolean hovered = inClip && mx >= x && mx < x + w && my >= y && my < y + CARD_H;
		float hv = anim("hov" + m.name, 16).target(hovered ? 1 : 0).get();
		float on = m.guiToggle.target(m.isEnabled() || !m.toggleable() ? 1 : 0).get();
		if (!m.toggleable()) on = 0.35f;

		if (hv > 0.01f) Render2D.shadow(g, x, y, w, h, Theme.alpha(Theme.accent1(), 0.16f * hv * open));
		Render2D.round(g, x, y, w, h, 7, Theme.alpha(Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hv), open));
		if (on > 0.01f) Render2D.roundGradient(g, x, y, w, CARD_H, 7, Theme.alpha(Theme.accent1(), 0.16f * on * open), Theme.alpha(Theme.accent2(), 0.0f));
		Render2D.outline(g, x, y, w, h, 7, Theme.alpha(Theme.lerp(0x1A9CC4FF, Theme.accent1(), on * 0.55f + hv * 0.15f), open));

		// icon tile
		float ix = x + 7, iy = y + 6;
		if (on > 0.01f) {
			Render2D.glow(g, ix + 11, iy + 11, 15, Theme.alpha(Theme.accent1(), 0.40f * on * open));
			Render2D.roundGradient(g, ix, iy, 22, 22, 6, Theme.alpha(Theme.accent1(), on * open), Theme.alpha(Theme.accent2(), on * open));
		}
		if (on < 0.99f) Render2D.round(g, ix, iy, 22, 22, 6, Theme.alpha(0x149CC4FF, (1 - on) * open));
		Render2D.icon(g, m.icon(), ix + 11, iy + 11, 11, Theme.alpha(Theme.lerp(Theme.SUBTEXT, 0xFFFFFFFF, Math.max(on, hv * 0.5f)), open));

		Render2D.text(g, HdFont.SEMIBOLD, m.name, x + 36 + hv, y + 9.5f, 7.8f, Theme.alpha(Theme.TEXT, open));
		Render2D.text(g, HdFont.REGULAR, Render2D.trim(HdFont.REGULAR, m.description, 6f, w - 140), x + 36 + hv, y + 20.5f, 6f, Theme.alpha(Theme.SUBTEXT, open));

		// chevron
		Render2D.icon(g, e > 0.5f ? "icon_chevron_down" : "icon_chevron_right", x + w - 11, y + CARD_H / 2f, 7,
			Theme.alpha(Theme.lerp(Theme.MUTED, Theme.TEXT, Math.max(hv, e)), open));

		// switch
		float tx = x + w - 46, ty = y + 10.5f;
		if (m.toggleable()) {
			Ui.toggle(g, tx, ty, 24, 13, m.guiToggle.peek(), open);
		} else {
			Ui.badge(g, "ALWAYS ON", tx - 18, ty + 1.5f, 5.2f, Theme.alpha(0x149CC4FF, open), Theme.alpha(Theme.MUTED, open));
		}

		// keybind chip
		boolean isBinding = binding == m;
		String key = isBinding ? "•••" : keyName(m.getKey());
		float kw = Math.max(16, HdFont.SEMIBOLD.width(key, 5.6f) + 9);
		float kx = tx - (m.toggleable() ? 6 : 24) - kw;
		float pulse = isBinding ? 0.5f + 0.5f * (float) Math.sin(Anim.time() * 8) : 0;
		Render2D.round(g, kx, ty + 1.5f, kw, 10, 3.5f, Theme.alpha(isBinding ? Theme.lerp(Theme.TRACK, Theme.accent1(), pulse) : 0x0FFFFFFF, open));
		Render2D.outline(g, kx, ty + 1.5f, kw, 10, 3.5f, Theme.alpha(0x1A9CC4FF, open));
		Render2D.textCentered(g, HdFont.SEMIBOLD, key, kx + kw / 2f, ty + 1.5f + (10 - HdFont.SEMIBOLD.capHeight(5.6f)) / 2f, 5.6f,
			Theme.alpha(isBinding ? Theme.TEXT : Theme.SUBTEXT, open));

		if (inClip) {
			regions.add(new Region(kx, ty + 1.5f, kw, 10, (b, mxx, myy) -> startBinding(m)));
			if (m.toggleable()) regions.add(new Region(tx - 2, ty - 2, 28, 17, (b, mxx, myy) -> {
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

		if (e > 0.01f) {
			int sy0 = (int) Math.max(clipTop, y + CARD_H);
			int sy1 = (int) Math.min(clipBottom, y + h);
			if (sy1 > sy0) {
				g.enableScissor((int) x, sy0, (int) (x + w), sy1);
				Render2D.rect(g, x + 9, y + CARD_H, w - 18, 0.35f, Theme.alpha(0x1F9CC4FF, open * e));
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

	private void drawSetting(GuiGraphics g, Setting<?> s, float x, float y, float w, float h, double mx, double my, float alpha, boolean interactive) {
		boolean hovered = interactive && mx >= x - 4 && mx < x + w + 4 && my >= y && my < y + h;
		float hv = anim(s, 16).target(hovered ? 1 : 0).get();
		if (hv > 0.01f) Render2D.round(g, x - 5, y + 1, w + 10, h - 2, 4, Theme.alpha(0x0E9CC4FF, hv * alpha));
		float labelSize = 6.4f;
		float rowMid = s instanceof NumberSetting ? 9 : h;
		Render2D.textMid(g, HdFont.MEDIUM, s.name, x + hv, y, rowMid, labelSize, Theme.alpha(Theme.lerp(Theme.SUBTEXT, Theme.TEXT, hv), alpha));
		if (hovered) hint = s.description;

		if (s instanceof BoolSetting b) {
			float on = anim(b.name + "#" + System.identityHashCode(b), 16).target(b.on() ? 1 : 0).get();
			Ui.toggle(g, x + w - 19, y + (h - 10) / 2f, 19, 10, on, alpha);
			if (interactive) regions.add(new Region(x - 4, y, w + 8, h, (btn, mxx, myy) -> {
				b.toggle();
				Sounds.toggle(b.on());
			}));
		} else if (s instanceof NumberSetting n) {
			String val = n.display();
			Render2D.textRight(g, HdFont.BOLD, val, x + w, y + (9 - HdFont.BOLD.capHeight(6.2f)) / 2f, 6.2f, Theme.alpha(Theme.accent2(), alpha));
			float trackY = y + 13.5f;
			float frac = anim(n.name + "#v" + System.identityHashCode(n), 20).target(n.fraction()).get();
			Render2D.capsule(g, x, trackY, w, 2.6f, Theme.alpha(Theme.TRACK, alpha));
			Render2D.roundGradient(g, x, trackY, Math.max(2.6f, w * frac), 2.6f, 1.3f, Theme.alpha(Theme.accent1(), alpha), Theme.alpha(Theme.accent2(), alpha));
			float kx = x + w * frac;
			boolean active = dragging == n;
			Render2D.glow(g, kx, trackY + 1.3f, active ? 9 : 6, Theme.alpha(Theme.accent2(), (active ? 0.7f : 0.35f) * alpha));
			Render2D.circle(g, kx, trackY + 1.3f, active ? 3.8f : 3.3f, Theme.alpha(0xFFFFFFFF, alpha));
			Render2D.circle(g, kx, trackY + 1.3f, 1.3f, Theme.alpha(Theme.accent1(), alpha));
			if (interactive) regions.add(new Region(x - 4, y, w + 8, h, (btn, mxx, myy) -> {
				dragging = n;
				dragX = x;
				dragW = w;
				updateDrag(mxx);
			}));
		} else if (s instanceof EnumSetting<?> en) {
			String val = en.get().toString();
			float vw = HdFont.SEMIBOLD.width(val, 6f);
			float pillW = vw + 22, pillH = 11;
			float pillX = x + w - pillW, pillY = y + (h - pillH) / 2f;
			Render2D.round(g, pillX, pillY, pillW, pillH, 5.5f, Theme.alpha(Theme.lerp(0x149CC4FF, Theme.accent1(), 0.18f + 0.22f * hv), alpha));
			Render2D.outline(g, pillX, pillY, pillW, pillH, 5.5f, Theme.alpha(Theme.lerp(0x229CC4FF, Theme.accent1(), hv), alpha));
			Render2D.textMid(g, HdFont.SEMIBOLD, "‹", pillX + 4, pillY, pillH, 6.6f, Theme.alpha(Theme.MUTED, alpha));
			Render2D.textMid(g, HdFont.SEMIBOLD, "›", pillX + pillW - 7.5f, pillY, pillH, 6.6f, Theme.alpha(Theme.MUTED, alpha));
			Render2D.textMid(g, HdFont.SEMIBOLD, val, pillX + 11, pillY, pillH, 6f, Theme.alpha(Theme.TEXT, alpha));
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
