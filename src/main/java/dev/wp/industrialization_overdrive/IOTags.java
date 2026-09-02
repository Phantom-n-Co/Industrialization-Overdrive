package dev.wp.industrialization_overdrive;

import com.google.common.collect.Maps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.Map;

public class IOTags {
    private final static Map<TagKey<Item>, String> TRANSLATIONS = Maps.newHashMap();
    private static final TagKey<Item> MULTI_PROCESSING_ARRAY_BLACKLIST = item("multi_processing_array_blacklist", "Multi Processing Array Blacklist");

    public static Map<TagKey<Item>, String> translations() {
        return Collections.unmodifiableMap(TRANSLATIONS);
    }

    public final static class Items {
        public final static TagKey<Item> MULTI_PROCESSING_ARRAY_BLACKLIST = IOTags.MULTI_PROCESSING_ARRAY_BLACKLIST;
    }

    public static TagKey<Item> item(String path, String englishName) {
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), IO.id(path));
        TRANSLATIONS.put(tag, englishName);
        return tag;
    }

    public static TagKey<Item> itemCommon(String path) {
        return TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static TagKey<Block> block(String path) {
        return TagKey.create(BuiltInRegistries.BLOCK.key(), IO.id(path));
    }

    public static TagKey<Block> blockCommon(String path) {
        return TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
