package com.demon.client.module.modules.render;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Anim;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Smooth, animated zoom while you hold the key (default Z). */
public class ZoomModule extends Module {
	public final NumberSetting amount = add(new NumberSetting("Zoom", "Zoom factor", 4, 1.5, 12, 0.5, "x"));
	public final BoolSetting smoothCamera = add(new BoolSetting("Cinematic", "Smooth camera while zoomed", true));
	private final Anim.Value value = new Anim.Value(1, 12);
	private boolean wasCinematic, zooming;

	public ZoomModule() {
		super("Zoom", "Hold the key to zoom smoothly", Category.RENDER, GLFW.GLFW_KEY_Z, "search");
	}

	@Override
	public void onKeyPressed() {
		// zoom is hold-to-use: the key is polled every frame instead of toggling the module
		if (!isEnabled()) setEnabled(true);
	}

	public float factor() {
		if (!isEnabled() || mc.screen != null) return value.target(1).get();
		boolean held = getKey() > 0 && InputConstants.isKeyDown(mc.getWindow(), getKey());
		if (held != zooming) {
			zooming = held;
			if (smoothCamera.on()) {
				if (held) {
					wasCinematic = mc.options.smoothCamera;
					mc.options.smoothCamera = true;
				} else {
					mc.options.smoothCamera = wasCinematic;
				}
			}
		}
		return value.target(held ? amount.asFloat() : 1).get();
	}
}
