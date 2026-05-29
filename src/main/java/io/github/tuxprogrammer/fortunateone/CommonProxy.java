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
        FortunateOneConfig.initDimensionConfig();
        MinecraftForge.EVENT_BUS.register(this);
        FortunateOneMod.LOG.info("[Fortunate One] Version " + Tags.VERSION + " initializing.");
        FortunateOneMod.LOG.info(
            "[Fortunate One] Unlimited fortune — GT={}, BW={}, GTPP={}",
            FortunateOneConfig.gregTechUnlimitedFortune,
            FortunateOneConfig.bartWorksUnlimitedFortune,
            FortunateOneConfig.gtPlusPlusUnlimitedFortune);
        FortunateOneMod.LOG
            .info("[Fortunate One] Allow placed ore fortune: {}", FortunateOneConfig.allowPlacedOreFortune);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (FortunateOneMod.MOD_ID.equals(event.modID)) {
            // This event fires from inside GuiConfig.actionPerformed BEFORE the screen switch
            // that triggers onGuiClosed. We must NOT call dimensionConfig.load() here (or
            // anything that does) — Forge has just written the user's GUI edits into the
            // in-memory Properties and disk is still stale. rebuildDimensionOverrideMap()
            // is load-free since the fix; it simply re-derives the override map from the
            // current in-memory Configuration. The actual save-to-disk happens later in
            // FortunateOneGuiConfig.onGuiClosed via persistAndRebuildFromMemory().
            FortunateOneMod.LOG.info(
                "[Fortunate One] In-game config change detected; refreshing dimension override map."
                    + " Changes apply to newly generated chunks only.");
            FortunateOneConfig.rebuildDimensionOverrideMap();
        }
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        FortunateOneConfig.initDimensionConfigLate();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandFortunateOne());
    }
}
