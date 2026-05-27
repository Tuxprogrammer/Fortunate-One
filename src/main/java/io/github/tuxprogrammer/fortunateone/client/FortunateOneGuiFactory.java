package io.github.tuxprogrammer.fortunateone.client;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

public class FortunateOneGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return FortunateOneGuiConfig.class;
    }
}
