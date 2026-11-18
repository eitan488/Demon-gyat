package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.render.HdFont;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.scan.Target;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Reads signs through walls and logs new ones to chat (bases are often labelled). */
public class SignReaderModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance", 64, 8, 256, 8, "m"));
	public final BoolSetting chatLog = add(new BoolSetting("Chat Log", "Print new signs in chat", true));

	private record Sign(BlockPos pos, List<String> lines) {}

	private final List<Sign> signs = new ArrayList<>();
	private final Set<String> logged = new HashSet<>();
	private int timer;

	public SignReaderModule() {
		super("Sign Reader", "Shows sign text through walls and logs new signs", Category.HUNTING, -1, "sign");
	}

	@Override
	public void onTick() {
		if (mc.level == null || timer++ % 20 != 0) return;
		signs.clear();
		double r2 = range.get() * range.get();
		for (ChunkData d : ChunkScanner.chunks()) {
			if (d.count(Target.SIGN) == 0) continue;
			for (int i = 0; i < d.hits(); i++) {
				if (d.kind(i) != Target.SIGN) continue;
				BlockPos p = BlockPos.of(d.pos(i));
				if (p.distToCenterSqr(mc.player.position()) > r2) continue;
				if (!(mc.level.getBlockEntity(p) instanceof SignBlockEntity sign)) continue;
				List<String> lines = new ArrayList<>();
				for (int l = 0; l < 4; l++) {
					String s = sign.getFrontText().getMessage(l, false).getString().trim();
					if (!s.isEmpty()) lines.add(s);
				}
				if (lines.isEmpty()) continue;
				signs.add(new Sign(p, lines));
				String key = p.toShortString() + String.join("|", lines);
				if (chatLog.on() && logged.add(key)) {
					mc.player.displayClientMessage(Component.literal("[Sign] ").withColor(0x38BDF8)
						.append(Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ() + ": ").withColor(0x93A6C6))
						.append(Component.literal(String.join(" / ", lines)).withColor(0xFFFFFF)), false);
				}
			}
		}
	}

	@Override
	public String info() {
		return String.valueOf(signs.size());
	}

	@Override
	public void onRenderOverlay(GuiGraphics g, DeltaTracker delta) {
		for (Sign s : signs) {
			float[] p = Render3D.toScreen(Vec3.atCenterOf(s.pos).add(0, 0.8, 0), g.guiWidth(), g.guiHeight());
			if (p == null) continue;
			float size = 5.6f, lh = 7.5f;
			float w = 0;
			for (String l : s.lines) w = Math.max(w, HdFont.MEDIUM.width(l, size));
			w += 10;
			float h = s.lines.size() * lh + 5;
			float x = p[0] - w / 2f, y = p[1] - h;
			Render2D.round(g, x, y, w, h, 3, 0xD9221a10);
			Render2D.outline(g, x, y, w, h, 3, 0x80E7C9A9);
			float ly = y + 3;
			for (String l : s.lines) {
				Render2D.textCentered(g, HdFont.MEDIUM, l, p[0], ly + 0.5f, size, Theme.TEXT);
				ly += lh;
			}
		}
	}
}
