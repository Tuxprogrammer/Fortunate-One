package io.github.tuxprogrammer.fortunateone;

/**
 * Accessor interface mixed into {@code WorldgenGTOreLayer$LayerGenerator} by
 * {@link io.github.tuxprogrammer.fortunateone.mixins.MixinLayerGenerator} to allow cross-mixin
 * interaction without direct access to the private inner class.
 *
 * <p>
 * Interface methods follow the standard Mixin accessor pattern (no mod prefix in declarations).
 *
 * <p>
 * Intentionally placed in the main mod package rather than the mixin package; SpongeMixin
 * prohibits non-mixin classes from residing in the mixin package.
 */
public interface ILayerGeneratorAccess {

    /** Invokes {@code LayerGenerator.generateLayer}, advancing one Y level after placement. */
    void callGenerateLayer(boolean secondary, boolean between, boolean primary);

    /** Returns {@code true} if at least one ore block has been placed in this vein. */
    boolean isPlaced();

    /** Sets the current Y level used by the next {@code generateLayer} call. */
    void setLevel(int level);

    int getLimitWestX();

    int getLimitEastX();

    int getLimitNorthZ();

    int getLimitSouthZ();
}
