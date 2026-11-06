package dev.wp.industrialization_overdrive;

import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import dev.wp.industrialization_overdrive.compat.EIMachineConfigCardCompat;
import dev.wp.industrialization_overdrive.machines.blocks.MEHatchBlock;
import dev.wp.industrialization_overdrive.machines.guicomponents.mehatch.MEHatchAmountMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.Comparator;
import java.util.function.Supplier;

public final class IOOtherRegistries {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, IO.ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, IO.ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IO.ID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, IO.ID);

    static {
        // Built by AE2's MenuTypeBuilder, which also wires AE2's opener; only the vanilla registration is ours.
        if (IOUtil.isAE2Loaded) MENU_TYPES.register("me_hatch_amount", () -> MEHatchAmountMenu.TYPE);
    }

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = IOOtherRegistries.CREATIVE_MODE_TABS.register(IO.ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.%s.%s".formatted(IO.ID, IO.ID)))
            .icon(() -> IOItems.MULTIBLOCK_BUILDER.get().getDefaultInstance())
            .displayItems((params, output) -> {
                Comparator<ItemHolder> compareBySortOrder = Comparator.comparing(ItemHolder::sortOrder);
                Comparator<ItemHolder> compareByName = Comparator.comparing((i) -> i.identifier().id());
                IOItems.values().stream()
                        .sorted(compareBySortOrder.thenComparing(compareByName))
                        .forEach((item) -> {
                            if (item.get() instanceof BlockItem bItem && bItem.getBlock() instanceof MEHatchBlock && !IOUtil.isAE2Loaded)
                                return;

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
        MENU_TYPES.register(bus);

        if (IOUtil.isEILoaded) EIMachineConfigCardCompat.register();
    }
}
