package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.StorageCell;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AECableType;
import aztech.modern_industrialization.MICapabilities;
import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.gui.MachineMenuServer;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOHatchTypes;
import dev.wp.industrialization_overdrive.machines.guicomponents.mehatch.MEHatchGui;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.swedz.tesseract.neoforge.compat.mi.guicomponent.slotpanel.ModularSlotPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class MEHatchBlockEntity extends HatchBlockEntity implements IGridConnectedBlockEntity, IPriorityHost,
        IStorageProvider {
    public static final long UNCONFIGURED_FLUID_CAPACITY = MEHatchInventories.UNCONFIGURED_FLUID_CAPACITY;

    public static final long OUTPUT_FLUID_CAPACITY = MEHatchInventories.OUTPUT_FLUID_CAPACITY;

    /** Stacks of each item channel MI may drop on a removal no player was warned about; the rest is lost. */
    private static final int MAX_DROPPED_STACKS = 27;

    private final MEHatchType hatchType;
    private final boolean input;
    private final MIInventory inventory;
    private final MEGridNodeHolder gridNode;
    private final MEHatchConfigComponent configuration;

    // Not persisted: a reloaded hatch warns again, the safe direction.
    private final Set<UUID> warnedPlayers = new HashSet<>();

    private boolean voidStockOnRemoval;

    private boolean transferDue = true;

    private @Nullable Direction exposedSide;

    // Not persisted: every controller rematches its shape after load.
    private @Nullable BlockPos controllerPos;

    private final MEHatchDemand demand = new MEHatchDemand();

    private final @Nullable MEHatchCellHost itemCell;
    private final @Nullable MEHatchCellHost fluidCell;

    private final long[] outputItemCapacity = new long[MEHatchConfigComponent.CHANNEL_COUNT];
    private final long[] outputFluidCapacity = new long[MEHatchConfigComponent.CHANNEL_COUNT];

    private boolean cellsMounted;

    private final MEHatchSlot.Channel[] itemChannelRefs = new MEHatchSlot.Channel[MEHatchConfigComponent.CHANNEL_COUNT];
    private final MEHatchSlot.Channel[] fluidChannelRefs = new MEHatchSlot.Channel[MEHatchConfigComponent.CHANNEL_COUNT];

    public MEHatchBlockEntity(BEP bep, MachineGuiParameters guiParams, MEHatchType hatchType, boolean input,
                              MIInventory inventory) {
        // Output direction, no auto-extract: the one face a cable may connect to; MI already supplies overlay, directions and wrench.
        super(bep, guiParams, OrientationComponent.Params.noFacing(false, false));

        this.hatchType = hatchType;
        this.input = input;
        this.inventory = inventory;
        this.gridNode = new MEGridNodeHolder(this, this.getBlockState().getBlock(), input ? null : this);
        this.configuration = new MEHatchConfigComponent();
        this.configuration.setTransferInterval(IO.config().meHatches().transferIntervalTicks());
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            itemChannelRefs[index] = new MEHatchSlot.Channel(this, false, index);
            fluidChannelRefs[index] = new MEHatchSlot.Channel(this, true, index);
        }

        // MachineBlockEntity.saveAdditional/loadAdditional are final and persist registered components
        // only, so the grid node holder must be registered here to round-trip its NBT.
        registerComponents(inventory, configuration, gridNode);
        registerGuiComponent(new MEHatchGui(this));

        // Output hatches buffer into a storage cell per resource kind they handle; MI drops the cells with the hatch.
        this.itemCell = !input && hatchType.handlesItems() ? new MEHatchCellHost("meItemCell", this) : null;
        this.fluidCell = !input && hatchType.handlesFluids() ? new MEHatchCellHost("meFluidCell", this) : null;
        if (itemCell != null || fluidCell != null) {
            ModularSlotPanel cellPanel = new ModularSlotPanel(this, 0);
            if (itemCell != null) {
                registerComponents(itemCell);
                cellPanel.with(MEHatchCellHost.ITEM_CELL, itemCell);
            }
            if (fluidCell != null) {
                registerComponents(fluidCell);
                cellPanel.with(MEHatchCellHost.FLUID_CELL, fluidCell);
            }
            registerGuiComponent(cellPanel);
        }
    }

    // Never trust client-supplied world data; the server-owned menu is the identity check.
    public static @Nullable MEHatchBlockEntity getActiveHatch(ServerPlayer player, int syncId) {
        if (player.containerMenu.containerId != syncId
                || !(player.containerMenu instanceof MachineMenuServer machineMenu)
                || !machineMenu.stillValid(player)
                || !(machineMenu.blockEntity instanceof MEHatchBlockEntity hatch)) {
            return null;
        }
        return hatch;
    }

    public @Nullable GenericStack getConfig(boolean fluid, int index) {
        return isValidChannel(fluid, index) ? configuration.getConfig(fluid, index) : null;
    }

    // Old config is left untouched when the network is offline or refuses returned stock; fluid target cuts reconcile
    // before setCapacity can clamp the amount.
    public boolean setConfigFromServer(boolean fluid, int index, @Nullable GenericStack config) {
        if (!isValidChannel(fluid, index) || !isValidConfigType(fluid, config)) {
            return false;
        }

        // Output amounts are filters only. Store a legal positive sentinel so the channel remains configured,
        // while input channels retain their requested target amount through the component's normalizer.
        GenericStack normalized = normalizeConfigForRole(fluid, config);

        MENetworkTransfer.Channel channel = channel(fluid, index);
        if (channel == null) {
            return false;
        }
        if (!MENetworkTransfer.reconcile(gridNode.getNetworkStorage(), channel, input,
                configuration.getConfig(fluid, index), normalized, gridNode.getActionSource())) {
            return false;
        }

        configuration.setConfig(fluid, index, normalized);
        transferDue = true;

        // Fluid capacities follow in applySlotRules: configuring an output's first channel changes the tank size
        // of every other one too.
        applySlotRules();
        setChanged();
        return true;
    }

    public CompoundTag saveConfiguration(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        configuration.writeNbt(tag, registries);
        return tag;
    }

    public boolean loadConfiguration(CompoundTag tag, HolderLookup.Provider registries, boolean simulate) {
        MEHatchConfigComponent loaded = new MEHatchConfigComponent();
        loaded.readNbt(tag, registries, false);
        boolean success = true;
        for (boolean fluid : new boolean[] {false, true}) {
            for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
                if (!isValidChannel(fluid, index)) {
                    continue;
                }
                GenericStack config = loaded.getConfig(fluid, index);
                if (simulate) {
                    success &= isValidConfigType(fluid, config);
                } else if (!Objects.equals(normalizeConfigForRole(fluid, config), configuration.getConfig(fluid, index))) {
                    success &= setConfigFromServer(fluid, index, config);
                }
            }
        }
        // Configurations saved before the interval or the smart pull switch existed leave this hatch's own alone.
        if (!simulate && tag.getInt(MEHatchConfigComponent.TRANSFER_INTERVAL_KEY) > 0) {
            setTransferInterval(loaded.getTransferInterval());
        }
        if (!simulate && tag.contains(MEHatchConfigComponent.SMART_PULL_KEY, Tag.TAG_BYTE)) {
            setSmartPull(loaded.isSmartPull());
        }
        if (!simulate && tag.contains(MEHatchConfigComponent.STORAGE_MODE_KEY, Tag.TAG_BYTE)) {
            setStorageMode(loaded.isStorageMode());
            setPriority(loaded.getPriority());
        }
        return success;
    }

    public int getTransferInterval() {
        return configuration.getTransferInterval();
    }

    public void linkController(BlockPos controller) {
        controllerPos = controller.immutable();
        transferDue = true;
    }

    @Override
    public void unlink() {
        super.unlink();
        controllerPos = null;
    }

    public void setTransferInterval(int ticks) {
        configuration.setTransferInterval(ticks);
        setChanged();
    }

    public boolean isStorageMode() {
        return configuration.isStorageMode();
    }

    public void setStorageMode(boolean storageMode) {
        if (!input && storageMode != configuration.isStorageMode()) {
            configuration.setStorageMode(storageMode);
            transferDue = true;
            setChanged();
        }
    }

    // Output: AE2 priority of the cells in Storage mode (lowest extracted first). Input: pull order, highest first.
    @Override
    public int getPriority() {
        return configuration.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        if (priority != configuration.getPriority()) {
            configuration.setPriority(priority);
            setChanged();
            if (!input) {
                // Only output hatches offer storage; AE2 throws when asked to remount a node that has none.
                gridNode.requestStorageUpdate();
            }
        }
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        cellsMounted = shouldMountCells();
        if (!cellsMounted) {
            return;
        }
        for (MEHatchCellHost host : new MEHatchCellHost[] {itemCell, fluidCell}) {
            if (cellOf(host) != null) {
                storageMounts.mount(new MEHatchCellHost.ExtractOnlyStorage(host), getPriority());
            }
        }
    }

    private static @Nullable StorageCell cellOf(@Nullable MEHatchCellHost host) {
        return host == null ? null : host.getCell();
    }

    private boolean hasCell() {
        return cellOf(itemCell) != null || cellOf(fluidCell) != null;
    }

    private boolean shouldMountCells() {
        return !input && configuration.isStorageMode() && gridNode.isActive() && hasCell();
    }

    // The mounted storage reads the current cell itself, so swapping cells needs no remount.
    private void updateStorageMount() {
        if (shouldMountCells() != cellsMounted) {
            cellsMounted = !cellsMounted;
            gridNode.requestStorageUpdate();
        }
    }

    public boolean isSmartPull() {
        return configuration.isSmartPull();
    }

    public void setSmartPull(boolean smartPull) {
        if (input && smartPull != configuration.isSmartPull()) {
            configuration.setSmartPull(smartPull);
            transferDue = true;
            setChanged();
        }
    }

    // Locking output slots makes the configuration a real filter: MI's crafter won't put a resource into a slot locked to another.
    // Runs every tick to upgrade old saves and retry locks that waited on a stale slot.
    private void applySlotRules() {
        boolean open = isOpenOutput();
        List<ConfigurableItemStack> itemSlots = inventory.getItemStacks();
        for (int index = 0; index < itemSlots.size() && index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            ConfigurableItemStack slot = itemSlots.get(index);
            if (input) {
                MEHatchInventoryAccess.uncapItemCapacity(slot);
            } else {
                syncItemCapacity(slot, outputItemCapacity[index]);
            }
            if (open) {
                MEHatchInventoryAccess.unlock(slot);
            } else {
                MEHatchInventoryAccess.lockToConfig(slot, configuration.getConfig(false, index));
            }
            bindChannel(slot, itemChannelRefs[index]);
        }
        List<ConfigurableFluidStack> fluidSlots = inventory.getFluidStacks();
        for (int index = 0; index < fluidSlots.size() && index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            ConfigurableFluidStack slot = fluidSlots.get(index);
            GenericStack config = configuration.getConfig(true, index);
            syncFluidCapacity(slot, fluidCapacityFor(index, config, open));
            if (open) {
                MEHatchInventoryAccess.unlock(slot);
            } else {
                MEHatchInventoryAccess.lockToConfig(slot, config);
            }
            bindChannel(slot, fluidChannelRefs[index]);
        }
    }

    private boolean isOpenOutput() {
        if (input) {
            return false;
        }
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            if (configuration.getConfig(false, index) != null || configuration.getConfig(true, index) != null) {
                return false;
            }
        }
        return true;
    }

    // Never below the held amount (setCapacity truncates, deleting fluid) nor while a recipe has reserved the tank
    // (the crafter places output unchecked); the resize follows on release.
    private static void syncFluidCapacity(ConfigurableFluidStack slot, long capacity) {
        if (slot.getCapacity() == capacity || slot.getAmount() > capacity) {
            return;
        }
        if (capacity < slot.getCapacity() && slot.isMachineLocked()) {
            return;
        }
        slot.setCapacity(capacity);
    }

    private static void syncItemCapacity(ConfigurableItemStack slot, long capacity) {
        long current = MEHatchInventoryAccess.itemCapacity(slot.getAdjustedCapacity());
        if (current == capacity || capacity < current && slot.isMachineLocked()) {
            return;
        }
        MEHatchInventoryAccess.setItemCapacity(slot, Math.max(capacity, slot.getAmount()));
    }

    // Any-resource channels (open hatch) get an equal share of the room so spillover can't exceed it; MI checks slots individually,
    // so a batch may overshoot and the excess waits in the slots.
    private void updateOutputCapacity(@Nullable MEStorage network) {
        if (input) {
            return;
        }
        // Storage mode is cells only: without one, there is nowhere to admit to.
        MEStorage fallback = configuration.isStorageMode() ? null : network;
        List<ConfigurableItemStack> itemSlots = inventory.getItemStacks();
        if (!itemSlots.isEmpty()) {
            MENetworkTransfer.OutputRoom room = new MENetworkTransfer.OutputRoom(roomTarget(itemCell, fallback), AEKeyType.items(),
                    gridNode.getActionSource());
            sizeOutputs(itemSlots, false, room, outputItemCapacity, Long.MAX_VALUE);
        }
        List<ConfigurableFluidStack> fluidSlots = inventory.getFluidStacks();
        if (!fluidSlots.isEmpty()) {
            MENetworkTransfer.OutputRoom room = new MENetworkTransfer.OutputRoom(roomTarget(fluidCell, fallback), AEKeyType.fluids(),
                    gridNode.getActionSource());
            sizeOutputs(fluidSlots, true, room, outputFluidCapacity, OUTPUT_FLUID_CAPACITY);
        }
    }

    private static @Nullable MEStorage roomTarget(@Nullable MEHatchCellHost cell, @Nullable MEStorage network) {
        StorageCell installed = cellOf(cell);
        return installed != null ? installed : network;
    }

    private void sizeOutputs(List<? extends AbstractConfigurableStack<?, ?>> slots, boolean fluid,
            MENetworkTransfer.OutputRoom room, long[] capacities, long maxCapacity) {
        int count = Math.min(slots.size(), MEHatchConfigComponent.CHANNEL_COUNT);
        Object2LongMap<AEKey> staged = new Object2LongOpenHashMap<>();
        int openSlots = 0;
        for (int index = 0; index < count; index++) {
            AbstractConfigurableStack<?, ?> slot = slots.get(index);
            AEKey key = heldKey(fluid, index);
            if (key != null) {
                staged.mergeLong(key, slot.getAmount(), Long::sum);
            } else if (slot.getLockedInstance() == null) {
                openSlots++;
            }
        }
        long openShare = openSlots == 0 ? 0L : room.anyResourceRoom() / openSlots;
        for (int index = 0; index < count; index++) {
            AbstractConfigurableStack<?, ?> slot = slots.get(index);
            AEKey key = heldKey(fluid, index);
            if (key == null) {
                key = MENetworkTransfer.keyOfLockInstance(fluid, slot.getLockedInstance());
            }
            long remaining;
            if (key != null) {
                remaining = Math.max(0L, room.room(key) - staged.getLong(key));
            } else {
                remaining = slot.getLockedInstance() == null ? openShare : 0L;
            }
            capacities[index] = Math.min(maxCapacity, slot.getAmount() + remaining);
        }
    }

    private @Nullable AEKey heldKey(boolean fluid, int index) {
        MENetworkTransfer.Channel channel = channel(fluid, index);
        return channel == null ? null : channel.key();
    }

    private static void bindChannel(Object stack, MEHatchSlot.Channel channel) {
        MEHatchSlot slot = (MEHatchSlot) stack;
        if (slot.io_getMEHatchChannel() != channel) {
            slot.io_setMEHatchChannel(channel);
        }
    }

    // Input stocks one run's consumption times the crafter's max batch size (MEHatchDemand). Returns the covered amount, 0 if refused.
    long configureForRecipeLock(boolean fluid, int index, Object instance, long requiredAmount) {
        if (requiredAmount <= 0) {
            return 0L;
        }
        AEKey key = MENetworkTransfer.keyOfLockInstance(fluid, instance);
        if (key == null) {
            return 0L;
        }
        long max = MEHatchConfigComponent.maxAmount(fluid);
        int batchSize = MEHatchDemand.batchSize();
        long target = requiredAmount > max / batchSize ? max : requiredAmount * batchSize;
        return setConfigFromServer(fluid, index, new GenericStack(key, target)) ? requiredAmount : 0L;
    }

    private long fluidCapacityFor(int index, @Nullable GenericStack config, boolean openOutput) {
        if (config == null) {
            return openOutput ? outputFluidCapacity[index] : UNCONFIGURED_FLUID_CAPACITY;
        }
        return input ? demand.target(true, index, config) : outputFluidCapacity[index];
    }

    public boolean setInputTarget(boolean fluid, int index, long amount) {
        if (!input || !isValidChannel(fluid, index) || amount <= 0) {
            return false;
        }

        GenericStack current = configuration.getConfig(fluid, index);
        if (current == null || !isValidConfigType(fluid, current)) {
            return false;
        }

        return setConfigFromServer(fluid, index, new GenericStack(current.what(), amount));
    }

    public MEHatchGui.Data collectGuiData() {
        List<@Nullable GenericStack> itemConfig = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);
        List<@Nullable GenericStack> itemStock = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);
        List<@Nullable GenericStack> fluidConfig = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);
        List<@Nullable GenericStack> fluidStock = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);
        List<Long> itemTarget = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);
        List<Long> fluidTarget = new ArrayList<>(MEHatchConfigComponent.CHANNEL_COUNT);

        boolean handlesItems = hatchType.handlesItems();
        boolean handlesFluids = hatchType.handlesFluids();

        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            GenericStack items = handlesItems ? configuration.getConfig(false, index) : null;
            GenericStack fluids = handlesFluids ? configuration.getConfig(true, index) : null;
            itemConfig.add(items);
            fluidConfig.add(fluids);
            itemTarget.add(input && items != null ? demand.target(false, index, items) : 0L);
            fluidTarget.add(input && fluids != null ? demand.target(true, index, fluids) : 0L);

            itemStock.add(stockOf(channel(false, index)));
            fluidStock.add(stockOf(channel(true, index)));
        }

        return new MEHatchGui.Data(itemConfig, itemStock, fluidConfig, fluidStock, configuration.getTransferInterval(),
                configuration.isSmartPull(), itemTarget, fluidTarget, configuration.isStorageMode(),
                configuration.getPriority(), hasCell());
    }

    private static @Nullable GenericStack stockOf(MENetworkTransfer.@Nullable Channel channel) {
        AEKey key = channel == null ? null : channel.key();
        return key == null ? null : new GenericStack(key, channel.amount());
    }

    private boolean isValidChannel(boolean fluid, int index) {
        return index >= 0
                && index < MEHatchConfigComponent.CHANNEL_COUNT
                && (fluid ? hatchType.handlesFluids() : hatchType.handlesItems());
    }

    private static boolean isValidConfigType(boolean fluid, @Nullable GenericStack config) {
        return config == null || (fluid
                ? config.what() instanceof AEFluidKey
                : config.what() instanceof AEItemKey);
    }

    private GenericStack normalizeConfigForRole(boolean fluid, @Nullable GenericStack config) {
        if (config == null || config.amount() <= 0) {
            return null;
        }
        long amount = Math.min(config.amount(), MEHatchConfigComponent.maxAmount(fluid));
        return input ? new GenericStack(config.what(), amount) : new GenericStack(config.what(), 1L);
    }

    public boolean isInput() {
        return input;
    }

    public MEHatchType getMEHatchType() {
        return hatchType;
    }

    @Override
    public HatchType getHatchType() {
        return switch (hatchType) {
            case ITEM -> input ? HatchTypes.ITEM_INPUT : HatchTypes.ITEM_OUTPUT;
            case FLUID -> input ? HatchTypes.FLUID_INPUT : HatchTypes.FLUID_OUTPUT;
            case MIXED -> input ? IOHatchTypes.MIXED_INPUT : IOHatchTypes.MIXED_OUTPUT;
        };
    }

    @Override
    public boolean upgradesToSteel() {
        return false;
    }

    @Override
    public MIInventory getInventory() {
        return inventory;
    }

    @Override
    public void appendItemInputs(List<ConfigurableItemStack> list) {
        if (input && hatchType.handlesItems()) {
            list.addAll(inventory.getItemStacks());
        }
    }

    @Override
    public void appendItemOutputs(List<ConfigurableItemStack> list) {
        if (!input && hatchType.handlesItems()) {
            list.addAll(inventory.getItemStacks());
        }
    }

    @Override
    public void appendFluidInputs(List<ConfigurableFluidStack> list) {
        if (input && hatchType.handlesFluids()) {
            list.addAll(inventory.getFluidStacks());
        }
    }

    @Override
    public void appendFluidOutputs(List<ConfigurableFluidStack> list) {
        if (!input && hatchType.handlesFluids()) {
            list.addAll(inventory.getFluidStacks());
        }
    }

    @Override
    protected void tickTransfer() {
        syncConnectionSide();
        applySlotRules();
        updateCellPower();
        updateStorageMount();
        stageIntoCells();

        boolean due = transferDue || isScheduledTransferTick();
        MEStorage storage = gridNode.getNetworkStorage();
        if (storage == null) {
            // Transfer as soon as the network is back, rather than waiting out the rest of an interval.
            transferDue = true;
        } else {
            transferWithNetwork(storage, due);
        }

        if (!input) {
            // Sizing queries every storage on the network, so it follows the transfer schedule. Capacities are
            // absolute, so between updates at most one interval's worth of extra output waits in the slots.
            if (due) {
                updateOutputCapacity(storage);
            }
            // Again after the push: a finished recipe's output pins the slot's lock, and waiting a tick would let the controller
            // re-reserve the empty open slot, so a restriction set mid-run would never apply.
            applySlotRules();
        }
    }

    private void transferWithNetwork(MEStorage storage, boolean due) {
        if (due) {
            transferDue = false;
            if (input) {
                demand.update(level, controllerPos, configuration);
                // Fluid tanks follow the new targets before the pull, or a rise would wait a whole interval.
                applySlotRules();
            }
            if (!input && !configuration.isStorageMode()) {
                pushCells(storage);
            }
            // channel() is the single gate on hatch type and slot count; a kind this hatch does not handle is null.
            for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
                transfer(storage, false, index);
                transfer(storage, true, index);
            }
        } else if (!input) {
            pushPendingLocks(storage);
        }
    }

    private void stageIntoCells() {
        if (input) {
            return;
        }
        StorageCell items = cellOf(itemCell);
        StorageCell fluids = cellOf(fluidCell);
        if (items == null && fluids == null) {
            return;
        }
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            if (items != null) {
                drain(items, channel(false, index));
            }
            if (fluids != null) {
                drain(fluids, channel(true, index));
            }
        }
    }

    private void pushCells(MEStorage storage) {
        for (MEHatchCellHost host : new MEHatchCellHost[] {itemCell, fluidCell}) {
            StorageCell cell = cellOf(host);
            if (cell != null) {
                MENetworkTransfer.moveAll(cell, storage, gridNode.getActionSource());
            }
        }
    }

    // Offset by position so hatches don't all transfer on the same tick; follows the game clock so interval changes apply at once.
    private boolean isScheduledTransferTick() {
        if (!input && configuration.isStorageMode()) {
            // No interval in Storage mode: sizing reads only the cell, and only leftovers ever go to the network.
            return true;
        }
        return Math.floorMod(level.getGameTime() + worldPosition.hashCode(), getTransferInterval()) == 0;
    }

    // Pushing between transfers lets a restriction set mid-run take effect; see tickTransfer.
    private void pushPendingLocks(MEStorage storage) {
        if (isOpenOutput()) {
            return;
        }
        List<ConfigurableItemStack> itemSlots = inventory.getItemStacks();
        List<ConfigurableFluidStack> fluidSlots = inventory.getFluidStacks();
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            if (index < itemSlots.size() && isWaitingForLock(itemSlots.get(index), configuration.getConfig(false, index))) {
                transfer(storage, false, index);
            }
            if (index < fluidSlots.size() && isWaitingForLock(fluidSlots.get(index), configuration.getConfig(true, index))) {
                transfer(storage, true, index);
            }
        }
    }

    private static boolean isWaitingForLock(AbstractConfigurableStack<?, ?> slot, @Nullable GenericStack config) {
        return !slot.isEmpty() && MEHatchInventoryAccess.isLockPending(slot, config);
    }

    private void transfer(MEStorage storage, boolean fluid, int index) {
        MENetworkTransfer.Channel channel = channel(fluid, index);
        if (channel == null) {
            return;
        }
        if (input) {
            GenericStack config = channel.config();
            if (config != null) {
                MENetworkTransfer.pull(storage, channel, demand.target(fluid, index, config),
                        () -> pullLimit(storage, config.what()), gridNode.getActionSource());
            }
        } else {
            // Unfiltered: slot locks already keep out unconfigured output, and leftovers must not block the slot.
            MENetworkTransfer.drain(storage, channel, gridNode.getActionSource());
        }
    }

    private long unmetDemand(AEKey key) {
        if (!input) {
            return 0L;
        }
        long unmet = 0L;
        for (boolean fluid : new boolean[] {false, true}) {
            for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
                MENetworkTransfer.Channel channel = channel(fluid, index);
                GenericStack config = channel == null ? null : channel.config();
                if (config != null && config.what().equals(key)) {
                    long held = key.equals(channel.key()) ? channel.amount() : 0L;
                    unmet = MEHatchPullOrder.saturatingAdd(unmet, Math.max(0L, demand.target(fluid, index, config) - held));
                }
            }
        }
        return unmet;
    }

    // Simulated extract, not the grid's cached inventory, which lags a tick and lets two pulls reach into a reservation.
    private long pullLimit(MEStorage storage, AEKey key) {
        long reserved = reservedAbove(key);
        if (reserved <= 0) {
            return Long.MAX_VALUE;
        }
        long available = storage.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, gridNode.getActionSource());
        return Math.max(0L, available - reserved);
    }

    private long reservedAbove(AEKey key) {
        IGrid grid = gridNode.getGrid();
        if (grid == null) {
            return 0L;
        }
        int priority = getPriority();
        List<MEHatchPullOrder.Claim> claims = new ArrayList<>();
        for (MEHatchBlockEntity other : grid.getActiveMachines(MEHatchBlockEntity.class)) {
            // Only hatches the rule can count, so needs it would ignore are never worked out.
            if (other.input && other.getPriority() > priority) {
                claims.add(new MEHatchPullOrder.Claim(other.getPriority(), other.unmetDemand(key)));
            }
        }
        return MEHatchPullOrder.reservedAbove(priority, claims);
    }

    private MENetworkTransfer.@Nullable Channel channel(boolean fluid, int index) {
        if (!isValidChannel(fluid, index)) {
            return null;
        }
        if (fluid) {
            List<ConfigurableFluidStack> slots = inventory.getFluidStacks();
            return index < slots.size()
                    ? MENetworkTransfer.channel(slots.get(index), index, configuration)
                    : null;
        }
        List<ConfigurableItemStack> slots = inventory.getItemStacks();
        return index < slots.size()
                ? MENetworkTransfer.channel(slots.get(index), index, configuration)
                : null;
    }

    // --------------------------------------------------- AE2 node lifecycle

    // Output direction rather than facing, so a cable can come from above or below.
    public Direction getConnectionSide() {
        return orientation.outputDirection;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        // A null side is an unsided query rather than a connection attempt, so it is answered.
        return dir == null || dir == getConnectionSide() ? gridNode.getGridNode() : null;
    }

    @Override
    public IManagedGridNode getMainNode() {
        return gridNode.getManagedNode();
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        if (player instanceof ServerPlayer serverPlayer && !isRemoved()) {
            openMenu(serverPlayer);
        }
    }

    // Close the menu server-side first so the client swaps screens directly instead of dropping to none (which recentres the mouse).
    public boolean openEditor(ServerPlayer player, MenuType<?> type) {
        player.doCloseContainer();
        if (MenuOpener.open(type, player, MenuLocators.forBlockEntity(this))) {
            return true;
        }
        // The client still shows the hatch screen: give it a server menu again.
        openMenu(player);
        return false;
    }

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return false;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getBlockState().getBlock());
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return dir == getConnectionSide() ? AECableType.SMART : AECableType.NONE;
    }

    private void updateCellPower() {
        boolean changed = itemCell != null && itemCell.pollChanged();
        changed |= fluidCell != null && fluidCell.pollChanged();
        if (changed) {
            double drain = (itemCell == null ? 0 : itemCell.getIdleDrain()) + (fluidCell == null ? 0 : fluidCell.getIdleDrain());
            gridNode.setExtraIdlePowerUsage(drain);
            // A new cell changes the room outputs are sized by.
            transferDue = true;
        }
    }

    /** Runs on placement and on load; AE2 wants the node created once the chunk ticks, not while it loads. */
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        syncConnectionSide();
        GridHelper.onFirstTick(this, hatch -> hatch.gridNode.create());
    }

    @Override
    public void onPlaced(@Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(placer, itemStack);
        // super picks the output direction from the placer's view, so the exposed face is only final now.
        syncConnectionSide();
        if (placer instanceof Player player) {
            gridNode.setOwningPlayer(player);
        }
    }

    @Override
    public boolean useWrench(Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!super.useWrench(player, hand, hitResult)) {
            return false;
        }
        syncConnectionSide();
        return true;
    }

    // Also runs every tick: a config card or pasted multiblock template sets MI's orientation without telling this hatch.
    public void syncConnectionSide() {
        Direction side = getConnectionSide();
        if (side != exposedSide) {
            exposedSide = side;
            gridNode.setExposedSide(side);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        gridNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        gridNode.destroy();
    }

    // --------------------------------------------------------------- extras

    // Without this, MachineBlock.onRemove would drop millions as item entities and forget fluids. A second sneaking break
    // bypasses the refusal and destroys the contents.
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof MEHatchBlockEntity hatch)
                || !hatch.hasUnreachableStock()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isShiftKeyDown() && hatch.warnedPlayers.contains(player.getUUID())) {
            hatch.voidStockOnRemoval = true;
            return;
        }

        hatch.warnedPlayers.add(player.getUUID());
        player.displayClientMessage(IO.text().meHatchBreakWouldLoseStock(), false);
        event.setCanceled(true);
    }

    private boolean hasUnreachableStock() {
        MEStorage storage = gridNode.getNetworkStorage();
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            if (isUnreachableStock(storage, channel(false, index))
                    || isUnreachableStock(storage, channel(true, index))) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnreachableStock(@Nullable MEStorage storage, MENetworkTransfer.@Nullable Channel channel) {
        if (channel == null || channel.amount() <= 0) {
            return false;
        }
        return storage == null || !MENetworkTransfer.fits(storage, channel, gridNode.getActionSource());
    }

    // MachineBlock.onRemove drops item slots and voids fluids; giving the network first refusal avoids huge entity drops.
    // Leftovers go to MI's handling, capped at MAX_DROPPED_STACKS.
    @Override
    public List<ItemStack> dropExtra() {
        MEStorage storage = gridNode.getNetworkStorage();
        if (storage != null) {
            for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
                drain(storage, channel(false, index));
                drain(storage, channel(true, index));
            }
        }

        // Only ever set by a warned player's deliberate sneaking break, and consumed here so it cannot outlive
        // the removal it was granted for.
        if (voidStockOnRemoval) {
            voidStockOnRemoval = false;
            inventory.getItemStacks().forEach(stack -> stack.setAmount(0));
            inventory.getFluidStacks().forEach(stack -> stack.setAmount(0));
        } else {
            capItemDrops();
        }
        return super.dropExtra();
    }

    /** MI drops an item channel a few items per entity, so an uncapped million-item channel stalls the server. */
    private void capItemDrops() {
        for (ConfigurableItemStack stack : inventory.getItemStacks()) {
            if (stack.isResourceBlank()) {
                continue;
            }
            long cap = (long) stack.getResource().getMaxStackSize() * MAX_DROPPED_STACKS;
            if (stack.getAmount() > cap) {
                IO.LOGGER.warn("ME hatch at {} voided {} {} its network would not take back", worldPosition,
                        stack.getAmount() - cap, stack.getResource().getItem());
                stack.setAmount(cap);
            }
        }
    }

    private void drain(MEStorage storage, MENetworkTransfer.@Nullable Channel channel) {
        if (channel != null) {
            MENetworkTransfer.drain(storage, channel, gridNode.getActionSource());
        }
    }

    @Override
    public List<Component> getTooltips(Item.TooltipContext ctx) {
        return input
                ? List.of(IO.text().meHatchInputDescription(), IO.text().meHatchConnectionFace())
                : List.of(IO.text().meHatchOutputDescription(), IO.text().meHatchOutputFiltering(),
                        IO.text().meHatchConnectionFace());
    }

    public static void registerGridNodeApi(BlockEntityType<?> bet) {
        MICapabilities.onEvent(event -> event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST, bet, (be, ctx) -> (MEHatchBlockEntity) be));
    }

}
