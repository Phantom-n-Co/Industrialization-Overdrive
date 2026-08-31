package dev.wp.industrialization_overdrive.client;

import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOItems;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MultiblockBuilderRenderer {
    private static final int GREEN = 0x55CC55;
    private static final int RED = 0xDD5555;

    private record Preview(BlockPos pos, BlockState state, int tint, CompoundTag settings) {}

    private MultiblockBuilderRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() == HitResult.Type.MISS) return;

        ItemStack stack = heldBuilder(player);
        if (stack.isEmpty()) return;

        List<Preview> previews = previews(level, player, hit, stack);
        if (previews.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        var camera = event.getCamera().getPosition();
        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -10.0F);
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            for (Preview preview : previews) {
                poseStack.pushPose();
                try {
                    poseStack.translate(preview.pos.getX(), preview.pos.getY(), preview.pos.getZ());
                    float scale = preview.tint == GREEN ? 0.999F : 1.001F;
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    poseStack.scale(scale, scale, scale);
                    poseStack.translate(-0.5F, -0.5F, -0.5F);
                    ModelData modelData = modelData(level, preview);
                    if (modelData == null) {
                        dispatcher.renderSingleBlock(preview.state, poseStack,
                                tintedSource(buffers, preview.tint), 0xF000F0, 0);
                    } else {
                        dispatcher.renderBatched(preview.state, preview.pos, level, poseStack,
                                tinted(buffers.getBuffer(RenderType.translucent()), preview.tint), false,
                                RandomSource.create(), modelData, RenderType.translucent());
                    }
                } finally {
                    poseStack.popPose();
                }
            }
            buffers.endBatch(RenderType.translucent());
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.disablePolygonOffset();
            poseStack.popPose();
        }
    }

    private static ItemStack heldBuilder(Player player) {
        for (var hand : player.getHandSlots()) {
            if (hand.is(IOItems.MULTIBLOCK_BUILDER.get())) return hand;
        }
        return ItemStack.EMPTY;
    }

    private static List<Preview> previews(ClientLevel level, Player player, BlockHitResult hit, ItemStack stack) {
        MultiblockBuilder.Mode mode = stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, MultiblockBuilder.Mode.BUILD);
        if (mode == MultiblockBuilder.Mode.BUILD) return buildPreviews(level, hit.getBlockPos());
        if (mode != MultiblockBuilder.Mode.COPY_PASTE) return List.of();

        IOComponents.Template storedTemplate = stack.get(IOComponents.MULTI_BUILDER_TEMPLATE);
        if (storedTemplate == null || storedTemplate.blocks().isEmpty()) return List.of();
        Map<BlockPos, IOComponents.BlockData> template = storedTemplate.expand();
        for (var entry : template.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) return List.of();
        }
        if (template.get(BlockPos.ZERO) == null) return List.of();

        Rotation rotation = MultiblockBuilder.getPasteRotation(template.get(BlockPos.ZERO).settings(), player.getDirection().getOpposite());
        BlockPos pasteTarget = player.isShiftKeyDown() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
        BlockPos anchor = MultiblockBuilder.pastePosition(pasteTarget, template, rotation);
        List<Preview> result = new ArrayList<>();
        for (var entry : template.entrySet()) {
            Block block = Block.byItem(entry.getValue().item());
            if (block == Blocks.AIR) return List.of();
            addPreview(level, result, anchor.offset(MultiblockBuilder.rotateOffset(entry.getKey(), rotation)),
                    block.defaultBlockState(), MultiblockBuilder.rotateSettings(entry.getValue().settings(), rotation));
        }
        return result;
    }

    private static List<Preview> buildPreviews(ClientLevel level, BlockPos controllerPos) {
        if (!(level.getBlockEntity(controllerPos) instanceof MultiblockMachineBlockEntity machine)) return List.of();
        ShapeMatcher matcher = machine.createShapeMatcher();
        List<Preview> result = new ArrayList<>();
        for (BlockPos pos : matcher.getPositions()) {
            if (!level.hasChunkAt(pos) || alreadyValid(matcher, level, pos)) continue;
            var member = matcher.getSimpleMember(pos);
            addPreview(level, result, pos, ShapeMatcher.toWorldState(level, pos,
                    member.getPreviewState(), matcher.controllerDirection), null);
        }
        return result;
    }

    private static void addPreview(ClientLevel level, List<Preview> result, BlockPos pos, BlockState state, CompoundTag settings) {
        if (!level.hasChunkAt(pos) || state.isAir()) return;
        BlockState current = level.getBlockState(pos);
        if (current.equals(state)) return;
        result.add(new Preview(pos, state, current.canBeReplaced() ? GREEN : RED, settings));
    }

    private static ModelData modelData(ClientLevel level, Preview preview) {
        if (preview.settings == null || !(preview.state.getBlock() instanceof MachineBlock machineBlock)) return null;
        try {
            MachineBlockEntity machine = machineBlock.newBlockEntity(preview.pos, preview.state);
            machine.load(preview.settings, level.registryAccess(), false);
            return machine.getModelData();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean alreadyValid(ShapeMatcher matcher, ClientLevel level, BlockPos pos) {
        var previewState = matcher.getSimpleMember(pos).getPreviewState();
        if (level.getBlockState(pos).getBlock().asItem().equals(previewState.getBlock().asItem())) return true;
        if (!(level.getBlockEntity(pos) instanceof HatchBlockEntity hatch)) return false;
        var flags = matcher.getHatchFlags(pos);
        return flags != null && flags.allows(hatch.getHatchType());
    }

    private static VertexConsumer tinted(VertexConsumer delegate, int tint) {
        int red = tint >> 16 & 255;
        int green = tint >> 8 & 255;
        int blue = tint & 255;
        return new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { return delegate.addVertex(x, y, z); }
            @Override public VertexConsumer setColor(int r, int g, int b, int alpha) {
                return delegate.setColor(r * red / 255, g * green / 255, b * blue / 255, 125);
            }
            @Override public VertexConsumer setUv(float u, float v) { return delegate.setUv(u, v); }
            @Override public VertexConsumer setUv1(int u, int v) { return delegate.setUv1(u, v); }
            @Override public VertexConsumer setUv2(int u, int v) { return delegate.setUv2(u, v); }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return delegate.setNormal(x, y, z); }
        };
    }

    private static MultiBufferSource tintedSource(MultiBufferSource source, int tint) {
        return renderType -> tinted(source.getBuffer(RenderType.translucent()), tint);
    }
}
