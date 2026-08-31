package dev.wp.industrialization_overdrive.datagen.client.provider.models;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOItems;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

public final class ItemModelsDatagenProvider extends ItemModelProvider {
    public ItemModelsDatagenProvider(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), IO.ID, event.getExistingFileHelper());
    }

    @Override
    protected void registerModels() {
        for (ItemHolder item : IOItems.values()) {
            if (item.equals(IOItems.MULTIBLOCK_BUILDER)) continue;

            if (item.hasModelProvider()) item.modelProvider().accept(this);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
