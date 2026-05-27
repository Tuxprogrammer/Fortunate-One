package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bartworks.system.material.Werkstoff;
import gregtech.GTMod;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.GTProxy.OreDropSystem;
import gregtech.common.ores.BWOreAdapter;
import gregtech.common.ores.OreInfo;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortuneDropCalculator;

/**
 * Mixin into {@link BWOreAdapter#getOreDrops} to remove the Fortune-3 cap
 * and optionally bypass the non-natural ore fortune zeroing for BartWorks big ores.
 *
 * <p>
 * Mirrors {@link MixinGTOreAdapter} for the BW ore path.
 */
@Mixin(value = BWOreAdapter.class, remap = false)
public class MixinBWOreAdapter {

    @Inject(method = "getOreDrops", at = @At("HEAD"), cancellable = true)
    private void fortuneone$getOreDrops(Random random, OreInfo<?> info2, boolean silktouch, int fortune,
        CallbackInfoReturnable<ArrayList<ItemStack>> cir) {

        // --- addon gate checks ---
        if (!FortunateOneConfig.bartWorksUnlimitedFortune) return;

        // BW adapter only handles Werkstoff materials
        if (!(info2.material instanceof Werkstoff)) return;

        @SuppressWarnings("unchecked")
        OreInfo<Werkstoff> info = (OreInfo<Werkstoff>) info2;

        // Keep small ore behavior unchanged
        if (info.isSmall) return;

        // Keep silk-touch behavior unchanged
        if (silktouch) return;

        // Only act in FortuneItem mode
        if (GTMod.proxy.oreDropSystem != OreDropSystem.FortuneItem) return;

        // Determine effective fortune
        int effectiveFortune = fortune;
        if (!info.isNatural) {
            if (FortunateOneConfig.allowPlacedOreFortune) {
                if (fortune == 0) return;
            } else {
                return;
            }
        }

        boolean needsUncap = info.isNatural && fortune > 3;
        boolean needsPlacedBypass = !info.isNatural && FortunateOneConfig.allowPlacedOreFortune && fortune > 0;
        if (!needsUncap && !needsPlacedBypass) return;

        if (info.stoneType == null) info.stoneType = gregtech.api.enums.StoneType.Stone;

        int richMult = info.stoneType.isRich() ? 2 : 1;
        int dropCount = FortuneDropCalculator.uncappedGTBigOreDropCount(random, effectiveFortune, richMult);

        ArrayList<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            drops.add(info.material.get(OrePrefixes.rawOre, 1));
        }
        cir.setReturnValue(drops);
    }
}
