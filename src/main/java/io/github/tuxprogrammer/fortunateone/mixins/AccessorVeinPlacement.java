package io.github.tuxprogrammer.fortunateone.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the package-private record {@code WorldgenGTOreLayer$VeinPlacement}.
 *
 * <p>
 * Since GT5U 5.09.54, a vein's geometry (including its bottom Y) is resolved once per oreseed by
 * {@code resolveVeinPlacement}, cached in {@code GTWorldgenerator.validOreveins}, and replayed for
 * the dry-run test and for every chunk the vein touches. {@link MixinWorldgenGTOreLayer} rewrites
 * {@code veinMinY} on the freshly resolved record so that a Y-range override applies consistently
 * to all of those passes.
 *
 * <p>
 * {@link Mutable} strips {@code ACC_FINAL} from the record component's backing field so the setter
 * can write it. The record is only mutated before {@code resolveVeinPlacement} returns, i.e. before
 * GT has cached or used it.
 */
@Mixin(targets = "gregtech.common.WorldgenGTOreLayer$VeinPlacement", remap = false)
public interface AccessorVeinPlacement {

    @Accessor("veinMinY")
    int fortuneone$getVeinMinY();

    @Mutable
    @Accessor("veinMinY")
    void fortuneone$setVeinMinY(int veinMinY);

    @Accessor("generationSeed")
    long fortuneone$getGenerationSeed();
}
