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

/** Every find (stash, base, farm, build, portal...) saved per server in config/demon/finds/. */
public final class Finds {
	private Finds() {}

	public static final class Find {
		public String kind;
		public int color;
		public int x, y, z;
		public String dim;
		public int score;
		public String summary;
		public long time;

		public Find(String kind, int color, int x, int y, int z, String dim, int score, String summary) {
			this.kind = kind;
			this.color = color;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dim = dim;
			this.score = score;
			this.summary = summary;
			this.time = System.currentTimeMillis();
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<Find> LIST = new ArrayList<>();
	private static String loadedKey;

	private static String key() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (mc.getSingleplayerServer() != null) return "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName().replaceAll("[^a-zA-Z0-9._-]", "_");
		return "unknown";
	}

	private static Path file(String key) {
		Path dir = Config.dir().resolve("finds");
		try {
			Files.createDirectories(dir);
		} catch (Exception ignored) {
		}
		return dir.resolve(key + ".json");
	}

	public static List<Find> all() {
		String k = key();
		if (!k.equals(loadedKey)) {
			loadedKey = k;
			LIST.clear();
			try {
				Path f = file(k);
				if (Files.exists(f)) {
					List<Find> l = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<List<Find>>() {}.getType());
					if (l != null) LIST.addAll(l);
				}
			} catch (Exception e) {
				DemonClient.LOG.warn("Could not load finds", e);
			}
		}
		return LIST;
	}

	public static List<Find> here() {
		String d = Waypoints.dimension();
		List<Find> out = new ArrayList<>();
		for (Find f : all()) if (d.equals(f.dim)) out.add(f);
		return out;
	}

	public static void add(String kind, int color, int x, int y, int z, int score, String summary) {
		String dim = Waypoints.dimension();
		for (Find f : all()) {
			if (f.dim.equals(dim) && f.kind.equals(kind) && Math.abs(f.x - x) < 16 && Math.abs(f.z - z) < 16) {
				f.score = Math.max(f.score, score);
				f.summary = summary;
				save();
				return;
			}
		}
		LIST.add(new Find(kind, color, x, y, z, dim, score, summary));
		while (LIST.size() > 2000) LIST.remove(0);
		save();
	}

	public static void remove(Find f) {
		LIST.remove(f);
		save();
	}

	public static void save() {
		if (loadedKey == null) return;
		try {
			Files.writeString(file(loadedKey), GSON.toJson(LIST), StandardCharsets.UTF_8);
		} catch (Exception e) {
			DemonClient.LOG.warn("Could not save finds", e);
		}
	}
}
