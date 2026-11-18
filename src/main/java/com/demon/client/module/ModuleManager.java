package com.demon.client.module;

import com.demon.client.module.modules.ChunkFinderModule;
import com.demon.client.module.modules.EspModule;
import com.demon.client.module.modules.FreecamModule;
import com.demon.client.module.modules.HudModule;
import com.demon.client.module.modules.InterfaceModule;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private ModuleManager() {}

	private static final List<Module> MODULES = new ArrayList<>();

	public static ChunkFinderModule chunkFinder;
	public static EspModule esp;
	public static FreecamModule freecam;
	public static HudModule hud;
	public static InterfaceModule ui;

	public static void init() {
		MODULES.add(chunkFinder = new ChunkFinderModule());
		MODULES.add(esp = new EspModule());
		MODULES.add(freecam = new FreecamModule());
		MODULES.add(hud = new HudModule());
		MODULES.add(ui = new InterfaceModule());
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) if (m.category == c) out.add(m);
		return out;
	}

	public static Module byName(String name) {
		for (Module m : MODULES) if (m.name.equalsIgnoreCase(name)) return m;
		return null;
	}

	public static void tick() {
		for (Module m : MODULES) {
			if (m.isEnabled() || !m.toggleable()) m.onTick();
		}
	}

	/** Called on key press while no screen is open. */
	public static void onKey(int key) {
		if (key <= 0) return;
		for (Module m : MODULES) {
			if (m.getKey() == key) m.onKeyPressed();
		}
	}
}
