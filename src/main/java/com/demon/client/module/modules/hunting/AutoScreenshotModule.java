package com.demon.client.module.modules.hunting;

import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.NumberSetting;
import net.minecraft.client.Screenshot;

/** Takes a screenshot automatically shortly after something is found. */
public class AutoScreenshotModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks to wait so the highlight is on screen", 10, 0, 60, 5));
	private int pending = -1;

	public AutoScreenshotModule() {
		super("Auto Screenshot", "Screenshots every find automatically (saved in screenshots/)", Category.HUNTING, -1, "camera");
	}

	public void request() {
		if (isEnabled() && pending < 0) pending = delay.asInt();
	}

	@Override
	public void onTick() {
		if (pending < 0) return;
		if (pending-- > 0) return;
		pending = -1;
		Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> mc.execute(() -> mc.gui.getChat().addMessage(msg)));
	}
}
