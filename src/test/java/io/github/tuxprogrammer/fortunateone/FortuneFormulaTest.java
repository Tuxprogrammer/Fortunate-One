package io.github.tuxprogrammer.fortunateone;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FortuneDropCalculator}.
 *
 * <p>
 * Requirements verified:
 * <ul>
 * <li>Req 3 (verify parity): capped == uncapped for Fortune 0, 1, 2, 3 (same random seed).</li>
 * <li>Req 4 (unclamped): uncapped can produce more than capped for Fortune 4, 10, 50.</li>
 * <li>Fortune 0 always returns base amount (richMult for GT/BW, 1 for GT++).</li>
 * <li>Non-fortune modes are unaffected (covered by formula returning base amount at fortune=0).</li>
 * </ul>
 */
class FortuneFormulaTest {

    // Fixed seed for deterministic tests
    private static final long SEED = 0xDEADBEEFL;

    // -----------------------------------------------------------------------
    // GT / BW formula tests
    // -----------------------------------------------------------------------

    @Test
    void gtFortuneZero_returnsRichMult() {
        // Fortune 0: always returns richMult regardless of RNG
        assertEquals(1, FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(SEED), 0, 1));
        assertEquals(2, FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(SEED), 0, 2));
    }

    @Test
    void gtCappedAndUncapped_parityForFortune1() {
        // With same RNG seed, capped and uncapped should produce identical results for fortune <= 3
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTBigOreDropCount(r1, 1, 1),
            FortuneDropCalculator.uncappedGTBigOreDropCount(r2, 1, 1),
            "Fortune 1: capped and uncapped must be identical");
    }

    @Test
    void gtCappedAndUncapped_parityForFortune2() {
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTBigOreDropCount(r1, 2, 1),
            FortuneDropCalculator.uncappedGTBigOreDropCount(r2, 2, 1),
            "Fortune 2: capped and uncapped must be identical");
    }

    @Test
    void gtCappedAndUncapped_parityForFortune3() {
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTBigOreDropCount(r1, 3, 1),
            FortuneDropCalculator.uncappedGTBigOreDropCount(r2, 3, 1),
            "Fortune 3: capped and uncapped must be identical");
    }

    @Test
    void gtUncapped_canExceedCappedForFortune4() {
        // Over many trials, the uncapped average for Fortune 4 must be > capped (Fortune 3) average.
        // Capped max = 3*(0+1)=3 for richMult=1, avg = ~1.5
        // Uncapped Fortune 4: nextInt(6)-1 clamped to 0, avg addedDrops ~1.67, avg amount ~2.67
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            long seed = i;
            cappedSum += FortuneDropCalculator.cappedGTBigOreDropCount(new Random(seed), 4, 1);
            uncappedSum += FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(seed), 4, 1);
        }
        double cappedAvg = cappedSum / trials;
        double uncappedAvg = uncappedSum / trials;
        assertTrue(
            uncappedAvg > cappedAvg,
            "Fortune 4: uncapped avg (" + uncappedAvg + ") must exceed capped avg (" + cappedAvg + ")");
    }

    @Test
    void gtUncapped_canExceedCappedForFortune10() {
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            cappedSum += FortuneDropCalculator.cappedGTBigOreDropCount(new Random(i), 10, 1);
            uncappedSum += FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(i), 10, 1);
        }
        assertTrue(uncappedSum / trials > cappedSum / trials, "Fortune 10: uncapped average must exceed capped");
    }

    @Test
    void gtUncapped_canExceedCappedForFortune50() {
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            cappedSum += FortuneDropCalculator.cappedGTBigOreDropCount(new Random(i), 50, 1);
            uncappedSum += FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(i), 50, 1);
        }
        assertTrue(uncappedSum / trials > cappedSum / trials, "Fortune 50: uncapped average must exceed capped");
    }

    @Test
    void gtUncapped_richMultDoubles() {
        // Rich stone should double all counts
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        int normal = FortuneDropCalculator.uncappedGTBigOreDropCount(r1, 5, 1);
        int rich = FortuneDropCalculator.uncappedGTBigOreDropCount(r2, 5, 2);
        assertEquals(normal * 2, rich, "Rich stone must produce exactly 2x normal drops");
    }

    @Test
    void gtUncapped_neverBelowRichMult() {
        // Drop count must never be less than richMult (even with bad RNG)
        for (int fortune = 1; fortune <= 50; fortune++) {
            for (int seed = 0; seed < 1000; seed++) {
                int count = FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(seed), fortune, 1);
                assertTrue(count >= 1, "GT uncapped drop count must be >= 1 for fortune=" + fortune);
                int richCount = FortuneDropCalculator.uncappedGTBigOreDropCount(new Random(seed), fortune, 2);
                assertTrue(richCount >= 2, "GT uncapped rich drop count must be >= 2 for fortune=" + fortune);
            }
        }
    }

    // -----------------------------------------------------------------------
    // GT++ formula tests
    // -----------------------------------------------------------------------

    @Test
    void gtppFortuneZero_returnsOne() {
        assertEquals(1L, FortuneDropCalculator.uncappedGTPPBigOreDropCount(new Random(SEED), 0));
    }

    @Test
    void gtppCappedAndUncapped_parityForFortune1() {
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTPPBigOreDropCount(r1, 1),
            FortuneDropCalculator.uncappedGTPPBigOreDropCount(r2, 1),
            "Fortune 1: capped and uncapped must be identical");
    }

    @Test
    void gtppCappedAndUncapped_parityForFortune2() {
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTPPBigOreDropCount(r1, 2),
            FortuneDropCalculator.uncappedGTPPBigOreDropCount(r2, 2),
            "Fortune 2: capped and uncapped must be identical");
    }

    @Test
    void gtppCappedAndUncapped_parityForFortune3() {
        Random r1 = new Random(SEED);
        Random r2 = new Random(SEED);
        assertEquals(
            FortuneDropCalculator.cappedGTPPBigOreDropCount(r1, 3),
            FortuneDropCalculator.uncappedGTPPBigOreDropCount(r2, 3),
            "Fortune 3: capped and uncapped must be identical");
    }

    @Test
    void gtppUncapped_canExceedCappedForFortune4() {
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            cappedSum += FortuneDropCalculator.cappedGTPPBigOreDropCount(new Random(i), 4);
            uncappedSum += FortuneDropCalculator.uncappedGTPPBigOreDropCount(new Random(i), 4);
        }
        assertTrue(uncappedSum / trials > cappedSum / trials, "Fortune 4: GT++ uncapped average must exceed capped");
    }

    @Test
    void gtppUncapped_canExceedCappedForFortune10() {
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            cappedSum += FortuneDropCalculator.cappedGTPPBigOreDropCount(new Random(i), 10);
            uncappedSum += FortuneDropCalculator.uncappedGTPPBigOreDropCount(new Random(i), 10);
        }
        assertTrue(uncappedSum / trials > cappedSum / trials, "Fortune 10: GT++ uncapped average must exceed capped");
    }

    @Test
    void gtppUncapped_canExceedCappedForFortune50() {
        double cappedSum = 0, uncappedSum = 0;
        int trials = 10_000;
        for (int i = 0; i < trials; i++) {
            cappedSum += FortuneDropCalculator.cappedGTPPBigOreDropCount(new Random(i), 50);
            uncappedSum += FortuneDropCalculator.uncappedGTPPBigOreDropCount(new Random(i), 50);
        }
        assertTrue(uncappedSum / trials > cappedSum / trials, "Fortune 50: GT++ uncapped average must exceed capped");
    }

    @Test
    void gtppUncapped_neverBelowOne() {
        for (int fortune = 1; fortune <= 50; fortune++) {
            for (int seed = 0; seed < 1000; seed++) {
                long count = FortuneDropCalculator.uncappedGTPPBigOreDropCount(new Random(seed), fortune);
                assertTrue(count >= 1L, "GT++ uncapped must be >= 1 for fortune=" + fortune);
            }
        }
    }
}
