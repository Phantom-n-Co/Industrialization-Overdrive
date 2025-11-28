package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.client.machines.MachineBlockEntityRenderer;
import aztech.modern_industrialization.client.machines.multiblocks.MultiblockMachineBER;
import aztech.modern_industrialization.client.machines.multiblocks.MultiblockTankBER;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.LargeTankMultiblockBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import dev.wp.industrialization_overdrive.item.Vajra;
import dev.wp.industrialization_overdrive.network.packet.ModifyVajraSpeedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(value = IO.ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = IO.ID)
public final class IOClient {

    public IOClient(IEventBus bus) {

        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, (event) -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.isShiftKeyDown()) {
                var stack = player.getMainHandItem();
                if (stack.getItem() == IOItems.VAJRA.get()) {
                    boolean increase = event.getScrollDeltaY() > 0;
                    int speed = Vajra.getToolSpeed(stack);
                    if (increase ? speed < Vajra.MAX_SPEED : speed > Vajra.MIN_SPEED) {
                        new ModifyVajraSpeedPacket(increase).sendToServer();
                    }

                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    private static void registerBlockEntityRenderers(FMLClientSetupEvent event) {
        for (DeferredHolder<Block, ? extends Block> blockDef : IOBlocks.Registry.BLOCKS.getEntries()) {
            if (blockDef.get() instanceof MachineBlock machine) {
                MachineBlockEntity blockEntity = machine.getBlockEntityInstance();
                BlockEntityType type = blockEntity.getType();

                BlockEntityRendererProvider provider = switch (blockEntity) {
                    case LargeTankMultiblockBlockEntity be -> MultiblockTankBER::new;
                    case MultiblockMachineBlockEntity be -> MultiblockMachineBER::new;
                    default -> MachineBlockEntityRenderer::new;
                };
                BlockEntityRenderers.register(type, provider);
            }
        }
    }
}
