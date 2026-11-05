package dev.wp.industrialization_overdrive.network.packet;

import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.network.IOCustomPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.swedz.tesseract.neoforge.packet.PacketContext;

/** Sets the positive target amount for one configured input channel. */
public record SetMEHatchAmountPacket(int syncId, int index, boolean fluid, long amount) implements IOCustomPacket {
    public static final StreamCodec<ByteBuf, SetMEHatchAmountPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetMEHatchAmountPacket::syncId,
            ByteBufCodecs.VAR_INT,
            SetMEHatchAmountPacket::index,
            ByteBufCodecs.BOOL,
            SetMEHatchAmountPacket::fluid,
            ByteBufCodecs.VAR_LONG,
            SetMEHatchAmountPacket::amount,
            SetMEHatchAmountPacket::new);

    @Override
    public void handle(PacketContext ctx) {
        ctx.assertServerbound();
        if (!(ctx.getPlayer() instanceof ServerPlayer player) || amount <= 0) {
            return;
        }

        MEHatchBlockEntity hatch = MEHatchBlockEntity.getActiveHatch(player, syncId);
        if (hatch == null) {
            return;
        }

        hatch.setInputTarget(fluid, index, amount);
    }
}
