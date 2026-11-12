# BlockScanner Ultimate

BlockScanner Ultimate is a Fabric mod for inspecting loaded chunks and auditing block activity. The visual chunk scanner remains limited to integrated single-player worlds. The activity audit runs server-side when the mod is installed on a server.

The analyzer checks chunks within the configured radius (up to 100 chunks in every horizontal direction) for configured block types, as well as chest pairs and hopper clusters. Flagged chunks and selected block targets are outlined by ESP. The scan is incremental, checks at most two loaded chunks per tick, and never loads chunks; results and tracer lines are limited to chunks Minecraft has already loaded.

In integrated single-player, a light-blue `BASE FOUND` HUD alert identifies the nearest base-like chunk with its coordinates, distance, and compass direction; the chunk is outlined in light blue on the world view and minimap. This is a structure heuristic: it flags two or more double-chest pairs, or a nearby group of at least four chest, barrel, hopper, dispenser, dropper, observer, piston, shulker, glass, TNT, or redstone blocks within one chunk. It cannot determine who placed existing blocks and may produce false positives.

On a server, successful block breaks and placements are attributed to the acting player. A chunk is reported to online operators and the server log after at least eight same-block replacements in that chunk within five minutes. This only records activity after the mod is installed; Minecraft worlds do not retain historical player attribution for existing blocks.

## Build

Use the Java version specified in `gradle.properties` (Java 25 for Minecraft 26.3) and run:

```sh
./gradlew build
```

Install the resulting `build/libs/blockscanner-1.0.0.jar` with Fabric Loader and Fabric API for the Minecraft version specified in `gradle.properties`.

Press `7` to open the module screen and enable or disable the scanner, ESP, radar, alerts, Freecam, and Name Protect. Click ESP's `Cfg` button to search registered block IDs, select one result, and add it to the tracked list. Search results are not scanned until added; each tracked block has its own six-digit RGB color. Double-chest pairs have a separate color. Click a tracked entry to edit or remove it. Click a module's key button to choose its quick-toggle key; press Backspace to clear it or Escape to cancel. Defaults are `6` for the scanner, `4` for ESP, `8` for the radar, `9` for alerts, `F` for Freecam, and `P` for Name Protect. The open-screen key can also be changed in Minecraft's Controls menu.

The block color editor accepts a block ID and six-digit RGB value. Freecam speed and flagged-chunk color are adjustable in the panel. Name Protect provides a 16-character pseudonym field and replaces the local username only while text is rendered on the client; it does not change chat input, packets, or server identity. Settings are saved in `config/blockscanner.json`.

The ArrayList HUD is enabled by default. Its settings control on/off state, screen corner, and rainbow or static RGB accent colors. Active entries update from the module states automatically.

Safety banners are available only as synthetic tests from `7` > `Safety Tests`. They do not inspect server anti-cheat state, player lists, or spectator state, and they are disabled outside integrated single-player worlds.

Freecam, the visual scanner, ESP, radar, and Name Protect are restricted to integrated single-player worlds. Server activity reports require installing the mod on a server; they do not reveal historical edits or activity from servers without the mod.