package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import dev.wp.industrialization_overdrive.IOHatchTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Stands in for {@link MEHatchBlockEntity} when AE2 is absent: a plain MI hatch with the same slots that carries the
 * full hatch's ME data through load and save untouched, so it is all there when AE2 returns. No AE2 imports here or in
 * anything this class reaches.
 */
public final class InertMEHatchBlockEntity extends HatchBlockEntity {
    /**
     * Every key the full hatch's own components write (MEGridNodeHolder, MEHatchConfigComponent, MEHatchCellHost). Keys
     * MI owns, such as the inventory and orientation, must stay out. Keep in step with those components.
     */
    static final List<String> ME_KEYS = List.of(
            "meGridNode",
            "meItemConfigs", "meFluidConfigs", "meTransferInterval", "meSmartPull", "meStorageMode", "mePriority",
            "meItemCell", "meFluidCell");

    private final MEHatchType hatchType;
    private final boolean input;
    private final MIInventory inventory;

    public InertMEHatchBlockEntity(BEP bep, MachineGuiParameters guiParams, MEHatchType hatchType, boolean input,
                                   MIInventory inventory) {
        super(bep, guiParams, OrientationComponent.Params.noFacing(false, false));
        this.hatchType = hatchType;
        this.input = input;
        this.inventory = inventory;
        registerComponents(inventory, new Passthrough());
    }

    private static final class Passthrough implements MachineComponent.ServerOnly {
        private final CompoundTag kept = new CompoundTag();

        @Override
        public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
            for (String key : kept.getAllKeys()) {
                tag.put(key, kept.get(key).copy());
            }
        }

        @Override
        public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
            for (String key : ME_KEYS) {
                if (tag.contains(key)) {
                    kept.put(key, tag.get(key).copy());
                }
            }
        }
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
}
