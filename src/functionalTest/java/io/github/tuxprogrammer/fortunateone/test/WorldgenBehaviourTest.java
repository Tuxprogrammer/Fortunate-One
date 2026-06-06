package io.github.tuxprogrammer.fortunateone.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;

/**
 * Integration tests for Fortunate One's worldgen behaviour.
 *
 * <p>
 * These tests run inside the live Forge server environment (via the
 * {@link FortunateOneTestMod} runner), so GT5U is fully initialised and all mixins
 * are active by the time {@code FMLServerStartedEvent} fires.
 */
class WorldgenBehaviourTest {

    private boolean savedEqualizeOreVeinWeights;

    @BeforeEach
    void saveConfig() {
        savedEqualizeOreVeinWeights = FortunateOneConfig.equalizeOreVeinWeights;
    }

    @AfterEach
    void restoreConfig() {
        FortunateOneConfig.equalizeOreVeinWeights = savedEqualizeOreVeinWeights;
    }

    // -------------------------------------------------------------------------
    // GT5U worldgen registration
    // -------------------------------------------------------------------------

    /**
     * Sanity check: GT5U must have registered at least one ore vein by the time the
     * server has started. An empty list means GT5U failed to initialise, which would
     * make all subsequent worldgen tests meaningless.
     */
    @Test
    void gt5OreVeinsAreRegistered() {
        assertFalse(
            WorldgenGTOreLayer.sList.isEmpty(),
            "WorldgenGTOreLayer.sList is empty — GT5U did not register any ore veins. "
                + "Check that GT5U is present at runtime (devOnlyNonPublishable in dependencies.gradle).");
    }

    // -------------------------------------------------------------------------
    // Ore vein weight equalization — tests that MixinWorldgenGTOreLayer loaded
    // -------------------------------------------------------------------------

    /**
     * When {@code equalizeOreVeinWeights} is {@code true} (the default), every call to
     * {@link WorldgenGTOreLayer#getWeight()} must return {@code 10}.
     *
     * <p>
     * This also serves as a smoke test that {@code MixinWorldgenGTOreLayer} was
     * loaded correctly by SpongeMixin. If the mixin failed silently, the original
     * {@code mWeight} values (which vary per vein) would be returned instead.
     */
    @Test
    void weightEqualizationEnabledReturnsTenForAllVeins() {
        FortunateOneConfig.equalizeOreVeinWeights = true;
        final List<WorldgenGTOreLayer> veins = WorldgenGTOreLayer.sList;
        for (int i = 0; i < veins.size(); i++) {
            final WorldgenGTOreLayer vein = veins.get(i);
            assertEquals(
                10,
                vein.getWeight(),
                "Vein at sList[" + i
                    + "] returned weight "
                    + vein.getWeight()
                    + " instead of 10 while equalizeOreVeinWeights=true. "
                    + "MixinWorldgenGTOreLayer may not have been applied.");
        }
    }

    /**
     * When {@code equalizeOreVeinWeights} is {@code false}, {@link WorldgenGTOreLayer#getWeight()}
     * must return the vein's original {@code mWeight} value. At least one vein must have
     * {@code mWeight > 1}, confirming that vanilla GT5U does not assign weight 1 to everything.
     */
    @Test
    void weightEqualizationDisabledPreservesOriginalWeights() {
        FortunateOneConfig.equalizeOreVeinWeights = false;
        boolean foundHighWeight = false;
        for (WorldgenGTOreLayer vein : WorldgenGTOreLayer.sList) {
            if (vein.getWeight() > 1) {
                foundHighWeight = true;
                break;
            }
        }
        assertTrue(
            foundHighWeight,
            "When equalizeOreVeinWeights=false, at least one GT5U vein must have weight > 1. "
                + "If all veins return 1, MixinWorldgenGTOreLayer is stuck in the always-return-1 path.");
    }

    // -------------------------------------------------------------------------
    // DimensionOverride value object logic (no config file I/O required)
    // -------------------------------------------------------------------------

    /** A full override (all five fields set) should have both flags true. */
    @Test
    void dimensionOverrideFullOverrideHasBothFlags() {
        DimensionOverride full = new DimensionOverride(10, 60, 4, 3, 2);
        assertTrue(full.hasHeightOverride(), "Full override must report hasHeightOverride()");
        assertTrue(full.hasLayerOverride(), "Full override must report hasLayerOverride()");
        assertEquals(10, full.minY);
        assertEquals(60, full.maxY);
        assertEquals(4, full.primaryLayers);
        assertEquals(3, full.secondaryLayers);
        assertEquals(2, full.betweenLayers);
    }

    /** A height-only override (layer counts all -1) should have only the height flag. */
    @Test
    void dimensionOverrideHeightOnlyHasOnlyHeightFlag() {
        DimensionOverride heightOnly = new DimensionOverride(5, 50, -1, -1, -1);
        assertTrue(heightOnly.hasHeightOverride(), "Height-only override must report hasHeightOverride()");
        assertFalse(heightOnly.hasLayerOverride(), "Height-only override must not report hasLayerOverride()");
    }

    /** A layer-only override (Y range both -1) should have only the layer flag. */
    @Test
    void dimensionOverrideLayerOnlyHasOnlyLayerFlag() {
        DimensionOverride layerOnly = new DimensionOverride(-1, -1, 4, 3, 2);
        assertFalse(layerOnly.hasHeightOverride(), "Layer-only override must not report hasHeightOverride()");
        assertTrue(layerOnly.hasLayerOverride(), "Layer-only override must report hasLayerOverride()");
    }

    /**
     * Known GTNH ore-bearing dimensions are pre-seeded as placeholder sections. Each placeholder
     * shadows the global default with all fields set to {@code -1}.
     */
    @Test
    void knownDimensionsArePreSeededWithPlaceholderOverrides() {
        java.io.File tmp;
        try {
            tmp = java.io.File.createTempFile("fortunateone-test", ".cfg");
            tmp.deleteOnExit();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not create temp config file", e);
        }
        net.minecraftforge.common.config.Configuration testCfg = new net.minecraftforge.common.config.Configuration(
            tmp);
        testCfg.load();
        FortunateOneConfig.testConfig = testCfg;
        FortunateOneConfig.initDimensionConfig();
        FortunateOneConfig.initDimensionConfigLate();
        FortunateOneConfig.testConfig = null;

        DimensionOverride overworld = FortunateOneConfig.getDimensionOverride("Overworld");
        assertNotNull(overworld, "known GTNH dimensions should be pre-seeded");
        assertEquals(-1, overworld.minY);
        assertEquals(-1, overworld.maxY);
        assertEquals(-1, overworld.primaryLayers);
        assertEquals(-1, overworld.secondaryLayers);
        assertEquals(-1, overworld.betweenLayers);
        assertNull(
            FortunateOneConfig.getDimensionOverride("Unknown Test Dimension"),
            "unknown dimensions without sections should still have no override");
    }
}
