package dev.wp.industrialization_overdrive.machines.blockentities.multiblock;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIBlock;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.guicomponents.ShapeSelection;
import aztech.modern_industrialization.machines.guicomponents.SlotPanel;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOTags;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformer;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformers;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;
import net.swedz.tesseract.neoforge.compat.mi.machine.multiblock.member.PredicateSimpleMember;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class MultiProcessingArrayBlockEntity extends AbstractElectricMultipliedCraftingMultiblockBlockEntity {
    private final MultiProcessingArrayMachineComponent machines;

    public MultiProcessingArrayBlockEntity(BEP bep) {
        super(bep, IO.id("multi_processing_array"), SHAPE_TEMPLATES, MachineTier.MULTIBLOCK);

        if (!IO.config().allowUpgradesInMultiProcessingArray()) {
            var slotPanel = guiComponents.getNullable(SlotPanel.class);
            if (slotPanel != null) guiComponents.unregister(slotPanel);

            this.registerGuiComponent(new SlotPanel(this)
                    .withRedstoneControl(redstoneControl)
                    .withOverdrive(overdrive));
        }

        this.machines = new MultiProcessingArrayMachineComponent();
        this.registerComponents(machines);
        this.registerGuiComponent(new MultiProcessingArrayMachineSlot(
                this,
                () -> this.getMachineStackSize(activeShape.getActiveShapeIndex()),
                machines
        ));

        this.registerGuiComponent(new ShapeSelection(
                new ShapeSelection.Behavior() {
                    @Override
                    public void handleClick(int clickedLine, int delta) {
                        int newShapeIndex = Mth.clamp(activeShape.getActiveShapeIndex() + delta, 0, SHAPE_TEMPLATES.length - 1);
                        int newMachineStackSize = MultiProcessingArrayBlockEntity.this.getMachineStackSize(newShapeIndex);
                        if (newMachineStackSize < machines.getMachines().getCount()) {
                            return;
                        }
                        activeShape.incrementShape(MultiProcessingArrayBlockEntity.this, delta);
                    }

                    @Override
                    public int getCurrentIndex(int line) {
                        return activeShape.getActiveShapeIndex();
                    }
                },
                new ShapeSelection.LineInfo(
                        IntStream.range(0, SPLIT).map(this::getMachineStackSize).mapToObj(IO.text()::multiProcessingArraySize).map((c) -> (Component) c).toList(),
                        false
                )
        ));
    }

    @Override
    public MachineRecipeType getRecipeType() {
        return machines.getMachineRecipeType();
    }

    @Override
    public int getMaxMultiplier() {
        return machines.getMachineCount();
    }

    @Override
    public EuCostTransformer getEuCostTransformer() {
        return EuCostTransformers.percentage(() -> (float) IO.config().batchingMachines().multiProcessingArrayEU());
    }

    private int getMachineStackSize(int sizeIndex) {
        return (int) (BASE_MACHINES * Math.pow(MULT_MACHINES, sizeIndex));
    }

    @Override
    public List<Component> getTooltips() {
        List<Component> lines = new ArrayList<>();
        lines.add(IO.text().multiProcessingArrayRecipe());
        lines.add(IO.text().multiProcessingArrayBatchSize());
        if (IO.config().batchingMachines().multiProcessingArrayEU() != 1) {
            lines.add(IO.text().multiProcessingArrayEuCostMultiplier(this.getEuCostTransformer()));
        }
        return lines;
    }

    private static final int MAX_MACHINES = 64;
    private static final int SPLIT = 4;
    private static final int BASE_MACHINES = 8;
    private static final int MULT_MACHINES = 2;

    private static final ShapeTemplate[] SHAPE_TEMPLATES;

    static {
        SHAPE_TEMPLATES = new ShapeTemplate[SPLIT];

        SimpleMember casing = SimpleMember.forBlock(MIBlock.BLOCK_DEFINITIONS.get(MI.id("solid_titanium_machine_casing")));
        SimpleMember pipe = SimpleMember.forBlock(MIBlock.BLOCK_DEFINITIONS.get(MI.id("titanium_machine_casing_pipe")));
        SimpleMember glass = new PredicateSimpleMember((state) -> state.is(IOTags.blockCommon("glass_blocks")), Blocks.GLASS);
        HatchFlags front = new HatchFlags.Builder().with(HatchTypes.ENERGY_INPUT).build();
        HatchFlags top = new HatchFlags.Builder().with(HatchTypes.ITEM_INPUT, HatchTypes.FLUID_INPUT).build();
        HatchFlags bottom = new HatchFlags.Builder().with(HatchTypes.ITEM_OUTPUT, HatchTypes.FLUID_OUTPUT).build();

        for (int
             i = 0, size = 3, machines = BASE_MACHINES;
             i < SPLIT && machines <= MAX_MACHINES;
             i++, size += 2, machines *= MULT_MACHINES) {
            ShapeTemplate.Builder builder = new ShapeTemplate.Builder(MachineCasings.SOLID_TITANIUM);
            for (int z = 0; z < size; z++) {
                boolean isFront = z == 0;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        boolean isTop = y == 1;
                        boolean isBottom = y == -1;
                        boolean isCenter = x == 0 && y == 0;
                        boolean isGlass = x != 0 && y == 0;

                        builder.add(x, y, z, isCenter ? pipe : isGlass ? glass : casing, isGlass ? null : isFront ? front : isTop ? top : isBottom ? bottom : null);
                    }
                }
            }
            SHAPE_TEMPLATES[i] = builder.build();
        }
    }

    public ShapeTemplate getBigShape() {
        return SHAPE_TEMPLATES[SHAPE_TEMPLATES.length - 1];
    }

    public static void registerReiShapes() {
        int index = 0;
        for (ShapeTemplate shapeTemplate : SHAPE_TEMPLATES) {
            ReiMachineRecipes.registerMultiblockShape(IO.id("multi_processing_array"), shapeTemplate, "" + index);
            index++;
        }
    }
}
