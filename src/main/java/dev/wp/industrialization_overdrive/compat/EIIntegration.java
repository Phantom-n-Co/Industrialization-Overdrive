package dev.wp.industrialization_overdrive.compat;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.swedz.extended_industrialization.item.machineconfig.MachineConfigPanel;
import net.swedz.extended_industrialization.machines.component.craft.processingarray.ProcessingArrayMachineComponent;
import net.swedz.extended_industrialization.machines.guicomponent.processingarraymachineslot.ProcessingArrayMachineSlot;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;

public final class EIIntegration {
    private EIIntegration() {}

    public static void registerMachineConfigPanel() {
        MachineConfigPanel.register("multi_processing_array_machines", MultiProcessingArrayMachineComponent.class,
                (player, target, component, holder, slotItem, item, simulation) -> {
                    if (MultiProcessingArrayMachineSlot.isMachine(item)) {
                        return MachineConfigPanel.ComponentTypeHandler.insertStack(player, target, component, slotItem, item, simulation);
                    }
                    return false;
                });
    }

    public static void writeProcessingArraySettings(MachineBlockEntity machine, CompoundTag tag, HolderLookup.Provider registries) {
        machine.components.forType(ProcessingArrayMachineComponent.class, machines -> machines.writeNbt(tag, registries));
    }

    public static void readProcessingArraySettings(MachineBlockEntity machine, CompoundTag tag, HolderLookup.Provider registries) {
        machine.components.forType(ProcessingArrayMachineComponent.class, machines -> machines.readNbt(tag, registries, false));
    }
}
