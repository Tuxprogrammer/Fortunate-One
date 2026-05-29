package io.github.tuxprogrammer.fortunateone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.ILayerGeneratorAccess;
import io.github.tuxprogrammer.fortunateone.VeinGenState;

/**
 * Tests for {@code MixinLayerGenerator.fortuneone$interceptGenerateLayer}.
 *
 * <p>
 * Each test constructs a {@code WorldgenGTOreLayer$LayerGenerator} directly via reflection,
 * installs a {@link VeinGenState} into the {@link VeinGenState#CURRENT} ThreadLocal, then calls
 * {@link ILayerGeneratorAccess#callGenerateLayer} one or more times. The mixin intercept fires on
 * each such call just as it would during real worldgen; we observe the outcome through
 * {@link ILayerGeneratorAccess#getLevel()} (level advancement == a layer was dispatched) and
 * {@link VeinGenState#callIndex} (counts every intercepted call, including skips).
 *
 * <p>
 * The test LayerGenerator uses a 0×0 empty tile (east limit == west limit, south == north), so the
 * inner loops in {@code generateLayer} never execute. No ore blocks are placed and the real Minecraft
 * world is never accessed, which prevents {@code OreManager.setOreForWorldGen} from inadvertently
 * triggering chunk population — which would call {@code fortuneone$initVeinState} and replace
 * {@link VeinGenState#CURRENT} mid-test. The only effect of the original {@code generateLayer} body
 * that matters here is the unconditional {@code level++} at the end, which confirms a layer was
 * dispatched.
 */
class LayerGeneratorTest {

    // -------------------------------------------------------------------------
    // Reflection handles (set up once for all tests)
    // -------------------------------------------------------------------------

    private static Constructor<?> lgCtor;
    private static Field fWorld;
    private static Field fRng;
    private static Field fLimitWestX;
    private static Field fLimitEastX;
    private static Field fLimitNorthZ;
    private static Field fLimitSouthZ;
    private static Field fVeinWestX;
    private static Field fVeinEastX;
    private static Field fVeinNorthZ;
    private static Field fVeinSouthZ;
    private static Field fLocalDensity;
    private static Field fPlaceCount;

    /** Outer WorldgenGTOreLayer instance required by the inner-class constructor. */
    private static WorldgenGTOreLayer vein;

    @BeforeAll
    static void initReflection() throws Exception {
        vein = WorldgenGTOreLayer.sList.stream()
            .filter(v -> v.mPrimary != null && v.mSecondary != null && v.mBetween != null)
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "No GT5U ore vein with non-null primary, secondary, and between ores found in sList. "
                        + "GT5U may not have initialised correctly."));

        Class<?> lgClass = Class.forName("gregtech.common.WorldgenGTOreLayer$LayerGenerator");

        // Non-static inner class: implicit constructor takes the outer instance.
        lgCtor = lgClass.getDeclaredConstructor(WorldgenGTOreLayer.class);
        lgCtor.setAccessible(true);

        fWorld = accessible(lgClass, "world");
        fRng = accessible(lgClass, "rng");
        fLimitWestX = accessible(lgClass, "limitWestX");
        fLimitEastX = accessible(lgClass, "limitEastX");
        fLimitNorthZ = accessible(lgClass, "limitNorthZ");
        fLimitSouthZ = accessible(lgClass, "limitSouthZ");
        fVeinWestX = accessible(lgClass, "veinWestX");
        fVeinEastX = accessible(lgClass, "veinEastX");
        fVeinNorthZ = accessible(lgClass, "veinNorthZ");
        fVeinSouthZ = accessible(lgClass, "veinSouthZ");
        fLocalDensity = accessible(lgClass, "localDensity");
        fPlaceCount = accessible(lgClass, "placeCount");
    }

    private static Field accessible(Class<?> cls, String name) throws NoSuchFieldException {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @AfterEach
    void clearState() {
        VeinGenState.CURRENT.remove();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code LayerGenerator}, populates all fields, and returns it as
     * {@link ILayerGeneratorAccess}.
     *
     * <p>
     * The tile is intentionally empty ({@code limitEastX == limitWestX},
     * {@code limitSouthZ == limitNorthZ}), so the inner loops in {@code generateLayer} never
     * execute. The only observable effect of each dispatched call is {@code level++}, which is
     * sufficient to verify that the correct number of layers were placed vs. skipped. No blocks are
     * placed, so {@code world} is never accessed and can safely be {@code null}.
     */
    private ILayerGeneratorAccess newLayerGenerator(int initialLevel) throws Exception {
        Object lg = lgCtor.newInstance(vein);

        fWorld.set(lg, null);
        fRng.set(lg, new Random(0L));
        // Empty tile: east == west and south == north, so the inner loops never execute.
        fLimitWestX.set(lg, 0);
        fLimitEastX.set(lg, 0);
        fLimitNorthZ.set(lg, 0);
        fLimitSouthZ.set(lg, 0);
        fVeinWestX.set(lg, 0);
        fVeinEastX.set(lg, 1);
        fVeinNorthZ.set(lg, 0);
        fVeinSouthZ.set(lg, 1);
        fLocalDensity.set(lg, 1);
        fPlaceCount.set(lg, new int[4]);

        ILayerGeneratorAccess access = (ILayerGeneratorAccess) lg;
        access.setLevel(initialLevel);
        return access;
    }

    /**
     * Creates a {@link VeinGenState} for the given override and installs it in
     * {@link VeinGenState#CURRENT}. The caller holds the returned reference; the state
     * object survives after the ThreadLocal is cleaned up at the end of
     * {@code executeWorldgenChunkified}, so assertions against it remain valid.
     */
    private VeinGenState setOverride(DimensionOverride override) {
        VeinGenState state = new VeinGenState(override, new Random(0L));
        VeinGenState.CURRENT.set(state);
        return state;
    }

    // -------------------------------------------------------------------------
    // Branch B: state == null → pass through
    // -------------------------------------------------------------------------

    /**
     * When no {@link VeinGenState} is present the mixin returns immediately, so all 8 calls
     * reach the original {@code generateLayer} body and {@code level} advances 8 times.
     */
    @Test
    void noState_callsPassThroughAndLevelAdvances() throws Exception {
        VeinGenState.CURRENT.remove();
        ILayerGeneratorAccess lg = newLayerGenerator(50);

        for (int i = 0; i < 8; i++) {
            lg.callGenerateLayer(false, false, false);
        }

        assertEquals(58, lg.getLevel(), "Level must advance once per call when there is no override");
        assertNull(VeinGenState.CURRENT.get(), "CURRENT must remain null");
    }

    // -------------------------------------------------------------------------
    // Branches C + D + F: height-only override, range > 0
    // -------------------------------------------------------------------------

    /**
     * On the first call the mixin adjusts {@code level} to a value within the configured Y range,
     * sets {@code heightAdjusted = true}, and stores a reference to this LayerGenerator in
     * {@code state.generator}. It then lets the original body run (no cancel), so
     * {@code callIndex} is never incremented. On the second call, no further height adjustment
     * takes place.
     */
    @Test
    void heightOnlyOverride_rangePositive_adjustsLevelOnFirstCallOnly() throws Exception {
        // range = maxY - minY - 5 = 55 > 0
        DimensionOverride override = new DimensionOverride(20, 80, -1, -1, -1);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(50);

        lg.callGenerateLayer(false, false, false);

        assertTrue(state.heightAdjusted, "heightAdjusted must be true after first call");
        assertEquals(0, state.callIndex, "callIndex must not be touched for height-only override");
        assertSame(lg, state.generator, "state.generator must reference this LayerGenerator after first call");

        // level was set to [minY-1 .. minY+range-1], then body increments → [minY .. minY+range]
        int level = lg.getLevel();
        int rangeEnd = override.minY + (override.maxY - override.minY - 5);
        assertTrue(
            level >= override.minY && level <= rangeEnd,
            "Level " + level + " not in expected range [" + override.minY + ", " + rangeEnd + "]");

        // Second call: height adjustment must NOT fire again
        int levelAfterFirst = level;
        lg.callGenerateLayer(false, false, false);
        assertEquals(levelAfterFirst + 1, lg.getLevel(), "Level must advance by exactly 1 on the second call");
    }

    // -------------------------------------------------------------------------
    // Branches C + E + F: height-only override, range <= 0
    // -------------------------------------------------------------------------

    /**
     * When {@code maxY - minY - 5 <= 0} the mixin uses the deterministic path
     * {@code level = minY - 1}. After the original body runs, level equals {@code minY}.
     */
    @Test
    void heightOnlyOverride_rangeZeroOrNegative_setsLevelToMinYMinusOne() throws Exception {
        // range = 24 - 20 - 5 = -1 <= 0
        DimensionOverride override = new DimensionOverride(20, 24, -1, -1, -1);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(50);

        lg.callGenerateLayer(false, false, false);

        assertTrue(state.heightAdjusted);
        // inject sets level = minY - 1 = 19; body increments → 20
        assertEquals(override.minY, lg.getLevel(), "Level must equal minY after first call with range <= 0");
    }

    // -------------------------------------------------------------------------
    // Placeholder override (all -1): both flags false → pass through
    // -------------------------------------------------------------------------

    /**
     * An all-{@code -1} override has neither a height nor a layer override active. The mixin
     * returns early after registering the generator reference, letting the original body run
     * normally.
     */
    @Test
    void placeholderOverride_allMinusOne_actsAsPassThrough() throws Exception {
        DimensionOverride override = new DimensionOverride(-1, -1, -1, -1, -1);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 8; i++) {
            lg.callGenerateLayer(false, false, false);
        }

        assertEquals(0, state.callIndex, "callIndex must not be incremented for placeholder override");
        assertEquals(48, lg.getLevel(), "Level must advance 8 times");
    }

    // -------------------------------------------------------------------------
    // Branch H: layer override, idx < secEnd → dispatch SECONDARY
    // -------------------------------------------------------------------------

    /**
     * With only secondary layers configured, the first {@code secondaryLayers} calls dispatch
     * {@code generateLayer(true, false, false)} through the recursion guard, advancing
     * {@code level} once per dispatch. The first call beyond the total is a skip: {@code callIndex}
     * still increments but {@code level} does not advance.
     */
    @Test
    void layerOverride_secondaryOnly_correctDispatchAndSkip() throws Exception {
        // sec=3, bet=0, pri=0 → secEnd=3, betEnd=3, priEnd=3
        DimensionOverride override = new DimensionOverride(-1, -1, 0, 3, 0);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 3; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(3, state.callIndex);
        assertEquals(43, lg.getLevel(), "Level must advance once per secondary dispatch");

        // Call 4: idx == priEnd (3) → skip
        lg.callGenerateLayer(false, false, false);
        assertEquals(4, state.callIndex, "callIndex increments even on a skip");
        assertEquals(43, lg.getLevel(), "Level must not advance on a skipped call");
    }

    // -------------------------------------------------------------------------
    // Branch I: layer override, idx in [secEnd, betEnd) → dispatch BETWEEN
    // -------------------------------------------------------------------------

    @Test
    void layerOverride_betweenOnly_correctDispatch() throws Exception {
        // sec=0, bet=3, pri=0 → secEnd=0, betEnd=3, priEnd=3
        DimensionOverride override = new DimensionOverride(-1, -1, 0, 0, 3);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 3; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(3, state.callIndex);
        assertEquals(43, lg.getLevel());
    }

    // -------------------------------------------------------------------------
    // Branch J: layer override, idx in [betEnd, priEnd) → dispatch PRIMARY
    // -------------------------------------------------------------------------

    @Test
    void layerOverride_primaryOnly_correctDispatch() throws Exception {
        // sec=0, bet=0, pri=3 → secEnd=0, betEnd=0, priEnd=3
        DimensionOverride override = new DimensionOverride(-1, -1, 3, 0, 0);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 3; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(3, state.callIndex);
        assertEquals(43, lg.getLevel());
    }

    // -------------------------------------------------------------------------
    // Branch G (explicit): idx >= priEnd → skip without advancing level
    // -------------------------------------------------------------------------

    /**
     * After all configured layers have been dispatched, every further call is a skip:
     * {@code callIndex} still increments (the cancel fires before the check) but
     * {@code level} freezes.
     */
    @Test
    void layerOverride_excessCallsBeyondTotal_skipWithoutAdvancingLevel() throws Exception {
        // total=3; after 3 dispatches, 5 excess calls → 8 total, matching GT's loop count
        DimensionOverride override = new DimensionOverride(-1, -1, 1, 1, 1);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 3; i++) lg.callGenerateLayer(false, false, false);
        assertEquals(43, lg.getLevel());

        for (int i = 0; i < 5; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(8, state.callIndex, "callIndex must equal total calls including skips");
        assertEquals(43, lg.getLevel(), "Level must not advance after all configured layers are placed");
    }

    // -------------------------------------------------------------------------
    // Mixed H + I + J + G: total < 8
    // -------------------------------------------------------------------------

    /**
     * A full sec→bet→pri block with total=6 produces 6 level advances and 2 skips across
     * GT's fixed 8-call loop.
     */
    @Test
    void layerOverride_mixedTotal6_correct8CallSequence() throws Exception {
        // sec=2 (idx 0-1), bet=2 (idx 2-3), pri=2 (idx 4-5), skip (idx 6-7)
        DimensionOverride override = new DimensionOverride(-1, -1, 2, 2, 2);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 8; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(8, state.callIndex);
        assertEquals(46, lg.getLevel(), "Level advances for all 6 dispatched layers; 2 skips must not advance it");
    }

    // -------------------------------------------------------------------------
    // total == 8: no skips, no extras needed
    // -------------------------------------------------------------------------

    @Test
    void layerOverride_total8_allCallsDispatchedNoSkips() throws Exception {
        // sec=3 (idx 0-2), bet=3 (idx 3-5), pri=2 (idx 6-7)
        DimensionOverride override = new DimensionOverride(-1, -1, 2, 3, 3);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 8; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(8, state.callIndex);
        assertEquals(48, lg.getLevel(), "All 8 calls dispatched; level must advance 8 times");
    }

    // -------------------------------------------------------------------------
    // total > 8: interceptGenerateLayer handles indices beyond GT's 8-call loop
    // -------------------------------------------------------------------------

    /**
     * When the total exceeds 8, in production {@code fortuneone$addExtraLayers} issues the
     * calls for indices 8+. This test exercises the same code path by calling
     * {@code callGenerateLayer} 11 times directly, confirming that
     * {@code interceptGenerateLayer} dispatches all indices correctly beyond position 7.
     */
    @Test
    void layerOverride_total11_extraCallsDispatchedCorrectly() throws Exception {
        // sec=4 (idx 0-3), bet=3 (idx 4-6), pri=4 (idx 7-10) → total=11
        DimensionOverride override = new DimensionOverride(-1, -1, 4, 4, 3);
        VeinGenState state = setOverride(override);
        ILayerGeneratorAccess lg = newLayerGenerator(40);

        for (int i = 0; i < 11; i++) lg.callGenerateLayer(false, false, false);

        assertEquals(11, state.callIndex);
        assertEquals(51, lg.getLevel(), "All 11 calls dispatched; level must advance 11 times");
    }
}
