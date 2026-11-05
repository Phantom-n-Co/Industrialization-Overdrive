package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import dev.wp.industrialization_overdrive.mixin.accessor.AbstractConfigurableStackAccessor;
import dev.wp.industrialization_overdrive.mixin.accessor.ConfigurableItemStackAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * What the full and the inert ME hatch share. Must never import or load AE2 classes: the inert hatch builds its
 * inventory from here when AE2 is absent.
 */
public final class MEHatchInventories {
    // Heights match the hatch backgrounds (me_*_hatch.png, see MEHatchGuiClient): two rows end at y=79, four at y=133.
    private static final int ME_GUI_HEIGHT_SINGLE = 184;
    private static final int ME_GUI_HEIGHT_MIXED = 246;

    // Two pixels higher than backgroundHeight's height - 82, leaving the inventory label room below the hatch rows.
    private static final int ME_GUI_PLAYER_INVENTORY_OFFSET = 84;

    public static final int CHANNEL_COUNT = 9;

    // Zero hides idle channels from fluid readouts (Jade's FluidView.readDefault returns null below a positive capacity);
    // the slot object never changes, so a controller's link-time snapshot stays valid.
    public static final long UNCONFIGURED_FLUID_CAPACITY = 0L;

    public static final long OUTPUT_FLUID_CAPACITY = Integer.MAX_VALUE * 1000L;

    /** MI clamps adjusted capacity to 0..64, so anything above marks the slot as uncapped. */
    public static final int UNCAPPED_ITEM_CAPACITY = Integer.MAX_VALUE;

    private MEHatchInventories() {
    }

    public static int stockY(boolean fluid, boolean mixed) {
        return mixed && fluid ? 133 : 71;
    }

    public static MachineGuiParameters guiParameters(ResourceLocation machineId, MEHatchType hatchType) {
        int height = hatchType == MEHatchType.MIXED ? ME_GUI_HEIGHT_MIXED : ME_GUI_HEIGHT_SINGLE;
        MachineGuiParameters.Builder builder = new MachineGuiParameters.Builder(machineId, false).backgroundHeight(height);
        builder.playerInventoryY = height - ME_GUI_PLAYER_INVENTORY_OFFSET;
        return builder.build();
    }

    public static MIInventory create(MEHatchType hatchType, boolean input) {
        List<ConfigurableItemStack> itemStacks = new ArrayList<>();
        List<ConfigurableFluidStack> fluidStacks = new ArrayList<>();
        SlotPositions.Builder itemPositions = new SlotPositions.Builder();
        SlotPositions.Builder fluidPositions = new SlotPositions.Builder();
        boolean mixed = hatchType == MEHatchType.MIXED;

        if (hatchType.handlesItems()) {
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                itemStacks.add(normalize(input
                        ? ConfigurableItemStack.standardInputSlot()
                        : ConfigurableItemStack.standardOutputSlot(), input));
            }
            itemPositions.addSlots(8, stockY(false, mixed), CHANNEL_COUNT, 1);
        }

        if (hatchType.handlesFluids()) {
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                // An input stocks nothing until configured; an unconfigured output accepts any fluid (MEHatchBlockEntity.isOpenOutput).
                fluidStacks.add(normalize(input
                        ? ConfigurableFluidStack.standardInputSlot(UNCONFIGURED_FLUID_CAPACITY)
                        : ConfigurableFluidStack.standardOutputSlot(OUTPUT_FLUID_CAPACITY), input));
            }
            fluidPositions.addSlots(8, stockY(true, mixed), CHANNEL_COUNT, 1);
        }

        return new MIInventory(itemStacks, fluidStacks, itemPositions.build(), fluidPositions.build());
    }

    // Apply at construction: MI matches persisted stacks by these SlotConfig flags, so later changes drop saved contents.
    // The initial lock is also needed: a controller can reserve an open slot before the first tick (see lock).
    public static <T extends AbstractConfigurableStack<?, ?>> T normalize(T stack, boolean input) {
        AbstractConfigurableStackAccessor accessor = (AbstractConfigurableStackAccessor) stack;
        accessor.io_setPlayerInsert(false);
        accessor.io_setPlayerExtract(false);
        accessor.io_setPlayerLockable(false);
        if (stack instanceof ConfigurableItemStack itemStack) {
            uncapItemCapacity(itemStack);
            if (input) {
                lock(itemStack, Items.AIR);
            }
        } else if (stack instanceof ConfigurableFluidStack fluidStack && input) {
            lock(fluidStack, Fluids.EMPTY);
        }
        return stack;
    }

    // Not part of SlotConfig, so safe after load; re-apply there since old saves load with MI's 64.
    public static void uncapItemCapacity(ConfigurableItemStack stack) {
        if (stack.getAdjustedCapacity() != UNCAPPED_ITEM_CAPACITY) {
            ((ConfigurableItemStackAccessor) stack).io_setAdjustedCapacity(UNCAPPED_ITEM_CAPACITY);
            ((AbstractConfigurableStackAccessor) stack).io_notifyListeners();
        }
    }

    // MI's own lock entry points are unusable here (slot is not player-lockable). Never overrides a machine lock
    // to a different resource: the crafter would lose the reserved output; the lock applies once it releases.
    public static void lock(AbstractConfigurableStack<?, ?> stack, Object instance) {
        if (stack.isPlayerLocked() && stack.getLockedInstance() == instance) {
            return;
        }
        if (stack.isMachineLocked() && stack.getLockedInstance() != instance) {
            return;
        }
        // Never lock to something other than what the slot holds: MI machine-locks a topped-up slot to its held
        // resource, which throws if it is player-locked to another. Drained first; lock follows once empty.
        if (!stack.isResourceBlank() && stack.getResource().getObject() != instance) {
            return;
        }
        AbstractConfigurableStackAccessor accessor = (AbstractConfigurableStackAccessor) stack;
        accessor.io_setPlayerLocked(true);
        accessor.io_setLockedInstance(instance);
        accessor.io_notifyListeners();
    }
}
