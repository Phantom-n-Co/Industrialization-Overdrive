package dev.wp.industrialization_overdrive;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
    MutableComponent multiProcessingArraySize(int size);

    @LangKey(text = "Batch size and cost is determined by coil used.")
    @WithStyle("tooltip")
    MutableComponent machineBatcherCoils();

    @LangKey(text = "Runs Pyrolyse Oven in batches of up to %d at %s the EU cost.")
    @WithStyle("tooltip")
    MutableComponent coilsPyroTier(@WithStyle("highlighted") int batchSize, @Parsed("percentage") @WithStyle("highlighted") float euCost);

    @LangKey(text = "- Press %s + %s on a MI multiblock to automatically build it.")
    @WithStyle("tooltip")
    MutableComponent terminalHelp1(@Parsed("keybind") @WithStyle("highlighted") String key1, @Parsed("keybind") @WithStyle("highlighted") String key2);

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

    @LangKey(text = "Silk Touch: %s")
    @WithStyle("tooltip")
    MutableComponent vajraSilkTouchInfo(@WithStyle("highlighted") Component status);

    @LangKey(text = "Speed: %d")
    @WithStyle("tooltip")
    MutableComponent vajraSpeedInfo(@WithStyle("highlighted") Component speed);

    @LangKey(text = "Speed changed to %d.")
    @WithStyle("tooltip")
    MutableComponent vajraSpeedChanged(@WithStyle("highlighted") Component speed);

    @LangKey(text = "Slow")
    @WithStyle("highlighted")
    MutableComponent vajraSpeedSlow();

    @LangKey(text = "Normal")
    @WithStyle("highlighted")
    MutableComponent vajraSpeedNormal();

    @LangKey(text = "Fast")
    @WithStyle("highlighted")
    MutableComponent vajraSpeedFast();

    @LangKey(text = "Instant")
    @WithStyle("highlighted")
    MutableComponent vajraSpeedInstant();

    @LangKey(text = "Enabled")
    @WithStyle("green")
    MutableComponent enabled();

    @LangKey(text = "Disabled")
    @WithStyle("red")
    MutableComponent disabled();

    @LangKey(text = "Energy: %s / %s")
    @WithStyle("tooltip")
    MutableComponent energyInfo(@Parsed("eu") @WithStyle("highlighted") long stored, @Parsed("eu") @WithStyle("highlighted") long capacity);

    @LangKey(text = "Mode changed to %s.")
    @WithStyle("tooltip")
    MutableComponent terminalModeChanged(@WithStyle("highlighted") String mode);

    @LangKey(text = "Template copied from %s, %s, %s")
    @WithStyle("green")
    MutableComponent terminalTemplateCopied(int x, int y, int z);

    @LangKey(text = "No template stored in terminal.")
    @WithStyle("red")
    MutableComponent terminalNoTemplate();

    @LangKey(text = "Multiblock dismantled.")
    @WithStyle("green")
    MutableComponent terminalDismantled();
    @LangKey(text = "- Press %s to cycle modes.")
    @WithStyle("tooltip")
    MutableComponent terminalHelp3(@Parsed("keybind") @WithStyle("highlighted") String key);
}
