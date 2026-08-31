package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.client.machines.MachineBlockEntityRenderer;
import aztech.modern_industrialization.client.machines.multiblocks.MultiblockMachineBER;
import aztech.modern_industrialization.client.machines.multiblocks.MultiblockTankBER;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.LargeTankMultiblockBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import dev.wp.industrialization_overdrive.client.MultiblockBuilderRenderer;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import dev.wp.industrialization_overdrive.item.Vajra;
import dev.wp.industrialization_overdrive.network.packet.ModifyMultiblockBuilderModePacket;
import dev.wp.industrialization_overdrive.network.packet.ModifyVajraSpeedPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lwjgl.glfw.GLFW;

@Mod(value = IO.ID, dist = Dist.CLIENT)
public final class IOClient {
    public static final KeyMapping MULTIBLOCK_BUILDER_MODE_SWITCH = new KeyMapping(
            "key.industrialization_overdrive.terminal_mode_switch",
            GLFW.GLFW_KEY_V,
            "key.categories.industrialization_overdrive"
    );

    public IOClient(IEventBus bus) {
        bus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(MULTIBLOCK_BUILDER_MODE_SWITCH));
        bus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> ItemProperties.register(
                IOItems.MULTIBLOCK_BUILDER.get(),
                IO.id("mode"),
                (stack, level, entity, seed) -> stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, MultiblockBuilder.Mode.BUILD).ordinal()
        )));
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, MultiblockBuilderRenderer::render);

        NeoForge.EVENT_BUS.addListener(InputEvent.Key.class, (event) -> {
            var player = Minecraft.getInstance().player;
            if (player != null && MULTIBLOCK_BUILDER_MODE_SWITCH.consumeClick()) {
                var stack = player.getMainHandItem();
                if (stack.getItem() == IOItems.MULTIBLOCK_BUILDER.get()) {
                    new ModifyMultiblockBuilderModePacket(true).sendToServer();
                }
            }
        });

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

    @EventBusSubscriber(value = Dist.CLIENT, modid = IO.ID)
    public static final class Renderers {
        @SubscribeEvent
        private static void registerBlockEntityRenderers(FMLClientSetupEvent event) {
            for (DeferredHolder<Block, ? extends Block> blockDef : IOBlocks.Registry.BLOCKS.getEntries()) {
                if (blockDef.get() instanceof MachineBlock machine) {
                    MachineBlockEntity blockEntity = machine.getBlockEntityInstance();
                    BlockEntityType type = blockEntity.getType();

                    BlockEntityRendererProvider provider = switch (blockEntity) {
                        case LargeTankMultiblockBlockEntity ignored -> MultiblockTankBER::new;
                        case MultiblockMachineBlockEntity ignored -> MultiblockMachineBER::new;
                        default -> MachineBlockEntityRenderer::new;
                    };
                    BlockEntityRenderers.register(type, provider);
                }
            }
        }
    }
}
