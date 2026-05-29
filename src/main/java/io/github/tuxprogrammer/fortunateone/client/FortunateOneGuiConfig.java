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
            FortunateOneMod.MOD_ID, // configID — required for saveConfigElements() to fire on Done
            false,
            false,
            FortunateOneMod.MOD_NAME + " Configuration",
            null);
    }

    @SuppressWarnings("rawtypes")
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
     *
     * <p>
     * <strong>State at entry:</strong> Forge's {@code GuiConfig.actionPerformed} has already
     * iterated every {@code IConfigEntry} (including drillable subcategories) and pushed each
     * edited widget value back into the underlying {@link net.minecraftforge.common.config.Property}
     * via {@code ConfigElement.set()}. The in-memory {@link net.minecraftforge.common.config.Configuration}
     * is the source of truth at this point; disk is stale.
     *
     * <p>
     * <strong>What we must do:</strong> flush the in-memory state to disk so it survives a
     * restart, then refresh our live override map from the (now-current) Properties.
     * <strong>What we must NOT do:</strong> call {@code Configuration.load()} before saving.
     * That was the bug — the {@code ConfigChangedEvent} handler in {@code CommonProxy} ran first
     * and called the old {@code rebuildDimensionOverrideMap()} which internally invoked
     * {@code load()}, overwriting the user's edits with the still-{@code -1} disk contents
     * before they were ever persisted.
     */
    @Override
    public void onGuiClosed() {
        // Log what the dimensionoverrides category looks like IN MEMORY before super.
        // After the fix, PRE-super should reflect the user's edits (e.g. minY=1), proving
        // Forge's GuiConfig wrote them. POST-super is unchanged because super.onGuiClosed
        // is a pass-through to entryList.onGuiClosed and never touches Properties.
        FortunateOneConfig.logDimensionOverridesState("[FO-DEBUG] onGuiClosed PRE-super");
        super.onGuiClosed();
        FortunateOneConfig.logDimensionOverridesState("[FO-DEBUG] onGuiClosed POST-super");
        try {
            // Save in-memory edits to disk and rebuild the live override map from memory.
            // This is the only safe ordering: load() before save() would destroy the edits.
            FortunateOneConfig.persistAndRebuildFromMemory();
        } catch (Exception e) {
            FortunateOneMod.LOG.warn("[Fortunate One] Failed to persist config after GUI close", e);
        }
    }
}
