package io.github.tuxprogrammer.fortunateone.client;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneMod;

public class FortunateOneGuiConfig extends SimpleGuiConfig {

    public FortunateOneGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, FortunateOneMod.MOD_ID, FortunateOneMod.MOD_NAME, false, FortunateOneConfig.class);
    }
}
