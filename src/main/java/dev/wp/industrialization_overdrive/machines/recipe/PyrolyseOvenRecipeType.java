package dev.wp.industrialization_overdrive.machines.recipe;

import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.ProxyableMachineRecipeType;
import dev.wp.industrialization_overdrive.IO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

public class PyrolyseOvenRecipeType extends ProxyableMachineRecipeType {
    public PyrolyseOvenRecipeType(ResourceLocation id) {
        super(id);
    }

    @Override
    protected void fillRecipeList(Level world, List<RecipeHolder<MachineRecipe>> recipeList) {
        recipeList.addAll(getManagerRecipes(world));
        for (var cokeOvenRecipe : world.getRecipeManager().getAllRecipesFor(MIMachineRecipeTypes.COKE_OVEN)) {
            ResourceLocation id = IO.id("/pyrolyse_oven/generated/%s/%s".formatted(cokeOvenRecipe.id().getNamespace(), cokeOvenRecipe.id().getPath()));
            recipeList.add(new RecipeHolder<>(id, cokeOvenRecipe.value()));
        }

        recipeList.sort(Comparator.comparing(r -> r.id().getNamespace().equals(IO.ID) ? 0 : 1));
    }
}
