package io.github.tuxprogrammer.fortunateone;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraftforge.common.config.Configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cpw.mods.fml.relauncher.FMLInjectionData;

class FortunateOneConfigReloadTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUpForgeConfigHome() throws ReflectiveOperationException {
        Field minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        minecraftHome.set(null, tempDir.toFile());
    }

    @AfterEach
    void clearTestConfig() {
        FortunateOneConfig.testConfig = null;
    }

    @Test
    void reloadFromDisk_updatesMainOptionsAndDimensionOverrides() {
        File configFile = tempDir.resolve("config")
            .resolve("fortunateone.cfg")
            .toFile();

        // Create and inject a test Configuration backed by the temp file.
        Configuration testCfg = new Configuration(configFile);
        testCfg.load();
        FortunateOneConfig.testConfig = testCfg;

        writeConfig(configFile, true, 10, 50, 3, 2, 1);
        // Load the config from testCfg (which is backed by the now-written file).
        testCfg.load();
        FortunateOneConfig.initDimensionConfig();

        writeConfig(configFile, false, 20, 90, 4, 3, 2);
        FortunateOneConfig.reloadFromDisk();

        assertFalse(FortunateOneConfig.gregTechUnlimitedFortune);
        assertFalse(FortunateOneConfig.bartWorksUnlimitedFortune);
        assertFalse(FortunateOneConfig.gtPlusPlusUnlimitedFortune);
        assertFalse(FortunateOneConfig.affectBigOresOnly);
        assertFalse(FortunateOneConfig.allowPlacedOreFortune);
        assertFalse(FortunateOneConfig.equalizeOreVeinWeights);

        FortunateOneConfig.DimensionOverride override = FortunateOneConfig.getDimensionOverride("testdim");
        assertNotNull(override);
        assertEquals(20, override.minY);
        assertEquals(90, override.maxY);
        assertEquals(4, override.primaryLayers);
        assertEquals(3, override.secondaryLayers);
        assertEquals(2, override.betweenLayers);

        FortunateOneConfig.DimensionOverride seeded = FortunateOneConfig.getDimensionOverride("Overworld");
        assertNotNull(seeded, "known GTNH dimensions should remain pre-seeded after reload");
        assertEquals(-1, seeded.minY);
        assertEquals(-1, seeded.maxY);
    }

    private static void writeConfig(File configFile, boolean enabled, int minY, int maxY, int primaryLayers,
        int secondaryLayers, int betweenLayers) {
        try {
            Files.createDirectories(
                configFile.toPath()
                    .getParent());
            Files.deleteIfExists(configFile.toPath());
        } catch (IOException e) {
            throw new AssertionError("Failed to reset test config file", e);
        }
        Configuration config = new Configuration(configFile);
        config.load();
        config.get("general", "gregTechUnlimitedFortune", true)
            .set(enabled);
        config.get("general", "bartWorksUnlimitedFortune", true)
            .set(enabled);
        config.get("general", "gtPlusPlusUnlimitedFortune", true)
            .set(enabled);
        config.get("general", "affectBigOresOnly", true)
            .set(enabled);
        config.get("general", "allowPlacedOreFortune", true)
            .set(enabled);
        config.get("general", "equalizeOreVeinWeights", true)
            .set(enabled);

        String category = "dimensionOverrides.TestDim";
        config.get(category, "minY", -1)
            .set(minY);
        config.get(category, "maxY", -1)
            .set(maxY);
        config.get(category, "primaryLayers", -1)
            .set(primaryLayers);
        config.get(category, "secondaryLayers", -1)
            .set(secondaryLayers);
        config.get(category, "betweenLayers", -1)
            .set(betweenLayers);
        config.save();
    }
}
