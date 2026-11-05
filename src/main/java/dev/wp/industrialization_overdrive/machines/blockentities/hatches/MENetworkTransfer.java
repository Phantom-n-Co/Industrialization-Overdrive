package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

/** Moves resources between an ME network's {@link MEStorage} and the channels of an ME hatch. */
public final class MENetworkTransfer {
    private MENetworkTransfer() {
    }

    public static Channel channel(ConfigurableItemStack slot, int index, MEHatchConfigComponent configuration) {
        return new ItemChannel(slot, index, configuration);
    }

    public static Channel channel(ConfigurableFluidStack slot, int index, MEHatchConfigComponent configuration) {
        return new FluidChannel(slot, index, configuration);
    }

    // --------------------------------------------------------------- transfer

    public static @Nullable AEKey keyOfLockInstance(boolean fluid, @Nullable Object instance) {
        if (fluid) {
            return instance instanceof Fluid f && f != Fluids.EMPTY ? AEFluidKey.of(f) : null;
        }
        return instance instanceof Item i && i != Items.AIR ? AEItemKey.of(i) : null;
    }

    public static void pull(MEStorage storage, Channel channel, long target, LongSupplier limit, IActionSource source) {
        GenericStack configured = channel.config();
        if (configured == null || !channel.accepts(configured.what())) {
            return;
        }
        AEKey wanted = configured.what();

        // Return any stale key before transferring anything.
        if (!reconcileStale(storage, channel, wanted, source) || !returnExcess(storage, channel, target, source)) {
            return;
        }

        long request = Math.min(target - channel.amount(), channel.room(wanted, target));
        if (request <= 0) {
            return;
        }
        request = Math.min(request, limit.getAsLong());
        if (request <= 0) {
            return;
        }

        long moved = storage.extract(wanted, request, Actionable.MODULATE, source);
        if (moved <= 0) {
            return;
        }

        // Already extracted: hand back what the slot cannot hold instead of voiding it.
        long kept = Math.min(moved, channel.room(wanted, target));
        if (kept > 0) {
            channel.store(wanted, kept);
        }
        if (kept < moved) {
            storage.insert(wanted, moved - kept, Actionable.MODULATE, source);
        }
    }

    public static boolean fits(MEStorage storage, Channel channel, IActionSource source) {
        AEKey key = channel.key();
        if (key == null) {
            return true;
        }
        long amount = channel.amount();
        return storage.insert(key, amount, Actionable.SIMULATE, source) >= amount;
    }

    /** Pushes everything the channel holds regardless of role or filter; keeps what the network refuses. */
    public static void drain(MEStorage storage, Channel channel, IActionSource source) {
        AEKey key = channel.key();
        if (key == null) {
            return;
        }

        long amount = channel.amount();
        long inserted = storage.insert(key, amount, Actionable.MODULATE, source);
        if (inserted > 0) {
            channel.remove(Math.min(inserted, amount));
        }
    }

    /** Moves what {@code to} accepts; anything refused after extraction is put back. */
    public static void moveAll(MEStorage from, MEStorage to, IActionSource source) {
        for (var entry : from.getAvailableStacks()) {
            AEKey key = entry.getKey();
            long accepted = to.insert(key, entry.getLongValue(), Actionable.SIMULATE, source);
            if (accepted <= 0) {
                continue;
            }
            long extracted = from.extract(key, accepted, Actionable.MODULATE, source);
            if (extracted <= 0) {
                continue;
            }
            long inserted = to.insert(key, extracted, Actionable.MODULATE, source);
            if (inserted < extracted) {
                from.insert(key, extracted - inserted, Actionable.MODULATE, source);
            }
        }
    }

    // --------------------------------------------------------------- reconciliation

    /**
     * Returns stock to the network before a configuration change (all of it on a key change, the surplus on an
     * input target reduction); clearing an output needs it too, or setCapacity would void the stock.
     *
     * @return {@code true} when the channel is safe to reconfigure
     */
    public static boolean reconcile(@Nullable MEStorage storage, Channel channel, boolean input,
            @Nullable GenericStack oldConfig, @Nullable GenericStack newConfig, IActionSource source) {
        if (oldConfig == null && newConfig == null) {
            return true;
        }

        if (!sameKey(oldConfig, newConfig)) {
            return returnAllStock(storage, channel, source);
        }

        if (!input) {
            return true;
        }

        AEKey wanted = newConfig.what();
        if (!channel.accepts(wanted) || !reconcileStale(storage, channel, wanted, source)) {
            return false;
        }

        // Must run before the caller changes a fluid slot's capacity: setCapacity() clamps the amount.
        return returnExcess(storage, channel, newConfig.amount(), source);
    }

    // --------------------------------------------------------------- stock-return helpers

    private static boolean reconcileStale(@Nullable MEStorage storage, Channel channel, AEKey wanted,
            IActionSource source) {
        AEKey visible = channel.key();
        if (visible == null || visible.equals(wanted)) {
            return true;
        }
        long amount = channel.amount();
        return returnStock(storage, channel, amount, source) >= amount;
    }

    private static boolean returnExcess(@Nullable MEStorage storage, Channel channel, long target,
            IActionSource source) {
        long excess = channel.amount() - target;
        if (excess <= 0) {
            return true;
        }
        return returnStock(storage, channel, excess, source) >= excess;
    }

    private static boolean returnAllStock(@Nullable MEStorage storage, Channel channel, IActionSource source) {
        AEKey visible = channel.key();
        if (visible == null) {
            return channel.amount() <= 0;
        }
        if (storage == null) {
            return false;
        }

        // Simulate the full return first so a refusal leaves stock and configuration untouched.
        long amount = channel.amount();
        if (storage.insert(visible, amount, Actionable.SIMULATE, source) < amount) {
            return false;
        }
        return returnStock(storage, channel, amount, source) >= amount;
    }

    private static long returnStock(@Nullable MEStorage storage, Channel channel, long amount,
            IActionSource source) {
        AEKey key = channel.key();
        if (key == null || storage == null || amount <= 0) {
            return 0L;
        }
        long inserted = storage.insert(key, amount, Actionable.MODULATE, source);
        long removed = Math.min(Math.max(0L, inserted), amount);
        if (removed > 0) {
            channel.remove(removed);
        }
        return removed;
    }

    private static boolean sameKey(@Nullable GenericStack left, @Nullable GenericStack right) {
        return left != null && right != null && left.what().equals(right.what());
    }

    // --------------------------------------------------------------- channels

    public abstract static class Channel {
        final int index;
        final boolean fluid;
        final MEHatchConfigComponent configuration;
        private final AbstractConfigurableStack<?, ?> slot;

        private Channel(AbstractConfigurableStack<?, ?> slot, int index, boolean fluid,
                MEHatchConfigComponent configuration) {
            this.slot = slot;
            this.index = index;
            this.fluid = fluid;
            this.configuration = configuration;
        }

        public @Nullable GenericStack config() {
            return configuration.getConfig(fluid, index);
        }

        abstract boolean accepts(AEKey key);

        abstract @Nullable AEKey key();

        abstract long freeSpace(long target);

        abstract void store(AEKey wanted, long amount);

        final boolean isHolding() {
            return !slot.isEmpty() && !slot.isResourceBlank() && slot.getAmount() > 0;
        }

        final long amount() {
            return Math.max(0L, slot.getAmount());
        }

        final void remove(long amount) {
            slot.decrement(amount);
        }

        final long room(AEKey wanted, long target) {
            AEKey current = key();
            if (current != null && !current.equals(wanted)) {
                return 0L;
            }
            return Math.max(0L, freeSpace(target));
        }
    }

    private static final class ItemChannel extends Channel {
        private final ConfigurableItemStack slot;

        private ItemChannel(ConfigurableItemStack slot, int index, MEHatchConfigComponent configuration) {
            super(slot, index, false, configuration);
            this.slot = slot;
        }

        @Override
        boolean accepts(AEKey key) {
            return key instanceof AEItemKey;
        }

        @Override
        @Nullable
        AEKey key() {
            return isHolding() ? AEItemKey.of(slot.getResource().toStack()) : null;
        }

        @Override
        long freeSpace(long target) {
            // Not bounded by the vanilla stack size: MI's limit only gates players/pipes, multiblocks read the amount directly.
            return target - amount();
        }

        @Override
        void store(AEKey wanted, long amount) {
            if (slot.isEmpty() || slot.isResourceBlank()) {
                slot.setKey(ItemVariant.of(((AEItemKey) wanted).toStack()));
            }
            slot.increment(amount);
        }
    }

    private static final class FluidChannel extends Channel {
        private final ConfigurableFluidStack slot;

        private FluidChannel(ConfigurableFluidStack slot, int index, MEHatchConfigComponent configuration) {
            super(slot, index, true, configuration);
            this.slot = slot;
        }

        @Override
        boolean accepts(AEKey key) {
            return key instanceof AEFluidKey;
        }

        @Override
        @Nullable
        AEKey key() {
            return isHolding() ? AEFluidKey.of(slot.getResource().toStack(1)) : null;
        }

        @Override
        long freeSpace(long target) {
            // Capacity is the stocking target, and a fluid slot throws above it.
            return slot.getRemainingSpace();
        }

        @Override
        void store(AEKey wanted, long amount) {
            slot.setKey(FluidVariant.of(((AEFluidKey) wanted).toStack(1)));
            slot.increment(amount);
        }
    }

    /** Room an ME output hatch's channels may admit this tick (cell, else network); simulated inserts cached per instance. */
    static final class OutputRoom {
        private final @Nullable MEStorage target;
        private final AEKeyType kind;
        private final IActionSource source;
        private final Object2LongMap<AEKey> cache = new Object2LongOpenHashMap<>();

        OutputRoom(@Nullable MEStorage target, AEKeyType kind, IActionSource source) {
            this.target = target;
            this.kind = kind;
            this.source = source;
        }

        long room(AEKey key) {
            if (target == null) {
                return 0L;
            }
            if (cache.containsKey(key)) {
                return cache.getLong(key);
            }
            long room = Math.max(0L, target.insert(key, Long.MAX_VALUE, Actionable.SIMULATE, source));
            cache.put(key, room);
            return room;
        }

        /**
         * Room for a not-yet-known output: the most the target takes of a new resource or one the cell already stores
         * (sizing only for a new type would stall once the cell has no bytes left for another). A refused output waits in
         * its slot and is never voided.
         */
        long anyResourceRoom() {
            long best = newTypeRoom();
            // Simulated inserts rather than free bytes, so partitions and void cards count.
            if (target instanceof StorageCell cell) {
                for (AEKey stored : cell.getAvailableStacks().keySet()) {
                    if (stored.getType() == kind) {
                        best = Math.max(best, room(stored));
                    }
                }
            }
            return best;
        }

        // Exact for basic cells (a new type costs bytes up front); anything else is probed with a common resource.
        private long newTypeRoom() {
            if (target instanceof BasicCellInventory cell) {
                if (!cell.canHoldNewItem()) {
                    return 0L;
                }
                long typeCost = (long) cell.getBytesPerType() * kind.getAmountPerByte();
                return Math.max(0L, cell.getRemainingItemCount() - typeCost);
            }
            return room(kind == AEKeyType.fluids() ? AEFluidKey.of(Fluids.WATER) : AEItemKey.of(Items.COBBLESTONE));
        }
    }
}
