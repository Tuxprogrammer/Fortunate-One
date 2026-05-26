package io.github.tuxprogrammer.fortunateone;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        FortunateOneConfig.synchronizeConfiguration(event.getSuggestedConfigurationFile());
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

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
