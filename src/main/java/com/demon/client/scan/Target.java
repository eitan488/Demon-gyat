package com.demon.client.scan;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Everything the scanner looks for. Each target has a colour, a group (used by the ESP filters)
 * and two weights: how much it suggests a stash/base and how much it suggests a redstone farm.
 */
public enum Target {
	// ---- storage
	SHULKER("Shulker Box", 0xFFFF4FD8, Group.STORAGE, 20, 0),
	ENDER_CHEST("Ender Chest", 0xFFB46CFF, Group.STORAGE, 8, 0),
	CHEST("Chest", 0xFFFFB547, Group.STORAGE, 2, 0),
	TRAPPED_CHEST("Trapped Chest", 0xFFFF7A45, Group.STORAGE, 2, 1),
	BARREL("Barrel", 0xFFD9A066, Group.STORAGE, 2, 0),
	// ---- redstone / farms
	HOPPER("Hopper", 0xFFA5B4FC, Group.REDSTONE, 1, 4),
	OBSERVER("Observer", 0xFF22D3EE, Group.REDSTONE, 0, 4),
	PISTON("Piston", 0xFF34D399, Group.REDSTONE, 0, 3),
	CRAFTER("Crafter", 0xFFFBBF24, Group.REDSTONE, 0, 4),
	DROPPER("Dropper", 0xFF94A3B8, Group.REDSTONE, 0.5, 2),
	DISPENSER("Dispenser", 0xFF7DD3FC, Group.REDSTONE, 0.5, 2),
	COMPARATOR("Comparator", 0xFFFB7185, Group.REDSTONE, 0, 2.5),
	REPEATER("Repeater", 0xFFF43F5E, Group.REDSTONE, 0, 2),
	REDSTONE("Redstone Dust", 0xFFEF4444, Group.REDSTONE, 0, 0.6),
	REDSTONE_SOURCE("Redstone Torch/Block", 0xFFDC2626, Group.REDSTONE, 0, 1),
	SLIME("Slime/Honey Block", 0xFF84CC16, Group.REDSTONE, 0, 2.5),
	SPAWNER("Spawner", 0xFF818CF8, Group.REDSTONE, 0, 4),
	// ---- base / utility blocks
	BEACON("Beacon", 0xFF67E8F9, Group.BASE, 20, 0),
	ENCHANTING("Enchanting Table", 0xFFC4B5FD, Group.BASE, 6, 0),
	ANVIL("Anvil", 0xFFCBD5E1, Group.BASE, 4, 0),
	BREWING("Brewing Stand", 0xFFFDE047, Group.BASE, 4, 0),
	FURNACE("Furnace", 0xFFFB923C, Group.BASE, 1.5, 0.5),
	BED("Bed", 0xFFF472B6, Group.BASE, 3, 0),
	SIGN("Sign", 0xFFE7C9A9, Group.BASE, 4, 0),
	PORTAL("Nether Portal", 0xFFA855F7, Group.BASE, 0.3, 0),
	// ---- entities
	ITEM_FRAME("Item Frame", 0xFFFCD34D, Group.ENTITY, 3, 0),
	ARMOR_STAND("Armor Stand", 0xFFE5E7EB, Group.ENTITY, 3, 0),
	CHEST_MINECART("Chest Minecart", 0xFFFFC46B, Group.ENTITY, 1, 0),
	HOPPER_MINECART("Hopper Minecart", 0xFFB8C2FF, Group.ENTITY, 0, 3),
	CHESTED_ANIMAL("Chested Donkey/Llama", 0xFFE0A96D, Group.ENTITY, 6, 0),
	// ---- trails (player activity; no score weight)
	TORCH("Torch", 0xFFFFD27A, Group.TRAIL, 0, 0),
	LANTERN("Lantern", 0xFFFFC14D, Group.TRAIL, 0, 0),
	CRAFTING_TABLE("Crafting Table", 0xFFC8955C, Group.TRAIL, 0, 0),
	OBSIDIAN("Obsidian", 0xFF6D5BD0, Group.TRAIL, 0, 0);

	public enum Group {
		STORAGE("Storage"), REDSTONE("Redstone"), BASE("Base Blocks"), ENTITY("Entities"), TRAIL("Trail Blocks");

		public final String title;

		Group(String title) {
			this.title = title;
		}
	}

	public static final Target[] VALUES = values();

	public final String title;
	public final int color;
	public final Group group;
	public final double stashWeight;
	public final double farmWeight;

	Target(String title, int color, Group group, double stashWeight, double farmWeight) {
		this.title = title;
		this.color = color;
		this.group = group;
		this.stashWeight = stashWeight;
		this.farmWeight = farmWeight;
	}

	public boolean isEntity() {
		return group == Group.ENTITY;
	}

	// ------------------------------------------------------------------ lookup

	private static final Target NONE_MARKER = null;
	private static final Reference2ObjectOpenHashMap<Block, Object> CACHE = new Reference2ObjectOpenHashMap<>();
	private static final Object NONE = new Object();

	/** Returns the target for a block state, or null if it is not interesting. Cached per block. */
	public static Target of(BlockState state) {
		Block block = state.getBlock();
		Object cached = CACHE.get(block);
		if (cached == null) {
			Target t = compute(block);
			CACHE.put(block, t == null ? NONE : t);
			return t;
		}
		return cached == NONE ? NONE_MARKER : (Target) cached;
	}

	private static Target compute(Block b) {
		if (b instanceof ShulkerBoxBlock) return SHULKER;
		if (b == Blocks.TRAPPED_CHEST) return TRAPPED_CHEST;
		if (b instanceof ChestBlock) return CHEST;
		if (b == Blocks.ENDER_CHEST) return ENDER_CHEST;
		if (b == Blocks.BARREL) return BARREL;
		if (b == Blocks.HOPPER) return HOPPER;
		if (b == Blocks.OBSERVER) return OBSERVER;
		if (b == Blocks.PISTON || b == Blocks.STICKY_PISTON) return PISTON;
		if (b == Blocks.CRAFTER) return CRAFTER;
		if (b == Blocks.DROPPER) return DROPPER;
		if (b == Blocks.DISPENSER) return DISPENSER;
		if (b == Blocks.COMPARATOR) return COMPARATOR;
		if (b == Blocks.REPEATER) return REPEATER;
		if (b == Blocks.REDSTONE_WIRE) return REDSTONE;
		if (b == Blocks.REDSTONE_TORCH || b == Blocks.REDSTONE_WALL_TORCH || b == Blocks.REDSTONE_BLOCK) return REDSTONE_SOURCE;
		if (b == Blocks.SLIME_BLOCK || b == Blocks.HONEY_BLOCK) return SLIME;
		if (b == Blocks.SPAWNER) return SPAWNER;
		if (b == Blocks.BEACON) return BEACON;
		if (b == Blocks.ENCHANTING_TABLE) return ENCHANTING;
		if (b instanceof AnvilBlock) return ANVIL;
		if (b == Blocks.BREWING_STAND) return BREWING;
		if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) return FURNACE;
		if (b instanceof BedBlock) return BED;
		if (b instanceof SignBlock) return SIGN;
		if (b == Blocks.NETHER_PORTAL) return PORTAL;
		if (b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.SOUL_TORCH || b == Blocks.SOUL_WALL_TORCH) return TORCH;
		if (b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN) return LANTERN;
		if (b == Blocks.CRAFTING_TABLE) return CRAFTING_TABLE;
		if (b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN) return OBSIDIAN;
		return null;
	}

	public static Target of(Entity e) {
		if (e instanceof ItemFrame) return ITEM_FRAME;
		if (e instanceof ArmorStand) return ARMOR_STAND;
		if (e instanceof MinecartChest) return CHEST_MINECART;
		if (e instanceof MinecartHopper) return HOPPER_MINECART;
		if (e instanceof AbstractChestedHorse h && h.hasChest()) return CHESTED_ANIMAL;
		return null;
	}
}
