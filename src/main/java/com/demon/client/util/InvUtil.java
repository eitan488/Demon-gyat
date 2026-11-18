package com.demon.client.util;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** Inventory helpers. Inventory indices: 0-8 hotbar, 9-35 main, 40 offhand. */
public final class InvUtil {
	private InvUtil() {}

	private static final Minecraft mc = Minecraft.getInstance();

	public static int findHotbar(Predicate<ItemStack> p) {
		for (int i = 0; i < 9; i++) if (p.test(mc.player.getInventory().getItem(i))) return i;
		return -1;
	}

	public static int findInventory(Predicate<ItemStack> p) {
		for (int i = 0; i < 36; i++) if (p.test(mc.player.getInventory().getItem(i))) return i;
		return -1;
	}

	public static int count(Predicate<ItemStack> p) {
		int n = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			if (p.test(s)) n += s.getCount();
		}
		ItemStack off = mc.player.getOffhandItem();
		if (p.test(off)) n += off.getCount();
		return n;
	}

	public static void select(int hotbar) {
		if (hotbar >= 0 && hotbar < 9) mc.player.getInventory().setSelectedSlot(hotbar);
	}

	public static int selected() {
		return mc.player.getInventory().getSelectedSlot();
	}

	/** Converts an inventory index to a slot id in the player's inventory menu. */
	public static int menuSlot(int invIndex) {
		return invIndex < 9 ? invIndex + 36 : invIndex;
	}

	public static int armorMenuSlot(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> 5;
			case CHEST -> 6;
			case LEGS -> 7;
			default -> 8;
		};
	}

	public static void click(int menuSlot, int button, ClickType type) {
		mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, menuSlot, button, type, mc.player);
	}

	/** Swaps an inventory index with the offhand in a single click. */
	public static void swapToOffhand(int invIndex) {
		click(menuSlot(invIndex), 40, ClickType.SWAP);
	}

	public static void quickMove(int menuSlot) {
		click(menuSlot, 0, ClickType.QUICK_MOVE);
	}
}
