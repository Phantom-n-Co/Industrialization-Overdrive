package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented on every MI configurable stack by {@code ConfigurableStackPlayerLockMixin}. Set by the hatch every
 * tick on the server (survives MI replacing stack objects); MI's copies never carry it.
 */
public interface MEHatchSlot {
    @Nullable Channel io_getMEHatchChannel();

    void io_setMEHatchChannel(@Nullable Channel channel);

    /** The ME hatch channel backing an MI stack, for code that only has the stack (e.g. MI's recipe-lock loop). */
    record Channel(MEHatchBlockEntity hatch, boolean fluid, int index) {
        /** @return the amount the channel now covers, or {@code 0} when it refused */
        public long configureForRecipeLock(Object instance, long requiredAmount) {
            return hatch.configureForRecipeLock(fluid, index, instance, requiredAmount);
        }
    }
}
