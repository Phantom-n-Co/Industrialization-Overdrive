package dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot;

import aztech.modern_industrialization.inventory.HackySlot;
import aztech.modern_industrialization.inventory.SlotGroup;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.gui.GuiComponent;
import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOConfig;
import dev.wp.industrialization_overdrive.IOTags;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.swedz.tesseract.neoforge.helper.RegistryHelper;

import java.util.function.Supplier;

public final class MultiProcessingArrayMachineSlot implements GuiComponentServer<Unit, Integer> {
    public static final Type<Unit, Integer> TYPE = new Type<>(IO.id("multi_processing_array_machine_slot"),
            StreamCodec.unit(Unit.INSTANCE), ByteBufCodecs.VAR_INT);

    public static int getSlotX(MachineGuiParameters guiParameters) {
        return guiParameters.backgroundWidth + 6;
    }

    public static int getSlotY() {
        return 106 - (IOConfig.allowUpgradesInMultiProcessingArray ? 0 : 20);
    }

    public static boolean isMachine(Item item) {
        if (item instanceof BlockItem blockItem &&
                blockItem.getBlock() instanceof MachineBlock machineBlock &&
                machineBlock.getBlockEntityInstance() instanceof AbstractElectricCraftingMultiblockBlockEntity blockEntity) {
            return !RegistryHelper.holder(BuiltInRegistries.ITEM, item).is(IOTags.Items.MULTI_PROCESSING_ARRAY_BLACKLIST);
        }
        return false;
    }

    public static boolean isMachine(ItemStack stack) {
        return isMachine(stack.getItem());
    }

    public static AbstractElectricCraftingMultiblockBlockEntity getMachine(ItemStack itemStack) {
        return (AbstractElectricCraftingMultiblockBlockEntity) ((MachineBlock) ((BlockItem) itemStack.getItem()).getBlock()).getBlockEntityInstance();
    }

    private final MachineBlockEntity machine;
    private final Supplier<Integer> getMaxMachines;
    private final MultiProcessingArrayMachineComponent machines;

    public MultiProcessingArrayMachineSlot(MachineBlockEntity machine, Supplier<Integer> getMaxMachines, MultiProcessingArrayMachineComponent machines) {
        this.machine = machine;
        this.getMaxMachines = getMaxMachines;
        this.machines = machines;
    }

    @Override
    public void setupMenu(GuiComponent.MenuFacade menu) {
        menu.addSlotToMenu(
                new HackySlot(getSlotX(machine.guiParams), getSlotY()) {
                    @Override
                    protected ItemStack getRealStack() {
                        return machines.getMachines();
                    }

                    @Override
                    protected void setRealStack(ItemStack itemStack) {
                        machines.setMachines(machine, itemStack);
                    }

                    @Override
                    public boolean mayPlace(ItemStack itemStack) {
                        return isMachine(itemStack.getItem());
                    }

                    @Override
                    public int getMaxStackSize() {
                        return getMaxMachines.get();
                    }
                },
                SlotGroup.CONFIGURABLE_STACKS
        );
    }

    @Override
    public Unit getParams() {
        return Unit.INSTANCE;
    }

    @Override
    public Integer extractData() {
        return getMaxMachines.get();
    }

    @Override
    public Type<Unit, Integer> getType() {
        return TYPE;
    }
}