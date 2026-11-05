package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.util.Simulation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchSlot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ME hatch slot locks mirror the channel configuration: recipe-viewer drags ({@code playerLock}) are refused, and a
 * controller's "lock recipe" ({@code playerLockNoOverride}) configures the channel instead. Also carries the
 * {@link MEHatchSlot} back-reference the lock path uses.
 */
@Mixin(value = AbstractConfigurableStack.class, remap = false)
public abstract class ConfigurableStackPlayerLockMixin implements MEHatchSlot {
    @Unique
    private @Nullable MEHatchSlot.Channel io_meHatchChannel;

    @Override
    public @Nullable MEHatchSlot.Channel io_getMEHatchChannel() {
        return io_meHatchChannel;
    }

    @Override
    public void io_setMEHatchChannel(@Nullable MEHatchSlot.Channel channel) {
        io_meHatchChannel = channel;
    }

    @Inject(method = "playerLock", at = @At("HEAD"), cancellable = true)
    private void io_refusePlayerLockWhenNotLockable(Object instance, Simulation simulation,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((AbstractConfigurableStack<?, ?>) (Object) this).canPlayerLock()) {
            cir.setReturnValue(false);
        }
    }

    // Reached only for slots with an empty lock (an unconfigured ME channel); reporting capacity claims the slot.
    @WrapOperation(method = "playerLockNoOverride",
            at = @At(value = "INVOKE",
                    target = "Laztech/modern_industrialization/inventory/AbstractConfigurableStack;getTotalCapacityFor(Ljava/lang/Object;)J"))
    private static long io_lockMEHatchChannels(AbstractConfigurableStack<?, ?> stack, Object instance,
            Operation<Long> original, @Local(argsOnly = true) long requiredAmount) {
        if (stack.canPlayerLock()) {
            return original.call(stack, instance);
        }
        MEHatchSlot.Channel channel = ((MEHatchSlot) stack).io_getMEHatchChannel();
        return channel == null ? 0L : channel.configureForRecipeLock(instance, requiredAmount);
    }
}
