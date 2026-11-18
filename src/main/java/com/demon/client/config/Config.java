package com.demon.client.config;

import com.demon.client.DemonClient;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Saves module state, keybinds and settings to config/demon-client.json. */
public final class Config {
	private Config() {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static boolean dirty;
	private static boolean loading;
	private static int saveTimer;

	public static Path dir() {
		Path p = FabricLoader.getInstance().getConfigDir().resolve("demon");
		try {
			Files.createDirectories(p);
		} catch (Exception ignored) {
		}
		return p;
	}

	private static Path file() {
		return dir().resolve("config.json");
	}

	public static void markDirty() {
		if (!loading) dirty = true;
	}

	/** Called every client tick; saves at most once a second when something changed. */
	public static void tick() {
		if (dirty && ++saveTimer >= 20) {
			saveTimer = 0;
			save();
		}
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		loading = true;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			// 1.0.x configs used the old purple theme by default; the new default style is blue
			String version = root.has("version") ? root.get("version").getAsString() : "1.0.0";
			boolean legacy = version.startsWith("1.0");
			for (Module m : ModuleManager.all()) {
				if (!mods.has(m.name)) continue;
				JsonObject o = mods.getAsJsonObject(m.name);
				if (o.has("key")) m.setKey(o.get("key").getAsInt());
				if (o.has("settings")) {
					JsonObject s = o.getAsJsonObject("settings");
					for (Setting<?> setting : m.settings()) {
						if (legacy && setting.name.equals("Theme")) continue;
						JsonElement e = s.get(setting.name);
						if (e != null) {
							try {
								setting.fromJson(e);
							} catch (Exception ex) {
								DemonClient.LOG.warn("Bad value for {}.{}", m.name, setting.name);
							}
						}
					}
				}
				if (o.has("enabled") && m.persistEnabled()) m.loadEnabled(o.get("enabled").getAsBoolean());
			}
		} catch (Exception e) {
			DemonClient.LOG.error("Failed to load Demon config", e);
		} finally {
			loading = false;
			dirty = false;
		}
	}

	public static void save() {
		dirty = false;
		JsonObject root = new JsonObject();
		root.addProperty("version", DemonClient.VERSION);
		JsonObject mods = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.persistEnabled() && m.isEnabled());
			o.addProperty("key", m.getKey());
			JsonObject s = new JsonObject();
			for (Setting<?> setting : m.settings()) s.add(setting.name, setting.toJson());
			o.add("settings", s);
			mods.add(m.name, o);
		}
		root.add("modules", mods);
		try {
			Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Exception e) {
			DemonClient.LOG.error("Failed to save Demon config", e);
		}
	}
}
