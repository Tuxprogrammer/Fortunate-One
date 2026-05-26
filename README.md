# Fortunate One

[![](https://jitpack.io/v/Tuxprogrammer/Fortunate-One.svg)](https://jitpack.io/#Tuxprogrammer/Fortunate-One)
[![](https://github.com/Tuxprogrammer/Fortunate-One/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/Tuxprogrammer/Fortunate-One/actions/workflows/build-and-test.yml)

A Minecraft 1.7.10 Forge addon for GT5-Unofficial that removes the Fortune III cap on big ore drops. When **FortuneItem** drop mode is active, ore adapters will scale drops beyond fortune level 3 instead of capping at it.

## User Documentation

- [Installing](docs/installing.md) — requirements, download, and step-by-step setup
- [Configuring](docs/configuring.md) — all config options with defaults and descriptions

## How It Works

GT5U hard-caps fortune-based big ore drops at level 3 inside `GTOreAdapter`, `BWOreAdapter`, and `GTPPOreAdapter`. This mod injects into those three classes via SpongeMixin and replaces the capped drop formula with an uncapped one when FortuneItem mode is active.

The uncapped formula is in `FortuneDropCalculator.java` and has no Minecraft dependencies, making it straightforward to unit test. The mixin classes delegate to it directly.

## Project Structure

```
src/main/java/.../fortunateone/
  FortunateOneMod.java         @Mod entry point, registers proxy
  FortunateOneConfig.java      Forge Configuration wrapper (generates config/fortunateone.cfg)
  CommonProxy.java             Loads config on FMLPreInitializationEvent
  FortuneDropCalculator.java   Pure-Java drop formula (no MC deps, fully unit tested)
  mixins/
    MixinGTOreAdapter.java     Injects into gregtech.common.ores.GTOreAdapter
    MixinBWOreAdapter.java     Injects into gregtech.common.ores.BWOreAdapter
    MixinGTPPOreAdapter.java   Injects into gregtech.common.ores.GTPPOreAdapter

src/test/java/.../fortunateone/
  FortuneFormulaTest.java      JUnit 5 tests for FortuneDropCalculator
```

## Building

Requires JDK 8 or 17+. The Gradle wrapper is included; no local Gradle installation needed.

```bash
# Build the mod jar
./gradlew jar

# Run unit tests
./gradlew test

# Build + test (what CI runs)
./gradlew build

# Generate a decompiled development workspace (for IDE use)
./gradlew setupDecompWorkspace
```

Output jars are written to `build/libs/`.

## Dependencies

Runtime dependency declared in `dependencies.gradle`:

```groovy
compileOnly(rfg.deobf("com.github.GTNewHorizons:GT5-Unofficial:5.09.52.551:dev"))
```

GT5U is `compileOnly` — it must be present in the player's `mods/` folder at runtime but is not bundled into the jar.

## CI / Releasing

Two GitHub Actions workflows handle CI:

| Workflow | Trigger | Purpose |
|---|---|---|
| `build-and-test.yml` | push to `main`, `workflow_dispatch` | Compile, test, run server smoke test, upload artifact |
| `release-tags.yml` | git tag push, `workflow_dispatch` | Build release jars and publish a GitHub Release |

To cut a release: tag the commit (`git tag v1.x.x && git push origin v1.x.x`), then trigger `release-tags.yml` via `workflow_dispatch` if it does not auto-fire.

## Credits

- [GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial) team for the ore drop system this mod hooks into.
