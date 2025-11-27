package dev.wp.industrialization_overdrive.network;

import net.swedz.tesseract.neoforge.packet.CustomPacket;

public interface IOCustomPacket extends CustomPacket {
    @Override
    default Type<IOCustomPacket> type() {
        return IOPackets.getType(this.getClass());
    }
}
