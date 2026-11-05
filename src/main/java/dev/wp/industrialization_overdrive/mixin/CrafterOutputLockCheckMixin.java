package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventoryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Counts ME hatch slots as player-lockable in the crafters' "all outputs locked" check, so their actual lock decides.
 * Otherwise an ME output hatch (never player-lockable) made overlapping recipes silently run the first match.
 */
@Mixin(targets = {
        "aztech.modern_industrialization.machines.components.CrafterComponent",
        "net.swedz.tesseract.neoforge.compat.mi.component.craft.AbstractModularCrafterComponent"
}, remap = false)
public abstract class CrafterOutputLockCheckMixin {
    @WrapOperation(method = "areAllSlotsLocked",
            at = @At(value = "INVOKE",
                    target = "Laztech/modern_industrialization/inventory/AbstractConfigurableStack;canPlayerLock()Z"))
    private static boolean io_meHatchSlotsAreLockable(AbstractConfigurableStack<?, ?> stack, Operation<Boolean> original) {
        return MEHatchInventoryAccess.isMEHatchStack(stack) || original.call(stack);
    }
}
