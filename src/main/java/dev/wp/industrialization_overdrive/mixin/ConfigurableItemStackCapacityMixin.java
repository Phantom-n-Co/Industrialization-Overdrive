package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventoryAccess;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item slots hold more than one vanilla stack when the adjusted capacity is above 64 — MI clamps it to 0..64, so the
 * value is an unambiguous marker set by {@link MEHatchInventoryAccess#uncapItemCapacity} that MI persists, copies and
 * syncs for free.
 */
@Mixin(value = ConfigurableItemStack.class, remap = false)
public abstract class ConfigurableItemStackCapacityMixin {
    @Unique
    private ConfigurableItemStack io_self() {
        return (ConfigurableItemStack) (Object) this;
    }

    @Unique
    private boolean io_isUncapped() {
        return MEHatchInventoryAccess.isUncapped(io_self().getAdjustedCapacity());
    }

    @Unique
    private int io_capacity() {
        return MEHatchInventoryAccess.itemCapacity(io_self().getAdjustedCapacity());
    }

    @Inject(method = "getCapacity", at = @At("HEAD"), cancellable = true)
    private void io_uncappedCapacity(CallbackInfoReturnable<Long> cir) {
        if (io_isUncapped()) {
            cir.setReturnValue((long) io_capacity());
        }
    }

    @Inject(method = "getRemainingCapacityFor(Laztech/modern_industrialization/thirdparty/fabrictransfer/api/item/ItemVariant;)J",
            at = @At("HEAD"), cancellable = true)
    private void io_uncappedRemainingCapacity(ItemVariant key, CallbackInfoReturnable<Long> cir) {
        if (io_isUncapped()) {
            cir.setReturnValue(Math.max(0L, io_capacity() - io_self().getAmount()));
        }
    }

    @Inject(method = "getTotalCapacityFor(Lnet/minecraft/world/item/Item;)J", at = @At("HEAD"), cancellable = true)
    private void io_uncappedTotalCapacity(Item instance, CallbackInfoReturnable<Long> cir) {
        if (io_isUncapped()) {
            cir.setReturnValue((long) io_capacity());
        }
    }

    // Scroll-to-adjust would clamp the marker back into 0..64.
    @Inject(method = "adjustCapacity", at = @At("HEAD"), cancellable = true)
    private void io_keepUncappedCapacity(boolean isIncrease, boolean isShiftDown, CallbackInfo ci) {
        if (io_isUncapped()) {
            ci.cancel();
        }
    }
}
