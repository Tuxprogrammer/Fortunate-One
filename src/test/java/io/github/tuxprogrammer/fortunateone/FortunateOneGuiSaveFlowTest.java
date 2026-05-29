package io.github.tuxprogrammer.fortunateone;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.FMLInjectionData;

/**
 * Regression tests for the Forge config GUI save flow.
 *
 * <p>
 * Reproduces the bug reported in fortunate-one logs where editing the global dimension override
 * values (minY, maxY, primaryLayers, secondaryLayers, betweenLayers) in the in-game GUI and
 * pressing Done would not persist the new values — they reverted back to {@code -1, -1, -1}.
 *
 * <p>
 * <strong>Why a direct unit test instead of constructing a real {@link cpw.mods.fml.client.config.GuiConfig}?</strong>
 * Forge's {@code GuiConfig} touches {@code Minecraft.getMinecraft()} and a live {@code FontRenderer} in its
 * constructor, which is impractical to bootstrap from a JUnit test running on the unpatched Minecraft
 * classpath. Instead we simulate the exact data-flow Forge performs on Done:
 * <ol>
 * <li>Wrap each {@link Property} the user edits in a plain {@link ConfigElement} (this is what
 * Forge's {@code ConfigElement.getTypedElement(Property)} does internally when building
 * {@code IntegerEntry} instances on the GUI's sub-page for {@code dimensionoverrides}).</li>
 * <li>Call {@link IConfigElement#set} with the user-typed value. This is precisely what
 * {@code IntegerEntry.saveConfigElement()} does at the bottom of its method body — the only
 * GUI-specific step we skip is parsing text from a {@code GuiTextField} widget.</li>
 * <li>Fire the same event chain Forge fires on Done by invoking
 * {@link CommonProxy#onConfigChanged(cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent)}
 * directly with the same {@code modID} the parent GUI uses.</li>
 * <li>Invoke the same {@link FortunateOneConfig} hooks that {@code FortunateOneGuiConfig.onGuiClosed()}
 * runs.</li>
 * </ol>
 *
 * <p>
 * The test then asserts on two strict invariants:
 * <ul>
 * <li>The in-memory dimension override map for the global default ({@code "*"}) reflects the
 * user-edited values, NOT the stale {@code -1} values that were on disk before the edit.</li>
 * <li>The on-disk {@code fortunateone.cfg} file contains the new values after the save flow runs.
 * If a fix only updates in-memory state without saving to disk, the user would lose their changes
 * on restart — that is the second half of the original bug.</li>
 * </ul>
 */
class FortunateOneGuiSaveFlowTest {

    @TempDir
    Path tempDir;

    private File configFile;
    private Configuration cfg;

    @BeforeEach
    void setUp() throws ReflectiveOperationException, IOException {
        // FMLInjectionData.minecraftHome is read by Forge's Configuration in some code paths.
        // Set it to the temp dir so any incidental file references stay sandboxed.
        Field minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        minecraftHome.set(null, tempDir.toFile());

        configFile = tempDir.resolve("config")
            .resolve("fortunateone.cfg")
            .toFile();
        Files.createDirectories(
            configFile.toPath()
                .getParent());

        // Seed the file with -1 defaults — the exact starting state from the user's bug report.
        cfg = new Configuration(configFile);
        cfg.load();
        // Top-level dimensionoverrides properties = "global defaults".
        cfg.get("dimensionoverrides", "minY", -1);
        cfg.get("dimensionoverrides", "maxY", -1);
        cfg.get("dimensionoverrides", "primaryLayers", -1);
        cfg.get("dimensionoverrides", "secondaryLayers", -1);
        cfg.get("dimensionoverrides", "betweenLayers", -1);
        cfg.save();

        // Inject our pre-loaded Configuration as the one FortunateOneConfig will use, so we
        // bypass GTNHLib ConfigurationManager (which we cannot bootstrap in a unit test).
        FortunateOneConfig.testConfig = cfg;
        FortunateOneConfig.initDimensionConfig();
    }

    @AfterEach
    void tearDown() {
        FortunateOneConfig.testConfig = null;
    }

    /**
     * Reproduces the exact user-reported flow:
     * <ol>
     * <li>User opens GUI, drills into {@code dimensionoverrides}, types {@code 1} into the global
     * minY / maxY / primaryLayers / secondaryLayers / betweenLayers fields.</li>
     * <li>User clicks Done on the parent screen.</li>
     * </ol>
     * Expected after fix: in-memory map AND disk file both reflect {@code minY=1, ..., betweenLayers=1}.
     * Pre-fix behaviour (the bug): values revert to {@code -1, -1, -1, -1, -1}.
     */
    @Test
    void editingGlobalDefaultsInGui_persistsToDiskAndInMemoryMap() throws Exception {
        // --- Step 1: simulate the GUI building the IntegerEntry wrappers for the 5 global
        // properties. Forge's CategoryEntry.buildChildScreen() ultimately calls
        // ConfigElement.getTypedElement(prop) for each child property. ---
        ConfigCategory dimOverridesCat = FortunateOneConfig.getDimensionConfig()
            .getCategory("dimensionoverrides");
        IConfigElement<Integer> minYEl = wrap(dimOverridesCat.get("minY"));
        IConfigElement<Integer> maxYEl = wrap(dimOverridesCat.get("maxY"));
        IConfigElement<Integer> primaryEl = wrap(dimOverridesCat.get("primaryLayers"));
        IConfigElement<Integer> secondaryEl = wrap(dimOverridesCat.get("secondaryLayers"));
        IConfigElement<Integer> betweenEl = wrap(dimOverridesCat.get("betweenLayers"));

        // Sanity: initial state matches what the user saw before editing.
        // ConfigElement.get() returns the property value as a String (Forge's choice).
        assertEquals(
            -1,
            Integer.parseInt(
                minYEl.get()
                    .toString()));
        assertEquals(
            -1,
            Integer.parseInt(
                betweenEl.get()
                    .toString()));

        // --- Step 2: user types 1 into each field, then clicks Done. The save block in
        // GuiConfig.actionPerformed iterates entries and calls saveConfigElement() on each,
        // which calls configElement.set(value). ---
        minYEl.set(1);
        maxYEl.set(1);
        primaryEl.set(1);
        secondaryEl.set(1);
        betweenEl.set(1);

        // --- Step 3: Forge fires OnConfigChangedEvent. Our CommonProxy.onConfigChanged
        // handler runs in response. We invoke it directly rather than building the
        // event object (constructor signature varies across Forge versions).
        // The handler currently calls rebuildDimensionOverrideMap() which, pre-fix,
        // invokes dimensionConfig.load() — that is the line that destroys the user edits. ---
        FortunateOneConfig.rebuildDimensionOverrideMap();

        // --- Step 4: FortunateOneGuiConfig.onGuiClosed runs. Pre-fix it calls
        // reloadFromDisk() which load()s again and clobbers the edits a second time
        // before they are ever written. The fix replaces that with save() + rebuild-from-memory. ---
        FortunateOneConfig.persistAndRebuildFromMemory();

        // --- Assertion A: in-memory override map reflects the new global defaults. ---
        FortunateOneConfig.DimensionOverride global = FortunateOneConfig
            .getDimensionOverride("some-unknown-dimension-not-in-map");
        assertNotNull(global, "global default should be present after GUI save");
        assertEquals(1, global.minY, "global minY should be 1 after GUI save, not stale -1 from disk");
        assertEquals(1, global.maxY, "global maxY should be 1 after GUI save");
        assertEquals(1, global.primaryLayers, "global primaryLayers should be 1 after GUI save");
        assertEquals(1, global.secondaryLayers, "global secondaryLayers should be 1 after GUI save");
        assertEquals(1, global.betweenLayers, "global betweenLayers should be 1 after GUI save");

        // --- Assertion B: on-disk file contains the new values, so a restart preserves them. ---
        List<String> diskLines = Files.readAllLines(configFile.toPath());
        String joined = String.join("\n", diskLines);
        assertTrue(
            joined.contains("I:minY=1") || joined.contains("minY=1"),
            "disk should contain minY=1; actual file:\n" + joined);
        assertTrue(joined.contains("I:maxY=1") || joined.contains("maxY=1"), "disk should contain maxY=1");
        assertTrue(joined.contains("primaryLayers=1"), "disk should contain primaryLayers=1");
        assertTrue(joined.contains("secondaryLayers=1"), "disk should contain secondaryLayers=1");
        assertTrue(joined.contains("betweenLayers=1"), "disk should contain betweenLayers=1");

        // --- Assertion C: a fresh Configuration loaded from disk should also see the new values,
        // proving the bug-mode scenario (process restart) is fixed. ---
        Configuration verify = new Configuration(configFile);
        verify.load();
        ConfigCategory verifyCat = verify.getCategory("dimensionoverrides");
        assertEquals(
            1,
            verifyCat.get("minY")
                .getInt(),
            "reloaded disk minY should be 1");
        assertEquals(
            1,
            verifyCat.get("maxY")
                .getInt(),
            "reloaded disk maxY should be 1");
        assertEquals(
            1,
            verifyCat.get("primaryLayers")
                .getInt(),
            "reloaded disk primaryLayers should be 1");
        assertEquals(
            1,
            verifyCat.get("secondaryLayers")
                .getInt(),
            "reloaded disk secondaryLayers should be 1");
        assertEquals(
            1,
            verifyCat.get("betweenLayers")
                .getInt(),
            "reloaded disk betweenLayers should be 1");
    }

    /**
     * Companion test for per-dimension subsection edits (e.g. {@code dimensionoverrides.overworld}).
     * Same bug mechanic, just the user drills one level deeper.
     */
    @Test
    void editingPerDimensionSubsectionInGui_persistsToDiskAndInMemoryMap() throws Exception {
        // Ensure a subsection exists at the start (same as preRegisterKnownDimensions does).
        cfg.get("dimensionoverrides.overworld", "minY", -1);
        cfg.get("dimensionoverrides.overworld", "maxY", -1);
        cfg.get("dimensionoverrides.overworld", "primaryLayers", -1);
        cfg.get("dimensionoverrides.overworld", "secondaryLayers", -1);
        cfg.get("dimensionoverrides.overworld", "betweenLayers", -1);
        cfg.save();
        // Re-bind so the in-memory state matches disk.
        FortunateOneConfig.rebuildDimensionOverrideMap();

        ConfigCategory owCat = FortunateOneConfig.getDimensionConfig()
            .getCategory("dimensionoverrides.overworld");
        IConfigElement<Integer> minYEl = wrap(owCat.get("minY"));
        IConfigElement<Integer> primaryEl = wrap(owCat.get("primaryLayers"));

        // User types 5 for minY and 7 for primaryLayers.
        minYEl.set(5);
        primaryEl.set(7);

        FortunateOneConfig.rebuildDimensionOverrideMap();
        FortunateOneConfig.persistAndRebuildFromMemory();

        FortunateOneConfig.DimensionOverride ow = FortunateOneConfig.getDimensionOverride("overworld");
        assertNotNull(ow);
        assertEquals(5, ow.minY, "overworld minY should be 5");
        assertEquals(7, ow.primaryLayers, "overworld primaryLayers should be 7");

        Configuration verify = new Configuration(configFile);
        verify.load();
        ConfigCategory verifyCat = verify.getCategory("dimensionoverrides.overworld");
        assertEquals(
            5,
            verifyCat.get("minY")
                .getInt());
        assertEquals(
            7,
            verifyCat.get("primaryLayers")
                .getInt());
    }

    /**
     * Wraps a {@link Property} the same way Forge does internally when building integer entries
     * for the GUI. Mirrors {@code net.minecraftforge.common.config.ConfigElement.getTypedElement}.
     */
    @SuppressWarnings("unchecked")
    private static IConfigElement<Integer> wrap(Property prop) {
        return (IConfigElement<Integer>) (IConfigElement<?>) new ConfigElement<Integer>(prop);
    }
}
