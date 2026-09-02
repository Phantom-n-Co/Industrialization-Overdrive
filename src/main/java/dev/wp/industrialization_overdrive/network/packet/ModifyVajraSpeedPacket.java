package dev.wp.industrialization_overdrive.network.packet;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.item.Vajra;
import dev.wp.industrialization_overdrive.network.IOCustomPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.swedz.tesseract.neoforge.packet.PacketContext;

public record ModifyVajraSpeedPacket(boolean increase) implements IOCustomPacket {
    public static final StreamCodec<ByteBuf, ModifyVajraSpeedPacket> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(ModifyVajraSpeedPacket::new, ModifyVajraSpeedPacket::increase);

    @Override
    public void handle(PacketContext ctx) {
        ctx.assertServerbound();

        var player = ctx.getPlayer();
        var stack = player.getMainHandItem();

        if (stack.getItem() instanceof Vajra item) {
            Vajra.Speed origSpeed = item.getToolSpeed(stack);
            int speedIndex = Math.clamp(origSpeed.ordinal() + (increase ? 1 : -1), 0, Vajra.Speed.values().length - 1);
            Vajra.Speed speed = Vajra.Speed.values()[speedIndex];

            if (speed != origSpeed) {
                item.setToolSpeed(stack, speed);

                Component vajraSpeed = null;
                switch (speed) {
                    case SLOW -> vajraSpeed = IO.text().vajraSpeedSlow();
                    case NORMAL -> vajraSpeed = IO.text().vajraSpeedNormal();
                    case FAST -> vajraSpeed = IO.text().vajraSpeedFast();
                    case INSTANT -> vajraSpeed = IO.text().vajraSpeedInstant();
                }

                player.displayClientMessage(IO.text().vajraSpeedChanged(vajraSpeed), true);
            }
        }
    }
}
