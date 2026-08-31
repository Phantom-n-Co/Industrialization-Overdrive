package dev.wp.industrialization_overdrive.network.packet;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOItems;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import dev.wp.industrialization_overdrive.network.IOCustomPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.swedz.tesseract.neoforge.packet.PacketContext;

public record ModifyMultiblockBuilderModePacket(boolean next) implements IOCustomPacket {
    public static final StreamCodec<ByteBuf, ModifyMultiblockBuilderModePacket> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(ModifyMultiblockBuilderModePacket::new, ModifyMultiblockBuilderModePacket::next);

    private static final MultiblockBuilder.Mode[] MODES = MultiblockBuilder.Mode.values();

    @Override
    public void handle(PacketContext ctx) {
        ctx.assertServerbound();

        var player = ctx.getPlayer();
        var stack = player.getMainHandItem();

        if (stack.getItem() == IOItems.MULTIBLOCK_BUILDER.get()) {
            MultiblockBuilder.Mode currentMode = stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, MultiblockBuilder.Mode.BUILD);
            int index = 0;
            for (int i = 0; i < MODES.length; i++) {
                if (MODES[i] == currentMode) {
                    index = i;
                    break;
                }
            }

            if (next) {
                index = (index + 1) % MODES.length;
            } else {
                index = (index - 1 + MODES.length) % MODES.length;
            }

            MultiblockBuilder.Mode newMode = MODES[index];
            stack.set(IOComponents.MULTI_BUILDER_MODE, newMode);

            Component mode = switch (newMode) {
                case BUILD -> IO.text().multiblockBuilderModeBuild();
                case COPY_PASTE -> IO.text().multiblockBuilderModeCopyPaste();
                case TEAR_DOWN -> IO.text().multiblockBuilderModeTearDown();
            };
            player.displayClientMessage(IO.text().multiblockBuilderModeChanged(mode), true);
        }
    }
}
