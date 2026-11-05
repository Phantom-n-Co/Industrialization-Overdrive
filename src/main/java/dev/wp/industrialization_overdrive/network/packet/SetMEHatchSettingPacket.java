package dev.wp.industrialization_overdrive.network.packet;

import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.network.IOCustomPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.swedz.tesseract.neoforge.packet.PacketContext;

/** Changes one setting of the ME hatch in the player's open menu; the hatch's setters validate the value. */
public record SetMEHatchSettingPacket(int syncId, Setting setting, int value) implements IOCustomPacket {
    public enum Setting {
        /** Transfer interval, in ticks. */
        INTERVAL,
        /** Smart pull on or off (0 or 1). */
        SMART_PULL,
        /** Push or Storage mode of an output hatch (0 or 1). */
        STORAGE_MODE
    }

    public static final StreamCodec<ByteBuf, SetMEHatchSettingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetMEHatchSettingPacket::syncId,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Setting::ordinal, Setting.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
                    Setting::ordinal),
            SetMEHatchSettingPacket::setting,
            ByteBufCodecs.VAR_INT,
            SetMEHatchSettingPacket::value,
            SetMEHatchSettingPacket::new);

    public static SetMEHatchSettingPacket of(int syncId, Setting setting, boolean enabled) {
        return new SetMEHatchSettingPacket(syncId, setting, enabled ? 1 : 0);
    }

    @Override
    public void handle(PacketContext ctx) {
        ctx.assertServerbound();
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        MEHatchBlockEntity hatch = MEHatchBlockEntity.getActiveHatch(player, syncId);
        if (hatch == null) {
            return;
        }
        switch (setting) {
            case INTERVAL -> hatch.setTransferInterval(value);
            case SMART_PULL -> hatch.setSmartPull(value != 0);
            case STORAGE_MODE -> hatch.setStorageMode(value != 0);
        }
    }
}
