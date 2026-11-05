package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import dev.wp.industrialization_overdrive.IOHatchTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Lets the ME Mixed hatches slot into any multiblock position that accepts a plain item OR fluid hatch of
 * the matching direction. Mirrors Yet Another Industrialization's approach for its Mixed Hatch.
 */
@Mixin(value = HatchFlags.class, remap = false)
public abstract class HatchFlagsMixin {
    @Inject(method = "allows", at = @At("HEAD"), cancellable = true)
    private void io_allowMixedHatches(HatchType type, CallbackInfoReturnable<Boolean> cir) {
        if (type != IOHatchTypes.MIXED_INPUT && type != IOHatchTypes.MIXED_OUTPUT) {
            return;
        }

        Set<HatchType> allowed = ((HatchFlags) (Object) this).values();

        if (type == IOHatchTypes.MIXED_INPUT
                && (allowed.contains(HatchTypes.ITEM_INPUT) || allowed.contains(HatchTypes.FLUID_INPUT))) {
            cir.setReturnValue(true);
        } else if (type == IOHatchTypes.MIXED_OUTPUT
                && (allowed.contains(HatchTypes.ITEM_OUTPUT) || allowed.contains(HatchTypes.FLUID_OUTPUT))) {
            cir.setReturnValue(true);
        }
    }
}
