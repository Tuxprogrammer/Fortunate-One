package io.github.tuxprogrammer.fortunateone.mixins;

import java.util.Random;

import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import galacticgreg.api.enums.DimensionDef;
import gregtech.api.objects.XSTR;
import gregtech.common.WorldgenGTOreLayer;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig;
import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.FortunateOneMod;
import io.github.tuxprogrammer.fortunateone.VeinGenState;

/**
 * Mixin into {@link WorldgenGTOreLayer} to:
 * <ol>
 * <li>Override {@code getWeight()} to return 10 for all veins when
 * {@link FortunateOneConfig#equalizeOreVeinWeights} is enabled, producing a uniform random
 * distribution over all registered vein types.</li>
 * <li>Apply the per-dimension Y range override to the vein's {@code VeinPlacement} as soon as GT
 * resolves it.</li>
 * <li>Resolve the per-dimension layer override at the start of each {@code generateWithPlacement}
 * call and store it in {@link VeinGenState#CURRENT} so that {@link MixinLayerGenerator} can access
 * it without capturing local variables.</li>
 * </ol>
 *
 * <p>
 * <b>GT5U 5.09.54 vein pipeline</b>: {@code GTWorldgenerator} calls {@code resolveVeinPlacement}
 * once per oreseed, dry-runs it via {@code testWorldgenChunkified}, caches the placement, and then
 * replays it through {@code executeWorldgenChunkified} for each chunk. Every path funnels into the
 * private {@code generateWithPlacement}, which builds the {@code LayerGenerator} and makes GT's
 * fixed {@code generateLayer} calls. Hooking {@code resolveVeinPlacement} (height) and
 * {@code generateWithPlacement} (layers) therefore covers the dry run and the real run alike.
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
     * Rewrites the resolved vein's bottom Y when a height override is configured for the dimension.
     *
     * <p>
     * The new Y mirrors GT's own formula ({@code minY + nextInt(maxY - minY - 5)}), but draws from a
     * {@link Random} seeded with the placement's {@code generationSeed} instead of {@code rng}. This
     * keeps the result deterministic per oreseed and leaves {@code rng}'s sequence untouched, so GT's
     * subsequent ore placement is unaffected.
     *
     * <p>
     * {@link DimensionDef#getDimensionName(World)} is used for dimension name resolution because it
     * handles Galacticraft celestial bodies and falls back to
     * {@code world.provider.getDimensionName()} for unknown or future dimensions.
     */
    @Inject(method = "resolveVeinPlacement", at = @At("RETURN"))
    private void fortuneone$overrideVeinHeight(World world, XSTR rng, int chunkX, int chunkZ, int seedX, int seedZ,
        CallbackInfoReturnable<Object> cir) {
        String dimName = DimensionDef.getDimensionName(world);
        FortunateOneConfig.registerDimension(dimName);
        DimensionOverride eff = FortunateOneConfig.getDimensionOverride(dimName);
        if (eff == null || !eff.hasHeightOverride()) return;

        AccessorVeinPlacement placement = (AccessorVeinPlacement) cir.getReturnValue();
        int oldMinY = placement.fortuneone$getVeinMinY();
        int range = eff.maxY - eff.minY - 5;
        int newMinY = range > 0 ? eff.minY + new Random(placement.fortuneone$getGenerationSeed()).nextInt(range)
            : eff.minY;
        placement.fortuneone$setVeinMinY(newMinY);

        if (FortunateOneConfig.verboseDebugLogging) {
            FortunateOneMod.LOG.info(
                "[FO-DEBUG] heightAdjust: dim='{}' chunk=[{},{}] oldMinY={} newMinY={} minY={} maxY={} range={}",
                dimName,
                chunkX,
                chunkZ,
                oldMinY,
                newMinY,
                eff.minY,
                eff.maxY,
                range);
        }
    }

    /**
     * Initialises per-vein layer override state before GT's {@code generateLayer} calls begin.
     *
     * <p>
     * Writes a {@link VeinGenState} to {@link VeinGenState#CURRENT} only when the dimension has a
     * layer override; {@link MixinLayerGenerator#fortuneone$interceptGenerateLayer} reads it on each
     * {@code generateLayer} call without needing to capture any local variables.
     */
    @Inject(method = "generateWithPlacement", at = @At("HEAD"))
    private void fortuneone$initVeinState(World world, Random rng, boolean dryRun, int chunkX, int chunkZ, int seedX,
        int seedZ, int veinMinY, int veinWestX, int veinEastX, int veinNorthZ, int veinSouthZ, int limitWestX,
        int limitEastX, int limitNorthZ, int limitSouthZ, CallbackInfoReturnable<Integer> cir) {
        String dimName = DimensionDef.getDimensionName(world);
        DimensionOverride eff = FortunateOneConfig.getDimensionOverride(dimName);
        if (FortunateOneConfig.verboseDebugLogging) {
            FortunateOneMod.LOG.info(
                "[FO-DEBUG] initVeinState: dim='{}' chunk=[{},{}] dryRun={} veinMinY={} override={}",
                dimName,
                chunkX,
                chunkZ,
                dryRun,
                veinMinY,
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
        if (eff != null && eff.hasLayerOverride()) {
            VeinGenState.CURRENT.set(new VeinGenState(eff));
        } else {
            VeinGenState.CURRENT.remove();
        }
    }

    /**
     * After all of GT's {@code generateLayer} calls have run, fires additional calls for layer
     * overrides that specify more layers than GT generates by default.
     *
     * <p>
     * Injects at the first {@code GETFIELD placed} read in {@code generateWithPlacement}, which
     * occurs immediately after the last {@code generateLayer} call and immediately before the
     * {@code if (!generator.placed)} early-exit check. This avoids relying on synthetic accessor
     * names (e.g. {@code access$100}) that change under jvmDowngrader.
     *
     * <p>
     * The extra calls pass through
     * {@link MixinLayerGenerator#fortuneone$interceptGenerateLayer} which uses
     * {@link VeinGenState#callIndex} to dispatch the correct ore type.
     */
    @Inject(
        method = "generateWithPlacement",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/WorldgenGTOreLayer$LayerGenerator;placed:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    private void fortuneone$addExtraLayers(CallbackInfoReturnable<Integer> cir) {
        VeinGenState state = VeinGenState.CURRENT.get();
        if (state == null) return;
        if (state.generator == null) return;
        DimensionOverride eff = state.override;
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
     * Cleans up the ThreadLocal state after each {@code generateWithPlacement} call, including the
     * {@code !generator.placed} early exit.
     */
    @Inject(method = "generateWithPlacement", at = @At("RETURN"))
    private void fortuneone$cleanVeinState(CallbackInfoReturnable<Integer> cir) {
        VeinGenState.CURRENT.remove();
    }
}
