package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.InertMEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventories;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchType;
import dev.wp.industrialization_overdrive.machines.blocks.MEHatchBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.HatchMIHookContext;

/**
 * ME hatch registration. Registered with or without AE2 so placed hatches survive its absence: without it they are
 * {@link InertMEHatchBlockEntity}. Nothing here may load an AE2-dependent class unless {@link IOUtil#isAE2Loaded}.
 */
public final class IOMEMachines {
    public static void hatches(HatchMIHookContext hook) {
        registerMEHatchPair(hook, "me_item", "ME Item", MEHatchType.ITEM, "me_hatch_item");
        registerMEHatchPair(hook, "me_fluid", "ME Fluid", MEHatchType.FLUID, "me_hatch_fluid");
        registerMEHatchPair(hook, "me_mixed", "ME Mixed", MEHatchType.MIXED, "me_hatch_mixed");
    }

    private static void registerMEHatchPair(HatchMIHookContext hook, String name, String englishName,
            MEHatchType hatchType, String overlayFolder) {
        hook.builder(name, englishName)
                .creator(MEHatchBlock::new)
                .special((bep, input, machineId) -> createHatch(bep, machineId, hatchType, input), true)
                // MachineCasings has no MV constant; CableTier.MV.casing is what MI's own advanced hatches use.
                // No front overlay: MI's output overlay already marks the face that matters.
                .builtinModel(CableTier.MV.casing, overlayFolder, model -> model.front(false))
                // Only the grid node capability: item/fluid handlers would let pipes, hoppers and storage buses connect
                // (ItemNetworkNode.canConnect, FluidNetworkNode.canConnect).
                .registrator(IOMEMachines::registerGridNodeApi)
                .registerMachine();
    }

    private static HatchBlockEntity createHatch(BEP bep, ResourceLocation machineId, MEHatchType hatchType, boolean input) {
        return IOUtil.isAE2Loaded
                ? FullHatch.create(bep, machineId, hatchType, input)
                : new InertMEHatchBlockEntity(bep, MEHatchInventories.guiParameters(machineId, hatchType), hatchType,
                        input, MEHatchInventories.create(hatchType, input));
    }

    private static void registerGridNodeApi(BlockEntityType<?> type) {
        if (IOUtil.isAE2Loaded) FullHatch.registerGridNodeApi(type);
    }

    // Separate class so the JVM only loads the AE2-dependent hatch once a call here actually runs.
    private static final class FullHatch {
        static HatchBlockEntity create(BEP bep, ResourceLocation machineId, MEHatchType hatchType, boolean input) {
            return new MEHatchBlockEntity(bep, MEHatchInventories.guiParameters(machineId, hatchType), hatchType, input,
                    MEHatchInventories.create(hatchType, input));
        }

        static void registerGridNodeApi(BlockEntityType<?> type) {
            MEHatchBlockEntity.registerGridNodeApi(type);
        }
    }
}
