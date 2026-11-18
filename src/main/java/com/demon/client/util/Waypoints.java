package com.demon.client.util;

import com.demon.client.DemonClient;
import com.demon.client.config.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Waypoints saved per server in config/demon/waypoints/. */
public final class Waypoints {
	private Waypoints() {}

	public static final class Waypoint {
		public String name;
		public int x, y, z;
		public String dim;
		public int color;
		public long created;
		public String kind;

		public Waypoint(String name, int x, int y, int z, String dim, int color, String kind) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dim = dim;
			this.color = color;
			this.kind = kind;
			this.created = System.currentTimeMillis();
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<Waypoint> LIST = new ArrayList<>();
	private static String loadedKey;

	private static String key() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (mc.getSingleplayerServer() != null) return "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName().replaceAll("[^a-zA-Z0-9._-]", "_");
		return "unknown";
	}

	private static Path file(String key) {
		Path dir = Config.dir().resolve("waypoints");
		try {
			Files.createDirectories(dir);
		} catch (Exception ignored) {
		}
		return dir.resolve(key + ".json");
	}

	public static String dimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? "?" : mc.level.dimension().identifier().toString();
	}

	public static List<Waypoint> all() {
		String k = key();
		if (!k.equals(loadedKey)) load(k);
		return LIST;
	}

	public static List<Waypoint> here() {
		String d = dimension();
		List<Waypoint> out = new ArrayList<>();
		for (Waypoint w : all()) if (d.equals(w.dim)) out.add(w);
		return out;
	}

	/**
	 * Waypoints from the linked dimension converted through the nether ratio (8:1), so overworld
	 * bases show up at the right nether coordinates and vice versa. Returned copies are not saved.
	 */
	public static List<Waypoint> linked() {
		String d = dimension();
		boolean nether = d.equals("minecraft:the_nether"), over = d.equals("minecraft:overworld");
		List<Waypoint> out = new ArrayList<>();
		if (!nether && !over) return out;
		String other = nether ? "minecraft:overworld" : "minecraft:the_nether";
		for (Waypoint w : all()) {
			if (!other.equals(w.dim)) continue;
			int x = nether ? Math.floorDiv(w.x, 8) : w.x * 8, z = nether ? Math.floorDiv(w.z, 8) : w.z * 8;
			Waypoint c = new Waypoint(w.name + (nether ? " (OW)" : " (Nether)"), x, nether ? 120 : 64, z, d, w.color, "linked");
			out.add(c);
		}
		return out;
	}

	private static void load(String k) {
		loadedKey = k;
		LIST.clear();
		try {
			Path f = file(k);
			if (Files.exists(f)) {
				List<Waypoint> l = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<List<Waypoint>>() {}.getType());
				if (l != null) LIST.addAll(l);
			}
		} catch (Exception e) {
			DemonClient.LOG.warn("Could not load waypoints", e);
		}
	}

	public static void save() {
		if (loadedKey == null) return;
		try {
			Files.writeString(file(loadedKey), GSON.toJson(LIST), StandardCharsets.UTF_8);
		} catch (Exception e) {
			DemonClient.LOG.warn("Could not save waypoints", e);
		}
	}

	public static Waypoint add(String name, int x, int y, int z, int color, String kind) {
		all();
		for (Waypoint w : LIST) if (w.x == x && w.z == z && w.dim.equals(dimension()) && kind.equals(w.kind)) return w;
		Waypoint w = new Waypoint(name, x, y, z, dimension(), color, kind);
		LIST.add(w);
		while (LIST.size() > 200) LIST.remove(0);
		save();
		return w;
	}

	public static void remove(Waypoint w) {
		LIST.remove(w);
		save();
	}
}
