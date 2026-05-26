# Installing Fortunate One

## Requirements

- Minecraft 1.7.10
- [Minecraft Forge](https://files.minecraftforge.net/) 10.13.4.1614 or later
- [GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) (required at runtime)
- [UniMixins](https://github.com/GTNewHorizons/UniMixins) (required; already included in GT New Horizons packs)

## Download

Download the latest release jar from the [Releases page](https://github.com/Tuxprogrammer/Fortunate-One/releases).

You want the file named `fortunateone-<version>.jar`. The `-sources` and `-dev` variants are not needed for playing.

## Installation

1. Navigate to your Minecraft instance folder (the folder containing `mods/`).
2. Copy `fortunateone-<version>.jar` into the `mods/` folder.
3. Launch Minecraft. The mod will generate its configuration file at `config/fortunateone.cfg` on first run.

## Verifying the Install

In-game, open the Forge mod list (`Mods` button on the main menu). **Fortunate One** should appear in the list with the correct version number.

If the mod is not listed, check that:
- The jar is directly inside `mods/`, not inside a subfolder.
- GT5-Unofficial and UniMixins are also present in `mods/`.
- You are running Forge 1.7.10, not a different Minecraft version.

## Configuration

After the first launch, you can tune which ore adapters are affected and other options.
See the [Configuration Guide](configuring.md) for a full reference.
