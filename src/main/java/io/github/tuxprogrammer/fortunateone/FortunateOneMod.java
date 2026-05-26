package io.github.tuxprogrammer.fortunateone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = FortunateOneMod.MOD_ID,
    version = Tags.VERSION,
    name = FortunateOneMod.MOD_NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech")
public class FortunateOneMod {

    public static final String MOD_ID = "fortunateone";
    public static final String MOD_NAME = "Fortunate One";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(
        clientSide = "io.github.tuxprogrammer.fortunateone.ClientProxy",
        serverSide = "io.github.tuxprogrammer.fortunateone.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
