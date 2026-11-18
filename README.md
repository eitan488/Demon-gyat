# Demon Client

A modern base-hunting utility mod for **Minecraft 1.21.11 (Fabric)**.

- **HD blue interface**: text is rendered with the Inter font at your screen's native pixel size, and every rounded shape, icon and logo is generated at exact pixel resolution, so it stays sharp at every GUI scale
- **Custom main menu** with an animated intro cutscene, a synthwave background, a server quick-join panel and smooth transitions
- **Click GUI** (Right Shift) with blur, animations, sounds, search, sliders and per-module keybinds
- **Chunk Finder**: finds stash, base and redstone-farm chunks and shows what is inside each one
- **Auto RTP**: runs `/rtp overworld`, `/rtp nether` or `/rtp end` (or cycles through them) on a timer you choose, scans each new spot, and stops when it finds something
- **ESP**: highlights storage, redstone and base blocks through walls, with live counts
- **Freecam**: fly the camera through walls while your body stays still
- HUD with watermark, module list, coordinates (with nether conversion) and toast notifications

---

## Install

1. Install **Fabric Loader** for Minecraft **1.21.11** (https://fabricmc.net/use/installer/).
2. Download **Fabric API** for 1.21.11 (for example `0.141.6+1.21.11`) and put it in your `mods` folder.
3. Put `demon-client-1.1.0.jar` in the same `mods` folder.
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

## Auto RTP

Turn it on in the **Hunting** tab. It turns on the Chunk Finder if needed and then:

1. waits until the loaded chunks are scanned (*Wait For Scan*)
2. counts down the **Delay** (5 s to 10 min) and sends the RTP command
3. waits for the teleport, scans the new area, and repeats

Settings:

- **World**: *Overworld*, *Nether*, *End*, or *Cycle All* (overworld, then nether, then end, and so on)
- **Command**: `/rtp <world>` (sends `/rtp overworld`, `/rtp nether`, `/rtp end`), `/rtp`, `/wild` or `/randomtp`, depending on your server
- **Stop On Find**: stops with a toast and a sound as soon as a stash, base or farm is found, so you can go and check it
- **Stop On Damage**, **Max RTPs**, and an on-screen **HUD panel** with a countdown ring, status and RTP/find counters

Auto RTP and Freecam never switch themselves back on when you restart the game.

## ESP

Toggle the groups you want (Storage, Redstone, Base Blocks, Entities), then set the range, style (fill and/or outline), opacity, line width, tracers, and name tags. Nearby blocks of the same type are merged into a single tag such as *Shulker Box x12*. A HUD counter shows totals in range.

## Interface settings

Theme palettes (Azure (default blue), Royal Blue, Ice, Neon Cyan, Demon, Inferno, Toxic), GUI sounds and volume, animation speed, background blur, particles, custom main menu on/off, and the intro cutscene on/off. *Vanilla menu ›* on the title screen switches back to the vanilla menu; the *✦ Demon Menu* button there brings the custom one back.

Settings are saved to `config/demon/config.json`.

## Build from source

Requires Java 21.

```
./gradlew build
```

The mod jar ends up in `build/libs/`. Every push to GitHub also builds it automatically: open the **Actions** tab, select the latest run, and download the `demon-client` artifact.

## Credits

The UI uses the [Inter](https://github.com/rsms/inter) typeface, licensed under the SIL Open Font License 1.1 (see `src/main/resources/assets/demon/fonts/OFL.txt`).

## Note

Many servers do not allow ESP, freecam or chunk-finding clients. Only use it where it is allowed, for example on anarchy servers or with the owner's permission.
