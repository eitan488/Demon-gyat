package com.demon.client.module.modules;

import com.demon.client.DemonClient;
import com.demon.client.config.Config;
import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.render.Render2D;
import com.demon.client.render.Render3D;
import com.demon.client.scan.ChunkData;
import com.demon.client.scan.ChunkScanner;
import com.demon.client.scan.Target;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Sounds;
import com.demon.client.util.Theme;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Finds suspicious chunks (stashes, bases, redstone farms) by scoring the blocks and entities inside them,
 * then highlights them through walls with an info card listing what is inside.
 */
public class ChunkFinderModule extends Module {
	public enum Mode {
		ALL("Everything"), STASH("Stash / Base"), FARMS("Redstone / Farms");

		final String title;

		Mode(String t) {
			title = t;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public enum Style {
		GLOW("Glow Walls"), BOX("Box"), FLAT("Flat");

		final String title;

		Style(String t) {
			title = t;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public enum Kind {
		STASH("STASH", 0xFFFF4FD8), BASE("BASE", 0xFFFFB547), FARM("FARM", 0xFF22D3EE), MIXED("BASE + FARM", 0xFFA78BFA);

		public final String title;
		public final int color;

		Kind(String title, int color) {
			this.title = title;
			this.color = color;
		}
	}

	public final EnumSetting<Mode> mode = add(new EnumSetting<>("Find", "Which kind of chunks to show", Mode.ALL));
	public final NumberSetting stashThreshold = add(new NumberSetting("Stash Sensitivity", "Points needed to flag a stash/base (lower = more results)", 20, 2, 150, 1, " pts"));
	public final NumberSetting farmThreshold = add(new NumberSetting("Farm Sensitivity", "Points needed to flag a redstone farm (lower = more results)", 30, 2, 150, 1, " pts"));
	public final EnumSetting<Style> style = add(new EnumSetting<>("Style", "How sus chunks are highlighted", Style.GLOW));
	public final BoolSetting showBlocks = add(new BoolSetting("Block ESP", "Outline every interesting block inside sus chunks", true));
	public final BoolSetting cards = add(new BoolSetting("Info Cards", "Floating card showing what is in each chunk", true));
	public final NumberSetting cardLines = add(new NumberSetting("Card Lines", "Max block types listed per card", 5, 1, 12, 1));
	public final BoolSetting beams = add(new BoolSetting("Beams", "Sky beam above each chunk, visible from far away", true));
	public final BoolSetting tracers = add(new BoolSetting("Tracers", "Lines pointing to sus chunks", false));
	public final BoolSetting hudList = add(new BoolSetting("HUD List", "Panel listing the nearest sus chunks", true));
	public final BoolSetting notify = add(new BoolSetting("Notify", "Toast + sound when a new sus chunk is found", true));
	public final BoolSetting chat = add(new BoolSetting("Chat Log", "Print finds with coordinates in chat", true));
	public final BoolSetting saveLog = add(new BoolSetting("Save Log", "Append finds to config/demon/found-chunks.txt", true));

	private final List<ChunkData> sus = new ArrayList<>();
	private int timer;

	public ChunkFinderModule() {
		super("Chunk Finder", "Finds stashes, bases and redstone farms and shows what is inside", Category.HUNTING, -1, "chunk");
	}

	public boolean isStash(ChunkData d) {
		return d.stashScore >= stashThreshold.get();
	}

	public boolean isFarm(ChunkData d) {
		return d.farmScore >= farmThreshold.get();
	}

	public boolean isSus(ChunkData d) {
		return switch (mode.get()) {
			case ALL -> isStash(d) || isFarm(d);
			case STASH -> isStash(d);
			case FARMS -> isFarm(d);
		};
	}

	public Kind kind(ChunkData d) {
		boolean stash = isStash(d) && mode.get() != Mode.FARMS;
		boolean farm = isFarm(d) && mode.get() != Mode.STASH;
		if (stash && farm) return Kind.MIXED;
		if (farm) return Kind.FARM;
		boolean hasLoot = d.count(Target.SHULKER) > 0 || d.count(Target.ENDER_CHEST) > 0 || d.count(Target.CHESTED_ANIMAL) > 0;
		return hasLoot ? Kind.STASH : Kind.BASE;
	}

	private double score(ChunkData d) {
		return switch (mode.get()) {
			case STASH -> d.stashScore;
			case FARMS -> d.farmScore;
			case ALL -> Math.max(d.stashScore, d.farmScore);
		};
	}

	@Override
	protected void onEnable() {
		sus.clear();
		timer = 0;
	}

	@Override
	public String info() {
		return String.valueOf(sus.size());
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		if (timer++ % 5 != 0) return;
		sus.clear();
		double px = mc.player.getX(), pz = mc.player.getZ();
		for (ChunkData d : ChunkScanner.chunks()) {
			if (!isSus(d)) continue;
			sus.add(d);
			if (!d.announced) {
				d.announced = true;
				announce(d);
			}
		}
		sus.sort((a, b) -> Double.compare(a.distanceSq(px, pz), b.distanceSq(px, pz)));
	}

	private void announce(ChunkData d) {
		Kind kind = kind(d);
		List<Target> top = d.sortedTargets();
		StringBuilder summary = new StringBuilder();
		for (int i = 0; i < Math.min(3, top.size()); i++) {
			if (i > 0) summary.append(", ");
			summary.append(d.count(top.get(i))).append(' ').append(top.get(i).title);
		}
		int bx = (int) d.centerX(), bz = (int) d.centerZ();
		if (notify.on() && ModuleManager.hud.notifications.on()) {
			Notifications.push(kind.title + " found", summary + " • " + bx + ", " + bz, kind.color, 4f);
			Sounds.notify(true);
		}
		if (chat.on() && mc.player != null) {
			MutableComponent msg = Component.literal("[Demon] ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal(kind.title).withColor(kind.color & 0xFFFFFF))
				.append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(bx + ", " + bz).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" (" + (int) score(d) + " pts) ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.literal(summary.toString()).withStyle(ChatFormatting.GRAY));
			mc.player.displayClientMessage(msg, false);
		}
		if (saveLog.on()) writeLog(d, kind, summary.toString());
	}

	private void writeLog(ChunkData d, Kind kind, String summary) {
		try {
			ServerData server = mc.getCurrentServer();
			String where = server != null ? server.ip : "singleplayer";
			String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "?";
			String line = String.format("[%s] %s | %s | chunk %d,%d (X %d Z %d) | %s %d pts | %s%n",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), where, dim, d.x, d.z,
				(int) d.centerX(), (int) d.centerZ(), kind.title, (int) score(d), summary);
			Path file = Config.dir().resolve("found-chunks.txt");
			Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception e) {
			DemonClient.LOG.warn("Could not write chunk log", e);
		}
	}

	public List<ChunkData> susChunks() {
		return sus;
	}

	// ------------------------------------------------------------------ world rendering

	@Override
	public void onRender3D(Render3D r) {
		float time = Anim.time();
		int n = 0;
		for (ChunkData d : sus) {
			if (n++ > 96) break;
			Kind kind = kind(d);
			float a = d.appear.target(1).get();
			float pulse = 0.8f + 0.2f * (float) Math.sin(time * 2.6 + d.x * 0.7 + d.z * 1.3);
			int color = kind.color;
			double x1 = d.x << 4, z1 = d.z << 4, x2 = x1 + 16, z2 = z1 + 16;
			double y1 = d.minY - 0.5, y2 = Math.max(d.maxY + 1.5, y1 + 4);
			int edge = Theme.withAlpha(color, (int) (230 * a * pulse));
			switch (style.get()) {
				case GLOW -> {
					int bottom = Theme.withAlpha(color, (int) (95 * a * pulse));
					int top = Theme.withAlpha(color, 0);
					r.wall(x1, z1, x2, z1, y1, y2, bottom, top);
					r.wall(x2, z1, x2, z2, y1, y2, bottom, top);
					r.wall(x2, z2, x1, z2, y1, y2, bottom, top);
					r.wall(x1, z2, x1, z1, y1, y2, bottom, top);
					r.plane(x1, z1, x2, z2, y1, Theme.withAlpha(color, (int) (40 * a)));
					square(r, x1, z1, x2, z2, y1, edge, 2.5f);
				}
				case BOX -> {
					AABB box = new AABB(x1, y1, z1, x2, y2, z2);
					r.filledBox(box, Theme.withAlpha(color, (int) (35 * a)));
					r.outlineBox(box, edge, 2.5f);
				}
				case FLAT -> {
					r.plane(x1, z1, x2, z2, y1, Theme.withAlpha(color, (int) (70 * a * pulse)));
					square(r, x1, z1, x2, z2, y1, edge, 2.5f);
				}
			}
			if (beams.on()) {
				double cx = x1 + 8, cz = z1 + 8;
				r.line(cx, y2, cz, cx, y2 + 120, Theme.withAlpha(color, (int) (220 * a)), Theme.withAlpha(color, 0), 4f);
				r.line(cx, y1, cz, cx, y2, cz, Theme.withAlpha(color, (int) (160 * a)), 2f);
			}
			if (tracers.on()) r.tracer(new Vec3(x1 + 8, y1 + 1, z1 + 8), color, 2f);
			if (showBlocks.on()) {
				for (int i = 0; i < d.hits(); i++) {
					Target t = d.kind(i);
					long p = d.pos(i);
					AABB box = new AABB(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p), BlockPos.getX(p) + 1, BlockPos.getY(p) + 1, BlockPos.getZ(p) + 1).inflate(0.002);
					r.filledBox(box, Theme.withAlpha(t.color, (int) (40 * a)));
					r.outlineBox(box, Theme.withAlpha(t.color, (int) (220 * a)), 1.8f);
				}
			}
		}
	}

	private static void square(Render3D r, double x1, double z1, double x2, double z2, double y, int color, float w) {
		r.line(x1, y, z1, x2, y, z1, color, w);
		r.line(x2, y, z1, x2, y, z2, color, w);
		r.line(x2, y, z2, x1, y, z2, color, w);
		r.line(x1, y, z2, x1, y, z1, color, w);
	}

	// ------------------------------------------------------------------ HUD

	@Override
	public void onRenderHud(GuiGraphics g, DeltaTracker delta) {
		if (mc.player == null) return;
		if (cards.on()) drawCards(g);
		if (hudList.on()) drawList(g);
	}

	private record Card(ChunkData data, float x, float y, float scale, int dist, boolean compact, float w, float h) {}

	private void drawCards(GuiGraphics g) {
		int sw = g.guiWidth(), sh = g.guiHeight();
		Vec3 cam = Render3D.cameraPos();
		float pixel = pixelScale();
		List<ChunkData> nearFirst = new ArrayList<>(sus.subList(0, Math.min(sus.size(), 32)));
		nearFirst.sort((a, b) -> Double.compare(a.distanceSq(cam.x, cam.z), b.distanceSq(cam.x, cam.z)));
		List<Card> placed = new ArrayList<>();
		for (ChunkData d : nearFirst) {
			Vec3 anchor = new Vec3(d.centerX(), Math.max(d.maxY + 3.0, d.minY + 5.0), d.centerZ());
			float[] s = Render3D.toScreen(anchor, sw, sh);
			if (s == null || s[0] < -100 || s[0] > sw + 100 || s[1] < -100 || s[1] > sh + 150) continue;
			int dist = (int) Math.sqrt(anchor.distanceToSqr(cam));
			boolean compact = dist > 112;
			float scale = pixel;
			float w = CARD_W * scale;
			float h = cardHeight(d, compact) * scale;
			// card sits above the anchor; push it up while it overlaps a nearer card
			float top = s[1] - h - 6 * scale;
			for (int iter = 0; iter < 8; iter++) {
				boolean moved = false;
				for (Card c : placed) {
					float cx0 = c.x - c.w / 2f, cy0 = c.y;
					if (s[0] - w / 2f < cx0 + c.w && s[0] + w / 2f > cx0 && top < cy0 + c.h && top + h > cy0) {
						top = cy0 - h - 3;
						moved = true;
					}
				}
				if (!moved) break;
			}
			placed.add(new Card(d, s[0], top, scale, dist, compact, w, h));
		}
		// draw far cards first so the nearest end up on top
		for (int i = placed.size() - 1; i >= 0; i--) {
			Card c = placed.get(i);
			float[] s = Render3D.toScreen(new Vec3(c.data.centerX(), Math.max(c.data.maxY + 3.0, c.data.minY + 5.0), c.data.centerZ()), sw, sh);
			drawCard(g, c, s == null ? c.y + c.h : s[1]);
		}
	}

	private static final int CARD_W = 124;

	/** Scale that maps one font pixel to exactly two screen pixels so card text stays crisp. */
	private float pixelScale() {
		int gui = (int) Math.round(mc.getWindow().getGuiScale());
		return gui <= 1 ? 1f : 2f / gui;
	}

	private int cardHeight(ChunkData d, boolean compact) {
		if (compact) return 30;
		int lines = Math.min(cardLines.asInt(), d.sortedTargets().size());
		return 36 + lines * 10 + (d.sortedTargets().size() > lines ? 9 : 2);
	}

	private void drawCard(GuiGraphics g, Card card, float anchorY) {
		ChunkData d = card.data;
		Kind kind = kind(d);
		float a = d.appear.peek();
		List<Target> targets = d.sortedTargets();
		int lines = card.compact ? 0 : Math.min(cardLines.asInt(), targets.size());
		int w = CARD_W;
		int h = cardHeight(d, card.compact);
		// pointer line from the card down to the chunk
		Render2D.line(g, card.x, card.y + card.h, card.x, anchorY, Theme.alpha(kind.color, 0.55f * a));
		Render2D.circle(g, card.x, anchorY, 2.5f, Theme.alpha(kind.color, a));
		g.pose().pushMatrix();
		g.pose().translate(card.x, card.y);
		g.pose().scale(card.scale, card.scale);
		float x = -w / 2f, y = 0;
		Render2D.shadow(g, x, y, w, h, Theme.alpha(kind.color, 0.30f * a));
		Render2D.round(g, x, y, w, h, 6, Theme.alpha(0xEA0D0A14, a));
		Render2D.outline(g, x, y, w, h, 6, Theme.alpha(kind.color, 0.45f * a));
		// header
		Render2D.round(g, x + 6, y + 5, Render2D.width(kind.title) + 10, 11, 3, Theme.alpha(kind.color, 0.25f * a));
		Render2D.text(g, kind.title, x + 11, y + 7, Theme.alpha(kind.color, a));
		Render2D.textRight(g, (int) score(d) + " pts", x + w - 7, y + 7, Theme.alpha(Theme.TEXT, a));
		String where = card.dist + "m  \u00B7  " + (int) d.centerX() + ", " + (int) d.centerZ();
		Render2D.text(g, where, x + 7, y + 19, Theme.alpha(Theme.SUBTEXT, a));
		if (card.compact) {
			g.pose().popMatrix();
			return;
		}
		// composition bar
		int total = Math.max(1, d.total());
		float bx = x + 7, bw = w - 14, cx = bx;
		for (Target t : targets) {
			float seg = bw * d.count(t) / total;
			Render2D.rect(g, Math.round(cx), (int) (y + 30), Math.max(1, Math.round(cx + seg) - Math.round(cx)), 2, Theme.alpha(t.color, a));
			cx += seg;
		}
		float ry = y + 36;
		for (int i = 0; i < lines; i++) {
			Target t = targets.get(i);
			Render2D.round(g, x + 7, ry + 1.5f, 5, 5, 2, Theme.alpha(t.color, a));
			Render2D.text(g, Render2D.trim(t.title, 80), x + 16, ry, Theme.alpha(Theme.TEXT, 0.92f * a));
			Render2D.textRight(g, "x" + d.count(t), x + w - 7, ry, Theme.alpha(t.color, a));
			ry += 10;
		}
		if (targets.size() > lines) {
			Render2D.textRight(g, "+" + (targets.size() - lines) + " more", x + w - 7, ry, Theme.alpha(Theme.MUTED, a));
		}
		g.pose().popMatrix();
	}

	private void drawList(GuiGraphics g) {
		int sw = g.guiWidth();
		int rows = Math.min(5, sus.size());
		int w = 150, rowH = 12;
		int h = 20 + Math.max(1, rows) * rowH + 4;
		int x = sw - w - 6;
		int y = 6 + listOffset();
		Render2D.shadow(g, x, y, w, h, 0x40000000);
		Render2D.round(g, x, y, w, h, 6, 0xD80E0B16);
		Render2D.round(g, x, y + 5, 2, 9, 1, Theme.wave(0.6f));
		Render2D.text(g, "SUS CHUNKS", x + 7, y + 6, Theme.TEXT);
		String count = String.valueOf(sus.size());
		Render2D.round(g, x + w - 12 - Render2D.width(count), y + 4, Render2D.width(count) + 8, 11, 3, Theme.alpha(Theme.accent1(), 0.3f));
		Render2D.text(g, count, x + w - 8 - Render2D.width(count), y + 6, Theme.TEXT);
		int ry = y + 20;
		if (rows == 0) {
			String scanning = ChunkScanner.queued() > 0 ? "Scanning… " + ChunkScanner.queued() + " left" : "Nothing found yet";
			Render2D.text(g, scanning, x + 7, ry + 1, Theme.MUTED);
			return;
		}
		float yaw = mc.player.getYRot();
		for (int i = 0; i < rows; i++) {
			ChunkData d = sus.get(i);
			Kind kind = kind(d);
			double dx = d.centerX() - mc.player.getX(), dz = d.centerZ() - mc.player.getZ();
			int dist = (int) Math.sqrt(dx * dx + dz * dz);
			Render2D.circle(g, x + 9, ry + 4.5f, 2.5f, kind.color);
			Render2D.text(g, (int) d.centerX() + ", " + (int) d.centerZ(), x + 16, ry + 1, Theme.TEXT);
			String right = dist + "m " + arrow(dx, dz, yaw);
			Render2D.textRight(g, right, x + w - 7, ry + 1, kind.color);
			ry += rowH;
		}
	}

	/** Vertical offset so the list sits below the HUD module list. */
	private int listOffset() {
		if (!ModuleManager.hud.isEnabled() || !ModuleManager.hud.moduleList.on()) return 0;
		float total = 0;
		for (Module m : ModuleManager.all()) {
			if (m.toggleable() && m != ModuleManager.hud) total += 15 * m.arrayAnim.peek();
		}
		return (int) total + 4;
	}

	private static String arrow(double dx, double dz, float yaw) {
		double angle = Math.toDegrees(Math.atan2(-dx, dz)) - yaw;
		angle = ((angle % 360) + 360 + 22.5) % 360;
		String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
		return arrows[(int) (angle / 45) % 8];
	}
}
