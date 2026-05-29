package io.github.tuxprogrammer.fortunateone;

import java.util.Random;

import io.github.tuxprogrammer.fortunateone.FortunateOneConfig.DimensionOverride;

/**
 * Carries per-vein override state between the HEAD inject in
 * {@link io.github.tuxprogrammer.fortunateone.mixins.MixinWorldgenGTOreLayer} and the
 * {@code generateLayer} intercept in
 * {@link io.github.tuxprogrammer.fortunateone.mixins.MixinLayerGenerator}.
 *
 * <p>
 * Lives in a {@link ThreadLocal} so parallel chunk generation on separate threads cannot
 * interfere with each other.
 *
 * <p>
 * Intentionally placed in the main mod package rather than the mixin package; SpongeMixin
 * prohibits non-mixin classes from residing in the mixin package.
 */
public final class VeinGenState {

    public static final ThreadLocal<VeinGenState> CURRENT = new ThreadLocal<>();

    public final DimensionOverride override;
    public final Random rng;
    public boolean heightAdjusted = false;
    public int callIndex = 0;

    /**
     * Reference to the active {@link ILayerGeneratorAccess} instance, set on the first
     * {@code generateLayer} intercept. Used by
     * {@link io.github.tuxprogrammer.fortunateone.mixins.MixinWorldgenGTOreLayer}
     * to fire extra {@code generateLayer} calls beyond GT's fixed 8 when a layer override
     * specifies a total greater than 8.
     */
    public ILayerGeneratorAccess generator = null;

    public VeinGenState(DimensionOverride override, Random rng) {
        this.override = override;
        this.rng = rng;
    }
}
