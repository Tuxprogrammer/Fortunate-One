package io.github.tuxprogrammer.fortunateone;

import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        try {
            ConfigurationManager.registerConfig(FortunateOneConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Fortunate One config", e);
        }
        FortunateOneConfig.rebuildDimensionOverrideMap();
        MinecraftForge.EVENT_BUS.register(this);
        FortunateOneMod.LOG.info("[Fortunate One] Version " + Tags.VERSION + " initializing.");
        FortunateOneMod.LOG
            .info("[Fortunate One] Unlimited Fortune mode: {}", FortunateOneConfig.enableUnlimitedFortuneMode);
        FortunateOneMod.LOG.info(
            "[Fortunate One] Apply to GT={}, BW={}, GTPP={}",
            FortunateOneConfig.applyToGT,
            FortunateOneConfig.applyToBW,
            FortunateOneConfig.applyToGTPP);
        FortunateOneMod.LOG
            .info("[Fortunate One] Allow placed ore fortune: {}", FortunateOneConfig.allowPlacedOreFortune);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (FortunateOneMod.MOD_ID.equals(event.modID)) {
            FortunateOneMod.LOG.info(
                "[Fortunate One] In-game config change detected; reloading dimension overrides."
                    + " Changes apply to newly generated chunks only.");
            FortunateOneConfig.rebuildDimensionOverrideMap();
        }
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
