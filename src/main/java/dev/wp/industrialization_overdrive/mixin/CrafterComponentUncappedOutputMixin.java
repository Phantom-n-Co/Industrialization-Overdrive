package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventoryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MI's second {@code putItemOutputs} pass sizes room as {@code maxStackSize - amount}, which goes negative on an
 * uncapped ME hatch slot holding over a stack and over-delivers to later slots; use the slot's own capacity instead.
 */
@Mixin(value = CrafterComponent.class, remap = false)
public abstract class CrafterComponentUncappedOutputMixin {
    @WrapOperation(method = "putItemOutputs",
            at = @At(value = "INVOKE",
                    target = "Laztech/modern_industrialization/thirdparty/fabrictransfer/api/item/ItemVariant;getMaxStackSize()I"))
    private int io_uncappedMaxStackSize(ItemVariant variant, Operation<Integer> original,
            @Local ConfigurableItemStack stack) {
        return MEHatchInventoryAccess.uncappedMaxStackSize(stack, original.call(variant));
    }
}
