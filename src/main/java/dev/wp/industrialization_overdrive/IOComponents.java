package dev.wp.industrialization_overdrive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class IOComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IO.ID);

    public record BlockData(Item item, CompoundTag settings) {
        public static final Codec<BlockData> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(BlockData::item),
                CompoundTag.CODEC.optionalFieldOf("settings", new CompoundTag()).forGetter(BlockData::settings)
        ).apply(instance, BlockData::new)));

        public static final StreamCodec<RegistryFriendlyByteBuf, BlockData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM),
                BlockData::item,
                ByteBufCodecs.COMPOUND_TAG,
                BlockData::settings,
                BlockData::new
        );
    }

    public static final Supplier<DataComponentType<Boolean>> SILK_TOUCH = create("silk_touch", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<Integer>> VAJRA_SPEED = create("vajra_speed", Codec.INT, ByteBufCodecs.INT);
    public static final Supplier<DataComponentType<Boolean>> HIDE_BAR = create("hide_bar", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<MultiblockBuilder.Mode>> MULTI_BUILDER_MODE = create(
            "multibuilder/mode",
            Codec.STRING.xmap(MultiblockBuilder.Mode::valueOf, Enum::name),
            ByteBufCodecs.VAR_INT.map(index -> MultiblockBuilder.Mode.values()[index], MultiblockBuilder.Mode::ordinal));
    public static final Supplier<DataComponentType<Map<BlockPos, BlockData>>> MULTI_BUILDER_TEMPLATE = create("multi_builder_template",
            Codec.unboundedMap(
                    Codec.STRING.xmap(
                            s -> {
                                String[] parts = s.split(",");
                                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            },
                            pos -> "%d,%d,%d".formatted(pos.getX(), pos.getY(), pos.getZ())
                    ),
                    BlockData.CODEC
            ),
            ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, BlockData.STREAM_CODEC));

    public static void init(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private static <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> create(String name, Codec<D> codec, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec) {
        return COMPONENTS.registerComponentType(name, (b) -> b.persistent(codec).networkSynchronized(streamCodec));
    }
}
