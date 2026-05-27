package io.github.tuxprogrammer.fortunateone.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.github.tuxprogrammer.fortunateone.ILayerGeneratorAccess;
import io.github.tuxprogrammer.fortunateone.VeinGenState;

/**
 * Regression tests for the SpongeMixin {@code IllegalClassLoadError} crash.
 *
 * <p>
 * SpongeMixin marks the entire declared mixin package
 * ({@code io.github.tuxprogrammer.fortunateone.mixins.*}) as exclusively owned by the
 * mixin transformer. Any non-{@code @Mixin}-annotated class residing in that package
 * causes {@code IllegalClassLoadError: cannot be referenced directly} the first time the
 * JVM classloader tries to instantiate or load it — which in practice means the server
 * crashes during {@code initialWorldChunkLoad} the first time worldgen fires.
 *
 * <p>
 * These tests run inside the live Forge/SpongeMixin environment. If either class were
 * accidentally moved back into the mixin package, {@link Class#forName} below would throw
 * the same {@code IllegalClassLoadError} that was seen in production, failing the test
 * with an unambiguous message instead of just generating an opaque crash report.
 *
 * <p>
 * See also: the {@code check-mixin-purity.yml} GitHub Actions workflow, which catches
 * the same regression at PR scan time without needing to start a server.
 */
class MixinPackagePurityTest {

    /**
     * {@link VeinGenState} holds the per-vein ThreadLocal state shared between the two
     * mixins. It must live in the main mod package, not the mixin subpackage.
     */
    @Test
    void veinGenStateIsNotInMixinPackage() {
        assertFalse(
            VeinGenState.class.getName()
                .contains(".mixins."),
            "VeinGenState must not be in the SpongeMixin-owned mixin package "
                + "(io.github.tuxprogrammer.fortunateone.mixins.*). "
                + "Move it to io.github.tuxprogrammer.fortunateone instead.");
    }

    /**
     * {@link VeinGenState} must be loadable inside the live Forge/SpongeMixin environment.
     * If it were in the mixin package, this call would throw
     * {@code IllegalClassLoadError: cannot be referenced directly}.
     */
    @Test
    void veinGenStateIsLoadableInForgeEnvironment() {
        assertDoesNotThrow(
            () -> Class.forName("io.github.tuxprogrammer.fortunateone.VeinGenState"),
            "Class.forName for VeinGenState threw an exception — "
                + "it may have been moved into the SpongeMixin-owned mixin package");
    }

    /**
     * {@link ILayerGeneratorAccess} is the accessor interface mixed into
     * {@code WorldgenGTOreLayer$LayerGenerator}. It must live in the main mod package.
     */
    @Test
    void iLayerGeneratorAccessIsNotInMixinPackage() {
        assertFalse(
            ILayerGeneratorAccess.class.getName()
                .contains(".mixins."),
            "ILayerGeneratorAccess must not be in the SpongeMixin-owned mixin package "
                + "(io.github.tuxprogrammer.fortunateone.mixins.*). "
                + "Move it to io.github.tuxprogrammer.fortunateone instead.");
    }

    /**
     * {@link ILayerGeneratorAccess} must be loadable inside the live Forge/SpongeMixin environment.
     */
    @Test
    void iLayerGeneratorAccessIsLoadableInForgeEnvironment() {
        assertDoesNotThrow(
            () -> Class.forName("io.github.tuxprogrammer.fortunateone.ILayerGeneratorAccess"),
            "Class.forName for ILayerGeneratorAccess threw an exception — "
                + "it may have been moved into the SpongeMixin-owned mixin package");
    }
}
