package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.GTMod;
import gregtech.api.enums.StoneType;
import gregtech.common.GTProxy.OreDropSystem;
import gregtech.common.ores.GTPPOreAdapter;
import gregtech.common.ores.OreInfo;
import gtPlusPlus.core.material.Material;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortuneDropCalculator;

/**
 * Mixin into {@link GTPPOreAdapter#getOreDrops} to remove the Fortune-3 cap
 * for GT++ big-ore FortuneItem drops.
 *
 * <p>
 * GT++ does not have a non-natural-ore check (GTPPOreAdapter.getOreInfo always sets
 * {@code isNatural = true}), so only the uncap logic is needed here.
 */
@Mixin(value = GTPPOreAdapter.class, remap = false)
public class MixinGTPPOreAdapter {

    @Inject(method = "getOreDrops", at = @At("HEAD"), cancellable = true)
    private void fortuneone$getOreDrops(Random random, OreInfo<?> info2, boolean silktouch, int fortune,
        CallbackInfoReturnable<ArrayList<ItemStack>> cir) {

        // --- addon gate checks ---
        if (!FortunateOneConfig.gtPlusPlusUnlimitedFortune) return;

        // GT++ adapter only handles gtPlusPlus Material
        if (!(info2.material instanceof Material)) return;

        @SuppressWarnings("unchecked")
        OreInfo<Material> info = (OreInfo<Material>) info2;

        // GT++ supports only big ores (supports() returns false for isSmall=true), but guard anyway
        if (info.isSmall) return;

        // Keep silk-touch behavior unchanged
        if (silktouch) return;

        // Only act in FortuneItem mode
        if (GTMod.proxy.oreDropSystem != OreDropSystem.FortuneItem) return;

        // Only intercept when fortune > 3 (the only case where behavior differs from vanilla)
        if (fortune <= 3) return;

        if (info.stoneType == null) info.stoneType = StoneType.Stone;

        long dropCount = FortuneDropCalculator.uncappedGTPPBigOreDropCount(random, fortune);

        ArrayList<ItemStack> drops = new ArrayList<>();
        for (long i = 0; i < dropCount; i++) {
            drops.add(info.material.getRawOre(1));
        }
        cir.setReturnValue(drops);
    }
}
