package dev.wp.industrialization_overdrive.compat;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.swedz.extended_industrialization.item.machineconfig.MachineConfigPanel;
import net.swedz.extended_industrialization.machines.component.craft.processingarray.ProcessingArrayMachineComponent;
import net.swedz.extended_industrialization.machines.component.tesla.AestheticTeslaCoilComponent;
import net.swedz.extended_industrialization.machines.component.tesla.network.receiver.TeslaReceiverComponent;

public final class EIIntegration {
    private EIIntegration() {
    }

    public static void registerMachineConfigPanel() {
        MachineConfigPanel.register("multi_processing_array_machines", MultiProcessingArrayMachineComponent.class,
                (player, target, component, holder, slotItem, item, simulation) -> {
                    if (MultiProcessingArrayMachineSlot.isMachine(item)) {
                        return MachineConfigPanel.ComponentTypeHandler.insertStack(player, target, component, slotItem, item, simulation);
                    }
                    return false;
                });
    }

    public static void writeSettings(MachineBlockEntity machine, CompoundTag tag, HolderLookup.Provider registries) {
        machine.components.forType(ProcessingArrayMachineComponent.class, machines -> machines.writeNbt(tag, registries));
        machine.components.forType(AestheticTeslaCoilComponent.class, aesthetic -> aesthetic.writeNbt(tag, registries));
        machine.components.forType(TeslaReceiverComponent.class, receiver -> receiver.writeNbt(tag, registries));
    }

    public static void readSettings(MachineBlockEntity machine, CompoundTag tag, HolderLookup.Provider registries) {
        machine.components.forType(ProcessingArrayMachineComponent.class, machines -> machines.readNbt(tag, registries, false));
        machine.components.forType(AestheticTeslaCoilComponent.class, aesthetic -> aesthetic.readNbt(tag, registries, false));
        machine.components.forType(TeslaReceiverComponent.class, receiver -> receiver.readNbt(tag, registries, false));
    }
}
