package dev.wp.industrialization_overdrive;

import net.swedz.tesseract.config.annotation.ConfigComment;
import net.swedz.tesseract.config.annotation.ConfigKey;
import net.swedz.tesseract.config.annotation.Range;
import net.swedz.tesseract.config.annotation.SubSection;

public interface IOConfig {
    @ConfigKey("allow_upgrades_in_multi_processing_array")
    @ConfigComment("Whether upgrades should be allowed in the Multi Processing Array")
    default boolean allowUpgradesInMultiProcessingArray() {
        return true;
    }

    @ConfigKey("machine_chainer_copy_depth")
    @ConfigComment("How many levels of Extended Industrialization Machine Chainers to copy; 0 disables copying and -1 is unlimited")
    @Range.Integer(min = -1, max = 1024)
    default int machineChainerCopyDepth() {
        return -1;
    }

    @ConfigKey("batching_machines")
    @SubSection
    BatchingMachines batchingMachines();

    @ConfigKey("me_hatches")
    @SubSection
    MEHatches meHatches();

    interface MEHatches {
        @ConfigKey("transfer_interval_ticks")
        @ConfigComment("How many ticks newly placed ME hatches wait between transfers with the ME network; each hatch can change it in its screen. 1 transfers every tick")
        @Range.Integer(min = 1, max = 1200)
        default int transferIntervalTicks() {
            return 40;
        }

        @ConfigKey("smart_pull_hold_intervals")
        @ConfigComment("How many transfer intervals ME input hatches keep stocking for the last recipe after the multiblock stops running it, so a short stall does not return the stock to the network")
        @Range.Integer(min = 0, max = 100)
        default int smartPullHoldIntervals() {
            return 3;
        }
    }

    interface BatchingMachines {
        @ConfigKey("multi_processing_array_eu")
        @ConfigComment("The multiplier to use for the EU cost of the Multi Processing Array")
        @Range.Double(min = 0.1D, max = Double.MAX_VALUE)
        default double multiProcessingArrayEU() {
            return 1D;
        }
    }
}
