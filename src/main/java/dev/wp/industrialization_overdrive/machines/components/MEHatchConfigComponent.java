package dev.wp.industrialization_overdrive.machines.components;

import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.machines.MachineComponent;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventories;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Persisted per-channel item/fluid configuration of an ME hatch, plus its interval, smart pull, storage mode and priority. */
public final class MEHatchConfigComponent implements MachineComponent.ServerOnly {
    public static final int CHANNEL_COUNT = MEHatchInventories.CHANNEL_COUNT;

    public static final long MAX_ITEM_AMOUNT = Integer.MAX_VALUE;

    /** In millibuckets. */
    public static final long MAX_FLUID_AMOUNT = MEHatchInventories.OUTPUT_FLUID_CAPACITY;

    public static final int MAX_TRANSFER_INTERVAL = 1200;

    private static final String ITEM_CONFIGS_KEY = "meItemConfigs";
    private static final String FLUID_CONFIGS_KEY = "meFluidConfigs";
    public static final String TRANSFER_INTERVAL_KEY = "meTransferInterval";
    public static final String SMART_PULL_KEY = "meSmartPull";
    public static final String STORAGE_MODE_KEY = "meStorageMode";
    private static final String PRIORITY_KEY = "mePriority";

    private final @Nullable GenericStack[] itemConfigs = new GenericStack[CHANNEL_COUNT];
    private final @Nullable GenericStack[] fluidConfigs = new GenericStack[CHANNEL_COUNT];

    private int transferInterval = 1;

    private boolean smartPull = true;

    /** Output hatch offers its cells as extract-only storage instead of pushing (off = Push mode). */
    private boolean storageMode;

    private int priority;

    public static long maxAmount(boolean fluid) {
        return fluid ? MAX_FLUID_AMOUNT : MAX_ITEM_AMOUNT;
    }

    public @Nullable GenericStack getConfig(boolean fluid, int index) {
        return configs(fluid)[checkIndex(index)];
    }

    public void setConfig(boolean fluid, int index, @Nullable GenericStack config) {
        configs(fluid)[checkIndex(index)] = normalizeAmount(fluid, config);
    }

    public int getTransferInterval() {
        return transferInterval;
    }

    public void setTransferInterval(int ticks) {
        transferInterval = Math.clamp(ticks, 1, MAX_TRANSFER_INTERVAL);
    }

    public boolean isSmartPull() {
        return smartPull;
    }

    public void setSmartPull(boolean smartPull) {
        this.smartPull = smartPull;
    }

    public boolean isStorageMode() {
        return storageMode;
    }

    public void setStorageMode(boolean storageMode) {
        this.storageMode = storageMode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(ITEM_CONFIGS_KEY, writeConfigs(itemConfigs, registries));
        tag.put(FLUID_CONFIGS_KEY, writeConfigs(fluidConfigs, registries));
        tag.putInt(TRANSFER_INTERVAL_KEY, transferInterval);
        tag.putBoolean(SMART_PULL_KEY, smartPull);
        tag.putBoolean(STORAGE_MODE_KEY, storageMode);
        tag.putInt(PRIORITY_KEY, priority);
    }

    @Override
    public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
        readConfigs(false, itemConfigs, tag.getList(ITEM_CONFIGS_KEY, Tag.TAG_COMPOUND), registries);
        readConfigs(true, fluidConfigs, tag.getList(FLUID_CONFIGS_KEY, Tag.TAG_COMPOUND), registries);
        // Older saves lack the interval/smart pull keys: keep the defaults.
        if (tag.getInt(TRANSFER_INTERVAL_KEY) > 0) {
            setTransferInterval(tag.getInt(TRANSFER_INTERVAL_KEY));
        }
        if (tag.contains(SMART_PULL_KEY, Tag.TAG_BYTE)) {
            smartPull = tag.getBoolean(SMART_PULL_KEY);
        }
        storageMode = tag.getBoolean(STORAGE_MODE_KEY);
        priority = tag.getInt(PRIORITY_KEY);
    }

    private @Nullable GenericStack[] configs(boolean fluid) {
        return fluid ? fluidConfigs : itemConfigs;
    }

    private static int checkIndex(int index) {
        if (index < 0 || index >= CHANNEL_COUNT) {
            throw new IndexOutOfBoundsException("ME hatch channel index " + index
                    + " is outside 0.." + (CHANNEL_COUNT - 1));
        }
        return index;
    }

    private static @Nullable GenericStack normalizeAmount(boolean fluid, @Nullable GenericStack stack) {
        if (stack == null || stack.amount() <= 0) {
            return null;
        }
        long max = maxAmount(fluid);
        return stack.amount() <= max ? stack : new GenericStack(stack.what(), max);
    }

    private static ListTag writeConfigs(@Nullable GenericStack[] configs, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (GenericStack config : configs) {
            list.add(GenericStack.writeTag(registries, config));
        }
        return list;
    }

    private static void readConfigs(boolean fluid, @Nullable GenericStack[] configs, ListTag stored,
            HolderLookup.Provider registries) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            GenericStack config = null;
            if (i < stored.size()) {
                CompoundTag configTag = stored.getCompound(i);
                if (!configTag.isEmpty()) {
                    try {
                        config = GenericStack.readTag(registries, configTag);
                    } catch (RuntimeException ignored) {
                        // Malformed or removed AE key: treat as missing.
                    }
                }
            }
            configs[i] = normalizeAmount(fluid, config);
        }
    }
}
