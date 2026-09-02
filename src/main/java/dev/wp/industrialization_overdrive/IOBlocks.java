package dev.wp.industrialization_overdrive;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swedz.tesseract.neoforge.registry.SortOrder;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;
import net.swedz.tesseract.neoforge.registry.holder.BlockWithItemHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IOBlocks {
    public static final class Registry {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IO.ID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IO.ID);
        private static final Set<BlockHolder> HOLDERS = new HashSet<>();

        private static void init(IEventBus bus) {
            BLOCKS.register(bus);
            BLOCK_ENTITIES.register(bus);
        }

        public static void include(BlockHolder holder) {
            HOLDERS.add(holder);
        }
    }

    public static void init(IEventBus bus) {
        Registry.init(bus);
    }

    public static Set<BlockHolder> values() {
        return Set.copyOf(Registry.HOLDERS);
    }

    public static Block get(String id) {
        return Registry.HOLDERS.stream().filter((b) -> b.identifier().id().equals(id)).findFirst().orElseThrow().get();
    }

    public static <BlockType extends Block> BlockHolder<BlockType> create(String id, String englishName,
                                                                          Function<BlockBehaviour.Properties, BlockType> blockCreator) {
        BlockHolder<BlockType> holder = new BlockHolder<>(
                IO.id(id), englishName,
                Registry.BLOCKS, blockCreator
        );
        Registry.include(holder);
        return holder;
    }

    public static <BlockType extends Block, ItemType extends BlockItem> BlockWithItemHolder<BlockType, ItemType> create(
            String id, String englishName,
            Function<BlockBehaviour.Properties, BlockType> blockCreator,
            BiFunction<Block, Item.Properties, ItemType> itemCreator,
            SortOrder sortOrder
    ) {
        BlockWithItemHolder<BlockType, ItemType> holder = new BlockWithItemHolder<>(
                IO.id(id), englishName,
                Registry.BLOCKS, blockCreator,
                IOItems.Registry.ITEMS, itemCreator
        );
        holder.item().sorted(sortOrder);
        Registry.include(holder);
        IOItems.Registry.include(holder.item());
        return holder;
    }
}
