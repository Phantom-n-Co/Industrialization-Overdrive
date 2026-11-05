package dev.wp.industrialization_overdrive.network.packet;

import appeng.menu.implementations.PriorityMenu;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.guicomponents.mehatch.MEHatchAmountMenu;
import dev.wp.industrialization_overdrive.network.IOCustomPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.swedz.tesseract.neoforge.packet.PacketContext;

/**
 * Opens an editor for the ME hatch in the player's open menu; menus can only be opened by the server. {@code index}
 * and {@code fluid} select the channel for {@link Editor#AMOUNT} and are ignored for {@link Editor#PRIORITY}, whose
 * back button returns to the hatch screen through {@code MEHatchBlockEntity.returnToMainMenu}.
 */
public record OpenMEHatchEditorPacket(int syncId, Editor editor, int index, boolean fluid) implements IOCustomPacket {
    public enum Editor {
        AMOUNT,
        /** AE2's own priority screen. */
        PRIORITY
    }

    public static final StreamCodec<ByteBuf, OpenMEHatchEditorPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OpenMEHatchEditorPacket::syncId,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Editor::ordinal, Editor.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
                    Editor::ordinal),
            OpenMEHatchEditorPacket::editor,
            ByteBufCodecs.VAR_INT,
            OpenMEHatchEditorPacket::index,
            ByteBufCodecs.BOOL,
            OpenMEHatchEditorPacket::fluid,
            OpenMEHatchEditorPacket::new);

    public static OpenMEHatchEditorPacket amount(int syncId, int index, boolean fluid) {
        return new OpenMEHatchEditorPacket(syncId, Editor.AMOUNT, index, fluid);
    }

    public static OpenMEHatchEditorPacket priority(int syncId) {
        return new OpenMEHatchEditorPacket(syncId, Editor.PRIORITY, 0, false);
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
        switch (editor) {
            case AMOUNT -> MEHatchAmountMenu.open(player, hatch, fluid, index);
            case PRIORITY -> hatch.openEditor(player, PriorityMenu.TYPE);
        }
    }
}
