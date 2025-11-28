package dev.wp.industrialization_overdrive.datagen.server.provider.recipes;

import aztech.modern_industrialization.MIFluids;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.compat.mi.recipe.MIMachineRecipeBuilder;
import net.swedz.tesseract.neoforge.compat.vanilla.recipe.ShapedRecipeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class CommonRecipesServerDatagenProvider extends RecipesServerDatagenProvider {
    public CommonRecipesServerDatagenProvider(GatherDataEvent event) {
        super(event);
    }

    private static void addBasicCraftingRecipes(String path, String name, boolean assembler, ItemLike result, int resultCount, Consumer<ShapedRecipeBuilder> crafting, RecipeOutput output) {
        ShapedRecipeBuilder shapedRecipeBuilder = new ShapedRecipeBuilder();
        crafting.accept(shapedRecipeBuilder);
        shapedRecipeBuilder.output(result, resultCount);
        shapedRecipeBuilder.offerTo(output, IO.id(path + "/craft/" + name));

        if (assembler) {
            MIMachineRecipeBuilder.fromShapedToAssembler(shapedRecipeBuilder).offerTo(output, IO.id(path + "/craft/" + name + "/assembler"));
        }
    }


    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        addBasicCraftingRecipes(
                "shaped", "terminal", true,
                IOItems.TERMINAL.get(), 1,
                (r) -> r
                        .pattern("DGD")
                        .pattern("TET")
                        .define('D', MIItem.DIODE)
                        .define('T', MIItem.TRANSISTOR)
                        .define('G', Tags.Items.GLASS_PANES)
                        .define('E', MIItem.ELECTRONIC_CIRCUIT),
                output
        );

        addMachineRecipe(
                "assembler", "vajra", MIMachineRecipeTypes.ASSEMBLER,
                32, 20 * 10,
                (r) -> r
                        .addItemInput(MIItem.DIGITAL_CIRCUIT, 8)
                        .addItemInput(MIItem.TURBO_UPGRADE, 8)
                        .addItemInput(MIItem.ADVANCED_MOTOR, 8)
                        .addItemInput("modern_industrialization:sodium_battery", 1)
                        .addItemInput(MIItem.DIESEL_MINING_DRILL, 1)
                        .addItemInput(MIItem.DIESEL_CHAINSAW, 1)
                        .addItemInput(MIItem.OVERDRIVE_MODULE, 1)
                        .addFluidInput(MIFluids.CRYOFLUID, 1000)
                        .addItemOutput(IOItems.VAJRA, 1),
                output
        );
    }
}
