package dev.wp.industrialization_overdrive.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wp.industrialization_overdrive.IODataMaps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.swedz.tesseract.neoforge.helper.RegistryHelper;

import java.util.Map;

public record PyrolyseOvenTier(int batchSize, float euCostMultiplier) {
    public static final Codec<PyrolyseOvenTier> CODEC = RecordCodecBuilder.create((instance) -> instance
            .group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("batch_size").forGetter(PyrolyseOvenTier::batchSize),
                    ExtraCodecs.POSITIVE_FLOAT.fieldOf("eu_cost_multiplier").forGetter(PyrolyseOvenTier::euCostMultiplier)
            ).apply(instance, PyrolyseOvenTier::new));

    public static PyrolyseOvenTier getFor(Block block) {
        return RegistryHelper.holder(BuiltInRegistries.BLOCK, block).getData(IODataMaps.PYROLYSE_OVEN_TIER);
    }

    public static Map<ResourceKey<Block>, PyrolyseOvenTier> getAll() {
        return BuiltInRegistries.BLOCK.getDataMap(IODataMaps.PYROLYSE_OVEN_TIER);
    }
}
