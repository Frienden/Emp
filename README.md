# Resource Vision — Fabric 1.20.1

Client-side block locator. Search any block registered in Minecraft, including modded blocks, with an in-game menu and 3D outlines through walls. Designed for Java 17 and Fabric API.

## Controls
- F6 — add/remove block under crosshair
- F7 — toggle ESP
- F8 — block selection and settings
- F9 — clear results
- In menu: click to toggle a block, Shift+click a selected block to rotate its color.

## Build
GitHub > Actions > **Build Resource Vision for Fabric 1.20.1** > **Run workflow**. After a successful workflow run, download the `resource-vision-fabric-1.20.1` artifact; unzip it and copy `resource-vision-0.2.0.jar` into Minecraft's `mods` folder. Install Fabric API for 1.20.1 separately.

## Status
Initial build has not been verified yet. Use the Actions build log to diagnose compile errors.

## Limitations
Only scans chunks sent to the client. Any anti-Xray server obfuscation will affect detected blocks. Common blocks can hit the maximum marker count very quickly. Check server rules before using ESP.
