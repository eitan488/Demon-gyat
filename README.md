# Demon Client

A modern base-hunting utility mod for **Minecraft 1.21.11 (Fabric)**.

- **Custom main menu** with an animated intro cutscene, a synthwave background, a server quick-join panel and smooth transitions
- **Click GUI** (Right Shift) with blur, animations, sounds, search, sliders and per-module keybinds
- **Chunk Finder**: finds stash, base and redstone-farm chunks and shows what is inside each one
- **ESP**: highlights storage, redstone and base blocks through walls, with live counts
- **Freecam**: fly the camera through walls while your body stays still
- HUD with watermark, module list, coordinates (with nether conversion) and toast notifications

---

## Install

1. Install **Fabric Loader** for Minecraft **1.21.11** (https://fabricmc.net/use/installer/).
2. Download **Fabric API** for 1.21.11 (for example `0.141.6+1.21.11`) and put it in your `mods` folder.
3. Put `demon-client-1.0.0.jar` in the same `mods` folder.
4. Start the game with the Fabric profile.

## Controls

| Key | Action |
| --- | --- |
| **Right Shift** | Open / close the Demon menu (also: *✦ Demon* button in the pause menu) |
| **V** | Toggle Freecam |
| Freecam: **W A S D** | Fly where you look |
| Freecam: **Space / Shift** | Up / down |
| Freecam: **Ctrl (sprint)** | Speed boost |

In the menu, use **left click** to toggle a module, **right click** to open its settings, and **middle click** or the key chip to bind a key (Esc cancels, Del unbinds). Start typing anywhere to search.

## Chunk Finder

Every loaded chunk is scanned in the background with a small time budget per tick, so it doesn't lag. Each block and entity type adds points:

| Looks like a **stash / base** | Looks like a **redstone farm** |
| --- | --- |
| Shulker boxes (20), beacons (20), ender chests (8), enchanting tables (6), donkeys/llamas with chests (6), anvils, brewing stands, signs, beds, item frames, armor stands, chests, barrels | Hoppers, observers, crafters (4), pistons, spawners, slime/honey blocks, comparators, repeaters, droppers, dispensers, redstone dust and torches, hopper minecarts |

When a chunk reaches the **Stash Sensitivity** or **Farm Sensitivity** threshold, it is flagged:

- glowing chunk walls (or a box or flat style) in a colour per type: **STASH**, **BASE**, **FARM**, **BASE + FARM**
- a floating **info card** with the score, distance, coordinates, a colour bar and the count of every block type inside (for example *Shulker Box x14, Chest x32, Hopper x6*)
- **Block ESP** on every interesting block inside the chunk
- a sky **beam** so you can spot it from far away, plus optional tracers
- a **HUD list** of the nearest sus chunks with distance and a direction arrow
- a toast, a sound, a chat message, and a line in `config/demon/found-chunks.txt` (server, dimension, coordinates, contents)

Use **Find** to show *Everything*, *Stash / Base* only, or *Redstone / Farms* only. Lower sensitivity finds more chunks (including small builds); higher sensitivity shows only big builds.

## ESP

Toggle the groups you want (Storage, Redstone, Base Blocks, Entities), then set the range, style (fill and/or outline), opacity, line width, tracers, and name tags. Nearby blocks of the same type are merged into a single tag such as *Shulker Box x12*. A HUD counter shows totals in range.

## Interface settings

Theme palettes (Demon, Inferno, Ocean, Toxic, Sakura, Frost, Gold), GUI sounds and volume, animation speed, background blur, particles, custom main menu on/off, and the intro cutscene on/off. *Vanilla menu ›* on the title screen switches back to the vanilla menu; the *✦ Demon Menu* button there brings the custom one back.

Settings are saved to `config/demon/config.json`.

## Build from source

Requires Java 21.

```
./gradlew build
```

The mod jar ends up in `build/libs/`. Every push to GitHub also builds it automatically: open the **Actions** tab, select the latest run, and download the `demon-client` artifact.

## Note

Many servers do not allow ESP, freecam or chunk-finding clients. Only use it where it is allowed, for example on anarchy servers or with the owner's permission.
