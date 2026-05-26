package io.github.tuxprogrammer.fortunateone;

import java.util.Random;

/**
 * Pure-Java formula implementations for uncapped big-ore Fortune drops.
 *
 * <p>
 * The {@code uncapped*} methods replicate the GT5U/BW/GT++ FortuneItem formula exactly,
 * but without the {@code if (fortune > 3) fortune = 3} cap. This class has no Minecraft
 * dependencies and is fully unit-testable.
 *
 * <p>
 * The {@code capped*} methods reproduce the vanilla GT behavior for parity comparison.
 */
public final class FortuneDropCalculator {

    private FortuneDropCalculator() {}

    // -----------------------------------------------------------------------
    // GT / BartWorks big-ore formula
    // -----------------------------------------------------------------------
    // Vanilla GT formula (capped at 3):
    // if fortune > 0:
    // fortune = min(fortune, 3)
    // addedDrops = max(0, nextInt(fortune + 2) - 1)
    // amount = richMultiplier * (addedDrops + 1)
    // else:
    // amount = richMultiplier
    //
    // Uncapped (this addon): same formula, fortune not clamped.

    /**
     * GT/BW vanilla capped formula. Returns the drop count.
     *
     * @param random   RNG
     * @param fortune  Fortune enchantment level (0 = no fortune)
     * @param richMult 2 for rich-stone ores, 1 otherwise
     */
    public static int cappedGTBigOreDropCount(Random random, int fortune, int richMult) {
        if (fortune > 0) {
            int f = Math.min(fortune, 3);
            int addedDrops = Math.max(0, random.nextInt(f + 2) - 1);
            return richMult * (addedDrops + 1);
        }
        return richMult;
    }

    /**
     * GT/BW uncapped formula (Fortunate One). Returns the drop count.
     *
     * @param random   RNG
     * @param fortune  Fortune enchantment level (0 = no fortune)
     * @param richMult 2 for rich-stone ores, 1 otherwise
     */
    public static int uncappedGTBigOreDropCount(Random random, int fortune, int richMult) {
        if (fortune > 0) {
            // No cap applied here
            int addedDrops = Math.max(0, random.nextInt(fortune + 2) - 1);
            return richMult * (addedDrops + 1);
        }
        return richMult;
    }

    // -----------------------------------------------------------------------
    // GT++ big-ore formula
    // -----------------------------------------------------------------------
    // Vanilla GT++ formula (capped at 3):
    // if fortune > 0:
    // fortune = min(fortune, 3)
    // amount = nextInt(fortune) + 1
    // else:
    // amount = 1
    //
    // Uncapped (this addon): same, fortune not clamped.

    /**
     * GT++ vanilla capped formula. Returns the drop count.
     *
     * @param random  RNG
     * @param fortune Fortune enchantment level
     */
    public static long cappedGTPPBigOreDropCount(Random random, int fortune) {
        if (fortune > 0) {
            int f = Math.min(fortune, 3);
            return (long) random.nextInt(f) + 1;
        }
        return 1;
    }

    /**
     * GT++ uncapped formula (Fortunate One). Returns the drop count.
     *
     * @param random  RNG
     * @param fortune Fortune enchantment level
     */
    public static long uncappedGTPPBigOreDropCount(Random random, int fortune) {
        if (fortune > 0) {
            // No cap applied here
            return (long) random.nextInt(fortune) + 1;
        }
        return 1;
    }
}
