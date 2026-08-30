package dev.wp.industrialization_overdrive.item;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.ActiveShapeComponent;
import aztech.modern_industrialization.machines.components.OverdriveComponent;
import aztech.modern_industrialization.machines.components.RedstoneControlComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.util.Simulation;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOUtil;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.compat.EIIntegration;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MultiblockBuilder extends Item {
    private static final int COLOR_MISSING = DyeColor.RED.getTextColor();
    private static final int COLOR_ENOUGH = DyeColor.GREEN.getTextColor();
    private static final int COLOR_HEADER = DyeColor.YELLOW.getTextColor();

    public MultiblockBuilder(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .component(IOComponents.MULTI_BUILDER_MODE, "build"));
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!IOUtil.isRealPlayer(player) || level.isClientSide)
            return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        String mode = stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, "build");

        var be = level.getBlockEntity(pos);

        if (mode.equals("tear_down")) {
            if (be instanceof MultiblockMachineBlockEntity multiblock) {
                return handleTearDown(level, player, multiblock);
            }
            return InteractionResult.PASS;
        }

        if (mode.equals("copy_paste")) {
            var isSneaking = player.isShiftKeyDown();
            if (isSneaking) {
                if (be instanceof MultiblockMachineBlockEntity multiblock) {
                    return handleCopy(stack, level, pos, player, multiblock);
                }
            }
            return handlePaste(stack, level, isSneaking ? pos : pos.relative(ctx.getClickedFace()), player);
        }

        if (!(be instanceof MultiblockMachineBlockEntity multiblock))
            return InteractionResult.PASS;
        return handleMultiblockUse(level, pos, player, multiblock);
    }

    private InteractionResult handleCopy(ItemStack stack, Level level, BlockPos pos, Player player, MultiblockMachineBlockEntity multiblock) {
        if (!multiblock.isShapeValid()) {
            player.sendSystemMessage(IO.text().multiblockBuilderCopyRequiresFormed());
            return InteractionResult.FAIL;
        }

        ShapeMatcher shapeMatcher = multiblock.createShapeMatcher();

        Map<BlockPos, IOComponents.BlockData> template = new HashMap<>();
        // Add controller
        template.put(BlockPos.ZERO, new IOComponents.BlockData(level.getBlockState(pos).getBlock().asItem(), copySettings(multiblock)));

        for (BlockPos memberPos : shapeMatcher.getPositions()) {
            BlockState state = level.getBlockState(memberPos);
            BlockEntity be = level.getBlockEntity(memberPos);
            CompoundTag settings = null;
            if (be instanceof MachineBlockEntity machineBe) {
                settings = copySettings(machineBe);
            }
            template.put(memberPos.subtract(pos), new IOComponents.BlockData(state.getBlock().asItem(), settings != null ? settings : new CompoundTag()));
        }

        stack.set(IOComponents.MULTI_BUILDER_TEMPLATE, template);
        player.displayClientMessage(IO.text().multiblockBuilderTemplateCopied(pos.getX(), pos.getY(), pos.getZ()), true);

        return InteractionResult.SUCCESS;
    }

    private CompoundTag copySettings(MachineBlockEntity machine) {
        CompoundTag tag = new CompoundTag();
        if (machine.getLevel() == null) return tag;
        var registries = machine.getLevel().registryAccess();

        // Always copy: facingDirection, activeShape, placer
        machine.orientation.writeNbt(tag, registries);
        machine.placedBy.writeNbt(tag, registries);

        machine.components.forType(ActiveShapeComponent.class, activeShape -> activeShape.writeNbt(tag, registries));

        // Copy optional modules (only consumed if present in paste)
        machine.components.forType(UpgradeComponent.class, upgrade -> upgrade.writeNbt(tag, registries));
        machine.components.forType(RedstoneControlComponent.class, redstone -> redstone.writeNbt(tag, registries));
        machine.components.forType(OverdriveComponent.class, overdrive -> overdrive.writeNbt(tag, registries));
        machine.components.forType(MultiProcessingArrayMachineComponent.class, machines -> machines.writeNbt(tag, registries));
        if (IOUtil.isEILoaded) EIIntegration.writeSettings(machine, tag, registries);

        // Hatch settings
        if (machine instanceof HatchBlockEntity) {
            MIInventory inventory = machine.getInventory();
            ListTag itemLocks = new ListTag();
            for (ConfigurableItemStack itemStack : inventory.getItemStacks()) {
                itemLocks.add(itemStack.toNbt(registries));
            }
            tag.put("itemLocks", itemLocks);

            ListTag fluidLocks = new ListTag();
            for (ConfigurableFluidStack fluidStack : inventory.getFluidStacks()) {
                fluidLocks.add(fluidStack.toNbt(registries));
            }
            tag.put("fluidLocks", fluidLocks);
        }

        return tag;
    }

    private InteractionResult handlePaste(ItemStack stack, Level level, BlockPos pos, Player player) {
        Map<BlockPos, IOComponents.BlockData> template = stack.get(IOComponents.MULTI_BUILDER_TEMPLATE);
        if (template == null || template.isEmpty()) {
            player.displayClientMessage(IO.text().multiblockBuilderNoTemplate(), true);
            return InteractionResult.FAIL;
        }

        Rotation rotation = getPasteRotation(template.get(BlockPos.ZERO).settings(), player.getDirection().getOpposite());
        BlockPos pastePos = pastePosition(pos, template, rotation);

        Map<Item, Integer> requiredBlocks = new HashMap<>();
        for (var entry : template.entrySet()) {
            BlockPos targetPos = pastePos.offset(rotateOffset(entry.getKey(), rotation));
            BlockState state = level.getBlockState(targetPos);
            Item item = entry.getValue().item();

            if (state.getBlock().asItem().equals(item)) continue;

            if (!state.canBeReplaced()) {
                player.sendSystemMessage(IO.text().multiblockBuilderBlockCannotBeReplaced(targetPos));
                return InteractionResult.SUCCESS;
            }
            requiredBlocks.merge(item, 1, Integer::sum);
        }

        // Check for optional modules/upgrades in settings and add to required blocks if player is not in creative
        if (!player.isCreative()) {
            for (var entry : template.entrySet()) {
                CompoundTag settings = entry.getValue().settings();
                var registries = level.registryAccess();
                if (settings.contains("upgradesItemStack")) {
                    ItemStack upgrades = ItemStack.parseOptional(registries, settings.getCompound("upgradesItemStack"));
                    if (!upgrades.isEmpty()) {
                        requiredBlocks.merge(upgrades.getItem(), upgrades.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("redstoneModuleStack")) {
                    ItemStack redstone = ItemStack.parseOptional(registries, settings.getCompound("redstoneModuleStack"));
                    if (!redstone.isEmpty()) {
                        requiredBlocks.merge(redstone.getItem(), redstone.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("overdriveModuleStack")) {
                    ItemStack overdrive = ItemStack.parseOptional(registries, settings.getCompound("overdriveModuleStack"));
                    if (!overdrive.isEmpty()) {
                        requiredBlocks.merge(overdrive.getItem(), overdrive.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("machinesStack")) {
                    ItemStack machines = ItemStack.parseOptional(registries, settings.getCompound("machinesStack"));
                    if (!machines.isEmpty()) {
                        requiredBlocks.merge(machines.getItem(), machines.getCount(), Integer::sum);
                    }
                }
            }
        }

        if (player.isCreative() || checkAndConsumeInventory(player, level, requiredBlocks)) {
            for (var entry : template.entrySet()) {
                BlockPos targetPos = pastePos.offset(rotateOffset(entry.getKey(), rotation));
                Item item = entry.getValue().item();
                level.setBlock(targetPos, Block.byItem(item).defaultBlockState(), 3);

                BlockEntity be = level.getBlockEntity(targetPos);
                if (be instanceof MachineBlockEntity machine) {
                    applySettings(machine, rotateSettings(entry.getValue().settings(), rotation));
                }
            }
            player.sendSystemMessage(IO.text().multiblockBuilderPasteSuccess());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static Rotation getPasteRotation(CompoundTag settings, Direction targetDirection) {
        if (!settings.contains("facingDirection") || !targetDirection.getAxis().isHorizontal()) return Rotation.NONE;

        Direction sourceDirection = Direction.from3DDataValue(settings.getInt("facingDirection"));
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(sourceDirection) == targetDirection) return rotation;
        }
        return Rotation.NONE;
    }

    public static BlockPos rotateOffset(BlockPos offset, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180 -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }

    public static BlockPos pastePosition(BlockPos clickedPos, Map<BlockPos, IOComponents.BlockData> template, Rotation rotation) {
        int lowestTemplateY = template.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        return clickedPos.above(Math.max(0, -lowestTemplateY));
    }

    public static CompoundTag rotateSettings(CompoundTag settings, Rotation rotation) {
        if (rotation == Rotation.NONE) return settings;

        CompoundTag rotated = settings.copy();
        if (rotated.contains("facingDirection")) {
            rotated.putInt("facingDirection", rotation.rotate(Direction.from3DDataValue(rotated.getInt("facingDirection"))).get3DDataValue());
        }
        if (rotated.contains("outputDirection")) {
            rotated.putInt("outputDirection", rotation.rotate(Direction.from3DDataValue(rotated.getInt("outputDirection"))).get3DDataValue());
        }
        return rotated;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyStackSettings(AbstractConfigurableStack target, AbstractConfigurableStack source) {
        // We only want to apply configuration, not the content (amount/key)
        // Unless it's locked, in which case the lock needs to be preserved.
        if (source.isPlayerLocked()) target.playerLock(source.getLockedInstance(), Simulation.ACT);

        if (target instanceof ConfigurableItemStack targetItem && source instanceof ConfigurableItemStack sourceItem) {
            int targetCap = targetItem.getAdjustedCapacity();
            int sourceCap = sourceItem.getAdjustedCapacity();
            while (targetCap < sourceCap) {
                targetItem.adjustCapacity(true, false);
                targetCap++;
            }
            while (targetCap > sourceCap) {
                targetItem.adjustCapacity(false, false);
                targetCap--;
            }
        }
    }

    private void applySettings(MachineBlockEntity machine, CompoundTag settings) {
        if (machine.getLevel() == null) return;
        var registries = machine.getLevel().registryAccess();

        // 1. Mandatory settings (orientation, shape, placer)
        machine.orientation.readNbt(settings, registries, false);
        machine.placedBy.readNbt(settings, registries, false);
        machine.components.forType(ActiveShapeComponent.class, activeShape -> activeShape.readNbt(settings, registries, false));

        // 2. Optional settings (upgrades, modules) - We already consumed them if survival, so we can just set them.
        // Actually, MI's readNbt for these components might handle things differently, but let's use the provided settings.
        machine.components.forType(UpgradeComponent.class, upgrade -> upgrade.readNbt(settings, registries, false));
        machine.components.forType(RedstoneControlComponent.class, redstone -> redstone.readNbt(settings, registries, false));
        machine.components.forType(OverdriveComponent.class, overdrive -> overdrive.readNbt(settings, registries, false));
        machine.components.forType(MultiProcessingArrayMachineComponent.class, machines -> machines.readNbt(settings, registries, false));
        if (IOUtil.isEILoaded) EIIntegration.readSettings(machine, settings, registries);

        // 3. Hatch locks
        if (machine instanceof HatchBlockEntity) {
            MIInventory inventory = machine.getInventory();
            if (settings.contains("itemLocks", Tag.TAG_LIST)) {
                ListTag itemLocks = settings.getList("itemLocks", Tag.TAG_COMPOUND);
                List<ConfigurableItemStack> stacks = inventory.getItemStacks();
                for (int i = 0; i < Math.min(itemLocks.size(), stacks.size()); i++) {
                    CompoundTag lockTag = itemLocks.getCompound(i);
                    ConfigurableItemStack copiedStack = new ConfigurableItemStack(lockTag, registries);
                    applyStackSettings(stacks.get(i), copiedStack);
                }
            }
            if (settings.contains("fluidLocks", Tag.TAG_LIST)) {
                ListTag fluidLocks = settings.getList("fluidLocks", Tag.TAG_COMPOUND);
                List<ConfigurableFluidStack> stacks = inventory.getFluidStacks();
                for (int i = 0; i < Math.min(fluidLocks.size(), stacks.size()); i++) {
                    CompoundTag lockTag = fluidLocks.getCompound(i);
                    ConfigurableFluidStack copiedStack = new ConfigurableFluidStack(lockTag, registries);
                    applyStackSettings(stacks.get(i), copiedStack);
                }
            }
        }

        machine.setChanged();
        machine.sync();
    }

    private InteractionResult handleTearDown(Level level, Player player, MultiblockMachineBlockEntity multiblock) {
        ShapeMatcher shapeMatcher = multiblock.createShapeMatcher();
        BlockPos pos = multiblock.getBlockPos();

        for (BlockPos memberPos : shapeMatcher.getPositions()) {
            if (isAlreadyValid(shapeMatcher, level, memberPos)) {
                tearDownBlock(level, player, memberPos);
            }
        }

        // Tear down controller
        tearDownBlock(level, player, pos);

        player.displayClientMessage(IO.text().multiblockBuilderDismantled(), true);
        return InteractionResult.SUCCESS;
    }

    private void tearDownBlock(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, (net.minecraft.server.level.ServerLevel) level, pos, be);
        if (be instanceof MachineBlockEntity machine) {
            machine.getInventory().getItemStacks().forEach(stack -> drops.add(stack.getResource().toStack((int) stack.getAmount())));
            drops.addAll(machine.dropExtra());
            level.removeBlockEntity(pos);
        }
        if (!player.isCreative()) {
            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }
        level.removeBlock(pos, false);
    }

    private InteractionResult handleMultiblockUse(Level level, BlockPos pos, Player player, MultiblockMachineBlockEntity multiblock) {
        ShapeMatcher shapeMatcher = multiblock.createShapeMatcher();
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
            BlockState previewState = member.getPreviewState();
            level.setBlock(memberPos, previewState, 3);
        }
        player.sendSystemMessage(IO.text().multiblockBuilderBuildSuccess(pos.getX(), pos.getY(), pos.getZ()));
    }

    private InteractionResult handleSurvivalBuild(Level level, BlockPos pos, Player player, ShapeMatcher shapeMatcher) {
        Map<Item, Integer> requiredBlocks = new HashMap<>();
        boolean obstructionFound = false;
        for (BlockPos checkPos : shapeMatcher.getPositions()) {
            if (isAlreadyValid(shapeMatcher, level, checkPos)) continue;
            BlockState state = level.getBlockState(checkPos);
            var member = shapeMatcher.getSimpleMember(checkPos);
            BlockState previewState = member.getPreviewState();
            if (!state.canBeReplaced()) {
                obstructionFound = true;
                player.sendSystemMessage(IO.text().multiblockBuilderBlockNotPart(checkPos));
            }
            if (state.canBeReplaced()) {
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
        Map<Item, Integer> meProvided = IOUtil.isAE2Loaded ? AE2Integration.getMissingFromME(player, level, inventoryCount, requiredBlocks) : new HashMap<>();

        boolean anyMissing = false;
        for (var entry : requiredBlocks.entrySet()) {
            Item item = entry.getKey();
            int required = entry.getValue();
            int totalAvailable = inventoryCount.getOrDefault(item, 0) + meProvided.getOrDefault(item, 0);

            if (totalAvailable < required) {
                anyMissing = true;
                break;
            }
        }

        if (anyMissing) {
            player.sendSystemMessage(IO.text().multiblockBuilderRequiredItems());
            for (var entry : requiredBlocks.entrySet()) {
                Item item = entry.getKey();
                int required = entry.getValue();
                int totalAvailable = inventoryCount.getOrDefault(item, 0) + meProvided.getOrDefault(item, 0);

                if (totalAvailable < required) {
                    player.sendSystemMessage(formatItemLine(item, required - totalAvailable, COLOR_MISSING));
                } else {
                    player.sendSystemMessage(formatItemLine(item, required, COLOR_ENOUGH));
                }
            }
            return false;
        }

        if (IOUtil.isAE2Loaded && !meProvided.isEmpty()) AE2Integration.extractFromME(player, level, meProvided);
        consumeFromInventory(player, requiredBlocks, meProvided);
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

    private static void consumeFromInventory(Player player, Map<Item, Integer> requiredBlocks, Map<Item, Integer> meProvided) {
        for (var entry : requiredBlocks.entrySet()) {
            Item item = entry.getKey();
            int totalRequired = entry.getValue();
            int fromMe = meProvided.getOrDefault(item, 0);
            int remainingToConsume = totalRequired - fromMe;

            if (remainingToConsume <= 0) continue;

            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem().equals(item)) {
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
        return IO.text().multiblockBuilderRequiredItem(count, name).withColor(color);
    }
}

