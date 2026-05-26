package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.GTMod;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.GTProxy.OreDropSystem;
import gregtech.common.ores.GTOreAdapter;
import gregtech.common.ores.OreInfo;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortuneDropCalculator;

/**
 * Mixin into {@link GTOreAdapter#getOreDrops} to:
 * <ol>
 * <li>Bypass the {@code if (!info.isNatural) fortune = 0} zeroing when
 * {@link FortunateOneConfig#allowPlacedOreFortune} is enabled.</li>
 * <li>Remove the {@code fortune > 3 ? fortune = 3} cap for big-ore FortuneItem drops.</li>
 * </ol>
 *
 * <p>
 * Only fires when:
 * <ul>
 * <li>{@link FortunateOneConfig#enableUnlimitedFortuneMode} is {@code true}</li>
 * <li>{@link FortunateOneConfig#applyToGT} is {@code true}</li>
 * <li>The ore is a big ore (not small)</li>
 * <li>Silk-touch is not active</li>
 * <li>The active GT drop mode is {@code FortuneItem}</li>
 * <li>Fortune is {@code > 0} AND (fortune {@code > 3} OR the ore is non-natural with placed-fortune allowed)</li>
 * </ul>
 * Otherwise the original GT logic runs unchanged.
 */
@Mixin(value = GTOreAdapter.class, remap = false)
public class MixinGTOreAdapter {

    @Inject(method = "getOreDrops", at = @At("HEAD"), cancellable = true)
    private void fortuneone$getOreDrops(Random random, OreInfo<?> info2, boolean silktouch, int fortune,
        CallbackInfoReturnable<ArrayList<ItemStack>> cir) {

        // --- addon gate checks ---
        if (!FortunateOneConfig.enableUnlimitedFortuneMode || !FortunateOneConfig.applyToGT) return;

        // GT adapter only handles Materials
        if (!(info2.material instanceof Materials)) return;

        @SuppressWarnings("unchecked")
        OreInfo<Materials> info = (OreInfo<Materials>) info2;

        // Keep small ore behavior unchanged (req 8)
        if (info.isSmall) return;

        // Keep silk-touch behavior unchanged (req 6)
        if (silktouch) return;

        // Only act in FortuneItem mode (req 2 / functional req 2)
        if (GTMod.proxy.oreDropSystem != OreDropSystem.FortuneItem) return;

        // Determine effective fortune, respecting placed-ore-fortune setting
        int effectiveFortune = fortune;
        if (!info.isNatural) {
            if (FortunateOneConfig.allowPlacedOreFortune) {
                // Keep fortune; we intercept to prevent the natural-check zero
                if (fortune == 0) return; // nothing to do
            } else {
                // Let original zero it out
                return;
            }
        }

        // Only intercept when our behavior differs from vanilla:
        // a) fortune > 3 (vanilla would cap, we don't), or
        // b) non-natural ore with fortune > 0 and allowPlacedOreFortune (vanilla would zero, we don't)
        boolean needsUncap = info.isNatural && fortune > 3;
        boolean needsPlacedBypass = !info.isNatural && FortunateOneConfig.allowPlacedOreFortune && fortune > 0;
        if (!needsUncap && !needsPlacedBypass) return;

        if (info.stoneType == null) info.stoneType = gregtech.api.enums.StoneType.Stone;

        int richMult = info.stoneType.isRich() ? 2 : 1;
        int dropCount = FortuneDropCalculator.uncappedGTBigOreDropCount(random, effectiveFortune, richMult);

        ArrayList<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            drops.add(GTOreDictUnificator.get(OrePrefixes.rawOre, info.material, 1));
        }
        cir.setReturnValue(drops);
    }
}
