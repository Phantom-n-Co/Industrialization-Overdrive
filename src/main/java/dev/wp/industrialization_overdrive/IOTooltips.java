package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.machines.components.UpgradeComponent;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import dev.wp.industrialization_overdrive.item.UpgradeStacker;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.PyrolyseOvenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.swedz.tesseract.neoforge.tooltip.BiParser;
import net.swedz.tesseract.neoforge.tooltip.Parser;
import net.swedz.tesseract.neoforge.tooltip.TooltipAttachment;

import java.util.ArrayList;
import java.util.List;

public class IOTooltips {
    private static final BiParser<Boolean, Float> MAYBE_SPACED_PERCENTAGE_PARSER = (space, ratio) ->
            Component.literal("%d%s%%".formatted((int) (ratio * 100), space ? " " : ""));

    public static final Parser<Float> PERCENTAGE_PARSER = (ratio) -> MAYBE_SPACED_PERCENTAGE_PARSER.parse(false, ratio);
    public static final Parser<BlockPos> POS_PARSER = (pos) -> Component.literal("%d, %d, %d".formatted(pos.getX(), pos.getY(), pos.getZ()));

    public static final TooltipAttachment COILS_PYRO = TooltipAttachment.singleLine(
            (stack, item) ->
                    item instanceof BlockItem blockItem &&
                            PyrolyseOvenBlockEntity.getTiersByCoil().containsKey(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock())),
            (flags, ctx, stack, item) -> {
                PyrolyseOvenBlockEntity.Tier tier = PyrolyseOvenBlockEntity.getTiersByCoil()
                        .get(BuiltInRegistries.BLOCK.getKey(((BlockItem) stack.getItem()).getBlock()));
                int batchSize = tier.batchSize();
                float euCostMultiplier = tier.euCostMultiplier();
                return IO.text().coilsPyroTier(batchSize, euCostMultiplier);
            }
    );

    public static final TooltipAttachment MULTIBLOCK_BUILDER = TooltipAttachment.multilines(
            IOItems.MULTIBLOCK_BUILDER,
            (flags, ctx, stack, item) -> {
                List<Component> tooltip = new ArrayList<>();
                Component mode = switch (stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, MultiblockBuilder.Mode.BUILD)) {
                    case BUILD -> IO.text().multiblockBuilderModeBuild();
                    case COPY_PASTE -> IO.text().multiblockBuilderModeCopyPaste();
                    case TEAR_DOWN -> IO.text().multiblockBuilderModeTearDown();
                };
                tooltip.add(IO.text().multiblockBuilderCurrentMode(mode));
                if (IOUtil.isAE2Loaded) {
                    GlobalPos linkPos = AE2Integration.getLinkPos(stack);
                    if (linkPos != null)
                        tooltip.add(IO.text().multiblockBuilderLinkInfo(linkPos.pos()));
                    else tooltip.add(IO.text().multiblockBuilderLinkNotLinked());
                }
                tooltip.add(IO.text().multiblockBuilderHelp1("sneak", "use"));
                tooltip.add(IO.text().multiblockBuilderHelp3("industrialization_overdrive.terminal_mode_switch"));
                tooltip.add(IOUtil.isAE2Loaded ? IO.text().multiblockBuilderHelp2Alt() : IO.text().multiblockBuilderHelp2());
                return tooltip;
            }
    );

    public static final TooltipAttachment UPGRADE_STACKER_CONTENTS = TooltipAttachment.multilines(
            UpgradeStacker.class,
            (flags, ctx, stack, item) -> {
                IOComponents.UpgradeStackerContents contents = stack.get(IOComponents.UPGRADE_STACKER_CONTENTS.get());
                if (contents == null || contents.count() <= 0) {
                    return List.of(IO.text().empty());
                }
                return List.of(
                        IO.text().contains(contents.count(), contents.upgradeType()),
                        IO.text().totalEuPerTick(UpgradeComponent.getExtraEu(contents.upgradeType()) * contents.count())
                );
            }
    ).noShiftRequired();

    public static final TooltipAttachment UPGRADE_STACKER_INFO = TooltipAttachment.multilines(
            UpgradeStacker.class,
            List.of(
                    IO.text().upgradeStackerInfo(64),
                    IO.text().upgradeStackerStore("use"),
                    IO.text().upgradeStackerTakeAll("sneak", "use")
            )
    );

    public static void init() {
    }
}
