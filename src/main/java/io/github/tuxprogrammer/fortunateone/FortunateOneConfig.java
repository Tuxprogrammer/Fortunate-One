package io.github.tuxprogrammer.fortunateone;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

@Config(modid = FortunateOneMod.MOD_ID, filename = "fortunateone", categoryCulling = false)
@Config.LangKey("fortunateone.config")
public class FortunateOneConfig {

    @Config.Comment("Apply unlimited Fortune to GregTech (GT5U) big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean gregTechUnlimitedFortune = true;

    @Config.Comment("Apply unlimited Fortune to BartWorks big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean bartWorksUnlimitedFortune = true;

    @Config.Comment("Apply unlimited Fortune to GT++ big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean gtPlusPlusUnlimitedFortune = true;

    @Config.Comment("Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true)")
    @Config.DefaultBoolean(true)
    public static boolean affectBigOresOnly = true;

    @Config.Comment("Allow placed (non-natural) big ores to receive Fortune."
        + " GT normally zeroes fortune for placed ores; this bypasses that.")
    @Config.DefaultBoolean(true)
    public static boolean allowPlacedOreFortune = true;

    @Config.Comment("Equalize the spawn weight of all GT ore veins so every vein type has an equal chance"
        + " of being selected per worldgen attempt.")
    @Config.DefaultBoolean(true)
    public static boolean equalizeOreVeinWeights = true;

    @Config.Comment("Enable verbose [FO-DEBUG] log output for dimension override resolution and vein layer"
        + " generation. Generates one log line per ore layer per generated chunk — disable in production.")
    @Config.DefaultBoolean(false)
    public static boolean verboseDebugLogging = false;

    @Config.Ignore
    private static final Map<String, DimensionOverride> dimensionOverrideMap = new HashMap<>();

    @Config.Ignore
    private static Configuration dimensionConfig = null;

    /** Path to the config file (same one GTNHLib manages). Guessed from GTNHLib's own config. */
    @Config.Ignore
    private static File configFile = null;

    /**
     * Test-only override: when non-null, this Configuration is used instead of
     * a fresh one pointed at the same file. Set from unit tests that run
     * without a live Forge/GTNHLib environment.
     * <strong>Must be set before calling {@link #initDimensionConfig()} or
     * {@link #reloadFromDisk()}.</strong>
     */
    @Config.Ignore
    public static Configuration testConfig = null;

    @Config.Ignore
    private static final String CAT_DIM_OVERRIDES = "dimensionoverrides";

    /**
     * Test-only: directly inserts an entry into the dimension override map, bypassing
     * config file I/O. Intended for use in functional tests that need to set a specific
     * override for a dimension name without a live config file.
     * Call with {@code null} to remove an entry.
     */
    public static void putTestDimensionOverride(String dimName, DimensionOverride override) {
        String key = normalizeDimensionName(dimName);
        if (override == null) {
            dimensionOverrideMap.remove(key);
        } else {
            dimensionOverrideMap.put(key, override);
        }
    }

    /**
     * All GTNH dimensions known to spawn GT ore veins, in rough progression order.
     * Pre-registered as placeholder sub-sections in {@code fortunateone.cfg} on first startup.
     */
    @Config.Ignore
    private static final List<String> GTNH_DIMENSIONS = Arrays.asList(
        // Vanilla / classic dimensions
        "Overworld",
        "Nether",
        "The End",
        "EndAsteroid",
        // Mod dimensions
        "Twilight Forest",
        "dimensionDarkWorld",
        "Underdark",
        // GalactiCraft Core
        "moon",
        // GalactiCraft Planets
        "mars",
        "asteroids",
        // BartWorks Crossmod
        "ross128b",
        "ross128ba",
        // GalaxySpace — Solar System
        "deimos",
        "phobos",
        "callisto",
        "ceres",
        "europa",
        "ganymed",
        "iojupiter",
        "enceladus",
        "miranda",
        "oberon",
        "titan",
        // GalaxySpace — Inner planets
        "mercury",
        "venus",
        // GalaxySpace — Outer / dwarf planets
        "pluto",
        "haumea",
        "makemake",
        "kuiperbelt",
        // GalaxySpace — Other star systems
        "centauribb",
        "vega1",
        "barnarda2",
        "barnarda4",
        "barnarda5",
        "tcetie",
        "triton",
        "proteus",
        // Amun-Ra
        "neper",
        "maahes",
        "anubis",
        "horus",
        "seth",
        "asteroidbeltmehen");

    /**
     * Returns the raw Forge {@link Configuration} backing the dimension override sections,
     * or {@code null} if not yet initialised. Used by the config GUI to build
     * a drillable category element for dimension overrides.
     */
    public static Configuration getDimensionConfig() {
        return dimensionConfig;
    }

    /**
     * Persists the current in-memory state of the dimension override config to disk.
     * Since {@link #dimensionConfig} is now the same instance GTNHLib manages, GTNHLib's
     * own save path (triggered by {@code super.onGuiClosed()}) already covers our categories.
     * This method is kept for explicit save points (e.g. {@link #registerDimension}).
     */
    public static void saveDimensionConfig() {
        if (dimensionConfig != null) {
            dimensionConfig.save();
        }
    }

    /**
     * Flushes the in-memory {@link Configuration} to disk and rebuilds the override map
     * from the current in-memory state. Intended to be called from
     * {@code FortunateOneGuiConfig.onGuiClosed()} after the user clicks Done.
     *
     * <p>
     * Forge's {@code GuiConfig.actionPerformed} has already written every edited
     * {@link Property} via {@code IntegerEntry.saveConfigElement()} → {@code ConfigElement.set()}.
     * At this point the in-memory state is the source of truth; disk is stale. We must:
     * <ol>
     * <li>{@link Configuration#save()} — flush in-memory edits to disk so they survive a
     * restart. GTNHLib's auto-save proxy covers the six top-level @Config booleans but NOT
     * the {@code dimensionoverrides} category, because that category is added to the GUI
     * via a raw {@link ConfigElement} wrapper rather than a {@code IConfigElementProxy}.
     * Without this explicit save, dimension edits would live only in this JVM's memory
     * and be lost on the next launch.</li>
     * <li>{@link #rebuildDimensionOverrideMap()} — refresh {@link #dimensionOverrideMap}
     * from the (now-current) in-memory Properties so live worldgen sees the new values
     * immediately, without having to round-trip through disk.</li>
     * </ol>
     *
     * <p>
     * <strong>This method must not call {@link Configuration#load()}.</strong> Loading
     * before saving would discard the user's GUI edits; loading after saving is a no-op
     * round trip. Use {@link #reloadFromDisk()} when you actually want to re-read the file.
     */
    public static void persistAndRebuildFromMemory() {
        if (dimensionConfig == null) return;
        // Always save: hasChanged() is unreliable here because Forge's IntegerEntry.set()
        // path on a shared Property may not flip the Configuration-level changed flag in
        // every Forge version. Saving when nothing changed is a cheap no-op writer.
        dimensionConfig.save();
        rebuildDimensionOverrideMap();
    }

    /**
     * Returns the effective {@link DimensionOverride} for the given dimension name,
     * applying the three-tier precedence:
     * <ol>
     * <li>GT5U native behaviour — when a field is {@code -1} all the way down.</li>
     * <li>Global default — top-level {@code dimensionoverrides.*} properties.</li>
     * <li>Per-dimension override — {@code dimensionoverrides.<dimName>.*} sub-section.</li>
     * </ol>
     * A per-dimension field of {@code -1} falls back to the global default.
     * A global default of {@code -1} means "no override; use GT5U native behaviour".
     * Returns {@code null} if no tier has any override active.
     */
    public static DimensionOverride getDimensionOverride(String dimName) {
        DimensionOverride perDim = dimensionOverrideMap.get(normalizeDimensionName(dimName));
        DimensionOverride global = dimensionOverrideMap.get("*");

        if (perDim == null && global == null) return null;

        // No per-dim section: only return the global if it has an actual effect.
        // A global of all -1 (no override) should not be surfaced for unknown dimensions.
        if (perDim == null) {
            return (global.hasHeightOverride() || global.hasLayerOverride()) ? global : null;
        }

        if (global == null) return perDim;

        // Merge: per-dim wins per field; -1 in per-dim falls back to global default.
        int minY = perDim.minY != -1 ? perDim.minY : global.minY;
        int maxY = perDim.maxY != -1 ? perDim.maxY : global.maxY;
        int primaryLayers = perDim.primaryLayers != -1 ? perDim.primaryLayers : global.primaryLayers;
        int secondaryLayers = perDim.secondaryLayers != -1 ? perDim.secondaryLayers : global.secondaryLayers;
        int betweenLayers = perDim.betweenLayers != -1 ? perDim.betweenLayers : global.betweenLayers;
        return new DimensionOverride(minY, maxY, primaryLayers, secondaryLayers, betweenLayers);
    }

    /** Parsed representation of one dimension override. */
    public static final class DimensionOverride {

        /** Minimum Y for ore vein placement, or {@code -1} to leave unchanged. */
        public final int minY;

        /** Maximum Y for ore vein placement, or {@code -1} to leave unchanged. */
        public final int maxY;

        /** Number of primary ore layers, or {@code -1} to use the default 9-layer vein. */
        public final int primaryLayers;

        /** Number of secondary ore layers, or {@code -1} to use the default 9-layer vein. */
        public final int secondaryLayers;

        /** Number of between ore layers, or {@code -1} to use the default 9-layer vein. */
        public final int betweenLayers;

        public DimensionOverride(int minY, int maxY, int primaryLayers, int secondaryLayers, int betweenLayers) {
            this.minY = minY;
            this.maxY = maxY;
            this.primaryLayers = primaryLayers;
            this.secondaryLayers = secondaryLayers;
            this.betweenLayers = betweenLayers;
        }

        /** Returns true if this override specifies a Y range (both minY and maxY are not -1). */
        public boolean hasHeightOverride() {
            return minY != -1 && maxY != -1;
        }

        /**
         * Returns true if this override specifies all three layer counts (none are -1).
         * All three must be specified together; partial layer overrides are not supported.
         */
        public boolean hasLayerOverride() {
            return primaryLayers != -1 && secondaryLayers != -1 && betweenLayers != -1;
        }
    }

    /**
     * Initialises the dimension override config with our own {@link Configuration}
     * pointed at the same physical file that GTNHLib manages ({@code config/fortunateone.cfg}).
     * Only loads from disk; does NOT write dimension sections yet — that happens in
     * {@link #initDimensionConfigLate()} after GTNHLib's culling phase.
     * <p>
     * In unit tests (no live Forge/GTNHLib), set {@link #testConfig} to a standalone
     * {@link Configuration} before calling this method.
     */
    public static void initDimensionConfig() {
        if (testConfig != null) {
            dimensionConfig = testConfig;
        } else {
            // Use GTNHLib's OWN Configuration instance — not a new one pointed at the same file.
            // Creating a separate instance causes a two-writer conflict: when GTNHLib saves its
            // instance after GUI close it overwrites whatever we saved to disk from our instance,
            // because GTNHLib's instance has a stale snapshot of the dimension categories.
            // By sharing the same instance, GTNHLib's save path covers our categories too.
            Configuration gtnhConfig = ConfigurationManager.getConfig(FortunateOneConfig.class);
            if (gtnhConfig == null) {
                throw new IllegalStateException(
                    "GTNHLib Configuration not yet registered for FortunateOneConfig. "
                        + "Ensure ConfigurationManager.registerConfig is called before initDimensionConfig.");
            }
            configFile = gtnhConfig.getConfigFile();
            dimensionConfig = gtnhConfig; // share the instance, not just the file
        }
        // Do NOT call dimensionConfig.load() here — GTNHLib already loaded it and
        // GTNHLib's onInit() culling pass runs after this. Just read the current state.
        rebuildDimensionOverrideMap();
    }

    /**
     * Called during {@code postInit}, after GTNHLib's culling phase has completed.
     * Writes the pre-seeded dimension sections to disk for the first time.
     */
    public static void initDimensionConfigLate() {
        if (dimensionConfig == null) {
            throw new IllegalStateException("Fortunate One config has not been initialized yet.");
        }
        // Load to pick up anything written to disk since initDimensionConfig ran.
        // (GTNHLib's culling pass may have trimmed categories; we re-read the clean file.)
        dimensionConfig.load();
        writeGlobalDefaultSection();
        preRegisterKnownDimensions();
        rebuildDimensionOverrideMap();
        if (dimensionConfig.hasChanged()) {
            dimensionConfig.save();
        }
        if (verboseDebugLogging) {
            FortunateOneMod.LOG.info("[FO-DEBUG] === Dimension override map after postInit ===");
            FortunateOneMod.LOG
                .info("[FO-DEBUG]   globalDefault (key='*'): {}", formatOverride(dimensionOverrideMap.get("*")));
            for (Map.Entry<String, DimensionOverride> e : dimensionOverrideMap.entrySet()) {
                FortunateOneMod.LOG.info("[FO-DEBUG]   map['{}'] raw={}", e.getKey(), formatOverride(e.getValue()));
            }
            FortunateOneMod.LOG.info("[FO-DEBUG] === End override map ({} entries) ===", dimensionOverrideMap.size());
        }
    }

    /**
     * Debug: logs the raw property values in the dimensionoverrides category
     * directly from the Configuration object (not from the override map).
     */
    public static void logDimensionOverridesState(String prefix) {
        if (!verboseDebugLogging) return;
        if (dimensionConfig == null) {
            FortunateOneMod.LOG.info("{} dimensionConfig=null", prefix);
            return;
        }
        if (!dimensionConfig.hasCategory(CAT_DIM_OVERRIDES)) {
            FortunateOneMod.LOG.info("{} category '{}' does not exist", prefix, CAT_DIM_OVERRIDES);
            return;
        }
        net.minecraftforge.common.config.ConfigCategory cat = dimensionConfig.getCategory(CAT_DIM_OVERRIDES);
        FortunateOneMod.LOG.info(
            "{} dimensionoverrides top-level: minY={} maxY={} primaryLayers={} secondaryLayers={} betweenLayers={} | instanceId={}",
            prefix,
            cat.containsKey("minY") ? cat.get("minY")
                .getInt(-999) : "(absent)",
            cat.containsKey("maxY") ? cat.get("maxY")
                .getInt(-999) : "(absent)",
            cat.containsKey("primaryLayers") ? cat.get("primaryLayers")
                .getInt(-999) : "(absent)",
            cat.containsKey("secondaryLayers") ? cat.get("secondaryLayers")
                .getInt(-999) : "(absent)",
            cat.containsKey("betweenLayers") ? cat.get("betweenLayers")
                .getInt(-999) : "(absent)",
            System.identityHashCode(dimensionConfig));
        if (dimensionConfig.hasCategory(CAT_DIM_OVERRIDES + ".overworld")) {
            net.minecraftforge.common.config.ConfigCategory ow = dimensionConfig
                .getCategory(CAT_DIM_OVERRIDES + ".overworld");
            FortunateOneMod.LOG.info(
                "{} dimensionoverrides.overworld: primaryLayers={} secondaryLayers={} betweenLayers={}",
                prefix,
                ow.containsKey("primaryLayers") ? ow.get("primaryLayers")
                    .getInt(-999) : "(absent)",
                ow.containsKey("secondaryLayers") ? ow.get("secondaryLayers")
                    .getInt(-999) : "(absent)",
                ow.containsKey("betweenLayers") ? ow.get("betweenLayers")
                    .getInt(-999) : "(absent)");
        }
    }

    /** Debug helper: formats a DimensionOverride as a compact string. */
    private static String formatOverride(DimensionOverride o) {
        if (o == null) return "null";
        return String.format(
            "Y=[%d,%d] layers=sec:%d bet:%d pri:%d hasLayer=%b hasHeight=%b",
            o.minY,
            o.maxY,
            o.secondaryLayers,
            o.betweenLayers,
            o.primaryLayers,
            o.hasLayerOverride(),
            o.hasHeightOverride());
    }

    /** Reloads all Fortunate One config values from disk. */
    public static void reloadFromDisk() {
        if (dimensionConfig == null) {
            throw new IllegalStateException("Fortunate One config has not been initialized yet.");
        }
        logDimensionOverridesState("[FO-DEBUG] reloadFromDisk PRE-load");
        // Re-read the shared GTNHLib-managed config file from disk.
        dimensionConfig.load();
        logDimensionOverridesState("[FO-DEBUG] reloadFromDisk POST-load");
        // Reload main options from the general category.
        reloadMainOptions(dimensionConfig);
        // Ensure global-default properties are present (idempotent if already written).
        writeGlobalDefaultSection();
        logDimensionOverridesState("[FO-DEBUG] reloadFromDisk POST-writeGlobal");
        // Pre-seed any new dimensions and rebuild the override map.
        preRegisterKnownDimensions();
        rebuildDimensionOverrideMap();
        FortunateOneMod.LOG.info(
            "[Fortunate One] Config reloaded from disk. GT={}, BW={}, GTPP={}, bigOnly={}, placed={}, equalWeights={}",
            gregTechUnlimitedFortune,
            bartWorksUnlimitedFortune,
            gtPlusPlusUnlimitedFortune,
            affectBigOresOnly,
            allowPlacedOreFortune,
            equalizeOreVeinWeights);
    }

    private static void reloadMainOptions(Configuration config) {
        gregTechUnlimitedFortune = config.getBoolean(
            "gregTechUnlimitedFortune",
            "general",
            true,
            "Apply unlimited Fortune to GregTech (GT5U) big ore drops.");
        bartWorksUnlimitedFortune = config.getBoolean(
            "bartWorksUnlimitedFortune",
            "general",
            true,
            "Apply unlimited Fortune to BartWorks big ore drops.");
        gtPlusPlusUnlimitedFortune = config.getBoolean(
            "gtPlusPlusUnlimitedFortune",
            "general",
            true,
            "Apply unlimited Fortune to GT++ big ore drops.");
        affectBigOresOnly = config.getBoolean(
            "affectBigOresOnly",
            "general",
            true,
            "Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true)");
        allowPlacedOreFortune = config.getBoolean(
            "allowPlacedOreFortune",
            "general",
            true,
            "Allow placed (non-natural) big ores to receive Fortune. GT normally zeroes fortune for placed ores; this bypasses that.");
        equalizeOreVeinWeights = config.getBoolean(
            "equalizeOreVeinWeights",
            "general",
            true,
            "Equalize the spawn weight of all GT ore veins so every vein type has an equal chance of being selected per worldgen attempt.");
    }

    /**
     * Writes the five global-default properties directly under the top-level
     * {@code dimensionoverrides} category (not a sub-section).
     * Values default to {@code -1} (no override / use GT5U native behaviour).
     * This call is idempotent — existing user values are never overwritten.
     * <p>
     * These act as the middle tier of the three-level precedence:
     * <em>GT5U defaults ← global defaults here ← per-dimension sub-section overrides.</em>
     */
    private static void writeGlobalDefaultSection() {
        if (dimensionConfig == null) return;
        dimensionConfig.getInt(
            "minY",
            CAT_DIM_OVERRIDES,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Global default minimum Y for ore vein placement. -1 = no override (use GT5U behaviour). "
                + "Per-dimension sub-sections below take precedence over this value.");
        dimensionConfig.getInt(
            "maxY",
            CAT_DIM_OVERRIDES,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Global default maximum Y for ore vein placement. -1 = no override (use GT5U behaviour).");
        dimensionConfig.getInt(
            "primaryLayers",
            CAT_DIM_OVERRIDES,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Global default primary ore layers. -1 = no override. Set all three layer fields together.");
        dimensionConfig.getInt(
            "secondaryLayers",
            CAT_DIM_OVERRIDES,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Global default secondary ore layers. -1 = no override. Set all three layer fields together.");
        dimensionConfig.getInt(
            "betweenLayers",
            CAT_DIM_OVERRIDES,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Global default between ore layers. -1 = no override. Set all three layer fields together.");
        if (dimensionConfig.hasCategory(CAT_DIM_OVERRIDES)) {
            dimensionConfig.getCategory(CAT_DIM_OVERRIDES)
                .setComment(
                    "Dimension override settings. The five properties at the TOP of this section are "
                        + "GLOBAL DEFAULTS applied to every dimension. A value of -1 means \"no override; "
                        + "use GT5U native behaviour\". Sub-sections below (one per dimension) take "
                        + "precedence: a per-dimension -1 falls back to the global default, and a "
                        + "global default of -1 falls back to GT5U native behaviour.");
        }
    }

    /**
     * Writes placeholder sub-sections for all {@link #GTNH_DIMENSIONS} in one batch.
     * Skips any dimension that already has a sub-section (user-configured or previously written).
     * Saves once at the end only if anything was actually added.
     */
    private static void preRegisterKnownDimensions() {
        if (dimensionConfig == null) return;
        for (String dimName : GTNH_DIMENSIONS) {
            // Forge lowercases category names on disk; normalize here so hasCategory()
            // correctly detects existing user-edited sections like "overworld".
            String category = CAT_DIM_OVERRIDES + "." + normalizeDimensionName(dimName);
            if (dimensionConfig.hasCategory(category)) continue;
            writeDefaultDimensionSection(category);
        }
        if (dimensionConfig.hasChanged()) {
            dimensionConfig.save();
            FortunateOneMod.LOG
                .info("[Fortunate One] Pre-registered {} known GTNH dimension(s) in config.", GTNH_DIMENSIONS.size());
        }
    }

    /**
     * Registers a dimension in the config file the first time the mod encounters it during worldgen.
     * Adds a {@code dimensionOverrides.<dimName>} sub-section with all fields at {@code -1}
     * (no override) and saves immediately so users can edit values between restarts.
     *
     * <p>
     * Called from {@link io.github.tuxprogrammer.fortunateone.mixins.MixinWorldgenGTOreLayer}
     * on the first vein generation attempt in each dimension.
     */
    public static void registerDimension(String dimName) {
        if (dimensionConfig == null) return;
        // Normalize to lowercase: Forge Configuration lowercases all category names
        // when writing to disk, so hasCategory() must use the same case.
        String category = CAT_DIM_OVERRIDES + "." + normalizeDimensionName(dimName);
        // Do NOT call dimensionConfig.load() here — that would clobber any in-memory
        // user edits (e.g. overworld 1 1 1) that haven't been re-read from disk yet.
        if (dimensionConfig.hasCategory(category)) return;
        writeDefaultDimensionSection(category);
        dimensionOverrideMap.put(normalizeDimensionName(dimName), new DimensionOverride(-1, -1, -1, -1, -1));
        if (dimensionConfig.hasChanged()) {
            dimensionConfig.save();
        }
        FortunateOneMod.LOG.info("[Fortunate One] Added config section for new dimension: '{}'", dimName);
    }

    /** Writes the five default {@code -1} properties into a dimension sub-section. */
    private static void writeDefaultDimensionSection(String category) {
        dimensionConfig.getInt(
            "minY",
            category,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Minimum Y for ore vein placement, or -1 to leave unchanged.");
        dimensionConfig.getInt(
            "maxY",
            category,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Maximum Y for ore vein placement, or -1 to leave unchanged.");
        dimensionConfig.getInt(
            "primaryLayers",
            category,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Primary ore layers. Set all three layer fields together, or all at -1 for no layer override.");
        dimensionConfig.getInt(
            "secondaryLayers",
            category,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Secondary ore layers. Set all three layer fields together, or all at -1 for no layer override.");
        dimensionConfig.getInt(
            "betweenLayers",
            category,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Between ore layers. Set all three layer fields together, or all at -1 for no layer override.");
    }

    /**
     * Rebuilds {@link #dimensionOverrideMap} from the current contents of {@code fortunateone.cfg}.
     *
     * <ul>
     * <li>Properties written directly in the {@code dimensionOverrides} category act as a global
     * default (stored under the {@code "*"} key) and apply to every dimension without its own
     * sub-section.</li>
     * <li>Sub-sections ({@code dimensionOverrides.<dimName>}) configure individual dimensions and
     * take precedence over the global default.</li>
     * </ul>
     *
     * Called after config is loaded at startup and after in-game GUI config changes.
     */
    public static void rebuildDimensionOverrideMap() {
        dimensionOverrideMap.clear();
        if (dimensionConfig == null) return;

        // DO NOT call dimensionConfig.load() here. This method is invoked from the
        // ConfigChangedEvent handler immediately after the user clicks Done in the GUI;
        // load() would re-read the (still stale) disk contents and clobber the in-memory
        // Property values that Forge's GuiConfig just wrote in saveConfigElements(). The
        // result was the bug where GUI edits to dimensionoverrides.* reverted to -1.
        // Disk reloads belong in {@link #reloadFromDisk()}, which is the only public
        // entry point for "discard in-memory state and re-read the file".

        // Read global default from top-level dimensionOverrides properties (only if set by user).
        if (dimensionConfig.hasCategory(CAT_DIM_OVERRIDES)) {
            ConfigCategory globalCat = dimensionConfig.getCategory(CAT_DIM_OVERRIDES);
            if (globalCat.containsKey("minY")) {
                int minY = readProp(globalCat, "minY");
                int maxY = readProp(globalCat, "maxY");
                int primaryLayers = readProp(globalCat, "primaryLayers");
                int secondaryLayers = readProp(globalCat, "secondaryLayers");
                int betweenLayers = readProp(globalCat, "betweenLayers");
                dimensionOverrideMap
                    .put("*", new DimensionOverride(minY, maxY, primaryLayers, secondaryLayers, betweenLayers));
                FortunateOneMod.LOG.info(
                    "[Fortunate One] Global dimension default: Y=[{},{}] layers=primary:{} secondary:{} between:{}",
                    minY,
                    maxY,
                    primaryLayers,
                    secondaryLayers,
                    betweenLayers);
            }
        }

        // Read per-dimension sub-sections.
        for (String catName : dimensionConfig.getCategoryNames()) {
            if (!catName.startsWith(CAT_DIM_OVERRIDES + ".")) continue;
            String dimName = catName.substring((CAT_DIM_OVERRIDES + ".").length());
            if (dimName.contains(".")) continue; // skip deeper nesting
            ConfigCategory dimCat = dimensionConfig.getCategory(catName);
            int minY = readProp(dimCat, "minY");
            int maxY = readProp(dimCat, "maxY");
            int primaryLayers = readProp(dimCat, "primaryLayers");
            int secondaryLayers = readProp(dimCat, "secondaryLayers");
            int betweenLayers = readProp(dimCat, "betweenLayers");
            DimensionOverride override = new DimensionOverride(
                minY,
                maxY,
                primaryLayers,
                secondaryLayers,
                betweenLayers);
            // Always store — a per-dimension section shadows the global default even when all -1.
            dimensionOverrideMap.put(normalizeDimensionName(dimName), override);
            if (override.hasHeightOverride() || override.hasLayerOverride()) {
                int total = primaryLayers + secondaryLayers + betweenLayers;
                FortunateOneMod.LOG.info(
                    "[Fortunate One] Dimension override: dim='{}' Y=[{},{}] layers=primary:{} secondary:{} between:{} (total {})",
                    dimName,
                    minY,
                    maxY,
                    primaryLayers,
                    secondaryLayers,
                    betweenLayers,
                    total > 0 ? total : "(height-only)");
            }
        }

        if (!dimensionOverrideMap.isEmpty()) {
            FortunateOneMod.LOG.info("[Fortunate One] {} dimension override(s) active.", dimensionOverrideMap.size());
        }
    }

    private static String normalizeDimensionName(String dimName) {
        return dimName.toLowerCase(Locale.ROOT);
    }

    /** Reads an integer property from a {@link ConfigCategory}, defaulting to {@code -1} if absent. */
    private static int readProp(ConfigCategory cat, String key) {
        Property prop = cat.get(key);
        return prop != null ? prop.getInt(-1) : -1;
    }
}
