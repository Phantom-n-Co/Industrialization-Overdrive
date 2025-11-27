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
            int origSpeed = item.getToolSpeed(stack);

            int speed = origSpeed;
            speed += increase ? 1 : -1;
            speed = Math.clamp(speed, Vajra.MIN_SPEED, Vajra.MAX_SPEED);

            if (speed != origSpeed) {
                item.setToolSpeed(stack, speed);

                Component vajraSpeed = null;
                switch (speed) {
                    case 1 -> vajraSpeed = IO.text().vajraSpeedSlow();
                    case 2 -> vajraSpeed = IO.text().vajraSpeedNormal();
                    case 3 -> vajraSpeed = IO.text().vajraSpeedFast();
                    case 4 -> vajraSpeed = IO.text().vajraSpeedInstant();
                }

                player.displayClientMessage(IO.text().vajraSpeedChanged(vajraSpeed), true);
            }
        }
    }
}
