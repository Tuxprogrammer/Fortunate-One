package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import galacticgreg.api.enums.DimensionDef;
import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.FortunateOneMod;
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
     * Overrides {@code getWeight()} to return {@code 10} for all veins when
     * {@link FortunateOneConfig#equalizeOreVeinWeights} is enabled.
     *
     * <p>
     * {@code getWeight()} is called by {@code WorldgenQuery.findRandomWithWeight()} during vein
     * selection. Returning 10 uniformly makes selection probability equal across all registered
     * veins. We use 10 rather than 1 so that the weighted-random algorithm in GT5U (which has an
     * off-by-one: it checks {@code remainingAmount <= 0} instead of {@code < 0}) does not
     * permanently exclude the last vein in the list — with weight 1 a roll of exactly
     * {@code total - 1} would land on the penultimate vein and leave the last unreachable; with
     * weight 10 the same edge case only biases the last vein slightly rather than zeroing it out.
     */
    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true)
    private void fortuneone$equalizeWeight(CallbackInfoReturnable<Integer> cir) {
        if (FortunateOneConfig.equalizeOreVeinWeights) {
            cir.setReturnValue(10);
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
        if (FortunateOneConfig.verboseDebugLogging) {
            FortunateOneMod.LOG.info(
                "[FO-DEBUG] initVeinState: dim='{}' chunk=[{},{}] override={}",
                dimName,
                chunkX,
                chunkZ,
                eff == null ? "null (no override)"
                    : String.format(
                        "Y=[%d,%d] layers=sec:%d bet:%d pri:%d hasLayer=%b hasHeight=%b",
                        eff.minY,
                        eff.maxY,
                        eff.secondaryLayers,
                        eff.betweenLayers,
                        eff.primaryLayers,
                        eff.hasLayerOverride(),
                        eff.hasHeightOverride()));
        }
        if (eff != null) {
            VeinGenState.CURRENT.set(new VeinGenState(eff, rng));
        } else {
            VeinGenState.CURRENT.remove();
        }
    }

    /**
     * After all of GT's {@code generateLayer} calls have run, fires additional calls for layer
     * overrides that specify more layers than GT generates by default.
     *
     * <p>
     * Injects at the first {@code GETFIELD placed} read in {@code executeWorldgenChunkified},
     * which occurs immediately after the last {@code generateLayer} call and immediately before
     * the {@code if (!generator.placed)} early-exit check. This avoids relying on synthetic
     * accessor names (e.g. {@code access$100}) that change under jvmDowngrader.
     *
     * <p>
     * The extra calls pass through
     * {@link MixinLayerGenerator#fortuneone$interceptGenerateLayer} which uses
     * {@link VeinGenState#callIndex} to dispatch the correct ore type.
     */
    @Inject(
        method = "executeWorldgenChunkified",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/WorldgenGTOreLayer$LayerGenerator;placed:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    private void fortuneone$addExtraLayers(World world, Random rng, String biome, int chunkX, int chunkZ, int seedX,
        int seedZ, IChunkProvider chunkGenerator, IChunkProvider chunkProvider, CallbackInfoReturnable<Integer> cir) {
        VeinGenState state = VeinGenState.CURRENT.get();
        if (state == null) return;
        DimensionOverride eff = state.override;
        if (!eff.hasLayerOverride()) return;
        if (state.generator == null) return;
        int total = eff.primaryLayers + eff.secondaryLayers + eff.betweenLayers;
        if (FortunateOneConfig.verboseDebugLogging && state.callIndex < total) {
            FortunateOneMod.LOG.info(
                "[FO-DEBUG] addExtraLayers FIRING: callIndex={} total={}, adding {} more",
                state.callIndex,
                total,
                total - state.callIndex);
        }
        while (state.callIndex < total) {
            if (FortunateOneConfig.verboseDebugLogging) {
                FortunateOneMod.LOG.info("[FO-DEBUG]   extraLayer callIndex-before={}", state.callIndex);
            }
            state.generator.callGenerateLayer(false, false, false);
            if (FortunateOneConfig.verboseDebugLogging) {
                FortunateOneMod.LOG.info("[FO-DEBUG]   extraLayer callIndex-after={}", state.callIndex);
            }
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
