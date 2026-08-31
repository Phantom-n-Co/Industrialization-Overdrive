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
    @Range.Integer(min = -1, max = 128)
    default int machineChainerCopyDepth() {
        return -1;
    }

    @ConfigKey("batching_machines")
    @SubSection
    BatchingMachines batchingMachines();

    interface BatchingMachines {
        @ConfigKey("multi_processing_array_eu")
        @ConfigComment("The multiplier to use for the EU cost of the Multi Processing Array")
        @Range.Double(min = 0.1D, max = Double.MAX_VALUE)
        default double multiProcessingArrayEU() {
            return 1D;
        }
    }
}
