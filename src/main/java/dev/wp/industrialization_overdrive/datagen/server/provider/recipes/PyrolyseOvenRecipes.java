package dev.wp.industrialization_overdrive.datagen.server.provider.recipes;

import aztech.modern_industrialization.MIFluids;
import dev.wp.industrialization_overdrive.IOMachines;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class PyrolyseOvenRecipes extends RecipesServerDatagenProvider {
    public PyrolyseOvenRecipes(GatherDataEvent event) {
        super(event);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        addMachineRecipe(
                "pyrolyse_oven", "charcoal_from_logs", IOMachines.RecipeTypes.PYROLYSE_OVEN,
                8, 32 * 20 / 4,
                recipe -> recipe
                        .addItemInput(ItemTags.LOGS_THAT_BURN, 4)
                        .addItemOutput(Items.CHARCOAL, 5)
                        .addFluidOutput(MIFluids.CREOSOTE, 1000),
                output
        );
        // Not sure how this would work, as you can't really select the recipe you want if the output is the same
        // Maybe I can add a recipe(type?) selector in the GUI?
//        addMachineRecipe(
//                "pyrolyse_oven", "charcoal_from_logs_fast", IOMachines.RecipeTypes.PYROLYSE_OVEN,
//                8, 32*20/4/2,
//                recipe -> recipe
//                        .addItemInput(ItemTags.LOGS_THAT_BURN, 4)
//                        .addFluidInput(MIFluids.NITROGEN, 250)
//                        .addItemOutput(Items.CHARCOAL, 5)
//                        .addFluidOutput(MIFluids.CREOSOTE, 1000),
//                output
//        );
    }
}
