package dev.wp.industrialization_overdrive;

import com.google.common.collect.Lists;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.PyrolyseOvenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.swedz.tesseract.neoforge.tooltip.BiParser;
import net.swedz.tesseract.neoforge.tooltip.Parser;
import net.swedz.tesseract.neoforge.tooltip.TooltipAttachment;

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

    public static final TooltipAttachment TERMINAL = TooltipAttachment.multilines(
            IOItems.TERMINAL,
            (flags, ctx, stack, item) -> {
                List<Component> tooltip = Lists.newArrayList();
                if (IOUtil.isAE2Loaded) {
                    GlobalPos linkPos = AE2Integration.getLinkPos(stack);
                    if (linkPos != null)
                        tooltip.add(IO.text().terminalLinkInfo(linkPos.pos()));
                    else tooltip.add(IO.text().terminalLinkNotLinked());
                }
                tooltip.add(IO.text().terminalHelp1("sneak", "use"));
                tooltip.add(IO.text().terminalHelp3("industrialization_overdrive.terminal_mode_switch"));
                tooltip.add(IOUtil.isAE2Loaded ? IO.text().terminalHelp2Alt() : IO.text().terminalHelp2());
                return tooltip;
            }
    );

    public static void init() {
    }
}
