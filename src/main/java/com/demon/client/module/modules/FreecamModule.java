package com.demon.client.module.modules;

import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;
import com.demon.client.setting.NumberSetting;
import com.demon.client.util.Theme;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Detaches the camera so you can fly through walls while your player stays put. */
public class FreecamModule extends Module {
	public final NumberSetting speed = add(new NumberSetting("Speed", "Flying speed in blocks per tick", 1.0, 0.1, 5.0, 0.1));
	public final NumberSetting sprint = add(new NumberSetting("Sprint Boost", "Speed multiplier while holding sprint", 2.5, 1.0, 6.0, 0.5, "x"));
	public final BoolSetting smooth = add(new BoolSetting("Smooth", "Glide with acceleration instead of instant movement", true));
	public final BoolSetting disableOnDamage = add(new BoolSetting("Exit On Damage", "Turn off when your player takes damage", true));

	private Vec3 pos = Vec3.ZERO, prevPos = Vec3.ZERO, velocity = Vec3.ZERO;
	private float yaw, pitch;
	private boolean ready;
	private boolean prevSmartCull = true;
	private float lastHealth;

	public FreecamModule() {
		super("Freecam", "Fly the camera through walls while your body stays still", Category.RENDER, GLFW.GLFW_KEY_V, "camera");
	}

	public boolean isActive() {
		return isEnabled() && ready && mc.player != null;
	}

	@Override
	protected void onEnable() {
		ready = false;
		if (mc.player != null) start();
	}

	private void start() {
		Camera cam = mc.gameRenderer.getMainCamera();
		pos = cam.isInitialized() ? cam.position() : mc.player.getEyePosition();
		prevPos = pos;
		velocity = Vec3.ZERO;
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		prevSmartCull = mc.smartCull;
		// Occlusion culling breaks when the camera is inside blocks, so render everything in view.
		mc.smartCull = false;
		lastHealth = mc.player.getHealth();
		ready = true;
	}

	@Override
	protected void onDisable() {
		if (ready) mc.smartCull = prevSmartCull;
		ready = false;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) {
			if (ready) onDisable();
			return;
		}
		if (!ready) start();

		if (disableOnDamage.on() && mc.player.getHealth() < lastHealth - 0.01f) {
			setEnabled(false);
			Notifications.push("Freecam", "Disabled — you took damage!", Theme.DANGER, 2.5f);
			return;
		}
		lastHealth = mc.player.getHealth();

		prevPos = pos;
		float forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		float strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
		float vertical = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
		double s = speed.get() * (mc.options.keySprint.isDown() ? sprint.get() : 1.0);

		double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch);
		Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
		Vec3 left = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
		Vec3 dir = look.scale(forward).add(left.scale(strafe));
		if (dir.lengthSqr() > 1e-6) dir = dir.normalize();
		Vec3 target = dir.scale(s).add(0, vertical * s, 0);

		velocity = smooth.on() ? velocity.lerp(target, 0.35) : target;
		if (velocity.lengthSqr() < 1e-6) velocity = Vec3.ZERO;
		pos = pos.add(velocity);
	}

	/** Mouse movement (same sensitivity as vanilla Entity#turn). */
	public void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
	}

	public float yaw(float partialTick) {
		return yaw;
	}

	public float pitch(float partialTick) {
		return pitch;
	}

	public Vec3 position(float partialTick) {
		return prevPos.lerp(pos, partialTick);
	}

	@Override
	public String info() {
		return speed.display();
	}
}
