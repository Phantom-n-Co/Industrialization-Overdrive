package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import aztech.modern_industrialization.machines.gui.GuiComponent;
import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import appeng.api.stacks.GenericStack;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MEHatchGui implements GuiComponentServer<Boolean, MEHatchGui.Data> {
    public static final Type<Boolean, Data> TYPE = new Type<>(
            IO.id("me_hatch"), ByteBufCodecs.BOOL, Data.STREAM_CODEC);

    private final MEHatchBlockEntity hatch;

    public MEHatchGui(MEHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    /** Amount-button strip above each config row: AE2's 16px cog plus a 2px gap. */
    public static final int COG_STRIP_HEIGHT = 18;

    public static boolean isMixed(int itemCount, int fluidCount) {
        return itemCount == Data.CHANNEL_COUNT
                && fluidCount == Data.CHANNEL_COUNT;
    }

    @Override
    public Boolean getParams() {
        return hatch.isInput();
    }

    @Override
    public Data extractData() {
        return hatch.collectGuiData();
    }

    @Override
    public Type<Boolean, Data> getType() {
        return TYPE;
    }

    @Override
    public void setupMenu(GuiComponent.MenuFacade menu) {
        boolean hasItems = hatch.getMEHatchType().handlesItems();
        boolean hasFluids = hatch.getMEHatchType().handlesFluids();
        boolean mixed = hasItems && hasFluids;

        if (hasItems) {
            MEHatchConfigSlot.addRow(menu, hatch, false, hatch.isInput(), mixed, i -> hatch.getConfig(false, i), i -> null);
        }
        if (hasFluids) {
            MEHatchConfigSlot.addRow(menu, hatch, true, hatch.isInput(), mixed, i -> hatch.getConfig(true, i), i -> null);
        }
    }

    /** Lists have nine positions; null is unconfigured (config) or empty (stock). Targets may exceed the configured amount while smart pull raises it. */
    public record Data(
            List<@Nullable GenericStack> itemConfig,
            List<@Nullable GenericStack> itemStock,
            List<@Nullable GenericStack> fluidConfig,
            List<@Nullable GenericStack> fluidStock,
            int transferInterval,
            boolean smartPull,
            List<Long> itemTarget,
            List<Long> fluidTarget,
            boolean storageMode,
            int priority,
            boolean hasCell) {
        public static final int CHANNEL_COUNT = MEHatchConfigComponent.CHANNEL_COUNT;

        // GenericStack.STREAM_CODEC is non-nullable; an empty channel must sync as a real null.
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Data decode(RegistryFriendlyByteBuf buffer) {
                return new Data(
                        readList(buffer),
                        readList(buffer),
                        readList(buffer),
                        readList(buffer),
                        buffer.readVarInt(),
                        buffer.readBoolean(),
                        readTargets(buffer),
                        readTargets(buffer),
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Data value) {
                writeList(buffer, value.itemConfig());
                writeList(buffer, value.itemStock());
                writeList(buffer, value.fluidConfig());
                writeList(buffer, value.fluidStock());
                buffer.writeVarInt(value.transferInterval());
                buffer.writeBoolean(value.smartPull());
                value.itemTarget().forEach(buffer::writeVarLong);
                value.fluidTarget().forEach(buffer::writeVarLong);
                buffer.writeBoolean(value.storageMode());
                buffer.writeVarInt(value.priority());
                buffer.writeBoolean(value.hasCell());
            }
        };

        public Data {
            itemConfig = checkedCopy(itemConfig, "itemConfig");
            itemStock = checkedCopy(itemStock, "itemStock");
            fluidConfig = checkedCopy(fluidConfig, "fluidConfig");
            fluidStock = checkedCopy(fluidStock, "fluidStock");
            itemTarget = checkedTargets(itemTarget, "itemTarget");
            fluidTarget = checkedTargets(fluidTarget, "fluidTarget");
        }

        private static List<Long> checkedTargets(List<Long> values, String name) {
            Objects.requireNonNull(values, name);
            if (values.size() != CHANNEL_COUNT) {
                throw new IllegalArgumentException(name + " must contain exactly " + CHANNEL_COUNT
                        + " entries, got " + values.size());
            }
            return List.copyOf(values);
        }

        private static List<Long> readTargets(RegistryFriendlyByteBuf buffer) {
            List<Long> values = new ArrayList<>(CHANNEL_COUNT);
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                values.add(buffer.readVarLong());
            }
            return values;
        }

        private static List<@Nullable GenericStack> checkedCopy(
                List<@Nullable GenericStack> values, String name) {
            Objects.requireNonNull(values, name);
            if (values.size() != CHANNEL_COUNT) {
                throw new IllegalArgumentException(name + " must contain exactly " + CHANNEL_COUNT
                        + " entries, got " + values.size());
            }
            for (GenericStack value : values) {
                if (value != null && value.amount() <= 0) {
                    throw new IllegalArgumentException(name + " contains a non-positive GenericStack amount");
                }
            }
            return Collections.unmodifiableList(new ArrayList<>(values));
        }

        private static void writeList(RegistryFriendlyByteBuf buffer, List<@Nullable GenericStack> values) {
            for (GenericStack value : values) {
                GenericStack.writeBuffer(value, buffer);
            }
        }

        private static List<@Nullable GenericStack> readList(RegistryFriendlyByteBuf buffer) {
            List<@Nullable GenericStack> values = new ArrayList<>(CHANNEL_COUNT);
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                values.add(GenericStack.readBuffer(buffer));
            }
            return values;
        }
    }
}
