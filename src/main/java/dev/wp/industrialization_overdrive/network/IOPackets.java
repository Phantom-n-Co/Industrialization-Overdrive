package dev.wp.industrialization_overdrive.network;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOUtil;
import dev.wp.industrialization_overdrive.network.packet.ModifyMultiblockBuilderModePacket;
import dev.wp.industrialization_overdrive.network.packet.ModifyVajraSpeedPacket;
import dev.wp.industrialization_overdrive.network.packet.OpenMEHatchEditorPacket;
import dev.wp.industrialization_overdrive.network.packet.SetMEHatchAmountPacket;
import dev.wp.industrialization_overdrive.network.packet.SetMEHatchSettingPacket;
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
        register("modify_terminal_mode", ModifyMultiblockBuilderModePacket.class, ModifyMultiblockBuilderModePacket.STREAM_CODEC);
        if (IOUtil.isAE2Loaded) {
            // Same mod set on both sides, so the payload ids match.
            register("set_me_hatch_amount", SetMEHatchAmountPacket.class, SetMEHatchAmountPacket.STREAM_CODEC);
            register("open_me_hatch_editor", OpenMEHatchEditorPacket.class, OpenMEHatchEditorPacket.STREAM_CODEC);
            register("set_me_hatch_setting", SetMEHatchSettingPacket.class, SetMEHatchSettingPacket.STREAM_CODEC);
        }
    }

    private static <P extends IOCustomPacket> void register(String id, Class<P> packetClass, StreamCodec<? super RegistryFriendlyByteBuf, P> packetCodec) {
        REGISTRY.create(id, packetClass, packetCodec);
    }
}
