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

@Config(modid = FortunateOneMod.MOD_ID, filename = "fortunateone")
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
     * Returns the effective {@link DimensionOverride} for the given dimension name.
     * Checks for an exact name match first, then falls back to the {@code "*"} global default.
     * Returns {@code null} if no override is configured.
     */
    public static DimensionOverride getDimensionOverride(String dimName) {
        DimensionOverride exact = dimensionOverrideMap.get(normalizeDimensionName(dimName));
        if (exact != null) return exact;
        return dimensionOverrideMap.get("*");
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
            // Extract the file path from GTNHLib's own Configuration object.
            Configuration gtnhConfig = ConfigurationManager.getConfig(FortunateOneConfig.class);
            if (gtnhConfig == null) {
                throw new IllegalStateException(
                    "GTNHLib Configuration not yet registered for FortunateOneConfig. "
                        + "Ensure ConfigurationManager.registerConfig is called before initDimensionConfig.");
            }
            configFile = gtnhConfig.getConfigFile();
            dimensionConfig = new Configuration(configFile);
        }
        dimensionConfig.load();
        // Do NOT write (save) here — GTNHLib's onInit() will cull unknown categories
        // and save over us. We just load to read the current state into the override map.
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
        dimensionConfig.load();
        preRegisterKnownDimensions();
        rebuildDimensionOverrideMap();
    }

    /** Reloads all Fortunate One config values from disk. */
    public static void reloadFromDisk() {
        if (dimensionConfig == null) {
            throw new IllegalStateException("Fortunate One config has not been initialized yet.");
        }
        // Re-read the shared GTNHLib-managed config file from disk.
        dimensionConfig.load();
        // Reload main options from the general category.
        reloadMainOptions(dimensionConfig);
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
     * Writes placeholder sub-sections for all {@link #GTNH_DIMENSIONS} in one batch.
     * Skips any dimension that already has a sub-section (user-configured or previously written).
     * Saves once at the end only if anything was actually added.
     */
    private static void preRegisterKnownDimensions() {
        if (dimensionConfig == null) return;
        for (String dimName : GTNH_DIMENSIONS) {
            String category = CAT_DIM_OVERRIDES + "." + dimName;
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
        String category = CAT_DIM_OVERRIDES + "." + dimName;
        // Reload to pick up any external edits before we potentially write.
        dimensionConfig.load();
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

        dimensionConfig.load();

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
