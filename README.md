# Fortunate One

[![](https://jitpack.io/v/Tuxprogrammer/Fortunate-One.svg)](https://jitpack.io/#Tuxprogrammer/Fortunate-One)
[![](https://github.com/Tuxprogrammer/Fortunate-One/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/Tuxprogrammer/Fortunate-One/actions/workflows/build-and-test.yml)

A Minecraft 1.7.10 Forge addon for GT5-Unofficial that removes the Fortune III cap on big ore drops. When **FortuneItem** drop mode is active, ore adapters will scale drops beyond fortune level 3 instead of capping at it.

## Installation

1. Drop `fortunateone-*.jar` into your `mods/` folder.
2. Requires [GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) at runtime.
3. UniMixins is required and is typically already present in GT New Horizons packs.

## Configuration

Edit `config/fortunateone.cfg` after the first launch:

| Option | Default | Description |
|---|---|---|
| `applyToGT` | `true` | Remove the cap for GT ore drops (`GTOreAdapter`) |
| `applyToBW` | `true` | Remove the cap for BartWorks ore drops (`BWOreAdapter`) |
| `applyToGTPP` | `true` | Remove the cap for GT++ ore drops (`GTPPOreAdapter`) |
| `allowPlacedOreFortune` | `false` | Also apply unlimited fortune to player-placed ores |

## How It Works

GT5U limits fortune-based big ore drops to a maximum of fortune level 3 regardless of the actual enchantment level. This mod injects into the three ore adapter classes via SpongeMixin and replaces the capped drop formula with an uncapped one when FortuneItem mode is selected.

## Building from Source

```bash
./gradlew jar
```

Output is in `build/libs/`.

## Credits

- [GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) team for the ore drop system this mod hooks into.
