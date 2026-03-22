package dev.wp.industrialization_overdrive;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class IOComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IO.ID);

    public static final Supplier<DataComponentType<Boolean>> SILK_TOUCH = create("silk_touch", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<Integer>> VAJRA_SPEED = create("vajra_speed", Codec.INT, ByteBufCodecs.INT);
    public static final Supplier<DataComponentType<Boolean>> HIDE_BAR = create("hide_bar", Codec.BOOL, ByteBufCodecs.BOOL);

    public static void init(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private static <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> create(String name, Codec<D> codec, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec) {
        return COMPONENTS.registerComponentType(name, (b) -> b.persistent(codec).networkSynchronized(streamCodec));
    }
}
