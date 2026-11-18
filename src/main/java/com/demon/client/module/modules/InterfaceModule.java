package com.demon.client.module.modules;

import com.demon.client.gui.ClickGuiScreen;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.module.ModuleManager;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.EnumSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.demon.client.util.Theme;
import org.lwjgl.glfw.GLFW;

/** Always-on module that holds GUI / theme / main menu options. Its keybind opens the click GUI. */
public class InterfaceModule extends Module {
	public final EnumSetting<Theme.Palette> theme = add(new EnumSetting<>("Theme", "Accent colours used everywhere", Theme.Palette.DEMON)
		.onChange(() -> Theme.palette = this.theme.get()));
	public final BoolSetting sounds = add(new BoolSetting("GUI Sounds", "Clicks, toggles and menu sound effects", true));
	public final NumberSetting volume = add(new NumberSetting("Sound Volume", "Volume of GUI sounds", 100, 0, 100, 5, "%"));
	public final NumberSetting animSpeed = add(new NumberSetting("Animation Speed", "Speed of every GUI animation", 1.0, 0.5, 2.0, 0.1, "x")
		.onChange(() -> Anim.speed = this.animSpeed.asFloat()));
	public final BoolSetting blur = add(new BoolSetting("Background Blur", "Blur the world behind the click GUI", true));
	public final BoolSetting particles = add(new BoolSetting("Particles", "Floating particles in menus", true));
	public final BoolSetting customMenu = add(new BoolSetting("Custom Main Menu", "Replace the vanilla title screen", true));
	public final BoolSetting intro = add(new BoolSetting("Intro Cutscene", "Animated intro when the game starts", true));

	public InterfaceModule() {
		super("Interface", "Click GUI, theme, sounds and the main menu", Category.CLIENT, GLFW.GLFW_KEY_RIGHT_SHIFT, "palette");
	}

	@Override
	public boolean toggleable() {
		return false;
	}

	@Override
	public void onKeyPressed() {
		mc.setScreen(new ClickGuiScreen(null));
	}

	/** Re-apply values that live in static fields after config load. */
	public void apply() {
		Theme.palette = theme.get();
		Anim.speed = animSpeed.asFloat();
	}

	public static boolean soundsEnabled() {
		return ModuleManager.ui == null || ModuleManager.ui.sounds.on();
	}

	public static float soundVolume() {
		return ModuleManager.ui == null ? 1f : ModuleManager.ui.volume.asFloat() / 100f;
	}

	public static boolean customMenu() {
		return ModuleManager.ui == null || ModuleManager.ui.customMenu.on();
	}

	public static boolean particlesEnabled() {
		return ModuleManager.ui == null || ModuleManager.ui.particles.on();
	}
}
