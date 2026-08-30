package dev.wp.industrialization_overdrive;

import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import dev.wp.industrialization_overdrive.compat.EIIntegration;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.Comparator;
import java.util.function.Supplier;

public final class IOOtherRegistries {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, IO.ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, IO.ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IO.ID);

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = IOOtherRegistries.CREATIVE_MODE_TABS.register(IO.ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.%s.%s".formatted(IO.ID, IO.ID)))
            .icon(() -> IOItems.TERMINAL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                Comparator<ItemHolder> compareBySortOrder = Comparator.comparing(ItemHolder::sortOrder);
                Comparator<ItemHolder> compareByName = Comparator.comparing((i) -> i.identifier().id());
                IOItems.values().stream()
                        .sorted(compareBySortOrder.thenComparing(compareByName))
                        .forEach((item) -> {
                            output.accept(item);

                            if (item.get() instanceof ISimpleEnergyItem energyItem) {
                                var chargedItem = item.get().getDefaultInstance();
                                if (energyItem.getEnergyCapacity(chargedItem) > 0) {
                                    energyItem.setStoredEnergy(chargedItem, energyItem.getEnergyCapacity(chargedItem));
                                    output.accept(chargedItem);
                                }
                            }
                        });
            })
            .build());

    public static void init(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);

        if (IOUtil.isEILoaded) EIIntegration.registerMachineConfigPanel();
    }
}
