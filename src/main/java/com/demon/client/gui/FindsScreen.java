package com.demon.client.gui;

import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.util.Anim;
import com.demon.client.util.Finds;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import com.demon.client.util.Waypoints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** HD browser for every find on this server: filter, sort, waypoint, copy, delete. */
public class FindsScreen extends Screen {
	private final Screen parent;
	private final float openStart = Anim.time();
	private String filter = "ALL";
	private boolean byDistance = true;
	private float scroll, scrollTarget, maxScroll;
	private final Map<Object, Anim.Value> hover = new HashMap<>();
	private final List<Region> regions = new ArrayList<>();

	private record Region(float x, float y, float w, float h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public FindsScreen(Screen parent) {
		super(Component.literal("Finds"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		float a = Anim.easeOutCubic(Anim.progress(Anim.time(), openStart, openStart + 0.3f));
		if (minecraft.level == null) MenuFx.background(g, width, height, 1f);
		else if (ModuleManager.ui.blur.on()) renderBlurredBackground(g);
		Render2D.vGradient(g, 0, 0, width, height, Theme.alpha(0x99020812, a), Theme.alpha(0xCC01040A, a));
	}

	private List<Finds.Find> list() {
		List<Finds.Find> out = new ArrayList<>();
		for (Finds.Find f : Finds.all()) if (filter.equals("ALL") || filter.equals(f.kind)) out.add(f);
		String dim = Waypoints.dimension();
		if (byDistance && minecraft.player != null) {
			out.sort((a, b) -> Double.compare(dist(a, dim), dist(b, dim)));
		} else {
			out.sort((a, b) -> Long.compare(b.time, a.time));
		}
		return out;
	}

	private double dist(Finds.Find f, String dim) {
		if (minecraft.player == null || !dim.equals(f.dim)) return Double.MAX_VALUE;
		double dx = f.x - minecraft.player.getX(), dz = f.z - minecraft.player.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	@Override
	public void render(GuiGraphics g, int mxi, int myi, float delta) {
		regions.clear();
		float t = Anim.time();
		float a = Anim.easeOutCubic(Anim.progress(t, openStart, openStart + 0.3f));
		float pop = Anim.lerp(0.94f, 1f, Anim.easeOutBack(Anim.progress(t, openStart, openStart + 0.4f)));
		float pw = Math.min(500, width - 24), ph = Math.min(300, height - 24);
		float px = Math.round((width - pw) / 2f), py = Math.round((height - ph) / 2f);
		g.pose().pushMatrix();
		g.pose().translate(width / 2f, height / 2f);
		g.pose().scale(pop, pop);
		g.pose().translate(-width / 2f, -height / 2f);
		double mx = width / 2f + (mxi - width / 2f) / pop, my = height / 2f + (myi - height / 2f) / pop;

		Render2D.shadow(g, px - 8, py - 8, pw + 16, ph + 16, Theme.alpha(Theme.accent1(), 0.25f * a));
		Ui.glass(g, px, py, pw, ph, 10, a);
		Render2D.hGradient(g, px + 40, py, pw - 80, 0.5f, Theme.alpha(Theme.accent1(), 0), Theme.alpha(Theme.accent2(), 0.9f * a));

		// header
		Render2D.roundGradient(g, px + 12, py + 11, 22, 22, 6, Theme.alpha(Theme.accent1(), a), Theme.alpha(Theme.accent2(), a));
		Render2D.icon(g, "icon_flag", px + 23, py + 22, 11, Theme.alpha(0xFFFFFFFF, a));
		Render2D.text(g, HdFont.SEMIBOLD, "Finds", px + 41, py + 12, 11f, Theme.alpha(Theme.TEXT, a));
		String server = minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().ip : "Singleplayer";
		Render2D.text(g, HdFont.MEDIUM, Finds.all().size() + " saved  ·  " + server, px + 41, py + 26, 5.6f, Theme.alpha(Theme.MUTED, a));
		String sort = byDistance ? "Nearest first" : "Newest first";
		float sw = HdFont.SEMIBOLD.width(sort, 6f) + 16;
		float sx = px + pw - 12 - sw, sy = py + 14;
		boolean sh = mx >= sx && mx < sx + sw && my >= sy && my < sy + 14;
		Render2D.round(g, sx, sy, sw, 14, 7, Theme.alpha(sh ? 0x2A8CB8FF : 0x149CC4FF, a));
		Render2D.outline(g, sx, sy, sw, 14, 7, Theme.alpha(0x2A8CB8FF, a));
		Render2D.textMid(g, HdFont.SEMIBOLD, sort, sx + 8, sy, 14, 6f, Theme.alpha(Theme.TEXT, a));
		regions.add(new Region(sx, sy, sw, 14, () -> {
			byDistance = !byDistance;
			scrollTarget = 0;
			Sounds.tab();
		}));

		// filter chips with counts
		Map<String, Integer> counts = new LinkedHashMap<>();
		counts.put("ALL", Finds.all().size());
		for (Finds.Find f : Finds.all()) counts.merge(f.kind, 1, Integer::sum);
		float cx = px + 12, cy = py + 40;
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			String label = (e.getKey().equals("ALL") ? "All" : e.getKey()) + "  " + e.getValue();
			float w = HdFont.SEMIBOLD.width(label, 5.6f) + 14;
			if (cx + w > px + pw - 12) break;
			boolean sel = filter.equals(e.getKey());
			float hv = anim("chip" + e.getKey()).target(sel || mx >= cx && mx < cx + w && my >= cy && my < cy + 13 ? 1 : 0).get();
			if (sel) Render2D.roundGradient(g, cx, cy, w, 13, 6.5f, Theme.alpha(Theme.accent1(), 0.75f * a), Theme.alpha(Theme.accent2(), 0.55f * a));
			else Render2D.round(g, cx, cy, w, 13, 6.5f, Theme.alpha(Theme.lerp(0x0E9CC4FF, 0x229CC4FF, hv), a));
			Render2D.outline(g, cx, cy, w, 13, 6.5f, Theme.alpha(0x229CC4FF, a));
			Render2D.textMid(g, HdFont.SEMIBOLD, label, cx + 7, cy, 13, 5.6f, Theme.alpha(sel ? 0xFFFFFFFF : Theme.SUBTEXT, a));
			final String key = e.getKey();
			regions.add(new Region(cx, cy, w, 13, () -> {
				filter = key;
				scrollTarget = 0;
				Sounds.tab();
			}));
			cx += w + 4;
		}

		// list
		float top = py + 60, bottom = py + ph - 10, rowH = 27;
		List<Finds.Find> rows = list();
		maxScroll = Math.max(0, rows.size() * (rowH + 3) - (bottom - top));
		scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
		scroll += (scrollTarget - scroll) * 0.3f;
		g.enableScissor((int) px + 6, (int) top, (int) (px + pw - 6), (int) bottom);
		if (rows.isEmpty()) {
			Render2D.textCentered(g, HdFont.SEMIBOLD, "No finds yet", px + pw / 2f, top + 30, 8f, Theme.alpha(Theme.SUBTEXT, a));
			Render2D.textCentered(g, HdFont.REGULAR, "Turn on Chunk Finder, Build Finder or Mob Finder and explore", px + pw / 2f, top + 44, 6f, Theme.alpha(Theme.MUTED, a));
		}
		String dim = Waypoints.dimension();
		float y = top - scroll;
		for (Finds.Find f : rows) {
			if (y + rowH >= top && y <= bottom) drawRow(g, f, px + 12, y, pw - 24, rowH, mx, my, a, dim, my >= top && my < bottom);
			y += rowH + 3;
		}
		g.disableScissor();
		if (maxScroll > 0) {
			float area = bottom - top, barH = Math.max(18, area * area / (area + maxScroll));
			Render2D.capsule(g, px + pw - 7, top + (area - barH) * (scroll / maxScroll), 1.8f, barH, Theme.alpha(Theme.accent1(), 0.7f * a));
		}
		g.pose().popMatrix();
	}

	private Anim.Value anim(Object key) {
		return hover.computeIfAbsent(key, k -> new Anim.Value(0, 16));
	}

	private void drawRow(GuiGraphics g, Finds.Find f, float x, float y, float w, float h, double mx, double my, float a, String dim, boolean interactive) {
		boolean hovered = interactive && mx >= x && mx < x + w && my >= y && my < y + h;
		float hv = anim(f).target(hovered ? 1 : 0).get();
		Render2D.round(g, x, y, w, h, 6, Theme.alpha(Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hv), a));
		Render2D.outline(g, x, y, w, h, 6, Theme.alpha(Theme.lerp(0x1A9CC4FF, f.color, 0.3f + 0.4f * hv), a));
		Render2D.capsule(g, x + 2, y + 6, 2, h - 12, Theme.alpha(f.color, a));
		float bw = Ui.badge(g, f.kind, x + 9, y + 5, 5.2f, Theme.alpha(f.color, 0.25f * a), Theme.alpha(Theme.brighten(f.color, 0.35f), a));
		Render2D.text(g, HdFont.SEMIBOLD, f.x + ", " + f.z, x + 13 + bw, y + 6.5f, 6.6f, Theme.alpha(Theme.TEXT, a));
		Render2D.text(g, HdFont.REGULAR, Render2D.trim(HdFont.REGULAR, f.summary == null ? "" : f.summary, 5.6f, w - 210), x + 9, y + 17, 5.6f, Theme.alpha(Theme.SUBTEXT, a));

		// distance + direction (same dimension only) and age
		String info;
		if (minecraft.player != null && dim.equals(f.dim)) {
			double dx = f.x - minecraft.player.getX(), dz = f.z - minecraft.player.getZ();
			double d = Math.sqrt(dx * dx + dz * dz);
			float rel = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - minecraft.player.getYRot());
			String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
			info = (d >= 1000 ? String.format("%.1fkm", d / 1000) : (int) d + "m") + " " + arrows[(int) (((rel + 360 + 22.5) % 360) / 45) % 8];
		} else {
			info = f.dim.replace("minecraft:", "").replace("the_", "");
		}
		long mins = (System.currentTimeMillis() - f.time) / 60000;
		String age = mins < 1 ? "just now" : mins < 60 ? mins + "m ago" : mins < 1440 ? mins / 60 + "h ago" : mins / 1440 + "d ago";
		float bx = x + w - 6;
		bx = button(g, "DELETE", bx, y + 7, a, hovered, () -> {
			Finds.remove(f);
			Sounds.play(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 0.8f, 0.4f);
		}, 0xFFFB7185);
		bx = button(g, "COPY", bx, y + 7, a, hovered, () -> {
			minecraft.keyboardHandler.setClipboard(f.x + " " + f.y + " " + f.z);
			Notifications.push("Copied", f.x + " " + f.y + " " + f.z, Theme.accent2(), 1.5f);
			Sounds.click();
		}, Theme.TEXT);
		bx = button(g, "WAYPOINT", bx, y + 7, a, hovered, () -> {
			if (!dim.equals(f.dim)) return;
			Waypoints.add(f.kind + " " + f.x + "," + f.z, f.x, f.y, f.z, f.color, "find" + (f.x >> 4) + "," + (f.z >> 4));
			if (!ModuleManager.waypoints.isEnabled()) ModuleManager.waypoints.setEnabled(true);
			Notifications.push("Waypoint added", f.kind + " at " + f.x + ", " + f.z, f.color, 1.8f);
			Sounds.click();
		}, Theme.accent2());
		Render2D.textRight(g, HdFont.BOLD, info, bx - 8, y + 6.5f, 6f, Theme.alpha(f.color, a));
		Render2D.textRight(g, HdFont.MEDIUM, age, bx - 8, y + 16.5f, 5.2f, Theme.alpha(Theme.MUTED, a));
	}

	/** Draws a small pill button right-aligned at rightX; returns its left edge. */
	private float button(GuiGraphics g, String label, float rightX, float y, float a, boolean rowHovered, Runnable action, int color) {
		float w = HdFont.SEMIBOLD.width(label, 5.2f) + 12, h = 13;
		float x = rightX - w;
		Render2D.round(g, x, y, w, h, 6.5f, Theme.alpha(rowHovered ? 0x229CC4FF : 0x0E9CC4FF, a));
		Render2D.outline(g, x, y, w, h, 6.5f, Theme.alpha(0x2A8CB8FF, a));
		Render2D.textMid(g, HdFont.SEMIBOLD, label, x + 6, y, h, 5.2f, Theme.alpha(color, a));
		regions.add(new Region(x, y, w, h, action));
		return x - 4;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float pop = Anim.lerp(0.94f, 1f, Anim.easeOutBack(Anim.progress(Anim.time(), openStart, openStart + 0.4f)));
		double mx = width / 2f + (event.x() - width / 2f) / pop, my = height / 2f + (event.y() - height / 2f) / pop;
		for (int i = regions.size() - 1; i >= 0; i--) {
			if (regions.get(i).contains(mx, my)) {
				regions.get(i).action().run();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		scrollTarget = Anim.clamp(scrollTarget - (float) sy * 30, 0, maxScroll);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == ModuleManager.byName("Finds").getKey() && Anim.time() - openStart > 0.25f) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Sounds.close();
		minecraft.setScreen(parent);
	}
}
