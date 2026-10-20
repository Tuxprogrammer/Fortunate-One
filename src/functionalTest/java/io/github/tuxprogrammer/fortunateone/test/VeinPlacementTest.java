package io.github.tuxprogrammer.fortunateone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import galacticgreg.api.enums.DimensionDef;
import gregtech.api.objects.XSTR;
import gregtech.api.world.GTWorldgen;
import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.VeinGenState;
import io.github.tuxprogrammer.fortunateone.mixins.AccessorVeinPlacement;

/**
 * Exercises the GT5U 5.09.54 vein pipeline ({@code resolveVeinPlacement} →
 * {@code testWorldgenChunkified}) end to end on the live Overworld, with the real mixins applied.
 *
 * <p>
 * Every test uses GT's dry-run path, so no blocks are placed. Coordinates are the spawn chunk,
 * which the server keeps loaded, so block reads cannot trigger nested chunk generation.
 */
class VeinPlacementTest {

    private static World world;
    private static String dimName;
    private static WorldgenGTOreLayer vein;
    private static Method resolveVeinPlacement;
    private static Method testWorldgenChunkified;
    private static int chunkX;
    private static int chunkZ;

    @BeforeAll
    static void init() throws Exception {
        world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        dimName = DimensionDef.getDimensionName(world);
        vein = WorldgenGTOreLayer.sList.stream()
            .filter(v -> v.canGenerateIn(dimName) && v.mPrimary != null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No GT5U ore vein can generate in " + dimName));

        Class<?> placementClass = Class.forName("gregtech.common.WorldgenGTOreLayer$VeinPlacement");
        resolveVeinPlacement = WorldgenGTOreLayer.class.getDeclaredMethod(
            "resolveVeinPlacement",
            World.class,
            XSTR.class,
            int.class,
            int.class,
            int.class,
            int.class);
        resolveVeinPlacement.setAccessible(true);
        testWorldgenChunkified = WorldgenGTOreLayer.class.getDeclaredMethod(
            "testWorldgenChunkified",
            World.class,
            XSTR.class,
            String.class,
            int.class,
            int.class,
            int.class,
            int.class,
            placementClass);
        testWorldgenChunkified.setAccessible(true);

        ChunkCoordinates spawn = world.getSpawnPoint();
        chunkX = (spawn.posX >> 4) << 4;
        chunkZ = (spawn.posZ >> 4) << 4;
    }

    @AfterEach
    void restoreOverrides() {
        FortunateOneConfig.rebuildDimensionOverrideMap();
        VeinGenState.CURRENT.remove();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Resolves a placement the same way GTWorldgenerator does, with the oreseed on this chunk. */
    private static Object resolve(long seed) throws Exception {
        return resolveVeinPlacement.invoke(vein, world, new XSTR(seed), chunkX, chunkZ, chunkX, chunkZ);
    }

    private static int veinMinY(Object placement) {
        return ((AccessorVeinPlacement) placement).fortuneone$getVeinMinY();
    }

    /** Resolves a placement for {@code seed} and dry-runs it on a fresh RNG, as GTWorldgenerator does. */
    private static int dryRun(long seed) throws Exception {
        Object placement = resolve(seed);
        return (int) testWorldgenChunkified
            .invoke(vein, world, new XSTR(seed), "None", chunkX, chunkZ, chunkX, chunkZ, placement);
    }

    private static boolean hasMethodContaining(Class<?> cls, String name) {
        return Arrays.stream(cls.getDeclaredMethods())
            .anyMatch(
                m -> m.getName()
                    .contains(name));
    }

    // -------------------------------------------------------------------------
    // Mixin application
    // -------------------------------------------------------------------------

    /**
     * The mixin config is {@code required: false}, so a target signature change in GT5U only logs a
     * warning and silently drops the whole mixin class. This test fails loudly instead: every
     * injector handler must have been merged into its target class.
     */
    @Test
    void allMixinHandlersAreMergedIntoTargets() throws Exception {
        String[][] expected = {
            { "gregtech.common.WorldgenGTOreLayer", "fortuneone$equalizeWeight", "fortuneone$overrideVeinHeight",
                "fortuneone$initVeinState", "fortuneone$addExtraLayers", "fortuneone$cleanVeinState" },
            { "gregtech.common.WorldgenGTOreLayer$LayerGenerator", "fortuneone$interceptGenerateLayer" },
            { "gregtech.common.ores.GTOreAdapter", "fortuneone$getOreDrops" },
            { "gregtech.common.ores.BWOreAdapter", "fortuneone$getOreDrops" },
            { "gregtech.common.ores.GTPPOreAdapter", "fortuneone$getOreDrops" },
            { "gregtech.common.WorldgenGTOreLayer$VeinPlacement", "fortuneone$getVeinMinY", "fortuneone$setVeinMinY",
                "fortuneone$getGenerationSeed" } };
        for (String[] row : expected) {
            Class<?> target = Class.forName(row[0]);
            for (int i = 1; i < row.length; i++) {
                assertTrue(
                    hasMethodContaining(target, row[i]),
                    row[0] + " is missing mixin handler "
                        + row[i]
                        + " — check the log for 'Mixin apply for mod fortunateone failed'");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Y range override (applied to VeinPlacement)
    // -------------------------------------------------------------------------

    @Test
    void heightOverride_placesEveryVeinInsideConfiguredRange() throws Exception {
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(20, 80, -1, -1, -1));

        Set<Integer> seen = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            int y = veinMinY(resolve(seed));
            assertTrue(y >= 20 && y < 75, "seed " + seed + " produced veinMinY " + y + ", expected [20, 75)");
            seen.add(y);
        }
        assertTrue(seen.size() > 1, "veinMinY must vary across seeds, got only " + seen);
    }

    /**
     * GT caches one placement per oreseed and replays it for every chunk, so the override must be a
     * pure function of the placement's generation seed.
     */
    @Test
    void heightOverride_isDeterministicPerSeed() throws Exception {
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(20, 80, -1, -1, -1));

        Object placement = resolve(42L);
        long generationSeed = ((AccessorVeinPlacement) placement).fortuneone$getGenerationSeed();

        assertEquals(veinMinY(placement), veinMinY(resolve(42L)), "Same seed must give the same veinMinY");
        assertEquals(20 + new Random(generationSeed).nextInt(55), veinMinY(placement));
    }

    @Test
    void heightOverride_rangeTooSmall_usesMinY() throws Exception {
        // range = 24 - 20 - 5 = -1 <= 0
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(20, 24, -1, -1, -1));

        assertEquals(20, veinMinY(resolve(7L)));
    }

    @Test
    void noHeightOverride_leavesGtPlacementUntouched() throws Exception {
        assumeTrue(
            DimensionDef.getDefForWorld(world)
                .respectsOreVeinHeights(),
            dimName + " does not use configured vein heights");
        // A per-dimension all -1 section shadows any global default.
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(-1, -1, -1, -1, -1));

        int minY = vein.getMinY(dimName);
        int maxY = vein.getMaxY(dimName);
        for (long seed = 0; seed < 50; seed++) {
            // GT's own formula: the first draw from rng in resolveVeinPlacement.
            int expected = minY + new XSTR(seed).nextInt(maxY - minY - 5);
            assertEquals(expected, veinMinY(resolve(seed)), "seed " + seed);
        }
    }

    // -------------------------------------------------------------------------
    // Layer override (applied in generateWithPlacement / generateLayer)
    // -------------------------------------------------------------------------

    /**
     * A 0/0/0 layer override cancels every one of GT's {@code generateLayer} calls, so the dry run
     * can never place anything and must report {@code NO_OVERLAP_AIR_BLOCK}. The control run with
     * no layer override, at the same stone-filled depth, must place ore for at least one seed. This
     * proves the {@code generateWithPlacement} hooks fire on GT's real code path.
     */
    @Test
    void layerOverride_zeroLayers_suppressesPlacementOnRealPath() throws Exception {
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(10, 30, -1, -1, -1));
        boolean controlPlaced = false;
        for (long seed = 0; seed < 20 && !controlPlaced; seed++) {
            controlPlaced = dryRun(seed) == GTWorldgen.ORE_PLACED;
        }
        assertTrue(controlPlaced, "Control dry run never placed ore at Y 10-30 in the spawn chunk");

        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(10, 30, 0, 0, 0));
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(GTWorldgen.NO_OVERLAP_AIR_BLOCK, dryRun(seed), "seed " + seed);
            assertNull(VeinGenState.CURRENT.get(), "VeinGenState must be cleared after generateWithPlacement");
        }
    }

    /** A layer override above GT's 9 calls runs the extra-layer hook and still cleans up. */
    @Test
    void layerOverride_moreThanNineLayers_completesAndCleansUp() throws Exception {
        FortunateOneConfig.putTestDimensionOverride(dimName, new DimensionOverride(10, 30, 4, 4, 4));

        boolean placed = false;
        for (long seed = 0; seed < 20; seed++) {
            placed |= dryRun(seed) == GTWorldgen.ORE_PLACED;
            assertNull(VeinGenState.CURRENT.get(), "VeinGenState must be cleared after generateWithPlacement");
        }
        assertTrue(placed, "A 12-layer vein at Y 10-30 must place ore for at least one seed");
    }
}
