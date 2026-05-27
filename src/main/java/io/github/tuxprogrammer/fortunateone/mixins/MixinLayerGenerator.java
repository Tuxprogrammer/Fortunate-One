package io.github.tuxprogrammer.fortunateone.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;
import io.github.tuxprogrammer.fortunateone.ILayerGeneratorAccess;
import io.github.tuxprogrammer.fortunateone.VeinGenState;

/**
 * Mixin into the private inner class {@code WorldgenGTOreLayer$LayerGenerator} to:
 * <ol>
 * <li>Implement the {@link ILayerGeneratorAccess} accessor interface, exposing fields and the
 * private {@code generateLayer} method for use by {@link MixinWorldgenGTOreLayer}.</li>
 * <li>Intercept each {@code generateLayer} call to apply per-dimension Y range and layer count
 * overrides stored in {@link VeinGenState#CURRENT}.</li>
 * </ol>
 *
 * <p>
 * <b>Layer override design</b>: when a {@link DimensionOverride#hasLayerOverride() layer override}
 * is active, this inject cancels the original call and replaces it with the appropriate ore type
 * (secondary → between → primary, sequential blocks). A recursion guard field
 * ({@code fortuneone$inCustomCall}) prevents the intercept from firing again on the replacement
 * call we issue ourselves. Calls beyond the configured total (secondary + between + primary) are
 * skipped but still advance {@code level} to maintain Y spacing.
 *
 * <p>
 * <b>Layer count cap</b>: because {@code executeWorldgenChunkified} makes exactly 9
 * {@code generateLayer} calls, the effective total cannot exceed 9. Any configured excess
 * is silently ignored.
 *
 * <p>
 * <b>Simplified layer order</b>: the custom sequence uses pure S→B→P blocks rather than the
 * original interspersed {@code [S,S,S,S+B,B,P+B,P+B,P,P]} pattern. Vein shape differs slightly
 * from vanilla; this is intentional.
 *
 * <p>
 * <b>Small ore placer</b>: because we never cancel {@code executeWorldgenChunkified} itself, the
 * original GT5U small ore placer block always runs after our custom layer sequence. No replication
 * needed.
 */
@Mixin(targets = "gregtech.common.WorldgenGTOreLayer$LayerGenerator", remap = false)
public abstract class MixinLayerGenerator implements ILayerGeneratorAccess {

    @Shadow
    boolean placed;

    @Shadow
    int level;

    @Shadow
    int limitWestX;

    @Shadow
    int limitEastX;

    @Shadow
    int limitNorthZ;

    @Shadow
    int limitSouthZ;

    @Shadow
    protected abstract void generateLayer(boolean secondary, boolean between, boolean primary);

    /**
     * Recursion guard: set to {@code true} while we are executing a replacement
     * {@code generateLayer} call inside the intercept, so the intercept does not fire again
     * on our own call.
     */
    private boolean fortuneone$inCustomCall = false;

    // -------------------------------------------------------------------------
    // ILayerGeneratorAccess implementation
    // -------------------------------------------------------------------------

    @Override
    public void callGenerateLayer(boolean secondary, boolean between, boolean primary) {
        generateLayer(secondary, between, primary);
    }

    @Override
    public boolean isPlaced() {
        return placed;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getLimitWestX() {
        return limitWestX;
    }

    @Override
    public int getLimitEastX() {
        return limitEastX;
    }

    @Override
    public int getLimitNorthZ() {
        return limitNorthZ;
    }

    @Override
    public int getLimitSouthZ() {
        return limitSouthZ;
    }

    // -------------------------------------------------------------------------
    // Layer intercept
    // -------------------------------------------------------------------------

    @Inject(method = "generateLayer", at = @At("HEAD"), cancellable = true)
    private void fortuneone$interceptGenerateLayer(boolean secondary, boolean between, boolean primary,
        CallbackInfo ci) {
        if (fortuneone$inCustomCall) {
            // Allow our own replacement call to run the original body unmodified.
            return;
        }

        VeinGenState state = VeinGenState.CURRENT.get();
        if (state == null) {
            // No active dimension override — pass through unchanged.
            return;
        }

        // Register this LayerGenerator instance on the first intercept so that
        // MixinWorldgenGTOreLayer.fortuneone$addExtraLayers can call extra layers beyond 9.
        if (state.generator == null) {
            state.generator = this;
        }

        DimensionOverride eff = state.override;

        // Adjust vein start Y on the first call if a height override is configured.
        // The original code set generator.level = veinMinY - 1 just before the first call;
        // we replace veinMinY with our configured range here.
        if (!state.heightAdjusted && eff.hasHeightOverride()) {
            int range = eff.maxY - eff.minY - 5;
            if (range > 0) {
                this.level = eff.minY + state.rng.nextInt(range) - 1;
            } else {
                this.level = eff.minY - 1;
            }
            state.heightAdjusted = true;
        }

        if (!eff.hasLayerOverride()) {
            // Height-only override: let the original call run with the adjusted level.
            return;
        }

        // --- Layer count override ---
        // Cancel the original call and dispatch the correct ore type for this index.
        ci.cancel();

        int idx = state.callIndex;
        state.callIndex++;

        int secEnd = eff.secondaryLayers;
        int betEnd = secEnd + eff.betweenLayers;
        int priEnd = betEnd + eff.primaryLayers;

        if (idx >= priEnd) {
            // Custom vein is shorter than 9 layers: skip ore placement but advance Y.
            this.level++;
            return;
        }

        fortuneone$inCustomCall = true;
        try {
            if (idx < secEnd) {
                generateLayer(true, false, false);
            } else if (idx < betEnd) {
                generateLayer(false, true, false);
            } else {
                generateLayer(false, false, true);
            }
        } finally {
            fortuneone$inCustomCall = false;
        }
    }
}
