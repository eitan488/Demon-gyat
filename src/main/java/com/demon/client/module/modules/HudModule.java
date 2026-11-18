package com.demon.client.module.modules;

import com.demon.client.DemonClient;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
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

	public HudModule() {
		super("HUD", "Watermark, module list, coordinates and toasts", Category.CLIENT, -1, "hud");
	}

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		if (watermark.on()) drawWatermark(g);
		if (moduleList.on()) drawModuleList(g);
		if (coords.on()) drawCoords(g);
	}

	private void drawWatermark(GuiGraphics g) {
		int x = 6, y = 6, h = 18;
		String fps = mc.getFps() + " fps";
		String ping = null;
		if (mc.getConnection() != null && !mc.isSingleplayer()) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) ping = info.getLatency() + "ms";
		}
		String right = ping == null ? fps : fps + "  ·  " + ping;
		float logoW = 7 * Render2D.LOGO_SMALL_ASPECT * 1.5f - 7 * 0.5f; // approx cap width
		int w = (int) (6 + 12 + 5 + logoW + 10 + Render2D.width(right) + 8);
		Render2D.shadow(g, x, y, w, h, 0x50000000);
		Render2D.round(g, x, y, w, h, 6, 0xD80E0B16);
		Render2D.outline(g, x, y, w, h, 6, Theme.BORDER);
		Render2D.sprite(g, Render2D.LOGO_ICON, x + 4, y + 3, 12, 12, 0xFFFFFFFF);
		float lw = Render2D.logoSmall(g, x + 21, y + 5.5f, 7, 1f);
		int tx = (int) (x + 21 + lw + 9);
		Render2D.rect(g, tx - 4, y + 5, 1, 8, 0x30FFFFFF);
		Render2D.text(g, right, tx + 1, y + 5, Theme.SUBTEXT);
	}

	private void drawModuleList(GuiGraphics g) {
		int screenW = g.guiWidth();
		List<Module> list = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (!m.toggleable() || m == this) continue;
			m.arrayAnim.target(m.isEnabled() ? 1 : 0);
			if (m.arrayAnim.get() > 0.01f) list.add(m);
		}
		list.sort((a, b) -> Integer.compare(width(b), width(a)));
		float y = 6;
		int i = 0;
		for (Module m : list) {
			float a = m.arrayAnim.get();
			int w = width(m) + 10;
			int h = 13;
			float x = screenW - 6 - w * a;
			int color = Theme.wave(i * 0.18f);
			Render2D.round(g, x, y, w, h, 3, Theme.alpha(0xD80E0B16, a));
			Render2D.round(g, x + w - 2.5f, y + 2, 2.5f, h - 4, 1, Theme.alpha(color, a));
			Render2D.text(g, m.name, x + 4, y + 3, Theme.alpha(color, a), false);
			String info = m.info();
			if (info != null) Render2D.text(g, info, x + 4 + Render2D.width(m.name) + 4, y + 3, Theme.alpha(Theme.SUBTEXT, a), false);
			y += (h + 2) * a;
			i++;
		}
	}

	private static int width(Module m) {
		String info = m.info();
		return Render2D.width(m.name) + (info != null ? 4 + Render2D.width(info) : 0);
	}

	private void drawCoords(GuiGraphics g) {
		int sh = g.guiHeight();
		double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
		boolean nether = mc.level != null && mc.level.dimension() == Level.NETHER;
		String main = String.format("%d %d %d", (int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
		String other = nether ? String.format("%d %d", (int) Math.floor(px * 8), (int) Math.floor(pz * 8))
			: String.format("%d %d", (int) Math.floor(px / 8), (int) Math.floor(pz / 8));
		String facing = mc.player.getDirection().getName().toUpperCase();
		int y = sh - (mc.gui.getChat().isChatFocused() ? 30 : 16);
		int x = 6;
		String l1 = "XYZ";
		String l2 = nether ? "OW" : "NETHER";
		int w = 8 + Render2D.width(l1) + 4 + Render2D.width(main) + 10 + Render2D.width(l2) + 4 + Render2D.width(other) + 10 + Render2D.width(facing) + 8;
		Render2D.round(g, x, y, w, 12, 3, 0xC00E0B16);
		int cx = x + 6;
		Render2D.text(g, l1, cx, y + 2, Theme.accent1());
		cx += Render2D.width(l1) + 4;
		Render2D.text(g, main, cx, y + 2, Theme.TEXT);
		cx += Render2D.width(main) + 10;
		Render2D.text(g, l2, cx, y + 2, nether ? 0xFF4ADE80 : 0xFFF87171);
		cx += Render2D.width(l2) + 4;
		Render2D.text(g, other, cx, y + 2, Theme.SUBTEXT);
		cx += Render2D.width(other) + 10;
		Render2D.text(g, facing, cx, y + 2, Theme.accent2());
	}

}
