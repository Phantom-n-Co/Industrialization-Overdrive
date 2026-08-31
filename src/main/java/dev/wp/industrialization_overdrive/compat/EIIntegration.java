package dev.wp.industrialization_overdrive.compat;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.swedz.extended_industrialization.item.machineconfig.MachineConfigPanel;
import net.swedz.extended_industrialization.EITags;
import net.swedz.extended_industrialization.machines.blockentity.MachineChainerMachineBlockEntity;
import net.swedz.extended_industrialization.machines.component.chainer.ChainerComponent;
import net.swedz.extended_industrialization.machines.component.craft.processingarray.ProcessingArrayMachineComponent;
import net.swedz.extended_industrialization.machines.component.tesla.AestheticTeslaCoilComponent;
import net.swedz.extended_industrialization.machines.component.tesla.network.receiver.TeslaReceiverComponent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    public static boolean isMachineChainer(BlockEntity blockEntity) {
        return blockEntity instanceof MachineChainerMachineBlockEntity;
    }

    public static Set<BlockPos> getChainedPositions(Level level, BlockPos pos) {
        Set<BlockPos> positions = new HashSet<>();
        if (level.getBlockEntity(pos) instanceof MachineChainerMachineBlockEntity chainer) {
            collectChainedPositions(chainer, positions);
        }
        return positions;
    }

    private static void collectChainedPositions(MachineChainerMachineBlockEntity chainer, Set<BlockPos> positions) {
        if (!positions.add(chainer.getBlockPos())) return;
        for (BlockPos pos : chainer.getChainerComponent().links().positions()) {
            if (chainer.getLevel().getBlockEntity(pos) instanceof MachineChainerMachineBlockEntity nested) {
                collectChainedPositions(nested, positions);
            } else {
                positions.add(pos);
            }
        }
    }

    public static void copyChainer(Level level, BlockPos pos, Map<BlockPos, IOComponents.BlockData> template,
                                   int depth, Function<MachineBlockEntity, CompoundTag> settings) {
        if (!(level.getBlockEntity(pos) instanceof MachineChainerMachineBlockEntity chainer)) return;

        template.put(BlockPos.ZERO, new IOComponents.BlockData(level.getBlockState(pos).getBlock().asItem(), settings.apply(chainer)));
        copyChainedBlocks(chainer, pos, template, depth, settings, new HashSet<>());
    }

    public static void copyChainedBlocks(Level level, BlockPos controllerPos, Map<BlockPos, IOComponents.BlockData> template,
                                         int depth, Function<MachineBlockEntity, CompoundTag> settings) {
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos relative : List.copyOf(template.keySet())) {
            BlockPos pos = controllerPos.offset(relative);
            if (level.getBlockEntity(pos) instanceof MachineChainerMachineBlockEntity chainer) {
                copyChainedBlocks(chainer, controllerPos, template, depth, settings, visited);
            }
        }
    }

    private static void copyChainedBlocks(MachineChainerMachineBlockEntity chainer, BlockPos controllerPos,
                                          Map<BlockPos, IOComponents.BlockData> template, int depth,
                                          Function<MachineBlockEntity, CompoundTag> settings, Set<BlockPos> visited) {
        if (depth == 0 || !visited.add(chainer.getBlockPos())) return;

        ChainerComponent component = chainer.getChainerComponent();
        for (BlockPos pos : component.links().positions()) {
            if (visited.contains(pos)) continue;

            BlockEntity blockEntity = chainer.getLevel().getBlockEntity(pos);
            if (blockEntity == null && !chainer.getLevel().getBlockState(pos).is(EITags.Blocks.MACHINE_CHAINER_RELAY)) continue;
            CompoundTag blockSettings = blockEntity instanceof MachineBlockEntity machine ? settings.apply(machine) : new CompoundTag();
            template.putIfAbsent(pos.subtract(controllerPos), new IOComponents.BlockData(
                    chainer.getLevel().getBlockState(pos).getBlock().asItem(), blockSettings));

            if (blockEntity instanceof MachineChainerMachineBlockEntity nested && depth != 1) {
                copyChainedBlocks(nested, controllerPos, template, depth < 0 ? -1 : depth - 1, settings, visited);
            } else {
                visited.add(pos);
            }
        }
    }
}
