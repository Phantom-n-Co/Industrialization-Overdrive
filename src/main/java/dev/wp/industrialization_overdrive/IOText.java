package dev.wp.industrialization_overdrive;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
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
    MutableComponent multiblockBuilderModeChanged(@WithStyle("highlighted") Component mode);

    @LangKey(text = "Mode: %s")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderCurrentMode(@WithStyle("highlighted") Component mode);

    @LangKey(text = "Build")
    @WithStyle("highlighted")
    MutableComponent multiblockBuilderModeBuild();

    @LangKey(text = "Copy/Paste")
    @WithStyle("highlighted")
    MutableComponent multiblockBuilderModeCopyPaste();

    @LangKey(text = "Tear Down")
    @WithStyle("highlighted")
    MutableComponent multiblockBuilderModeTearDown();

    @LangKey(text = "Template copied from %s, %s, %s")
    @WithStyle("green")
    MutableComponent multiblockBuilderTemplateCopied(int x, int y, int z);

    @LangKey(text = "No template stored in Multiblock Builder.")
    @WithStyle("red")
    MutableComponent multiblockBuilderNoTemplate();

    @LangKey(text = "Multiblock dismantled.")
    @WithStyle("green")
    MutableComponent multiblockBuilderDismantled();

    @LangKey(text = "- Press %s + %s on a MI multiblock to automatically build it.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderHelp1(@Parsed("keybind") @WithStyle("highlighted") String key1, @Parsed("keybind") @WithStyle("highlighted") String key2);

    @LangKey(text = "- Requires parts to be in your inventory.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderHelp2();

    @LangKey(text = "- Requires parts to be in your inventory or a linked ME system.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderHelp2Alt();

    @LangKey(text = "- Press %s to cycle modes.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderHelp3(@Parsed("keybind") @WithStyle("highlighted") String key);

    @LangKey(text = "Linked to an ME system at %s.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderLinkInfo(@Parsed("block_pos") @WithStyle("highlighted") BlockPos location);

    @LangKey(text = "Not linked to an ME system.")
    @WithStyle("tooltip")
    MutableComponent multiblockBuilderLinkNotLinked();

    @LangKey(text = "Multiblock must be formed to copy it.")
    @WithStyle("red")
    MutableComponent multiblockBuilderCopyRequiresFormed();

    @LangKey(text = "Block at %s is not part of the multiblock and cannot be replaced.")
    @WithStyle("red")
    MutableComponent multiblockBuilderBlockCannotBeReplaced(@Parsed("block_pos") BlockPos location);

    @LangKey(text = "Successfully pasted multiblock structure.")
    @WithStyle("green")
    MutableComponent multiblockBuilderPasteSuccess();

    @LangKey(text = "Successfully built multiblock at %s, %s, %s")
    @WithStyle("green")
    MutableComponent multiblockBuilderBuildSuccess(int x, int y, int z);

    @LangKey(text = "Block at %s is not part of the multiblock")
    @WithStyle("red")
    MutableComponent multiblockBuilderBlockNotPart(@Parsed("block_pos") BlockPos location);

    @LangKey(text = "Required items:")
    @WithStyle("highlighted")
    MutableComponent multiblockBuilderRequiredItems();

    @LangKey(text = "- %sx %s")
    MutableComponent multiblockBuilderRequiredItem(int count, String item);

    @LangKey(text = "- %s: store all matching upgrades from your inventory")
    @WithStyle("tooltip")
    MutableComponent upgradeStackerStore(
            @Parsed("keybind") @WithStyle("highlighted") String use
    );

    @LangKey(text = "- %s + %s: take all upgrades out")
    @WithStyle("tooltip")
    MutableComponent upgradeStackerTakeAll(
            @Parsed("keybind") @WithStyle("highlighted") String sneak,
            @Parsed("keybind") @WithStyle("highlighted") String use
    );

    @LangKey(text = "Stores more than %s upgrades of one type for batching machines.")
    @WithStyle("tooltip")
    MutableComponent upgradeStackerInfo(
            @WithStyle("highlighted") int count
    );

    @LangKey(text = "Empty")
    @WithStyle("gray")
    MutableComponent empty();

    @LangKey(text = "Contains %s %s")
    @WithStyle("tooltip")
    MutableComponent contains(
            @WithStyle("highlighted") int count,
            @WithStyle("highlighted") Item upgradeType
    );

    @LangKey(text = "Total EU/t: %s")
    @WithStyle("tooltip")
    MutableComponent totalEuPerTick(
            @Parsed("eu_per_tick") @WithStyle("highlighted") long amount
    );
}
