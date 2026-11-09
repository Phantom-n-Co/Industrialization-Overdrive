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

    @LangKey(text = "Block at %s is outside the build height limit and cannot be placed.")
    @WithStyle("red")
    MutableComponent multiblockBuilderOutsideBuildHeight(@Parsed("block_pos") BlockPos location);

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

    @LangKey(text = "Mixed Input Hatch")
    MutableComponent mixedInputHatch();

    @LangKey(text = "Mixed Output Hatch")
    MutableComponent mixedOutputHatch();

    // ME hatch transfer interval

    @LangKey(text = "Interval")
    MutableComponent meHatchIntervalLabel();

    @LangKey(text = "t")
    MutableComponent meHatchIntervalUnit();

    @LangKey(text = "Ticks between transfers with the ME network")
    @WithStyle("tooltip")
    MutableComponent meHatchIntervalDescription();

    @LangKey(text = "Type a value and press Enter to apply")
    @WithStyle("gray")
    MutableComponent meHatchIntervalControlType();

    @LangKey(text = "Scroll: adjust by 1, +Shift: by 20")
    @WithStyle("gray")
    MutableComponent meHatchIntervalControlScroll();

    @LangKey(text = "Disabled in Storage mode: outputs go into the cells as they land.")
    @WithStyle("red")
    MutableComponent meHatchIntervalStorageMode();

    // ME output hatch cell slots

    @LangKey(text = "Item Storage Cell: buffers the hatch's item outputs")
    @WithStyle("tooltip")
    MutableComponent meHatchItemCellSlot();

    @LangKey(text = "Fluid Storage Cell: buffers the hatch's fluid outputs")
    @WithStyle("tooltip")
    MutableComponent meHatchFluidCellSlot();

    // ME output hatch storage mode and priority

    @LangKey(text = "Storage")
    MutableComponent meHatchStorageModeLabel();

    @LangKey(text = "Storage mode: %s")
    @WithStyle("tooltip")
    MutableComponent meHatchStorageModeState(Component state);

    @LangKey(text = "Offers the installed storage cells to the network to take from, instead of pushing their contents.")
    @WithStyle("gray")
    MutableComponent meHatchStorageModeDescription();

    @LangKey(text = "The network cannot insert into them; only this hatch's machine fills them.")
    @WithStyle("gray")
    MutableComponent meHatchStorageModeExtractOnly();

    @LangKey(text = "Needs a storage cell: without one, the hatch accepts no outputs.")
    @WithStyle("red")
    MutableComponent meHatchStorageModeNeedsCell();

    @LangKey(text = "Priority: %s")
    @WithStyle("tooltip")
    MutableComponent meHatchPriority(@WithStyle("highlighted") int priority);

    @LangKey(text = "The network takes from the lowest priority first: lower it to have these outputs used before other storage.")
    @WithStyle("gray")
    MutableComponent meHatchPriorityDescription();

    @LangKey(text = "Applies in Storage mode.")
    @WithStyle("gray")
    MutableComponent meHatchPriorityStorageOnly();

    @LangKey(text = "Higher priority stocks first: lower-priority input hatches on the network leave what this one still needs.")
    @WithStyle("gray")
    MutableComponent meHatchPriorityInputDescription();

    // ME hatch smart pull

    @LangKey(text = "Smart pull")
    MutableComponent meHatchSmartPullLabel();

    @LangKey(text = "Smart pull: %s")
    @WithStyle("tooltip")
    MutableComponent meHatchSmartPullState(Component state);

    @LangKey(text = "Stocks enough for the multiblock's running recipe to keep crafting until the next transfer.")
    @WithStyle("gray")
    MutableComponent meHatchSmartPullDescription();

    @LangKey(text = "Configured amounts stay the minimum.")
    @WithStyle("gray")
    MutableComponent meHatchSmartPullMinimum();

    @LangKey(text = "Raised by smart pull from %s")
    @WithStyle("gray")
    MutableComponent meHatchSmartPullRaised(Component configured);

    // ME hatch configuration cell

    @LangKey(text = "No item set")
    @WithStyle("tooltip")
    MutableComponent meHatchNoItemSet();

    @LangKey(text = "No fluid set")
    @WithStyle("tooltip")
    MutableComponent meHatchNoFluidSet();

    @LangKey(text = "Click or drag in a resource to set it")
    @WithStyle("tooltip")
    MutableComponent meHatchSetHint();

    @LangKey(text = "Keeps %s stocked")
    @WithStyle("tooltip")
    MutableComponent meHatchKeepsStocked(@WithStyle("highlighted") Component amount);

    @LangKey(text = "Only this can be output here")
    @WithStyle("tooltip")
    MutableComponent meHatchOutputOnlyThis();

    @LangKey(text = "Hold Shift for controls")
    @WithStyle("tooltip")
    MutableComponent meHatchHoldShiftForControls();

    @LangKey(text = "Scroll: adjust amount")
    @WithStyle("tooltip")
    MutableComponent meHatchControlScroll();

    @LangKey(text = "  +Shift: x16, +Ctrl: double/halve, +both: x64")
    @WithStyle("tooltip")
    MutableComponent meHatchControlScrollModifiers();

    @LangKey(text = "%s or #: enter amount")
    @WithStyle("tooltip")
    MutableComponent meHatchControlEnterAmount(@Parsed("keybind") @WithStyle("highlighted") String key);

    @LangKey(text = "Click with a resource: replace")
    @WithStyle("tooltip")
    MutableComponent meHatchControlReplace();

    @LangKey(text = "Right-click: clear")
    @WithStyle("tooltip")
    MutableComponent meHatchControlClear();

    // ME hatch stock slot

    @LangKey(text = "Empty")
    @WithStyle("tooltip")
    MutableComponent meHatchStockEmpty();

    @LangKey(text = "Stocked from the ME network")
    @WithStyle("tooltip")
    MutableComponent meHatchStockedFromNetwork();

    @LangKey(text = "Sent to the ME network")
    @WithStyle("tooltip")
    MutableComponent meHatchSentToNetwork();

    @LangKey(text = "Stocks nothing until set above")
    @WithStyle("tooltip")
    MutableComponent meHatchInputUnconfigured();

    @LangKey(text = "Accepts no output until set above")
    @WithStyle("tooltip")
    MutableComponent meHatchOutputUnconfigured();

    @LangKey(text = "Accepts any output")
    @WithStyle("tooltip")
    MutableComponent meHatchOutputOpen();

    @LangKey(text = "%s %s")
    MutableComponent meHatchNameWithAmount(Component name, @WithStyle("tooltip") Component amount);

    @LangKey(text = "×%s")
    MutableComponent meHatchItemCount(Component amount);

    @LangKey(text = "Reserved for the running recipe")
    @WithStyle("yellow")
    MutableComponent meHatchReservedForRecipe();

    @LangKey(text = "This ME hatch still holds resources and its network cannot take them. Break it again while sneaking to destroy them.")
    @WithStyle("red")
    MutableComponent meHatchBreakWouldLoseStock();

    // ME hatch block tooltip

    @LangKey(text = "Keeps each configured resource stocked from the ME network, up to its set amount.")
    @WithStyle("tooltip")
    MutableComponent meHatchInputDescription();

    @LangKey(text = "Sends machine outputs to the ME network.")
    @WithStyle("tooltip")
    MutableComponent meHatchOutputDescription();

    @LangKey(text = "Accepts any output until a channel is configured, then only configured ones.")
    @WithStyle("tooltip")
    MutableComponent meHatchOutputFiltering();

    @LangKey(text = "Connects to the ME network through its output face.")
    @WithStyle("tooltip")
    MutableComponent meHatchConnectionFace();
}
