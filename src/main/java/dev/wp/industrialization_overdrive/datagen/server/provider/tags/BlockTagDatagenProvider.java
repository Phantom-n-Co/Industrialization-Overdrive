package dev.wp.industrialization_overdrive.datagen.server.provider.tags;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;

import java.util.Comparator;

public final class BlockTagDatagenProvider extends BlockTagsProvider {
    public BlockTagDatagenProvider(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), event.getLookupProvider(), IO.ID, event.getExistingFileHelper());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (BlockHolder<?> block : IOBlocks.values().stream().sorted(Comparator.comparing((block) -> block.identifier().id())).toList()) {
            for (TagKey<Block> tag : block.tags()) {
                this.tag(tag).add(block.get());
            }
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
