# Configuring Fortunate One

The configuration file is created at `config/fortunateone.cfg` on the first launch.
All options are booleans (`true` / `false`) in the `general` category.

## Options

| Key | Default | Description |
|---|---|---|
| `enableUnlimitedFortuneMode` | `true` | Master toggle. When `false`, all other options are ignored and the mod does nothing. |
| `applyToGT` | `true` | Remove the Fortune 3 cap for GT5-Unofficial ore drops (`GTOreAdapter`). |
| `applyToBW` | `true` | Remove the Fortune 3 cap for BartWorks ore drops (`BWOreAdapter`). |
| `applyToGTPP` | `true` | Remove the Fortune 3 cap for GT++ ore drops (`GTPPOreAdapter`). |
| `affectBigOresOnly` | `true` | Only big (mixed) ores are affected. Small ores always use vanilla GT behavior. It is strongly recommended to leave this `true`. |
| `allowPlacedOreFortune` | `true` | GT normally treats player-placed ores as non-natural and zeroes the fortune bonus entirely. Set to `true` to bypass that and allow fortune on placed ores. |

## Notes

- Changes to the config file take effect on the next game launch (or server restart). Hot-reloading is not supported.
- The `affectBigOresOnly` option cannot be meaningfully set to `false` — small ore processing is not intercepted by this mod regardless of the setting.
- If `enableUnlimitedFortuneMode` is `false`, the mod is completely inactive; fortune behaviour reverts to vanilla GT5U (capped at level 3).

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

    # Remove the Fortune 3 cap on big ore drops when GT drop mode is FortuneItem. [default: true]
    B:enableUnlimitedFortuneMode=true
}
```
