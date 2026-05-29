package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import galacticgreg.api.enums.DimensionDef;
import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.VeinGenState;

/**
 * Mixin into {@link WorldgenGTOreLayer} to:
 * <ol>
 * <li>Override {@code getWeight()} to return 1 for all veins when
 * {@link FortunateOneConfig#equalizeOreVeinWeights} is enabled, producing a uniform random
 * distribution over all registered vein types.</li>
 * <li>Resolve the per-dimension {@link DimensionOverride} at the start of each
 * {@code executeWorldgenChunkified} call and store it in {@link VeinGenState#CURRENT} so that
 * {@link MixinLayerGenerator} can access it without capturing local variables.</li>
 * </ol>
 *
 * <p>
 * The actual Y range and layer count override logic lives in {@link MixinLayerGenerator} to avoid
 * needing to capture the {@code LayerGenerator} local variable from
 * {@code executeWorldgenChunkified}.
 */
@Mixin(value = WorldgenGTOreLayer.class, remap = false)
public abstract class MixinWorldgenGTOreLayer {

    /**
     * Overrides {@code getWeight()} to return {@code 1} for all veins when
     * {@link FortunateOneConfig#equalizeOreVeinWeights} is enabled.
     *
     * <p>
     * {@code getWeight()} is called by {@code WorldgenQuery.findRandomWithWeight()} during vein
     * selection. Returning 1 uniformly makes selection probability equal across all registered
     * veins.
     */
    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true)
    private void fortuneone$equalizeWeight(CallbackInfoReturnable<Integer> cir) {
        if (FortunateOneConfig.equalizeOreVeinWeights) {
            cir.setReturnValue(1);
        }
    }

    /**
     * Initialises per-vein override state before ore layer generation begins.
     *
     * <p>
     * Resolves the {@link DimensionOverride} for the current chunk's dimension and writes it to
     * {@link VeinGenState#CURRENT}. {@link MixinLayerGenerator#fortuneone$interceptGenerateLayer}
     * reads this state on each of the 9 {@code generateLayer} calls without needing to capture any
     * local variables.
     *
     * <p>
     * {@link DimensionDef#getDimensionName(World)} is used for dimension name resolution because it
     * handles Galacticraft celestial bodies and falls back to
     * {@code world.provider.getDimensionName()} for unknown or future dimensions.
     */
    @Inject(method = "executeWorldgenChunkified", at = @At("HEAD"))
    private void fortuneone$initVeinState(World world, Random rng, String biome, int chunkX, int chunkZ, int seedX,
        int seedZ, IChunkProvider chunkGenerator, IChunkProvider chunkProvider, CallbackInfoReturnable<Integer> cir) {
        String dimName = DimensionDef.getDimensionName(world);
        FortunateOneConfig.registerDimension(dimName);
        DimensionOverride eff = FortunateOneConfig.getDimensionOverride(dimName);
        if (eff != null) {
            VeinGenState.CURRENT.set(new VeinGenState(eff, rng));
        } else {
            VeinGenState.CURRENT.remove();
        }
    }

    /**
     * After GT's fixed 9 {@code generateLayer} calls (invoked via the synthetic {@code access$100}
     * accessor), fires additional calls for layer overrides that specify more than 9 total layers
     * (primary + secondary + between > 9).
     *
     * <p>
     * The extra calls pass through
     * {@link MixinLayerGenerator#fortuneone$interceptGenerateLayer} which uses
     * {@link VeinGenState#callIndex} to dispatch the correct ore type, so layer ordering is
     * consistent with the &le;9 case.
     */
    @Inject(
        method = "executeWorldgenChunkified",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/WorldgenGTOreLayer$LayerGenerator;access$100(Lgregtech/common/WorldgenGTOreLayer$LayerGenerator;ZZZ)V",
            ordinal = 7,
            shift = At.Shift.AFTER))
    private void fortuneone$addExtraLayers(World world, Random rng, String biome, int chunkX, int chunkZ, int seedX,
        int seedZ, IChunkProvider chunkGenerator, IChunkProvider chunkProvider, CallbackInfoReturnable<Integer> cir) {
        VeinGenState state = VeinGenState.CURRENT.get();
        if (state == null) return;
        DimensionOverride eff = state.override;
        if (!eff.hasLayerOverride()) return;
        int total = eff.primaryLayers + eff.secondaryLayers + eff.betweenLayers;
        if (total <= 8 || state.generator == null) return;
        for (int i = 8; i < total; i++) {
            // Args are ignored; MixinLayerGenerator dispatches the correct ore type
            // based on VeinGenState.callIndex.
            state.generator.callGenerateLayer(false, false, false);
        }
    }

    /**
     * Cleans up the ThreadLocal state after each {@code executeWorldgenChunkified} call, including
     * all early-exit paths (wrong dimension, wrong biome, no overlap, etc.).
     */
    @Inject(method = "executeWorldgenChunkified", at = @At("RETURN"))
    private void fortuneone$cleanVeinState(CallbackInfoReturnable<Integer> cir) {
        VeinGenState.CURRENT.remove();
    }
}
