# Fortunate One Reload Command Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/fortunateone reload` so server operators can reload all Fortunate One config values from disk without restarting, with new settings applied to newly generated chunks.

**Architecture:** Register a Forge 1.7.10 server command during `FMLServerStartingEvent`. Add one explicit config reload entry point in `FortunateOneConfig` that reloads the main GTNHLib-managed fields and dimension override sections from `fortunateone.cfg`. Keep command handling in a focused command class and reuse existing dimension override parsing.

**Tech Stack:** Minecraft Forge 1.7.10, Java 8, GTNHLib `@Config`, Forge `Configuration`, `CommandBase`, Gradle/GTNHGradle.

---

### Task 1: Add explicit disk reload API

**Files:**
- Modify: `src/main/java/io/github/tuxprogrammer/fortunateone/FortunateOneConfig.java`
- Test later: `src/test/java/io/github/tuxprogrammer/fortunateone/FortunateOneConfigReloadTest.java`

- [ ] Store the config file path passed to `initDimensionConfig(File configFile)` in a `@Config.Ignore private static File configFile` field.
- [ ] Add `public static void reloadFromDisk()` that throws `IllegalStateException` if called before init.
- [ ] Inside `reloadFromDisk()`, create/load a Forge `Configuration` for the saved config file and explicitly read the `general` booleans using the existing default values and comments.
- [ ] Update all six public static config booleans.
- [ ] Recreate/load `dimensionConfig`, call `preRegisterKnownDimensions()`, then call `rebuildDimensionOverrideMap()`.
- [ ] Add a private helper for reading main booleans to avoid duplicating the reload logic.

### Task 2: Add `/fortunateone reload` server command

**Files:**
- Create: `src/main/java/io/github/tuxprogrammer/fortunateone/CommandFortunateOne.java`
- Modify: `src/main/java/io/github/tuxprogrammer/fortunateone/CommonProxy.java`

- [ ] Implement `CommandFortunateOne extends CommandBase`.
- [ ] `getCommandName()` returns `fortunateone`.
- [ ] `getCommandUsage(ICommandSender sender)` returns `/fortunateone reload`.
- [ ] `getRequiredPermissionLevel()` returns `2`.
- [ ] `processCommand(ICommandSender sender, String[] args)` accepts exactly one argument, case-insensitive `reload`.
- [ ] On success, call `FortunateOneConfig.reloadFromDisk()` and send a success chat message noting new chunks use new worldgen settings.
- [ ] On invalid arguments, throw `WrongUsageException(getCommandUsage(sender))`.
- [ ] On reload failure, log the exception and throw `CommandException` with a concise failure message.
- [ ] Register the command in `CommonProxy.serverStarting(FMLServerStartingEvent event)` with `event.registerServerCommand(new CommandFortunateOne())`.

### Task 3: Add tests for config reload behavior

**Files:**
- Create: `src/test/java/io/github/tuxprogrammer/fortunateone/FortunateOneConfigReloadTest.java`

- [ ] Create a temp `fortunateone.cfg` using Forge `Configuration`.
- [ ] Write a `general` category with non-default boolean values.
- [ ] Write at least one `dimensionOverrides.TestDim` section with height/layer values.
- [ ] Call `FortunateOneConfig.initDimensionConfig(tempFile)` then mutate the file on disk to different values.
- [ ] Call `FortunateOneConfig.reloadFromDisk()`.
- [ ] Assert public static booleans reflect the mutated disk values.
- [ ] Assert `getDimensionOverride("TestDim")` reflects the mutated disk values.
- [ ] Assert missing known dimensions are pre-seeded without overwriting `TestDim`.

### Task 4: Update documentation

**Files:**
- Modify: `README.md`
- Modify if useful: `docs/configuring.md`

- [ ] Add a short command section documenting `/fortunateone reload`.
- [ ] State permission level 2/server operator requirement.
- [ ] State it reloads all Fortunate One config values from disk.
- [ ] State worldgen/dimension override changes affect newly generated chunks only.

### Task 5: Verify, commit, release

**Commands:**
- [ ] Run `./gradlew.bat spotlessApply compileJava`.
- [ ] Run `./scripts/verify-local.ps1 -ServerTimeoutSeconds 300`.
- [ ] Commit to main with a clear feature message.
- [ ] Push main to GitHub.
- [ ] Tag `v1.1.2` and push the tag.
- [ ] Watch GitHub Actions until Build & Test, Mixin Purity, and Release workflows pass.
- [ ] Confirm `https://github.com/Tuxprogrammer/Fortunate-One/releases/tag/v1.1.2` exists and includes the production jar asset.

---

## Self-review

- Spec coverage: command name, permission level 2, all config values from disk, newly generated chunks, commit/push/tag/release are covered.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: class and method names are consistent with existing `FortunateOneConfig`, `CommonProxy`, and Forge 1.7.10 command APIs.
