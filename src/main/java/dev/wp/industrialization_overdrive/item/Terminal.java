package dev.wp.industrialization_overdrive.item;

import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOUtil;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class Terminal extends Item {
    private static final int COLOR_MISSING = DyeColor.RED.getTextColor();
    private static final int COLOR_ENOUGH = DyeColor.GREEN.getTextColor();
    private static final int COLOR_HEADER = DyeColor.YELLOW.getTextColor();

    public Terminal(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!IOUtil.isRealPlayer(player) || level.isClientSide)
            return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MultiblockMachineBlockEntity multiblock))
            return InteractionResult.PASS;
        return handleMultiblockUse(level, pos, player, multiblock);
    }

    private InteractionResult handleMultiblockUse(Level level, BlockPos pos, Player player, MultiblockMachineBlockEntity multiblock) {
        ShapeMatcher shapeMatcher = multiblock.createShapeMatcher();
        if (shapeMatcher == null) {
            player.sendSystemMessage(Component.literal("Unable to get shape matcher for this multiblock.").withColor(COLOR_MISSING));
            return InteractionResult.FAIL;
        }
        if (player.isCreative()) {
            buildMultiblock(level, pos, player, shapeMatcher);
            return InteractionResult.SUCCESS;
        }
        return handleSurvivalBuild(level, pos, player, shapeMatcher);
    }

    private void buildMultiblock(Level level, BlockPos pos, Player player, ShapeMatcher shapeMatcher) {
        for (BlockPos memberPos : shapeMatcher.getPositions()) {
            if (isAlreadyValid(shapeMatcher, level, memberPos)) continue;
            var member = shapeMatcher.getSimpleMember(memberPos);
            BlockState previewState = member != null ? member.getPreviewState() : null;
            if (previewState != null) {
                level.setBlock(memberPos, previewState, 3);
            }
        }
        player.sendSystemMessage(Component.literal(String.format("Successfully built multiblock at %s, %s, %s", pos.getX(), pos.getY(), pos.getZ())).withColor(COLOR_ENOUGH));
    }

    private InteractionResult handleSurvivalBuild(Level level, BlockPos pos, Player player, ShapeMatcher shapeMatcher) {
        Map<Item, Integer> requiredBlocks = new HashMap<>();
        boolean obstructionFound = false;
        for (BlockPos checkPos : shapeMatcher.getPositions()) {
            if (isAlreadyValid(shapeMatcher, level, checkPos)) continue;
            BlockState state = level.getBlockState(checkPos);
            var member = shapeMatcher.getSimpleMember(checkPos);
            BlockState previewState = member != null ? member.getPreviewState() : null;
            if (!state.canBeReplaced()) {
                obstructionFound = true;
                player.sendSystemMessage(Component.literal(String.format("Block at %s is not part of the multiblock", checkPos.toShortString())).withColor(COLOR_MISSING));
            }
            if (state.canBeReplaced() && previewState != null) {
                Item block = previewState.getBlock().asItem();
                requiredBlocks.merge(block, 1, Integer::sum);
            }
        }
        if (obstructionFound) return InteractionResult.SUCCESS;
        if (!checkAndConsumeInventory(player, level, requiredBlocks)) {
            return InteractionResult.FAIL;
        }
        buildMultiblock(level, pos, player, shapeMatcher);
        return InteractionResult.SUCCESS;
    }

    private boolean checkAndConsumeInventory(Player player, Level level, Map<Item, Integer> requiredBlocks) {
        Map<Item, Integer> inventoryCount = countInventory(player);
        Map<Item, Integer> missingItems = new HashMap<>();
        for (var entry : requiredBlocks.entrySet()) {
            Item block = entry.getKey();
            int requiredCount = entry.getValue();
            int countInInventory = inventoryCount.getOrDefault(block, 0);
            if (countInInventory < requiredCount) {
                missingItems.put(block, requiredCount - countInInventory);
            }
        }
        Map<Item, Integer> meProvided = IOUtil.isAE2Loaded ? AE2Integration.simulateExtractFromME(player, level, missingItems) : new HashMap<>();
        boolean anyMissing = false;
        for (var entry : requiredBlocks.entrySet()) {
            Item item = entry.getKey();
            int required = entry.getValue();
            int inInv = inventoryCount.getOrDefault(item, 0);
            int inMe = meProvided.getOrDefault(item, 0);
            int missing = required - (inInv + inMe);
            if (missing > 0) {
                anyMissing = true;
                break;
            }
        }
        if (anyMissing) {
            player.sendSystemMessage(Component.literal("Required items:").withColor(COLOR_HEADER));
            for (var entry : requiredBlocks.entrySet()) {
                Item item = entry.getKey();
                int required = entry.getValue();
                int inInv = inventoryCount.getOrDefault(item, 0);
                int inMe = meProvided.getOrDefault(item, 0);
                int missing = required - (inInv + inMe);
                if (missing > 0) {
                    player.sendSystemMessage(formatItemLine(item, missing, COLOR_MISSING));
                } else {
                    player.sendSystemMessage(formatItemLine(item, required, COLOR_ENOUGH));
                }
            }
            return false;
        }
        if (IOUtil.isAE2Loaded && !meProvided.isEmpty()) {
            AE2Integration.extractFromME(player, level, meProvided);
        }
        consumeFromInventory(player, requiredBlocks);
        return true;
    }

    private static Map<Item, Integer> countInventory(Player player) {
        Map<Item, Integer> inventoryCount = new HashMap<>();
        for (ItemStack stack : player.getInventory().items) {
            Item id = stack.getItem();
            inventoryCount.merge(id, stack.getCount(), Integer::sum);
        }
        return inventoryCount;
    }

    private static void consumeFromInventory(Player player, Map<Item, Integer> requiredBlocks) {
        for (var entry : requiredBlocks.entrySet()) {
            Item block = entry.getKey();
            int remainingToConsume = entry.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem().equals(block) && remainingToConsume > 0) {
                    int amountToShrink = Math.min(remainingToConsume, stack.getCount());
                    stack.shrink(amountToShrink);
                    remainingToConsume -= amountToShrink;
                    if (remainingToConsume == 0) break;
                }
            }
        }
    }

    private static boolean isAlreadyValid(ShapeMatcher shapeMatcher, Level level, BlockPos pos) {
        if (shapeMatcher.matches(pos, level)) return true;
        var be = level.getBlockEntity(pos);
        var hatchFlags = shapeMatcher.getHatchFlags(pos);
        return be instanceof HatchBlockEntity hatch && hatchFlags != null && hatchFlags.allows(hatch.getHatchType());
    }

    private static Component formatItemLine(Item item, int count, int color) {
        String name = item.getDefaultInstance().getHoverName().getString();
        return Component.literal(String.format("- %sx %s", count, name)).withColor(color);
    }

    public static GlobalPos getLinkPos(ItemStack stack) {
        return stack.get(IOComponents.TERMINAL_LINK);
    }

    public static void setLinkPos(ItemStack stack, GlobalPos pos) {
        stack.set(IOComponents.TERMINAL_LINK, pos);
    }

    public static void clearLinkPos(ItemStack stack) {
        stack.remove(IOComponents.TERMINAL_LINK);
    }
}

