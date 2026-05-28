# Configuring Fortunate One

The configuration file is created at `config/fortunateone.cfg` on the first launch.
All options live in the `general` category.

## Fortune Options

| Key | Default | Description |
|---|---|---|
| `gregTechUnlimitedFortune` | `true` | Remove the Fortune 3 cap for GT5-Unofficial ore drops (`GTOreAdapter`). |
| `bartWorksUnlimitedFortune` | `true` | Remove the Fortune 3 cap for BartWorks ore drops (`BWOreAdapter`). |
| `gtPlusPlusUnlimitedFortune` | `true` | Remove the Fortune 3 cap for GT++ ore drops (`GTPPOreAdapter`). |
| `affectBigOresOnly` | `true` | Only big (mixed) ores are affected. Small ores always use vanilla GT behavior. It is strongly recommended to leave this `true`. |
| `allowPlacedOreFortune` | `true` | GT normally treats player-placed ores as non-natural and zeroes the fortune bonus entirely. Set to `true` to bypass that and allow fortune on placed ores. |

## Worldgen Options

| Key | Default | Description |
|---|---|---|
| `equalizeOreVeinWeights` | `true` | Equalize spawn weight of all GT ore veins (GT5U, BartWorks, GT++) so every vein type has an equal chance of being selected. |

### `dimensionOverrides` — per-dimension worldgen config

Dimension overrides are configured in a dedicated section of `config/fortunateone.cfg`.

**Auto-discovery:** The first time the mod generates ore veins in a dimension, it automatically adds a sub-section for that dimension with all fields set to `-1` (no override). You can then edit the values and reload the config to apply them to newly generated chunks.

**Config structure:**

```
dimensionOverrides {
    # --- Global default (optional) ---
    # Properties written directly here apply to every dimension that does not have
    # its own sub-section. Add these manually if you want a universal baseline.
    # Example: lower Y cap for all dimensions:
    # I:minY=10
    # I:maxY=60
    # I:primaryLayers=-1
    # I:secondaryLayers=-1
    # I:betweenLayers=-1

    # --- Per-dimension sub-sections (added automatically on first encounter) ---
    Overworld {
        I:betweenLayers=-1
        I:maxY=-1
        I:minY=-1
        I:primaryLayers=-1
        I:secondaryLayers=-1
    }

    Nether {
        I:betweenLayers=2
        I:maxY=50
        I:minY=20
        I:primaryLayers=3
        I:secondaryLayers=3
    }
}
```

**Fields** (apply to both the global default and per-dimension sections):

| Field | Meaning |
|---|---|
| `minY` / `maxY` | Override the Y range in which veins can spawn. Set both, or leave both at `-1` to keep the vein's configured range. |
| `primaryLayers` / `secondaryLayers` / `betweenLayers` | Override the layer counts. Must all be set together (none may be `-1`) or all left at `-1` (partial override not supported). Any total layer count is supported — not capped at 9. |

**Priority:** A per-dimension sub-section always takes precedence over the global default, even when all fields are `-1`. This lets you explicitly opt a dimension out of the global default.

**Dimension names** (common examples):

| Dimension | Name string |
|---|---|
| Overworld | `Overworld` |
| Nether | `Nether` |
| End | `The End` |
| Galacticraft Moon | `Moon` |
| Galacticraft Mars | `Mars` |

> **Note:** Layer override changes the internal vein structure from the vanilla GT5U pattern
> (`S,S,S,S+B,B,P+B,P+B,P,P`) to pure sequential blocks (all S, then all B, then all P).
> The small ore placer always runs afterwards regardless of layer settings.

## Reloading Config In-Game

Server operators can run `/fortunateone reload` to reload all Fortunate One config values from `config/fortunateone.cfg` without restarting the server.

- Requires command permission level `2` (server operator level).
- Reloads all main `general` options and all `dimensionOverrides` values from disk.
- Worldgen changes apply to newly generated chunks only; already-generated chunks are not modified.

## Notes

- The config can be edited in-game via the **Mods** screen → select *Fortunate One* → **Config**. Changes saved through the GUI apply immediately to newly generated chunks without restarting. Dimension override sub-sections are not shown in the GUI but can be edited directly in the file.
- Changes made directly to `config/fortunateone.cfg` take effect after running `/fortunateone reload` or on the next game launch/server restart.
- The `affectBigOresOnly` option cannot be meaningfully set to `false` — small ore processing is not intercepted by this mod regardless of the setting.
- To disable the fortune uncap entirely, set all three `*UnlimitedFortune` flags to `false`. Worldgen options remain active independently.

## Default Config File

This is what `config/fortunateone.cfg` looks like with all default values (dimension sub-sections are added automatically as dimensions are visited):

```
# Configuration file

general {
    # Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true) [default: true]
    B:affectBigOresOnly=true

    # Allow placed (non-natural) big ores to receive Fortune. GT normally zeroes fortune for placed ores; this bypasses that. [default: true]
    B:allowPlacedOreFortune=true

    # Apply unlimited Fortune to BartWorks big ore drops. [default: true]
    B:bartWorksUnlimitedFortune=true

    # Equalize the spawn weight of all GT ore veins so every vein type has an equal chance of being selected per worldgen attempt. [default: true]
    B:equalizeOreVeinWeights=true

    # Apply unlimited Fortune to GregTech (GT5U) big ore drops. [default: true]
    B:gregTechUnlimitedFortune=true

    # Apply unlimited Fortune to GT++ big ore drops. [default: true]
    B:gtPlusPlusUnlimitedFortune=true
}

dimensionOverrides {
    # (Sub-sections are added here automatically when the mod generates ore veins in a new dimension.)
    # To apply a universal default to all dimensions, add minY/maxY/primaryLayers/secondaryLayers/betweenLayers
    # properties directly in this section (not inside a sub-section).
}
```
