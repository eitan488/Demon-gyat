package com.demon.client.module.modules;

import com.demon.client.gui.Ui;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.setting.BoolSetting;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.Level;

/** Watermark, active module list and coordinates. */
public class HudModule extends Module {
	public final BoolSetting watermark = add(new BoolSetting("Watermark", "Logo with FPS and ping in the top-left", true));
	public final BoolSetting moduleList = add(new BoolSetting("Module List", "Animated list of enabled modules", true));
	public final BoolSetting coords = add(new BoolSetting("Coordinates", "Position, nether coords and facing", true));
	public final BoolSetting notifications = add(new BoolSetting("Notifications", "Pop-up toasts for toggles and finds", true));

	/** Bottom edge of the module list, so other right-side panels can stack below it. */
	public float listBottom = 6;

	private static final float SIZE = 6.6f;

	public HudModule() {
		super("HUD", "Watermark, module list, coordinates and toasts", Category.CLIENT, -1, "hud");
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		if (watermark.on()) drawWatermark(g);
		listBottom = 5;
		if (moduleList.on()) drawModuleList(g);
		if (coords.on()) drawCoords(g);
	}

	private void drawWatermark(GuiGraphics g) {
		float x = 5, y = 5, h = 14;
		String fps = mc.getFps() + " FPS";
		String ping = null;
		if (mc.getConnection() != null && !mc.isSingleplayer()) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) ping = info.getLatency() + " ms";
		}
		float cap = 5.4f;
		float logoW = cap / (1 - 2 * Render2D.LOGO_SMALL_PAD) * Render2D.LOGO_SMALL_ASPECT - 2 * Render2D.LOGO_SMALL_PAD * cap / (1 - 2 * Render2D.LOGO_SMALL_PAD);
		float statsW = HdFont.MEDIUM.width(fps, SIZE) + (ping != null ? 9 + HdFont.MEDIUM.width(ping, SIZE) : 0);
		float w = 4 + 10 + 4 + logoW + 7 + statsW + 6;
		Ui.hud(g, x, y, w, h, 1f);
		Render2D.image(g, Render2D.LOGO_ICON, x + 3, y + 2, 10, 10, 0xFFFFFFFF);
		Render2D.logoSmall(g, x + 17, y + (h - cap) / 2f, cap, 1f);
		float tx = x + 17 + logoW + 4;
		Render2D.rect(g, tx, y + 3.5f, 0.35f, h - 7, 0x3A9CC4FF);
		tx += 3.5f;
		Render2D.textMid(g, HdFont.MEDIUM, fps, tx, y, h, SIZE, Theme.SUBTEXT);
		if (ping != null) {
			float px = tx + HdFont.MEDIUM.width(fps, SIZE) + 4;
			Render2D.circle(g, px, y + h / 2f, 0.9f, Theme.MUTED);
			Render2D.textMid(g, HdFont.MEDIUM, ping, px + 4, y, h, SIZE, Theme.SUBTEXT);
		}
	}

	private void drawModuleList(GuiGraphics g) {
		int screenW = g.guiWidth();
		List<Module> list = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (!m.toggleable() || m == this) continue;
			m.arrayAnim.target(m.isEnabled() ? 1 : 0);
			if (m.arrayAnim.get() > 0.01f) list.add(m);
		}
		list.sort((a, b) -> Float.compare(width(b), width(a)));
		float y = 5;
		int i = 0;
		for (Module m : list) {
			float a = m.arrayAnim.get();
			float w = width(m) + 11;
			float h = 11;
			float x = screenW - 5 - w * a;
			int color = Theme.wave(i * 0.18f);
			Render2D.round(g, x, y, w, h, 3, Theme.alpha(Theme.HUD_BG, a));
			Render2D.capsule(g, x + w - 2.6f, y + 2.5f, 1.6f, h - 5, Theme.alpha(color, a));
			Render2D.textMid(g, HdFont.SEMIBOLD, m.name, x + 4.5f, y, h, SIZE, Theme.alpha(Theme.TEXT, a));
			String info = m.info();
			if (info != null) Render2D.textMid(g, HdFont.MEDIUM, info, x + 4.5f + HdFont.SEMIBOLD.width(m.name, SIZE) + 3.5f, y, h, SIZE, Theme.alpha(color, a));
			y += (h + 2) * a;
			i++;
		}
		listBottom = y;
	}

	private static float width(Module m) {
		String info = m.info();
		return HdFont.SEMIBOLD.width(m.name, SIZE) + (info != null ? 3.5f + HdFont.MEDIUM.width(info, SIZE) : 0);
	}

	private void drawCoords(GuiGraphics g) {
		int sh = g.guiHeight();
		double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
		boolean nether = mc.level != null && mc.level.dimension() == Level.NETHER;
		String main = String.format("%d  %d  %d", (int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
		String other = nether ? String.format("%d  %d", (int) Math.floor(px * 8), (int) Math.floor(pz * 8))
			: String.format("%d  %d", (int) Math.floor(px / 8), (int) Math.floor(pz / 8));
		String facing = mc.player.getDirection().getName().toUpperCase();
		String l2 = nether ? "OVERWORLD" : "NETHER";
		float h = 11;
		float y = sh - (mc.gui.getChat().isChatFocused() ? 28 : 16);
		float x = 5;
		float s = 6.2f;
		float w = 6 + HdFont.BOLD.width("XYZ", s) + 4 + HdFont.MEDIUM.width(main, s) + 8 + HdFont.BOLD.width(l2, s) + 4
			+ HdFont.MEDIUM.width(other, s) + 8 + HdFont.SEMIBOLD.width(facing, s) + 6;
		Ui.hud(g, x, y, w, h, 1f);
		float cx = x + 6;
		cx += Render2D.text(g, HdFont.BOLD, "XYZ", cx, y + (h - HdFont.BOLD.capHeight(s)) / 2f, s, Theme.accent1()) + 4;
		cx += Render2D.text(g, HdFont.MEDIUM, main, cx, y + (h - HdFont.MEDIUM.capHeight(s)) / 2f, s, Theme.TEXT) + 8;
		cx += Render2D.text(g, HdFont.BOLD, l2, cx, y + (h - HdFont.BOLD.capHeight(s)) / 2f, s, nether ? 0xFF4ADE80 : 0xFFFB7185) + 4;
		cx += Render2D.text(g, HdFont.MEDIUM, other, cx, y + (h - HdFont.MEDIUM.capHeight(s)) / 2f, s, Theme.SUBTEXT) + 8;
		Render2D.text(g, HdFont.SEMIBOLD, facing, cx, y + (h - HdFont.SEMIBOLD.capHeight(s)) / 2f, s, Theme.accent2());
	}
}
