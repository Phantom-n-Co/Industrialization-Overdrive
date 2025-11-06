package dev.wp.industrialization_overdrive.machines.blockentities.multiblock;


import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIBlock;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOMachines;
import dev.wp.industrialization_overdrive.datamap.PyrolyseOvenTier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swedz.tesseract.neoforge.compat.mi.TesseractMI;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformer;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.helper.CommonGuiComponents;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static aztech.modern_industrialization.machines.models.MachineCasings.BRONZE_PLATED_BRICKS;

public final class PyrolyseOvenBlockEntity extends AbstractElectricMultipliedCraftingMultiblockBlockEntity {
    public PyrolyseOvenBlockEntity(BEP bep) {
        super(bep, IO.id("pyrolyse_oven"), SHAPE_TEMPLATES, MachineTier.MULTIBLOCK);
        List<Component> tierComponents = TIERS.stream().map(PyrolyseOvenBlockEntity.Tier::getDisplayName).toList();
        this.registerGuiComponent(CommonGuiComponents.rangedShapeSelection(this, activeShape, tierComponents, true));
    }

    public Tier getActiveTier() {
        return TIERS.get(activeShape.getActiveShapeIndex());
    }

    @Override
    public MachineRecipeType getRecipeType() {
        return IOMachines.RecipeTypes.PYROLYSE_OVEN;
    }

    @Override
    public int getMaxMultiplier() {
        return this.getActiveTier().batchSize();
    }

    @Override
    public EuCostTransformer getEuCostTransformer() {
        return EuCostTransformers.percentage(() -> this.getActiveTier().euCostMultiplier());
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(
                TesseractMI.text().machineBatcherRecipe(true, MIMachineRecipeTypes.COKE_OVEN),
                IO.text().machineBatcherCoils()
        );
    }

    private static List<Tier> TIERS = List.of();
    private static Map<ResourceLocation, Tier> TIERS_BY_COIL = Collections.unmodifiableMap(Maps.newHashMap());
    private static ShapeTemplate[] SHAPE_TEMPLATES = new ShapeTemplate[0];

    public static List<Tier> getTiers() {
        return TIERS;
    }

    public static Map<ResourceLocation, Tier> getTiersByCoil() {
        return TIERS_BY_COIL;
    }

    public record Tier(ResourceLocation blockId, int batchSize, float euCostMultiplier) {
        public String getTranslationKey() {
            return "pyro_tier.%s.%s.%s".formatted(IO.ID, blockId.getNamespace(), blockId.getPath());
        }

        public Component getDisplayName() {
            return Component.translatable(getTranslationKey());
        }
    }

    public static void initTiers() {
        List<Tier> tiers = Lists.newArrayList();
        PyrolyseOvenTier.getAll().forEach((block, tier) ->
                tiers.add(new Tier(block.location(), tier.batchSize(), tier.euCostMultiplier())));
        tiers.sort(Comparator.comparingInt(Tier::batchSize));

        TIERS = Collections.unmodifiableList(tiers);
        TIERS_BY_COIL = TIERS.stream().collect(Collectors.toMap(PyrolyseOvenBlockEntity.Tier::blockId, Function.identity()));

        SHAPE_TEMPLATES = new ShapeTemplate[TIERS.size()];

        SimpleMember casing = SimpleMember.forBlock(MIBlock.BLOCK_DEFINITIONS.get(MI.id("bronze_plated_bricks")));
        HatchFlags hatches = new HatchFlags.Builder().with(HatchTypes.FLUID_OUTPUT, HatchTypes.ITEM_INPUT, HatchTypes.ITEM_OUTPUT, HatchTypes.ENERGY_INPUT).build();

        for (int i = 0; i < TIERS.size(); i++) {
            Tier tier = TIERS.get(i);
            SimpleMember coil = SimpleMember.forBlockId(tier.blockId());
            ShapeTemplate.Builder builder = new ShapeTemplate.Builder(BRONZE_PLATED_BRICKS);
            for (int z = 0; z < 4; z++) {
                boolean isFront = z == 0;
                boolean isBack = z == 3;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        boolean isCenter = x == 0 && y == 0;
                        if (isFront || isBack) {
                            builder.add(x, y, z, casing, hatches);
                        } else if (!isCenter) {
                            builder.add(x, y, z, coil);
                        }
                    }
                }
            }
            SHAPE_TEMPLATES[i] = builder.build();
        }
        registerReiShapes();
    }

    private static void registerReiShapes() {
        ReiMachineRecipes.multiblockShapes.removeIf((e) -> e.machine().equals(IO.id("pyrolyse_oven")));
        int index = 0;
        for (ShapeTemplate shapeTemplate : SHAPE_TEMPLATES) {
            ReiMachineRecipes.registerMultiblockShape(IO.id("pyrolyse_oven"), shapeTemplate, "" + index);
            index++;
        }
    }
}
