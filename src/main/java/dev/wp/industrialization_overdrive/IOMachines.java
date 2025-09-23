package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.compat.rei.machines.SteamMode;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.google.common.collect.Maps;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.MultiProcessingArrayBlockEntity;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.PyrolyseOvenBlockEntity;
import dev.wp.industrialization_overdrive.machines.recipe.PyrolyseOvenRecipeType;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineRecipeTypesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;

import java.util.Map;
import java.util.function.Function;

import static aztech.modern_industrialization.machines.models.MachineCasings.BRONZE_PLATED_BRICKS;
import static aztech.modern_industrialization.machines.models.MachineCasings.SOLID_TITANIUM;

public final class IOMachines {
//    public static void blastFurnaceTiers(BlastFurnaceTiersMIHookContext hook) {
//    }
//
//    public static final class Casings {
//    }
//
//    public static void casings(MachineCasingsMIHookContext hook) {
//    }

    public static final class RecipeTypes {
        public static MachineRecipeType PYROLYSE_OVEN;

        private static final Map<MachineRecipeType, String> RECIPE_TYPE_NAMES = Maps.newHashMap();

        public static Map<MachineRecipeType, String> getNames() {
            return RECIPE_TYPE_NAMES;
        }

        private static MachineRecipeType create(MachineRecipeTypesMIHookContext hook, String englishName, String id, Function<ResourceLocation, MachineRecipeType> creator) {
            MachineRecipeType recipeType = hook.create(id, creator);
            RECIPE_TYPE_NAMES.put(recipeType, englishName);
            return recipeType;
        }

        private static MachineRecipeType create(MachineRecipeTypesMIHookContext hook, String englishName, String id) {
            return create(hook, englishName, id, MachineRecipeType::new);
        }
    }

    public static void recipeTypes(MachineRecipeTypesMIHookContext hook) {
        RecipeTypes.PYROLYSE_OVEN = RecipeTypes.create(hook, "Pyrolyse Oven", "pyrolyse_oven", PyrolyseOvenRecipeType::new).withFluidInputs().withFluidOutputs().withItemInputs().withItemOutputs();
    }

    public static void multiblocks(MultiblockMachinesMIHookContext hook) {
        hook.builder("multi_processing_array", "Multi Processing Array", MultiProcessingArrayBlockEntity::new)
                .builtinModel(SOLID_TITANIUM, "multi_processing_array", (model) -> model.front(true))
                .registrator((__) -> MultiProcessingArrayBlockEntity.registerReiShapes())
                .registerMachine();
        hook.builder("pyrolyse_oven", "Pyrolyse Oven", PyrolyseOvenBlockEntity::new)
                .builtinModel(BRONZE_PLATED_BRICKS, "pyrolyse_oven", (model) -> model.front(true))
                .gui(SteamMode.ELECTRIC_ONLY, RecipeTypes.PYROLYSE_OVEN, (gui -> {
                    gui.slots(slots -> {
                        slots.itemInput(56, 35);
                        slots.itemOutput(102, 35);
                        slots.fluidInput(36, 35);
                        slots.fluidOutput(122, 35);
                    });
                    gui.progressBar(77, 33, "arrow");
                }))
                .registerRecipeCategory()
                .registerMachine();
        ReiMachineRecipes.registerWorkstation(MI.id("coke_oven"), IO.id("pyrolyse_oven"));
    }

//    public static void singleBlockCrafting(SingleBlockCraftingMachinesMIHookContext hook) {
//    }
//
//    public static void singleBlockSpecial(SingleBlockSpecialMachinesMIHookContext hook) {
//    }


}
