package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.compat.viewer.ReiDraggable;
import aztech.modern_industrialization.inventory.BackgroundRenderedSlot;
import aztech.modern_industrialization.inventory.SlotGroup;
import aztech.modern_industrialization.machines.gui.GuiComponent;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import aztech.modern_industrialization.util.Simulation;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class MEHatchConfigSlot extends Slot implements ReiDraggable, BackgroundRenderedSlot {
    private final @Nullable MEHatchBlockEntity hatch;
    private final boolean fluid;
    private final int channel;
    private final boolean inputChannel;
    private final Supplier<@Nullable GenericStack> configSupplier;
    private final Supplier<@Nullable GenericStack> stockSupplier;

    public MEHatchConfigSlot(@Nullable MEHatchBlockEntity hatch, boolean fluid, int channel,
            boolean inputChannel, int x, int y, Supplier<@Nullable GenericStack> configSupplier,
            Supplier<@Nullable GenericStack> stockSupplier) {
        super(new SimpleContainer(1), 0, x, y);
        this.hatch = hatch;
        this.fluid = fluid;
        this.channel = channel;
        this.inputChannel = inputChannel;
        this.configSupplier = configSupplier;
        this.stockSupplier = stockSupplier;
    }

    public static void addRow(GuiComponent.MenuFacade menu, @Nullable MEHatchBlockEntity hatch, boolean fluid,
            boolean input, boolean mixed, IntFunction<@Nullable GenericStack> config,
            IntFunction<@Nullable GenericStack> stock) {
        for (int index = 0; index < MEHatchGui.Data.CHANNEL_COUNT; index++) {
            int channel = index;
            menu.addSlotToMenu(new MEHatchConfigSlot(hatch, fluid, index, input, channelX(index),
                    configY(fluid, mixed), () -> config.apply(channel), () -> stock.apply(channel)),
                    SlotGroup.CONFIGURABLE_STACKS);
        }
    }

    /** Read from synced data, not the stock slot: an ItemStack count is an int and renders saturated at a stack size. */
    public @Nullable GenericStack displayedStock() {
        return stockSupplier.get();
    }

    public static int channelX(int channel) {
        return 8 + channel * 18;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isFluid() {
        return fluid;
    }

    public @Nullable GenericStack displayedConfig() {
        return configSupplier.get();
    }

    public boolean isInputChannel() {
        return inputChannel;
    }

    // Each row reserves a COG_STRIP_HEIGHT band above it for the amount buttons.
    public static int configY(boolean fluid, boolean mixed) {
        return mixed && fluid ? 115 : 53;
    }

    private boolean validItem(ItemVariant itemKey) {
        return !fluid && itemKey != null && !itemKey.isBlank();
    }

    private boolean validFluid(FluidVariant fluidKey) {
        return fluid && fluidKey != null && !fluidKey.isBlank();
    }

    @Override
    public boolean dragItem(ItemVariant itemKey, Simulation simulation) {
        if (!validItem(itemKey)) {
            return false;
        }

        GenericStack assignment = GenericStack.fromItemStack(itemKey.toStack(1));
        if (assignment == null) {
            return false;
        }

        // MI sends DoSlotDraggingPacket only if the client ACT pass returns true, so accept without mutating or the drag is dropped.
        if (simulation.isSimulating() || hatch == null) {
            return true;
        }
        return hatch.setConfigFromServer(false, channel, assignment);
    }

    @Override
    public boolean dragFluid(FluidVariant fluidKey, Simulation simulation) {
        if (!validFluid(fluidKey)) {
            return false;
        }

        // Built from the variant, not just its Fluid, to keep data components.
        GenericStack assignment = GenericStack.fromFluidStack(fluidKey.toStack(AEFluidKey.AMOUNT_BUCKET));
        if (assignment == null) {
            return false;
        }

        if (simulation.isSimulating() || hatch == null) {
            return true;
        }
        return hatch.setConfigFromServer(true, channel, assignment);
    }

    /** Never a real stack: vanilla could pick it up or quick-move it, duplicating the configured resource. */
    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    public boolean isServerSide() {
        return hatch != null;
    }

    public void clearConfiguration() {
        if (hatch != null) {
            hatch.setConfigFromServer(fluid, channel, null);
        }
    }

    public void assignFromCarried(ItemStack carried) {
        if (hatch == null) {
            return;
        }
        if (carried.isEmpty()) {
            hatch.setConfigFromServer(fluid, channel, null);
            return;
        }

        GenericStack assignment;
        if (fluid) {
            FluidStack contained = FluidUtil.getFluidContained(carried).orElse(FluidStack.EMPTY);
            if (contained.isEmpty()) {
                return;
            }
            assignment = GenericStack.fromFluidStack(contained);
        } else {
            assignment = GenericStack.fromItemStack(carried);
        }

        if (assignment == null || assignment.amount() <= 0) {
            return;
        }
        if (fluid != (assignment.what() instanceof AEFluidKey)) {
            return;
        }
        hatch.setConfigFromServer(fluid, channel, assignment);
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

}
