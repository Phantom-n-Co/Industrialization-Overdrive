package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import dev.wp.industrialization_overdrive.mixin.accessor.AbstractConfigurableStackAccessor;
import dev.wp.industrialization_overdrive.mixin.accessor.ConfigurableItemStackAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public final class MEHatchInventoryAccess {
    /** MI clamps adjusted capacity to 0..64, so anything above marks the slot as uncapped. */
    public static final int UNCAPPED_ITEM_CAPACITY = MEHatchInventories.UNCAPPED_ITEM_CAPACITY;

    private static final int MI_MAX_ADJUSTED_CAPACITY = 64;

    private MEHatchInventoryAccess() {
    }

    // A machine lock (running recipe reservation) is left in place.
    public static void unlock(AbstractConfigurableStack<?, ?> stack) {
        if (!stack.isPlayerLocked()) {
            return;
        }
        AbstractConfigurableStackAccessor accessor = (AbstractConfigurableStackAccessor) stack;
        accessor.io_setPlayerLocked(false);
        if (!stack.isMachineLocked()) {
            accessor.io_setLockedInstance(null);
        }
        accessor.io_notifyListeners();
    }

    public static boolean isUncapped(int adjustedCapacity) {
        return adjustedCapacity > MI_MAX_ADJUSTED_CAPACITY || adjustedCapacity < 0;
    }

    public static int itemCapacity(int adjustedCapacity) {
        return adjustedCapacity < 0 ? -(adjustedCapacity + 1) : adjustedCapacity;
    }

    // Sizes <= 64 are stored as -1 - capacity so they stay off MI's vanilla-stack-size path.
    public static void setItemCapacity(ConfigurableItemStack stack, long capacity) {
        int encoded = encodeItemCapacity(capacity);
        if (stack.getAdjustedCapacity() != encoded) {
            ((ConfigurableItemStackAccessor) stack).io_setAdjustedCapacity(encoded);
            ((AbstractConfigurableStackAccessor) stack).io_notifyListeners();
        }
    }

    static int encodeItemCapacity(long capacity) {
        if (capacity >= UNCAPPED_ITEM_CAPACITY) {
            return UNCAPPED_ITEM_CAPACITY;
        }
        if (capacity > MI_MAX_ADJUSTED_CAPACITY) {
            return (int) capacity;
        }
        return (int) (-1 - Math.max(0L, capacity));
    }

    // Matched by flags, not identity: MI replaces stack objects on every resync.
    public static boolean isMEHatchStack(AbstractConfigurableStack<?, ?> stack) {
        return !stack.canPlayerInsert() && !stack.canPlayerExtract() && !stack.canPlayerLock();
    }

    // Never below what the slot holds: callers subtract the amount from this and would go negative.
    public static int uncappedMaxStackSize(ConfigurableItemStack stack, int original) {
        int adjusted = stack.getAdjustedCapacity();
        return isUncapped(adjusted) ? Math.max(itemCapacity(adjusted), (int) stack.getAmount()) : original;
    }

    public static void uncapItemCapacity(ConfigurableItemStack stack) {
        MEHatchInventories.uncapItemCapacity(stack);
    }

    public static void lockToConfig(ConfigurableItemStack stack, @Nullable GenericStack config) {
        lock(stack, lockInstance(stack, config));
    }

    public static void lockToConfig(ConfigurableFluidStack stack, @Nullable GenericStack config) {
        lock(stack, lockInstance(stack, config));
    }

    public static boolean isLockPending(AbstractConfigurableStack<?, ?> stack, @Nullable GenericStack config) {
        return !stack.isPlayerLocked() || stack.getLockedInstance() != lockInstance(stack, config);
    }

    private static Object lockInstance(AbstractConfigurableStack<?, ?> stack, @Nullable GenericStack config) {
        if (stack instanceof ConfigurableFluidStack) {
            return config != null && config.what() instanceof AEFluidKey key ? key.getFluid() : Fluids.EMPTY;
        }
        return config != null && config.what() instanceof AEItemKey key ? key.getItem() : Items.AIR;
    }

    private static void lock(AbstractConfigurableStack<?, ?> stack, Object instance) {
        MEHatchInventories.lock(stack, instance);
    }
}
