package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import aztech.modern_industrialization.inventory.SlotGroup;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.swedz.tesseract.neoforge.compat.mi.component.SimpleItemStackComponent;
import net.swedz.tesseract.neoforge.compat.mi.guicomponent.slotpanel.ModularSlotPanel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Cell slot of an ME output hatch; AE2 cells only write back on {@link StorageCell#persist()}, so persist whenever the stack leaves. */
public final class MEHatchCellHost extends SimpleItemStackComponent implements ISaveProvider {
    private static final ResourceLocation ATLAS = IO.id("textures/gui/container/me_hatch_slot_atlas.png");

    public static final ResourceLocation ITEM_CELL = registerSlot("me_hatch_item_cell", AEKeyType.items(), 1,
            () -> IO.text().meHatchItemCellSlot());

    public static final ResourceLocation FLUID_CELL = registerSlot("me_hatch_fluid_cell", AEKeyType.fluids(), 2,
            () -> IO.text().meHatchFluidCellSlot());

    private final MachineBlockEntity owner;

    private @Nullable StorageCell cell;
    private boolean opened;
    private boolean changed = true;

    public MEHatchCellHost(String stackTagKey, MachineBlockEntity owner) {
        super(stackTagKey);
        this.owner = owner;
    }

    public static void init() {
    }

    /** Accepts AE2 cells not in {@link IOTags.Items#ME_OUTPUT_HATCH_CELL_BLACKLIST}; basic cells must match the slot's kind, other handlers' cells fit either. */
    public static boolean accepts(ItemStack stack, AEKeyType kind) {
        if (stack.isEmpty() || stack.is(IOTags.Items.ME_OUTPUT_HATCH_CELL_BLACKLIST)) {
            return false;
        }
        if (stack.getItem() instanceof IBasicCellItem basicCell) {
            return basicCell.getKeyType() == kind;
        }
        return StorageCells.isCellHandled(stack);
    }

    private static ResourceLocation registerSlot(String name, AEKeyType kind, int atlasColumn, Supplier<Component> tooltip) {
        return ModularSlotPanel.registerSlot(IO.id(name), SlotGroup.UPGRADES, 1, stack -> accepts(stack, kind),
                ATLAS, atlasColumn * 18, 0, tooltip);
    }

    public @Nullable StorageCell getCell() {
        if (!opened) {
            opened = true;
            cell = stack.isEmpty() ? null : StorageCells.getCellInventory(stack, this);
        }
        return cell;
    }

    public double getIdleDrain() {
        StorageCell current = getCell();
        return current == null ? 0 : current.getIdleDrain();
    }

    public boolean pollChanged() {
        boolean was = changed;
        changed = false;
        return was;
    }

    @Override
    public void saveChanges() {
        if (owner.getLevel() != null) {
            owner.getLevel().blockEntityChanged(owner.getBlockPos());
        }
    }

    @Override
    public ItemStack getStack() {
        persist();
        return super.getStack();
    }

    @Override
    public ItemStack getDrop() {
        persist();
        return super.getDrop();
    }

    @Override
    public void setStack(ItemStack stack) {
        if (isSameCell(stack)) {
            return;
        }
        persist();
        super.setStack(stack);
        invalidate();
    }

    @Override
    public void setStackServer(MachineBlockEntity machine, ItemStack stack) {
        if (isSameCell(stack)) {
            return;
        }
        persist();
        super.setStackServer(machine, stack);
        invalidate();
    }

    @Override
    public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
        persist();
        super.writeNbt(tag, registries);
    }

    @Override
    public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
        super.readNbt(tag, registries, isUpgradingMachine);
        invalidate();
    }

    // MI's slot writes back a copy of the stack after every click; keep the open cell so contents aren't lost to a reopen.
    private boolean isSameCell(ItemStack replacement) {
        persist();
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, replacement)
                && stack.getCount() == replacement.getCount();
    }

    private void persist() {
        if (cell != null) {
            cell.persist();
        }
    }

    private void invalidate() {
        cell = null;
        opened = false;
        changed = true;
    }

    /** Storage-mode view of an output hatch's cell: extract-only, reading the host's current cell on every call (no remount on swap). */
    static final class ExtractOnlyStorage implements MEStorage {
        private final MEHatchCellHost host;

        ExtractOnlyStorage(MEHatchCellHost host) {
            this.host = host;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            StorageCell cell = host.getCell();
            return cell == null ? 0L : cell.extract(what, amount, mode, source);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            StorageCell cell = host.getCell();
            if (cell != null) {
                cell.getAvailableStacks(out);
            }
        }

        @Override
        public Component getDescription() {
            StorageCell cell = host.getCell();
            return cell == null ? Component.empty() : cell.getDescription();
        }
    }
}
