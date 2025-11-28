package dev.wp.industrialization_overdrive.compat.mi;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOMachines;
import dev.wp.industrialization_overdrive.IOTooltips;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlotClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookEntrypoint;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookListener;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.ClientGuiComponentsMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineRecipeTypesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.ViewerSetupMIHookContext;

@MIHookEntrypoint
public class IOMIHookListener implements MIHookListener {
    @Override
    public void clientGuiComponents(ClientGuiComponentsMIHookContext hook) {
        hook.register(MultiProcessingArrayMachineSlot.TYPE, MultiProcessingArrayMachineSlotClient::new);
    }

//    @Override
//    public void machineCasings(MachineCasingsMIHookContext hook) {
//        IOMachines.casings(hook);
//    }

    @Override
    public void machineRecipeTypes(MachineRecipeTypesMIHookContext hook) {
        IOMachines.recipeTypes(hook);
    }

    @Override
    public void multiblockMachines(MultiblockMachinesMIHookContext hook) {
        IOMachines.multiblocks(hook);
    }

//    @Override
//    public void singleBlockCraftingMachines(SingleBlockCraftingMachinesMIHookContext hook) {
//        IOMachines.singleBlockCrafting(hook);
//    }

//    @Override
//    public void singleBlockSpecialMachines(SingleBlockSpecialMachinesMIHookContext hook) {
//        IOMachines.singleBlockSpecial(hook);
//    }

    @Override
    public void tooltips() {
        IOTooltips.init();
    }

    @Override
    public void viewerSetup(ViewerSetupMIHookContext hook) {
        ReiMachineRecipes.categories.forEach((__, params) -> {
            boolean isValidForMultiProcessingArray = false;
            for (ResourceLocation workstationId : params.workstations) {
                Item workstationItem = BuiltInRegistries.ITEM.get(workstationId);
                if (MultiProcessingArrayMachineSlot.isMachine(workstationItem)) {
                    isValidForMultiProcessingArray = true;
                    break;
                }
            }
            if (isValidForMultiProcessingArray)
                params.workstations.add(IO.id("multi_processing_array"));
        });
    }
}
