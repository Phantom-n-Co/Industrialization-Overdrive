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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class IOComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IO.ID);

    public record BlockData(Item item, CompoundTag settings) {
    }

    public record Template(Map<Item, List<BlockPos>> blocks, Map<BlockPos, CompoundTag> settings) {
        private static final Codec<BlockPos> POSITION_CODEC = Codec.STRING.xmap(
                s -> {
                    String[] parts = s.split(",");
                    return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                },
                pos -> "%d,%d,%d".formatted(pos.getX(), pos.getY(), pos.getZ())
        );

        public static final Codec<Template> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), POSITION_CODEC.listOf()).fieldOf("blocks").forGetter(Template::blocks),
                Codec.unboundedMap(POSITION_CODEC, CompoundTag.CODEC).optionalFieldOf("settings", Map.of()).forGetter(Template::settings)
        ).apply(instance, Template::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Template> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC)),
                Template::blocks,
                ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.COMPOUND_TAG),
                Template::settings,
                Template::new
        );

        public static Template from(Map<BlockPos, BlockData> data) {
            Map<Item, List<BlockPos>> blocks = new HashMap<>();
            Map<BlockPos, CompoundTag> settings = new HashMap<>();
            data.forEach((pos, block) -> {
                blocks.computeIfAbsent(block.item(), ignored -> new ArrayList<>()).add(pos);
                if (!block.settings().isEmpty()) settings.put(pos, block.settings().copy());
            });
            return new Template(blocks, settings);
        }

        public Map<BlockPos, BlockData> expand() {
            Map<BlockPos, BlockData> data = new HashMap<>();
            blocks.forEach((item, positions) -> positions.forEach(pos -> data.put(pos,
                    new BlockData(item, settings.getOrDefault(pos, new CompoundTag())))));
            return data;
        }
    }

    public static final Supplier<DataComponentType<Boolean>> SILK_TOUCH = create("silk_touch", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<Integer>> VAJRA_SPEED = create("vajra_speed", Codec.INT, ByteBufCodecs.INT);
    public static final Supplier<DataComponentType<Boolean>> HIDE_BAR = create("hide_bar", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final Supplier<DataComponentType<MultiblockBuilder.Mode>> MULTI_BUILDER_MODE = create(
            "multibuilder/mode",
            Codec.STRING.xmap(MultiblockBuilder.Mode::valueOf, Enum::name),
            ByteBufCodecs.VAR_INT.map(index -> MultiblockBuilder.Mode.values()[index], MultiblockBuilder.Mode::ordinal));
    public static final Supplier<DataComponentType<Template>> MULTI_BUILDER_TEMPLATE = create("multibuilder/template",
            Template.CODEC, Template.STREAM_CODEC);

    public static void init(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private static <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> create(String name, Codec<D> codec, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec) {
        return COMPONENTS.registerComponentType(name, (b) -> b.persistent(codec).networkSynchronized(streamCodec));
    }
}
