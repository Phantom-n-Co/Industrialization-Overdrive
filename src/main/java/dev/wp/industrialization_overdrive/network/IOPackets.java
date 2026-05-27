package dev.wp.industrialization_overdrive.network;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.network.packet.ModifyTerminalModePacket;
import dev.wp.industrialization_overdrive.network.packet.ModifyVajraSpeedPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.swedz.tesseract.neoforge.packet.PacketRegistry;

public class IOPackets {
    private static final PacketRegistry<IOCustomPacket> REGISTRY = PacketRegistry.create(IO.ID);

    public static CustomPacketPayload.Type<IOCustomPacket> getType(Class<? extends IOCustomPacket> packetClass) {
        return REGISTRY.getType(packetClass);
    }

    public static void init(RegisterPayloadHandlersEvent event) {
        REGISTRY.registerAll(event);
    }

    static {
        register("modify_vajra_speed", ModifyVajraSpeedPacket.class, ModifyVajraSpeedPacket.STREAM_CODEC);
        register("modify_terminal_mode", ModifyTerminalModePacket.class, ModifyTerminalModePacket.STREAM_CODEC);
    }

    private static <P extends IOCustomPacket> void register(String id, Class<P> packetClass, StreamCodec<? super RegistryFriendlyByteBuf, P> packetCodec) {
        REGISTRY.create(id, packetClass, packetCodec);
    }
}
