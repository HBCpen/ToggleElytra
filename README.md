# Toggle Elytra Mod

English | [日本語](README.ja.md)

A client-side Minecraft Fabric mod that automatically manages your Elytra and Chestplate based on your player state, and can auto-use fireworks while gliding.

## Features

* **Automatic Chestplate Equip**: When you are on the ground, the mod automatically equips your chestplate if you are wearing an Elytra.
* **Airborne Toggle**: When you are in the air, pressing the **Jump** key toggles between your Elytra and chestplate.
* **Vanilla Glide Start**: If you are already wearing an Elytra in mid-air but not gliding yet, pressing **Jump** starts gliding instead of toggling back to a chestplate.
* **Auto Firework Use**: While gliding, right-clicking with empty hands can temporarily swap a firework from your inventory into your hotbar, use it, and swap it back.
* **Menu Safe**: Elytra toggles and firework handling are ignored while inventory or menu screens are open.
* **Client-Side Only**: Works on multiplayer servers without needing to be installed on the server.

## Requirements

* Minecraft Java Edition **1.21.11**
* Fabric Loader
* Fabric API

## Installation

1. Download the latest release `.jar` file.
2. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11.
3. Place the downloaded `.jar` file and the [Fabric API](https://modrinth.com/mod/fabric-api) `.jar` into your `.minecraft/mods` folder.
4. Launch Minecraft using the Fabric profile.

## Usage

1. **Grounded**: Walk or stand on the ground. If you have a chestplate in your inventory and are wearing an Elytra, it will be swapped automatically.
2. **Airborne Toggle**: Jump off a ledge. Press **Space** in mid-air to swap between Elytra and chestplate.
3. **Glide Start**: If Elytra is already equipped in mid-air, press **Space** to start gliding as in vanilla.
4. **Fireworks**: While gliding, right-click with empty hands to use a firework from your inventory.

## Building from Source

```bash
./gradlew build
```

The output jar will be in `build/libs/`.
