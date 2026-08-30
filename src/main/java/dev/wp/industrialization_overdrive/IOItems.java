package dev.wp.industrialization_overdrive;

import com.google.common.collect.Sets;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import dev.wp.industrialization_overdrive.item.Vajra;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swedz.tesseract.neoforge.registry.SortOrder;
import net.swedz.tesseract.neoforge.registry.common.CommonModelBuilders;
import net.swedz.tesseract.neoforge.registry.common.MICommonCapabitilies;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.Set;
import java.util.function.Function;

public final class IOItems {
    public static final class Registry {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IO.ID);
        private static final Set<ItemHolder> HOLDERS = Sets.newHashSet();

        private static void init(IEventBus bus) {
            ITEMS.register(bus);
        }

        public static void include(ItemHolder holder) {
            HOLDERS.add(holder);
        }
    }

    public static void init(IEventBus bus) {
        Registry.init(bus);
    }

    public static final ItemHolder<MultiblockBuilder> TERMINAL = create("terminal", "Multiblock Builder", MultiblockBuilder::new, IOSortOrder.GEAR).withModelBuilder(CommonModelBuilders::generated).register();
    public static final ItemHolder<Vajra> VAJRA = create("vajra", "Vajra", Vajra::new, IOSortOrder.GEAR)
            .tag(ItemTags.PICKAXES, ItemTags.AXES, ItemTags.SHOVELS, ItemTags.HOES)
            .withModelBuilder(CommonModelBuilders::handheld)
            .withCapabilities(MICommonCapabitilies::simpleEnergyItem)
            .register();

    public static Set<ItemHolder> values() {
        return Set.copyOf(Registry.HOLDERS);
    }

    public static ItemHolder valueOf(String id) {
        return Registry.HOLDERS.stream()
                .filter((holder) -> holder.identifier().id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    public static <Type extends Item> ItemHolder<Type> create(String id, String englishName, Function<Item.Properties, Type> creator, SortOrder sortOrder) {
        ItemHolder<Type> holder = new ItemHolder<>(IO.id(id), englishName, Registry.ITEMS, creator).sorted(sortOrder);
        Registry.include(holder);
        return holder;
    }
}
