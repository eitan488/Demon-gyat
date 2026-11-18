package com.demon.client.module;

import com.demon.client.module.modules.AutoRtpModule;
import com.demon.client.module.modules.ChunkFinderModule;
import com.demon.client.module.modules.EspModule;
import com.demon.client.module.modules.FreecamModule;
import com.demon.client.module.modules.HudModule;
import com.demon.client.module.modules.InterfaceModule;
import com.demon.client.module.modules.combat.ArmorAlertModule;
import com.demon.client.module.modules.combat.CombatStatsModule;
import com.demon.client.module.modules.combat.CrosshairModule;
import com.demon.client.module.modules.combat.DamageNumbersModule;
import com.demon.client.module.modules.combat.HealthBarsModule;
import com.demon.client.module.modules.combat.HitSoundsModule;
import com.demon.client.module.modules.combat.KillEffectsModule;
import com.demon.client.module.modules.combat.LowHealthModule;
import com.demon.client.module.modules.combat.TargetHudModule;
import com.demon.client.module.modules.combat.TotemPopsModule;
import com.demon.client.module.modules.hud.ArmorHudModule;
import com.demon.client.module.modules.hud.ClockModule;
import com.demon.client.module.modules.hud.CompassBarModule;
import com.demon.client.module.modules.hud.CpsModule;
import com.demon.client.module.modules.hud.HuntStatsModule;
import com.demon.client.module.modules.hud.InventoryViewerModule;
import com.demon.client.module.modules.hud.KeystrokesModule;
import com.demon.client.module.modules.hud.PerformanceModule;
import com.demon.client.module.modules.hud.PotionHudModule;
import com.demon.client.module.modules.hud.RadarModule;
import com.demon.client.module.modules.hud.ServerInfoModule;
import com.demon.client.module.modules.hud.SpeedModule;
import com.demon.client.module.modules.hud.TpsModule;
import com.demon.client.module.modules.hunting.AutoScreenshotModule;
import com.demon.client.module.modules.hunting.BuildFinderModule;
import com.demon.client.module.modules.hunting.ChunkBordersModule;
import com.demon.client.module.modules.hunting.FindsModule;
import com.demon.client.module.modules.hunting.HeatmapModule;
import com.demon.client.module.modules.hunting.MobFinderModule;
import com.demon.client.module.modules.hunting.NavigatorModule;
import com.demon.client.module.modules.hunting.OldChunksModule;
import com.demon.client.module.modules.hunting.ItemFramesModule;
import com.demon.client.module.modules.hunting.LogoutSpotsModule;
import com.demon.client.module.modules.hunting.NewChunksModule;
import com.demon.client.module.modules.hunting.PortalFinderModule;
import com.demon.client.module.modules.hunting.SignReaderModule;
import com.demon.client.module.modules.hunting.SpawnerFinderModule;
import com.demon.client.module.modules.hunting.TrailFinderModule;
import com.demon.client.module.modules.hunting.VisualRangeModule;
import com.demon.client.module.modules.hunting.WaypointsModule;
import com.demon.client.module.modules.movement.AntiAfkModule;
import com.demon.client.module.modules.movement.AutoWalkModule;
import com.demon.client.module.modules.movement.ElytraBoostModule;
import com.demon.client.module.modules.movement.ParkourModule;
import com.demon.client.module.modules.movement.SafeWalkModule;
import com.demon.client.module.modules.movement.SprintModule;
import com.demon.client.module.modules.movement.YawLockModule;
import com.demon.client.module.modules.player.AutoEatModule;
import com.demon.client.module.modules.player.AutoReconnectModule;
import com.demon.client.module.modules.player.AutoRespawnModule;
import com.demon.client.module.modules.player.AutoToolModule;
import com.demon.client.module.modules.player.ChatTimestampsModule;
import com.demon.client.module.modules.player.CopyCoordsModule;
import com.demon.client.module.modules.player.DeathCoordsModule;
import com.demon.client.module.modules.render.BlockHighlightModule;
import com.demon.client.module.modules.render.BreadcrumbsModule;
import com.demon.client.module.modules.render.FullbrightModule;
import com.demon.client.module.modules.render.NametagsModule;
import com.demon.client.module.modules.render.NoRenderModule;
import com.demon.client.module.modules.render.TimeChangerModule;
import com.demon.client.module.modules.render.TrailsModule;
import com.demon.client.module.modules.render.ViewModelModule;
import com.demon.client.module.modules.render.ZoomModule;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private ModuleManager() {}

	private static final List<Module> MODULES = new ArrayList<>();

	public static ChunkFinderModule chunkFinder;
	public static AutoRtpModule autoRtp;
	public static EspModule esp;
	public static FreecamModule freecam;
	public static HudModule hud;
	public static InterfaceModule ui;
	public static NewChunksModule newChunks;
	public static WaypointsModule waypoints;
	public static LogoutSpotsModule logoutSpots;
	public static PortalFinderModule portalFinder;
	public static SpawnerFinderModule spawnerFinder;
	public static SignReaderModule signReader;
	public static TrailFinderModule trailFinder;
	public static OldChunksModule oldChunks;
	public static BuildFinderModule buildFinder;
	public static HeatmapModule heatmap;
	public static AutoScreenshotModule autoScreenshot;
	public static CrosshairModule crosshair;
	public static ZoomModule zoom;
	public static NoRenderModule noRender;
	public static FullbrightModule fullbright;
	public static ViewModelModule viewModel;
	public static TimeChangerModule timeChanger;
	public static NametagsModule nametags;
	public static SafeWalkModule safeWalk;
	public static ChatTimestampsModule chatTimestamps;

	private static <T extends Module> T reg(T m) {
		MODULES.add(m);
		return m;
	}

	public static void init() {
		// hunting
		chunkFinder = reg(new ChunkFinderModule());
		autoRtp = reg(new AutoRtpModule());
		reg(new FindsModule());
		reg(new NavigatorModule());
		oldChunks = reg(new OldChunksModule());
		buildFinder = reg(new BuildFinderModule());
		reg(new MobFinderModule());
		heatmap = reg(new HeatmapModule());
		autoScreenshot = reg(new AutoScreenshotModule());
		newChunks = reg(new NewChunksModule());
		waypoints = reg(new WaypointsModule());
		portalFinder = reg(new PortalFinderModule());
		spawnerFinder = reg(new SpawnerFinderModule());
		signReader = reg(new SignReaderModule());
		trailFinder = reg(new TrailFinderModule());
		reg(new ItemFramesModule());
		logoutSpots = reg(new LogoutSpotsModule());
		reg(new VisualRangeModule());
		reg(new ChunkBordersModule());
		// pvp visuals
		reg(new TargetHudModule());
		reg(new DamageNumbersModule());
		reg(new KillEffectsModule());
		crosshair = reg(new CrosshairModule());
		reg(new HitSoundsModule());
		reg(new HealthBarsModule());
		reg(new LowHealthModule());
		reg(new ArmorAlertModule());
		reg(new TotemPopsModule());
		reg(new CombatStatsModule());
		// render
		esp = reg(new EspModule());
		freecam = reg(new FreecamModule());
		nametags = reg(new NametagsModule());
		fullbright = reg(new FullbrightModule());
		zoom = reg(new ZoomModule());
		noRender = reg(new NoRenderModule());
		reg(new BlockHighlightModule());
		reg(new BreadcrumbsModule());
		timeChanger = reg(new TimeChangerModule());
		viewModel = reg(new ViewModelModule());
		reg(new TrailsModule());
		// movement
		reg(new SprintModule());
		reg(new AutoWalkModule());
		reg(new YawLockModule());
		reg(new ElytraBoostModule());
		safeWalk = reg(new SafeWalkModule());
		reg(new ParkourModule());
		reg(new AntiAfkModule());
		// player
		reg(new AutoReconnectModule());
		reg(new AutoRespawnModule());
		reg(new AutoToolModule());
		reg(new AutoEatModule());
		chatTimestamps = reg(new ChatTimestampsModule());
		reg(new CopyCoordsModule());
		reg(new DeathCoordsModule());
		// hud
		hud = reg(new HudModule());
		reg(new RadarModule());
		reg(new CompassBarModule());
		reg(new HuntStatsModule());
		reg(new ArmorHudModule());
		reg(new PotionHudModule());
		reg(new KeystrokesModule());
		reg(new CpsModule());
		reg(new SpeedModule());
		reg(new TpsModule());
		reg(new ServerInfoModule());
		reg(new ClockModule());
		reg(new InventoryViewerModule());
		reg(new PerformanceModule());
		// client
		ui = reg(new InterfaceModule());
	}

	/** Defaults applied before the config is loaded. */
	public static void defaults() {
		hud.enableByDefault();
		waypoints.enableByDefault();
		zoom.enableByDefault();
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

	/** Extra modules that need the chunk scanner running. */
	public static boolean scanNeeded() {
		return portalFinder != null && (portalFinder.isEnabled() || spawnerFinder.isEnabled() || signReader.isEnabled() || trailFinder.isEnabled()
			|| oldChunks.isEnabled() || buildFinder.isEnabled() || heatmap.isEnabled());
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
