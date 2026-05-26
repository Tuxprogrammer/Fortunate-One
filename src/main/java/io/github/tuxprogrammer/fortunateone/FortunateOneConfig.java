package io.github.tuxprogrammer.fortunateone;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class FortunateOneConfig {

    /** Master toggle: enable unlimited Fortune mode for big ore drops. */
    public static boolean enableUnlimitedFortuneMode = true;

    /** Apply unlimited Fortune to GT (gregtech) big ore drops. */
    public static boolean applyToGT = true;

    /** Apply unlimited Fortune to BartWorks big ore drops. */
    public static boolean applyToBW = true;

    /** Apply unlimited Fortune to GT++ big ore drops. */
    public static boolean applyToGTPP = true;

    /**
     * When true, only big ores are affected (small ores always use vanilla GT behavior).
     * This is always true; small ores are never intercepted by this addon.
     */
    public static boolean affectBigOresOnly = true;

    /**
     * When true, placed (non-natural) ores also receive the fortune bonus.
     * GT normally zeroes fortune for non-natural ores; this bypasses that zero.
     */
    public static boolean allowPlacedOreFortune = true;

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

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }
}
