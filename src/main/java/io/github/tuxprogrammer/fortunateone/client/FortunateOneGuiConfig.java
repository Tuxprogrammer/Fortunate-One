package io.github.tuxprogrammer.fortunateone.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneMod;

@SideOnly(Side.CLIENT)
public class FortunateOneGuiConfig extends GuiConfig {

    public FortunateOneGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(
            parentScreen,
            buildElements(),
            FortunateOneMod.MOD_ID,
            false,
            false,
            FortunateOneMod.MOD_NAME + " Configuration");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<IConfigElement> buildElements() throws ConfigException {
        // Standard @Config-managed fields (the 6 boolean options).
        List<IConfigElement> elements = new ArrayList<>(
            ConfigurationManager.getConfigElements(FortunateOneConfig.class));

        // Append the dimension overrides category as a drillable button.
        // ConfigElement on a ConfigCategory renders as a category row that opens
        // a sub-page listing global defaults + one button per registered dimension.
        Configuration rawCfg = FortunateOneConfig.getDimensionConfig();
        if (rawCfg != null && rawCfg.hasCategory("dimensionoverrides")) {
            elements.add(new ConfigElement<>(rawCfg.getCategory("dimensionoverrides")));
        }

        return elements;
    }

    /**
     * Called when the user closes this GUI (Done button or Escape).
     * The Forge GuiConfig framework has already written edited values into the
     * in-memory {@link net.minecraftforge.common.config.Configuration} object, but
     * has NOT called {@code save()} on it — that is our responsibility.
     * We save first, then reload so the live override map reflects the new values.
     */
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        try {
            // Flush GUI edits from memory to disk before reloading.
            FortunateOneConfig.saveDimensionConfig();
            FortunateOneConfig.reloadFromDisk();
        } catch (Exception e) {
            FortunateOneMod.LOG.warn("[Fortunate One] Failed to save/reload config after GUI close", e);
        }
    }
}
