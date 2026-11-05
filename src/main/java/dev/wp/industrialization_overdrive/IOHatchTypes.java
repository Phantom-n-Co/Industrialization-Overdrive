package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;

/**
 * Hatch types owned by this mod.
 * <p>
 * The ME item and fluid hatches deliberately reuse MI's stock {@link HatchTypes} entries so they are
 * accepted by every existing multiblock with no mixin. Only the mixed hatches need new types, and those
 * are made acceptable by {@code dev.wp.industrialization_overdrive.mixin.HatchFlagsMixin}.
 */
public final class IOHatchTypes {
    private IOHatchTypes() {
    }

    public static final HatchType MIXED_INPUT = HatchTypes.register(
            IO.id("mixed_input"), IO.text().mixedInputHatch());
    public static final HatchType MIXED_OUTPUT = HatchTypes.register(
            IO.id("mixed_output"), IO.text().mixedOutputHatch());

    /**
     * Forces class initialisation so the types above are present in MI's registry before any shape
     * template or {@code HatchFlags} instance is consulted.
     * <p>
     * {@link HatchTypes#register} throws on a duplicate id, so this must be called exactly once, and only
     * after {@code IO.text()} is available.
     */
    public static void init() {
    }
}
