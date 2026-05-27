package io.github.tuxprogrammer.fortunateone;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = FortunateOneMod.MOD_ID, filename = "fortunateone")
@Config.LangKey("fortunateone.config")
public class FortunateOneConfig {

    @Config.Comment("Master toggle: enable unlimited Fortune mode for big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean enableUnlimitedFortuneMode = true;

    @Config.Comment("Apply unlimited Fortune to GT (gregtech) big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean applyToGT = true;

    @Config.Comment("Apply unlimited Fortune to BartWorks big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean applyToBW = true;

    @Config.Comment("Apply unlimited Fortune to GT++ big ore drops.")
    @Config.DefaultBoolean(true)
    public static boolean applyToGTPP = true;

    @Config.Comment("Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true)")
    @Config.DefaultBoolean(true)
    public static boolean affectBigOresOnly = true;

    @Config.Comment("Allow placed (non-natural) big ores to receive Fortune when unlimited mode is enabled."
        + " GT normally zeroes fortune for placed ores; this bypasses that.")
    @Config.DefaultBoolean(true)
    public static boolean allowPlacedOreFortune = true;

    @Config.Comment("Equalize the spawn weight of all GT ore veins so every vein type has an equal chance"
        + " of being selected per worldgen attempt.")
    @Config.DefaultBoolean(true)
    public static boolean equalizeOreVeinWeights = true;

    @Config.Comment({ "Per-dimension overrides for ore vein Y range and layer structure.",
        "Format: \"DimensionName:minY:maxY:primaryLayers:secondaryLayers:betweenLayers\"",
        "Use \"*\" as the dimension name for a global default (lower priority than exact name).",
        "Use -1 for any field to leave it unchanged. Layer overrides require all three",
        "layer counts to be set (none -1). Any total layer count is supported (not capped at 9).",
        "Layer order bottom-to-top: secondary, between, primary.",
        "Examples: \"*:10:60:-1:-1:-1\", \"Nether:20:50:3:3:2\", \"Overworld:5:80:10:5:10\"" })
    @Config.DefaultStringList({})
    public static String[] dimensionOverrides = {};

    @Config.Ignore
    private static final Map<String, DimensionOverride> dimensionOverrideMap = new HashMap<>();

    /**
     * Returns the effective {@link DimensionOverride} for the given dimension name.
     * Checks for an exact name match first, then falls back to the {@code "*"} wildcard.
     * Returns {@code null} if no override is configured.
     */
    public static DimensionOverride getDimensionOverride(String dimName) {
        DimensionOverride exact = dimensionOverrideMap.get(dimName);
        if (exact != null) return exact;
        return dimensionOverrideMap.get("*");
    }

    /** Parsed representation of one {@code dimensionOverrides} entry. */
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
     * Rebuilds {@link #dimensionOverrideMap} from the current value of {@link #dimensionOverrides}.
     * Called after config is loaded (either by gtnhlib at startup or via the in-game GUI), and also
     * by {@link #synchronizeConfiguration} for the integration test path.
     */
    public static void rebuildDimensionOverrideMap() {
        dimensionOverrideMap.clear();
        for (String entry : dimensionOverrides) {
            String[] parts = entry.split(":", 6);
            if (parts.length != 6) {
                FortunateOneMod.LOG.warn("[Fortunate One] Skipping malformed dimensionOverride entry: '{}'", entry);
                continue;
            }
            try {
                String dimName = parts[0].trim();
                int minY = Integer.parseInt(parts[1].trim());
                int maxY = Integer.parseInt(parts[2].trim());
                int primaryLayers = Integer.parseInt(parts[3].trim());
                int secondaryLayers = Integer.parseInt(parts[4].trim());
                int betweenLayers = Integer.parseInt(parts[5].trim());
                dimensionOverrideMap
                    .put(dimName, new DimensionOverride(minY, maxY, primaryLayers, secondaryLayers, betweenLayers));
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
            } catch (NumberFormatException ignored) {
                FortunateOneMod.LOG
                    .warn("[Fortunate One] Skipping dimensionOverride entry with non-integer field: '{}'", entry);
            }
        }
        if (!dimensionOverrideMap.isEmpty()) {
            FortunateOneMod.LOG.info("[Fortunate One] {} dimension override(s) active.", dimensionOverrideMap.size());
        }
    }

    public static void synchronizeConfiguration(File configFile) {
        Configuration cfg = new Configuration(configFile);

        cfg.load();

        enableUnlimitedFortuneMode = cfg.getBoolean(
            "enableUnlimitedFortuneMode",
            Configuration.CATEGORY_GENERAL,
            true,
            "Remove the Fortune 3 cap on big ore drops when GT drop mode is FortuneItem.");

        applyToGT = cfg.getBoolean(
            "applyToGT",
            Configuration.CATEGORY_GENERAL,
            true,
            "Apply unlimited Fortune to gregtech (GT5U) big ore drops.");

        applyToBW = cfg.getBoolean(
            "applyToBW",
            Configuration.CATEGORY_GENERAL,
            true,
            "Apply unlimited Fortune to BartWorks big ore drops.");

        applyToGTPP = cfg.getBoolean(
            "applyToGTPP",
            Configuration.CATEGORY_GENERAL,
            true,
            "Apply unlimited Fortune to GT++ big ore drops.");

        affectBigOresOnly = cfg.getBoolean(
            "affectBigOresOnly",
            Configuration.CATEGORY_GENERAL,
            true,
            "Only affect big ores. Small ores always use vanilla GT behavior. (Recommended: true)");

        allowPlacedOreFortune = cfg.getBoolean(
            "allowPlacedOreFortune",
            Configuration.CATEGORY_GENERAL,
            true,
            "Allow placed (non-natural) big ores to receive Fortune when unlimited mode is enabled."
                + " GT normally zeroes fortune for placed ores; this bypasses that.");

        equalizeOreVeinWeights = cfg.getBoolean(
            "equalizeOreVeinWeights",
            Configuration.CATEGORY_GENERAL,
            true,
            "Equalize the spawn weight of all GT ore veins so every vein type has an equal chance"
                + " of being selected per worldgen attempt.");

        dimensionOverrides = cfg.getStringList(
            "dimensionOverrides",
            Configuration.CATEGORY_GENERAL,
            new String[0],
            "Per-dimension overrides for ore vein Y range and layer structure.\n"
                + "Format: \"DimensionName:minY:maxY:primaryLayers:secondaryLayers:betweenLayers\"\n"
                + "Use \"*\" as the dimension name for a global default (lower priority than exact name).\n"
                + "Use -1 for any field to leave it unchanged. Layer overrides require all three\n"
                + "layer counts to be set (none -1). Any total layer count is supported (not capped at 9).\n"
                + "Layer order bottom-to-top: secondary, between, primary.\n"
                + "Examples: \"*:10:60:-1:-1:-1\", \"Nether:20:50:3:3:2\"");

        rebuildDimensionOverrideMap();

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }
}
