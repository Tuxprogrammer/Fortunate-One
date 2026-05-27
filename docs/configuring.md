# Configuring Fortunate One

The configuration file is created at `config/fortunateone.cfg` on the first launch.
All options live in the `general` category.

## Fortune Options

| Key | Default | Description |
|---|---|---|
| `enableUnlimitedFortuneMode` | `true` | Master toggle. When `false`, all other options are ignored and the mod does nothing. |
| `applyToGT` | `true` | Remove the Fortune 3 cap for GT5-Unofficial ore drops (`GTOreAdapter`). |
| `applyToBW` | `true` | Remove the Fortune 3 cap for BartWorks ore drops (`BWOreAdapter`). |
| `applyToGTPP` | `true` | Remove the Fortune 3 cap for GT++ ore drops (`GTPPOreAdapter`). |
| `affectBigOresOnly` | `true` | Only big (mixed) ores are affected. Small ores always use vanilla GT behavior. It is strongly recommended to leave this `true`. |
| `allowPlacedOreFortune` | `true` | GT normally treats player-placed ores as non-natural and zeroes the fortune bonus entirely. Set to `true` to bypass that and allow fortune on placed ores. |

## Worldgen Options

| Key | Default | Description |
|---|---|---|
| `equalizeOreVeinWeights` | `true` | Equalize spawn weight of all GT ore veins (GT5U, BartWorks, GT++) so every vein type has an equal chance of being selected. |
| `dimensionOverrides` | *(empty)* | Per-dimension overrides for ore vein Y range and layer structure (see below). |

### `dimensionOverrides` Format

Each entry is a single string with six colon-separated fields:

```
DimensionName:minY:maxY:primaryLayers:secondaryLayers:betweenLayers
```

| Field | Meaning |
|---|---|
| `DimensionName` | Dimension name as returned by `world.provider.getDimensionName()` (case-sensitive). Use `*` for a global default that applies to every dimension not matched by an exact entry. |
| `minY` / `maxY` | Override the Y range in which veins can spawn. Use `-1` for both to leave the vein's configured range unchanged. |
| `primaryLayers` / `secondaryLayers` / `betweenLayers` | Override the number of ore layers of each type. Must all be set together (none may be `-1`) or all left at `-1` (partial override not supported). Any total layer count is supported. |

**Dimension names** (common examples):

| Dimension | Name string |
|---|---|
| Overworld | `Overworld` |
| Nether | `Nether` |
| End | `The End` |
| Galacticraft Moon | `Moon` |
| Galacticraft Mars | `Mars` |

**Examples:**

```
# Lower Y cap for all dimensions, keep default layer structure:
*:10:60:-1:-1:-1

# Nether: shallow range, custom layer counts:
Nether:20:50:3:3:2

# Moon: deep range only, default layers:
Moon:15:80:-1:-1:-1

# Overworld custom layers (no height change):
Overworld:-1:-1:4:3:2
```

> **Note:** Layer override changes the internal vein structure from the vanilla GT5U pattern
> (`S,S,S,S+B,B,P+B,P+B,P,P`) to pure sequential blocks (all S, then all B, then all P).
> The small ore placer always runs afterwards regardless of layer settings.

## Notes

- The config can be edited in-game via the **Mods** screen → select *Fortunate One* → **Config**. Changes saved through the GUI apply to newly generated chunks without restarting.
- Changes made directly to `config/fortunateone.cfg` take effect on the next game launch (or server restart).
- The `affectBigOresOnly` option cannot be meaningfully set to `false` — small ore processing is not intercepted by this mod regardless of the setting.
- If `enableUnlimitedFortuneMode` is `false`, the fortune portion of the mod is completely inactive; fortune behaviour reverts to vanilla GT5U (capped at level 3). Worldgen options remain active independently.

## Default Config File

This is what `config/fortunateone.cfg` looks like with all default values:

```
# Configuration file

general {
    # Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true) [default: true]
    B:affectBigOresOnly=true

    # Allow placed (non-natural) big ores to receive Fortune when unlimited mode is enabled. GT normally zeroes fortune for placed ores; this bypasses that. [default: true]
    B:allowPlacedOreFortune=true

    # Apply unlimited Fortune to BartWorks big ore drops. [default: true]
    B:applyToBW=true

    # Apply unlimited Fortune to gregtech (GT5U) big ore drops. [default: true]
    B:applyToGT=true

    # Apply unlimited Fortune to GT++ big ore drops. [default: true]
    B:applyToGTPP=true

    # Per-dimension overrides for ore vein Y range and layer structure. [default: ]
    S:dimensionOverrides <
     >

    # Remove the Fortune 3 cap on big ore drops when GT drop mode is FortuneItem. [default: true]
    B:enableUnlimitedFortuneMode=true

    # Equalize the spawn weight of all GT ore veins so every vein type has an equal chance of being selected per worldgen attempt. [default: true]
    B:equalizeOreVeinWeights=true
}
```
