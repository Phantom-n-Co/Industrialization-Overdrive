package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.MachineSource;
import aztech.modern_industrialization.machines.MachineComponent;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

// A MachineComponent because MachineBlockEntity.saveAdditional/loadAdditional are final and only persist components.
public final class MEGridNodeHolder implements MachineComponent.ServerOnly {
    private static final double IDLE_POWER_USAGE = 1.0D;

    private static final String NBT_KEY = "meGridNode";

    private final BlockEntity owner;
    private final IManagedGridNode node;
    private final IActionSource actionSource;

    private boolean destroyed;

    /** Services can only be added before the node is created, hence the storage provider here. */
    public MEGridNodeHolder(BlockEntity owner, ItemLike visualRepresentation, @Nullable IStorageProvider storageProvider) {
        this.owner = owner;
        // Every setter must run before create(...).
        this.node = GridHelper.createManagedNode(owner, Listener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(IDLE_POWER_USAGE)
                .setVisualRepresentation(visualRepresentation)
                .setInWorldNode(true)
                .setTagName(NBT_KEY);
        if (storageProvider != null) {
            node.addService(IStorageProvider.class, storageProvider);
        }
        this.actionSource = new MachineSource(node::getNode);
    }

    public boolean isActive() {
        return !destroyed && node.isActive();
    }

    public @Nullable IGrid getGrid() {
        return isActive() ? node.getGrid() : null;
    }

    public void requestStorageUpdate() {
        if (!destroyed) {
            IStorageProvider.requestUpdate(node);
        }
    }

    /** Call from {@code GridHelper.onFirstTick}. AE2 throws on a second create; a destroyed holder stays dead. */
    public void create() {
        if (!destroyed && !node.isReady() && owner.getLevel() != null && !owner.getLevel().isClientSide()) {
            node.create(owner.getLevel(), owner.getBlockPos());
        }
    }

    public void destroy() {
        destroyed = true;
        node.destroy();
    }

    public void setExposedSide(Direction side) {
        if (!destroyed) {
            node.setExposedOnSides(EnumSet.of(side));
        }
    }

    public void setOwningPlayer(Player player) {
        node.setOwningPlayer(player);
    }

    public void setExtraIdlePowerUsage(double extra) {
        if (!destroyed) {
            node.setIdlePowerUsage(IDLE_POWER_USAGE + extra);
        }
    }

    public IActionSource getActionSource() {
        return actionSource;
    }

    @Nullable
    public IGridNode getGridNode() {
        return node.getNode();
    }

    public IManagedGridNode getManagedNode() {
        return node;
    }

    /** Null unless powered with a channel; transfers draw AE like an ME Interface's. */
    @Nullable
    public MEStorage getNetworkStorage() {
        if (!isActive()) {
            return null;
        }
        IGrid grid = node.getGrid();
        if (grid == null) {
            return null;
        }
        return new PoweredStorage(grid.getStorageService().getInventory(), grid.getEnergyService());
    }

    @Override
    public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
        node.saveToNBT(tag);
    }

    @Override
    public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
        node.loadFromNBT(tag);
    }

    private record PoweredStorage(MEStorage inventory, IEnergySource energy) implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            return StorageHelper.poweredInsert(energy, inventory, what, amount, source, mode);
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            return StorageHelper.poweredExtraction(energy, inventory, what, amount, source, mode);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            inventory.getAvailableStacks(out);
        }

        @Override
        public Component getDescription() {
            return inventory.getDescription();
        }
    }

    private static final class Listener implements IGridNodeListener<BlockEntity> {
        private static final Listener INSTANCE = new Listener();

        @Override
        public void onSaveChanges(BlockEntity nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    }
}
