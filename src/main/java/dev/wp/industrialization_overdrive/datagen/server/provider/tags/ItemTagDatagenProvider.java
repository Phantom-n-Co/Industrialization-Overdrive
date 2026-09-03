package dev.wp.industrialization_overdrive.datagen.server.provider.tags;

import aztech.modern_industrialization.MI;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOItems;
import dev.wp.industrialization_overdrive.IOTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

public final class ItemTagDatagenProvider extends ItemTagsProvider {
    public ItemTagDatagenProvider(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), event.getLookupProvider(), CompletableFuture.completedFuture(TagLookup.empty()), IO.ID, event.getExistingFileHelper());

    }

    private void addMPABlacklistTag() {
        this.tag(IOTags.Items.MULTI_PROCESSING_ARRAY_BLACKLIST)
                .add(
                        TagEntry.element(MI.id("fusion_reactor"))
                );
    }

    private void removeEnchantableTags(ItemHolder itemHolder) {
        var item = itemHolder.asItem();
        this.tag(ItemTags.DURABILITY_ENCHANTABLE).remove(item);
        this.tag(ItemTags.MINING_ENCHANTABLE).remove(item);
        this.tag(ItemTags.MINING_LOOT_ENCHANTABLE).remove(item);
        this.tag(ItemTags.SWORD_ENCHANTABLE).remove(item);
        this.tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).remove(item);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (ItemHolder<?> item : IOItems.values().stream().sorted(Comparator.comparing((item) -> item.identifier().id())).toList()) {
            for (TagKey<Item> tag : item.tags()) {
                this.tag(tag).add(item.asItem());
            }
        }
        this.addMPABlacklistTag();
        removeEnchantableTags(IOItems.VAJRA);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
