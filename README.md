# BlockScanner Ultimate

BlockScanner Ultimate is a client-side Fabric utility for inspecting loaded chunks in integrated single-player worlds. Scanning and rendering are disabled unless an integrated server is present; remote multiplayer worlds are not scanned or rendered.

The analyzer checks loaded chunk block entities for chest pairs and hopper clusters. Flagged chunks and detected targets are outlined in green. The scan is incremental, checks at most two loaded chunks per tick, and bounds unloaded-chunk lookups to keep client work predictable.

## Build

Use a Java 21 toolchain and run:

```sh
./gradlew build
```

Install the resulting `build/libs/blockscanner-1.0.0.jar` with Fabric Loader and Fabric API for Minecraft 1.21.4.

Press `O` to open the settings screen. Each module has an on/off control and a click-to-rebind key control. Defaults are `6` for the scanner, `7` for ESP, `8` for the radar, `9` for alerts, `5` for Freecam, and `P` for Name Protect.

The block color editor accepts a block ID and six-digit RGB value. Freecam speed and flagged-chunk color are adjustable in the panel. Name Protect provides a 16-character pseudonym field and replaces the local username only while text is rendered on the client; it does not change chat input, packets, or server identity. Settings are saved in `config/blockscanner.json`.

The ArrayList HUD is enabled by default. Its settings control on/off state, screen corner, and rainbow or static RGB accent colors. Active entries update from the module states automatically.

Safety banners are available only as synthetic tests from `O` > `Safety Tests`. They do not inspect server anti-cheat state, player lists, or spectator state, and they are disabled outside integrated single-player worlds.

Freecam, scanning, ESP, radar, and Name Protect are restricted to integrated single-player worlds. This build contains no anti-cheat bypass, server monitor, or staff tracker.