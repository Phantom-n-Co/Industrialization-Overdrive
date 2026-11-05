package dev.wp.industrialization_overdrive.datagen.server.provider.recipes;

import aztech.modern_industrialization.MIFluids;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.compat.mi.recipe.MIMachineRecipeBuilder;
import net.swedz.tesseract.neoforge.compat.vanilla.recipe.ShapedRecipeBuilder;
import net.swedz.tesseract.neoforge.compat.vanilla.recipe.ShapelessRecipeBuilder;
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


    /**
     * An ME hatch is "the MV-tier MI hatch of the same kind, surrounded by ME Interfaces". Ids are passed
     * as strings because the MI and AE2 items are not on our classpath as constants.
     */
    private static void addMEHatchRecipe(String name, String baseHatchId, RecipeOutput output) {
        ShapedRecipeBuilder builder = new ShapedRecipeBuilder();
        builder.pattern(" I ")
                .pattern("IHI")
                .pattern(" I ")
                .define('I', "ae2:interface")
                .define('H', baseHatchId);
        builder.output("%s:%s".formatted(IO.ID, name), 1);
        builder.offerTo(output, IO.id("shaped/craft/" + name));
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        addBasicCraftingRecipes(
                "shaped", "terminal", false,
                IOItems.MULTIBLOCK_BUILDER, 1,
                (r) -> r
                        .pattern("SGS")
                        .pattern("SES")
                        .define('S', "#c:plates/steel")
                        .define('G', Tags.Items.GLASS_PANES)
                        .define('E', MIItem.ELECTRONIC_CIRCUIT),
                output
        );

        addBasicCraftingRecipes(
                "shaped", "upgrade_stacker", false,
                IOItems.UPGRADE_STACKER, 1,
                (r) -> r
                        .pattern("GGG")
                        .pattern("ECA")
                        .pattern("AAA")
                        .define('G', Tags.Items.GLASS_PANES)
                        .define('E', MIItem.ELECTRONIC_CIRCUIT)
                        .define('A', "#c:plates/aluminum")
                        .define('C', Tags.Items.CHESTS_WOODEN),
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

        // The ME hatch items don't exist without AE2, so these recipes would fail to parse.
        RecipeOutput meOutput = output.withConditions(new ModLoadedCondition("ae2"));
        addMEHatchRecipe("me_item_input_hatch", "modern_industrialization:advanced_item_input_hatch", meOutput);
        addMEHatchRecipe("me_item_output_hatch", "modern_industrialization:advanced_item_output_hatch", meOutput);
        addMEHatchRecipe("me_fluid_input_hatch", "modern_industrialization:advanced_fluid_input_hatch", meOutput);
        addMEHatchRecipe("me_fluid_output_hatch", "modern_industrialization:advanced_fluid_output_hatch", meOutput);

        ShapelessRecipeBuilder mixedInput = new ShapelessRecipeBuilder();
        mixedInput.with(IO.id("me_item_input_hatch"));
        mixedInput.with(IO.id("me_fluid_input_hatch"));
        mixedInput.output(IO.id("me_mixed_input_hatch"), 1);
        mixedInput.offerTo(meOutput, IO.id("shaped/craft/me_mixed_input_hatch"));

        ShapelessRecipeBuilder mixedOutput = new ShapelessRecipeBuilder();
        mixedOutput.with(IO.id("me_item_output_hatch"));
        mixedOutput.with(IO.id("me_fluid_output_hatch"));
        mixedOutput.output(IO.id("me_mixed_output_hatch"), 1);
        mixedOutput.offerTo(meOutput, IO.id("shaped/craft/me_mixed_output_hatch"));
    }
}
