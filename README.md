# Toggle Elytra Mod

A client-side Minecraft Fabric mod that automatically manages your Elytra and Chestplate based on your player state.

## Features

*   **Automatic Chestplate Equip**: When you are on the ground, the mod automatically equips your Chestplate (if you are wearing an Elytra).
*   **Airborne Toggle**: When you are in the air, pressing the **Jump** key toggles between your Elytra and Chestplate.
    *   Useful for quickly switching to Elytra for flight or Chestplate for protection/landing.
*   **Client-Side Only**: Works on multiplayer servers without needing to be installed on the server.

## Requirements

*   Minecraft Java Edition **1.21.10**
*   Fabric Loader
*   Fabric API

## Installation

1.  Download the latest release `.jar` file.
2.  Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.10.
3.  Place the downloaded `.jar` file and the [Fabric API](https://modrinth.com/mod/fabric-api) `.jar` into your `.minecraft/mods` folder.
4.  Launch Minecraft using the Fabric profile.

## Usage

1.  **Grounded**: Just walk or stand on the ground. If you have a Chestplate in your inventory and are wearing an Elytra, it will be swapped automatically.
2.  **Airborne**: Jump off a ledge. Press **Space** (Jump) in mid-air to swap between Elytra and Chestplate.
3.  **Flight**: Equip Elytra (via toggle) and press Jump again to activate flight as usual.

## Building from Source

```bash
./gradlew build
```

The output jar will be in `build/libs/`.
