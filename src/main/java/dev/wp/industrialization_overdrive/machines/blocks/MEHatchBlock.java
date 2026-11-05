package dev.wp.industrialization_overdrive.machines.blocks;

import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/**
 * Own block class because AE2's tooltip framework casts the block entity to the paired class unchecked, so registering
 * against MI's shared {@code MachineBlock} would throw on every other MI machine's tooltip.
 */
public final class MEHatchBlock extends MachineBlock {
    public MEHatchBlock(BiFunction<BlockPos, BlockState, MachineBlockEntity> blockEntityConstructor,
            Properties properties) {
        super(blockEntityConstructor, properties);
    }
}
