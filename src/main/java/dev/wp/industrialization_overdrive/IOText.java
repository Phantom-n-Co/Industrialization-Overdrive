package dev.wp.industrialization_overdrive;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformer;
import net.swedz.tesseract.neoforge.lang.annotation.LangKey;
import net.swedz.tesseract.neoforge.lang.annotation.Parsed;
import net.swedz.tesseract.neoforge.lang.annotation.WithStyle;

public interface IOText {
    @LangKey(text = "Batch size is determined by the amount of machines provided to it.")
    @WithStyle("tooltip")
    MutableComponent multiProcessingArrayBatchSize();

    @LangKey(text = "Runs at %s the EU cost.")
    @WithStyle("tooltip")
    MutableComponent multiProcessingArrayEuCostMultiplier(@WithStyle("highlighted") EuCostTransformer euCost);

    @LangKey(text = "Insert electric crafting multiblocks to run in parallel.")
    @WithStyle("tooltip")
    MutableComponent multiProcessingArrayMachineInput();

    @LangKey(text = "Can run recipes of any electric crafting multiblock provided to it in batches.")
    @WithStyle("tooltip")
    MutableComponent multiProcessingArrayRecipe();

    @LangKey(text = "Machines: %d")
    MutableComponent multiProcessingArraySize(@WithStyle("highlighted") int size);

    @LangKey(text = "Batch size and cost is determined by coil used.")
    @WithStyle("tooltip")
    MutableComponent machineBatcherCoils();

    @LangKey(text = "Runs Pyrolyse Oven in batches of up to %d at %s the EU cost.")
    @WithStyle("tooltip")
    MutableComponent coilsPyroTier(@WithStyle("highlighted") int batchSize, @Parsed("percentage") @WithStyle("highlighted") float euCost);

    @LangKey(text = "- Press %s + %s on a MI multiblock to automatically build it.")
    @WithStyle("tooltip")
    MutableComponent terminalHelp1(
            @Parsed("keybind") @WithStyle("highlighted") String key1,
            @Parsed("keybind") @WithStyle("highlighted") String key2
    );

    @LangKey(text = "- Requires parts to be in your inventory.")
    @WithStyle("tooltip")
    MutableComponent terminalHelp2();

    @LangKey(text = "- Requires parts to be in your inventory or a linked ME system.")
    @WithStyle("tooltip")
    MutableComponent terminalHelp2Alt();

    @LangKey(text = "Linked to an ME system at %s.")
    @WithStyle("tooltip")
    MutableComponent terminalLinkInfo(@Parsed("block_pos") @WithStyle("highlighted") BlockPos location);

    @LangKey(text = "Not linked to an ME system.")
    @WithStyle("tooltip")
    MutableComponent terminalLinkNotLinked();
}
