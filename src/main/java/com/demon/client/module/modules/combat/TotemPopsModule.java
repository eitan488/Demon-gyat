package com.demon.client.module.modules.combat;

import com.demon.client.combat.CombatFx;
import com.demon.client.gui.Notifications;
import com.demon.client.module.Category;
import com.demon.client.module.Module;
import com.demon.client.setting.BoolSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Counts totem pops the server shows you and announces them. */
public class TotemPopsModule extends Module {
	public final BoolSetting chat = add(new BoolSetting("Chat", "Also print pops in chat", false));

	public TotemPopsModule() {
		super("Totem Pops", "Announces how many totems each player has popped", Category.PVP, -1, "heart");
		CombatFx.POP_LISTENERS.add(this::onPop);
	}

	private void onPop(Player p) {
		if (!isEnabled() || mc.player == null) return;
		int n = CombatFx.POPS.get(p.getId());
		String who = p == mc.player ? "You" : p.getGameProfile().name();
		Notifications.push("Totem popped", who + " popped " + n + (n == 1 ? " totem" : " totems"), 0xFFFBBF24, 2.5f);
		if (chat.on()) mc.player.displayClientMessage(Component.literal("[Demon] ").withColor(0x38BDF8)
			.append(Component.literal(who + " popped " + n + (n == 1 ? " totem" : " totems")).withColor(0xFBBF24)), false);
	}
}
