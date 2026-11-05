package dev.wp.industrialization_overdrive.item;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.ActiveShapeComponent;
import aztech.modern_industrialization.machines.components.CasingComponent;
import aztech.modern_industrialization.machines.components.OverdriveComponent;
import aztech.modern_industrialization.machines.components.RedstoneControlComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.util.Simulation;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOItems;
import dev.wp.industrialization_overdrive.IOUtil;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.compat.EIIntegration;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
    public enum Mode {
        BUILD,
        COPY_PASTE,
        TEAR_DOWN
    }

    private static final int COLOR_MISSING = DyeColor.RED.getTextColor();
    private static final int COLOR_ENOUGH = DyeColor.GREEN.getTextColor();

    private static final String ME_HATCH_CONFIG_KEY = "meHatchConfig";
    private boolean isSilkTouch = false;

    public MultiblockBuilder(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .component(IOComponents.MULTI_BUILDER_MODE, Mode.BUILD));
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
        Mode mode = stack.getOrDefault(IOComponents.MULTI_BUILDER_MODE, Mode.BUILD);

        var be = level.getBlockEntity(pos);

        if (mode == Mode.TEAR_DOWN) {
            if (be instanceof MultiblockMachineBlockEntity multiblock) {
                return handleTearDown(level, player, multiblock);
            }
            if (IOUtil.isEILoaded && EIIntegration.isMachineChainer(be)) {
                return handleChainerTearDown(level, player, pos);
            }
            return InteractionResult.PASS;
        }

        if (mode == Mode.COPY_PASTE) {
            var isSneaking = player.isShiftKeyDown();
            if (isSneaking) {
                if (be instanceof MultiblockMachineBlockEntity multiblock) {
                    return handleCopy(stack, level, pos, player, multiblock);
                }
                if (IOUtil.isEILoaded && IO.config().machineChainerCopyDepth() != 0 && EIIntegration.isMachineChainer(be)) {
                    return handleChainerCopy(stack, level, pos, player);
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

        if (IOUtil.isEILoaded && IO.config().machineChainerCopyDepth() != 0) {
            EIIntegration.copyChainedBlocks(level, pos, template, IO.config().machineChainerCopyDepth(), this::copySettings);
        }

        stack.set(IOComponents.MULTI_BUILDER_TEMPLATE, IOComponents.Template.from(template));
        player.displayClientMessage(IO.text().multiblockBuilderTemplateCopied(pos.getX(), pos.getY(), pos.getZ()), true);

        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleChainerCopy(ItemStack stack, Level level, BlockPos pos, Player player) {
        Map<BlockPos, IOComponents.BlockData> template = new HashMap<>();
        EIIntegration.copyChainer(level, pos, template, IO.config().machineChainerCopyDepth(), this::copySettings);
        IOComponents.BlockData root = template.get(BlockPos.ZERO);
        CompoundTag rootSettings = root.settings().copy();
        rootSettings.putInt("chainerCopyFacingDirection", player.getDirection().getOpposite().get3DDataValue());
        template.put(BlockPos.ZERO, new IOComponents.BlockData(root.item(), rootSettings));
        stack.set(IOComponents.MULTI_BUILDER_TEMPLATE, IOComponents.Template.from(template));
        player.displayClientMessage(IO.text().multiblockBuilderTemplateCopied(pos.getX(), pos.getY(), pos.getZ()), true);
        return InteractionResult.SUCCESS;
    }

    private CompoundTag copySettings(MachineBlockEntity machine) {
        CompoundTag tag = new CompoundTag();
        if (machine.getLevel() == null) return tag;
        var registries = machine.getLevel().registryAccess();

        machine.orientation.writeNbt(tag, registries);

        machine.components.forType(ActiveShapeComponent.class, activeShape -> {
            if (activeShape.getActiveShapeIndex() != 0) activeShape.writeNbt(tag, registries);
        });

        machine.components.forType(UpgradeComponent.class, upgrade -> upgrade.writeNbt(tag, registries));
        machine.components.forType(RedstoneControlComponent.class, redstone -> redstone.writeNbt(tag, registries));
        machine.components.forType(OverdriveComponent.class, overdrive -> overdrive.writeNbt(tag, registries));
        machine.components.forType(MultiProcessingArrayMachineComponent.class, machines -> machines.writeNbt(tag, registries));
        machine.components.forType(CasingComponent.class, casing -> {
            CompoundTag casingTag = new CompoundTag();
            casing.writeNbt(casingTag, registries);
            if (!casingTag.getCompound("casing").isEmpty()) tag.put("casing", casingTag.get("casing"));
        });
        if (IOUtil.isEILoaded) EIIntegration.writeSettings(machine, tag, registries);
        for (String key : List.of("redstoneModuleStack", "upgradesItemStack", "overdriveModuleStack")) {
            if (tag.getCompound(key).isEmpty()) tag.remove(key);
        }

        if (IOUtil.isAE2Loaded && machine instanceof MEHatchBlockEntity hatch) {
            // An ME hatch's slot locks are derived from its channel configuration, so only that is copied.
            tag.put(ME_HATCH_CONFIG_KEY, hatch.saveConfiguration(registries));
            return tag;
        }

        MIInventory inventory = machine.getInventory();
        ListTag itemLocks = new ListTag();
        boolean hasItemLocks = false;
        for (ConfigurableItemStack itemStack : inventory.getItemStacks()) {
            boolean configured = itemStack.isPlayerLocked() || itemStack.getAdjustedCapacity() != 1;
            itemLocks.add(configured ? itemStack.toNbt(registries) : new CompoundTag());
            hasItemLocks |= configured;
        }
        if (hasItemLocks) tag.put("itemLocks", itemLocks);

        ListTag fluidLocks = new ListTag();
        boolean hasFluidLocks = false;
        for (ConfigurableFluidStack fluidStack : inventory.getFluidStacks()) {
            boolean configured = fluidStack.isPlayerLocked();
            fluidLocks.add(configured ? fluidStack.toNbt(registries) : new CompoundTag());
            hasFluidLocks |= configured;
        }
        if (hasFluidLocks) tag.put("fluidLocks", fluidLocks);

        return tag;
    }

    private InteractionResult handlePaste(ItemStack stack, Level level, BlockPos pos, Player player) {
        IOComponents.Template storedTemplate = stack.get(IOComponents.MULTI_BUILDER_TEMPLATE);
        if (storedTemplate == null || storedTemplate.blocks().isEmpty()) {
            player.displayClientMessage(IO.text().multiblockBuilderNoTemplate(), true);
            return InteractionResult.FAIL;
        }
        Map<BlockPos, IOComponents.BlockData> template = storedTemplate.expand();

        Rotation rotation = getPasteRotation(template.get(BlockPos.ZERO).settings(), player.getDirection().getOpposite());
        BlockPos pastePos = pastePosition(pos, template);

        Map<ItemStack, Integer> requiredBlocks = stackCounts();
        for (var entry : template.entrySet()) {
            BlockPos targetPos = pastePos.offset(rotateOffset(entry.getKey(), rotation));
            BlockState state = level.getBlockState(targetPos);
            Item item = entry.getValue().item();

            if (state.getBlock().asItem().equals(item)) continue;

            if (!state.canBeReplaced()) {
                player.sendSystemMessage(IO.text().multiblockBuilderBlockCannotBeReplaced(targetPos));
                return InteractionResult.SUCCESS;
            }
            requiredBlocks.merge(new ItemStack(item), 1, Integer::sum);
        }

        if (!player.isCreative()) {
            for (var entry : template.entrySet()) {
                CompoundTag settings = entry.getValue().settings();
                var registries = level.registryAccess();
                if (settings.contains("upgradesItemStack")) {
                    ItemStack upgrades = ItemStack.parseOptional(registries, settings.getCompound("upgradesItemStack"));
                    if (!upgrades.isEmpty()) {
                        requiredBlocks.merge(upgrades.copyWithCount(1), upgrades.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("redstoneModuleStack")) {
                    ItemStack redstone = ItemStack.parseOptional(registries, settings.getCompound("redstoneModuleStack"));
                    if (!redstone.isEmpty()) {
                        requiredBlocks.merge(redstone.copyWithCount(1), redstone.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("overdriveModuleStack")) {
                    ItemStack overdrive = ItemStack.parseOptional(registries, settings.getCompound("overdriveModuleStack"));
                    if (!overdrive.isEmpty()) {
                        requiredBlocks.merge(overdrive.copyWithCount(1), overdrive.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("machinesStack")) {
                    ItemStack machines = ItemStack.parseOptional(registries, settings.getCompound("machinesStack"));
                    if (!machines.isEmpty()) {
                        requiredBlocks.merge(machines.copyWithCount(1), machines.getCount(), Integer::sum);
                    }
                }
                if (settings.contains("casing")) {
                    ItemStack casing = ItemStack.parseOptional(registries, settings.getCompound("casing"));
                    if (!casing.isEmpty()) {
                        requiredBlocks.merge(casing.copyWithCount(1), casing.getCount(), Integer::sum);
                    }
                }
            }
        }

        if (!player.isCreative()) topUpUpgradeStackers(player, level, requiredBlocks);
        if (player.isCreative() || checkAndConsumeInventory(player, level, requiredBlocks)) {
            for (var entry : template.entrySet()) {
                BlockPos targetPos = pastePos.offset(rotateOffset(entry.getKey(), rotation));
                Item item = entry.getValue().item();
                level.setBlock(targetPos, Block.byItem(item).defaultBlockState(), 3);

                BlockEntity be = level.getBlockEntity(targetPos);
                if (be instanceof MachineBlockEntity machine) {
                    applySettings(machine, rotateSettings(entry.getValue().settings(), rotation), player);
                }
            }
            player.sendSystemMessage(IO.text().multiblockBuilderPasteSuccess());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static Rotation getPasteRotation(CompoundTag settings, Direction targetDirection) {
        if (!targetDirection.getAxis().isHorizontal()) return Rotation.NONE;

        if (!settings.contains("chainerCopyFacingDirection") && !settings.contains("facingDirection")) return Rotation.NONE;
        Direction sourceDirection = Direction.from3DDataValue(settings.getInt(
                settings.contains("chainerCopyFacingDirection") ? "chainerCopyFacingDirection" : "facingDirection"));
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

    /** Lifts the paste so the template's lowest block lands on the clicked position; rotation about Y leaves heights alone. */
    public static BlockPos pastePosition(BlockPos clickedPos, Map<BlockPos, IOComponents.BlockData> template) {
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
        // Apply configuration only, not content (amount/key), except a lock, which must be preserved.
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

    private void applySettings(MachineBlockEntity machine, CompoundTag settings, Player player) {
        if (machine.getLevel() == null) return;
        var registries = machine.getLevel().registryAccess();

        machine.orientation.readNbt(settings, registries, false);
        machine.placedBy.onPlaced(player);
        machine.components.forType(ActiveShapeComponent.class, activeShape -> activeShape.readNbt(settings, registries, false));

        // Upgrades and modules were already consumed in survival, so just set them.
        machine.components.forType(UpgradeComponent.class, upgrade -> upgrade.readNbt(settings, registries, false));
        machine.components.forType(RedstoneControlComponent.class, redstone -> redstone.readNbt(settings, registries, false));
        machine.components.forType(OverdriveComponent.class, overdrive -> overdrive.readNbt(settings, registries, false));
        machine.components.forType(MultiProcessingArrayMachineComponent.class, machines -> machines.readNbt(settings, registries, false));
        machine.components.forType(CasingComponent.class, casing -> casing.readNbt(settings, registries, false));
        if (IOUtil.isEILoaded) EIIntegration.readSettings(machine, settings, registries);

        if (IOUtil.isAE2Loaded && machine instanceof MEHatchBlockEntity hatch) {
            hatch.syncConnectionSide();
            if (settings.contains(ME_HATCH_CONFIG_KEY, Tag.TAG_COMPOUND)) {
                hatch.loadConfiguration(settings.getCompound(ME_HATCH_CONFIG_KEY), registries, false);
            }
            machine.setChanged();
            machine.sync();
            return;
        }

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

        tearDownBlock(level, player, pos);

        player.displayClientMessage(IO.text().multiblockBuilderDismantled(), true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleChainerTearDown(Level level, Player player, BlockPos pos) {
        for (BlockPos chainedPos : EIIntegration.getChainedPositions(level, pos)) {
            tearDownBlock(level, player, chainedPos);
        }
        player.displayClientMessage(IO.text().multiblockBuilderDismantled(), true);
        return InteractionResult.SUCCESS;
    }

    private void tearDownBlock(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        isSilkTouch = true; // silk touch drops while tearing down
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, be, player, player.getMainHandItem());
        isSilkTouch = false;
        if (be instanceof MachineBlockEntity machine) {
            drops.addAll(machine.dropExtra());
            machine.getInventory().getItemStacks().forEach(stack -> drops.add(stack.getResource().toStack((int) stack.getAmount())));
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
        Map<ItemStack, Integer> requiredBlocks = stackCounts();
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
                requiredBlocks.merge(new ItemStack(block), 1, Integer::sum);
            }
        }
        if (obstructionFound) return InteractionResult.SUCCESS;
        if (!checkAndConsumeInventory(player, level, requiredBlocks)) {
            return InteractionResult.FAIL;
        }
        buildMultiblock(level, pos, player, shapeMatcher);
        return InteractionResult.SUCCESS;
    }

    /** Keyed by item and components, so a filled Upgrade Stacker is only paid for by an identical stack. */
    public static Map<ItemStack, Integer> stackCounts() {
        return new Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
    }

    /** Pays for a copied filled Upgrade Stacker with one the player has (identical first, then fullest) plus loose upgrades; one holding more than the copy is never used, as installing would lose its surplus. */
    private static void topUpUpgradeStackers(Player player, Level level, Map<ItemStack, Integer> requiredBlocks) {
        Item stackerItem = IOItems.UPGRADE_STACKER.get();
        var contentsType = IOComponents.UPGRADE_STACKER_CONTENTS.get();
        Map<ItemStack, Integer> available = stackCounts();
        countInventory(player).forEach((stack, count) -> {
            if (stack.is(stackerItem)) available.merge(stack, count, Integer::sum);
        });
        if (IOUtil.isAE2Loaded) {
            AE2Integration.countInME(player, level, stackerItem).forEach((stack, count) -> available.merge(stack, count, Integer::sum));
        }

        for (ItemStack wanted : List.copyOf(requiredBlocks.keySet())) {
            IOComponents.UpgradeStackerContents copied = wanted.get(contentsType);
            if (!wanted.is(stackerItem) || copied == null) continue;
            int needed = requiredBlocks.get(wanted);
            int identical = Math.min(needed, available.getOrDefault(wanted, 0));
            if (identical == needed) continue;
            if (identical > 0) requiredBlocks.put(wanted, identical);
            else requiredBlocks.remove(wanted);

            for (int missing = needed - identical; missing > 0; missing--) {
                ItemStack best = null;
                int bestCount = -1;
                for (var entry : available.entrySet()) {
                    IOComponents.UpgradeStackerContents held = entry.getKey().get(contentsType);
                    int heldCount = held == null ? 0 : held.count();
                    boolean usable = heldCount == 0 || (held.upgradeType() == copied.upgradeType() && heldCount < copied.count());
                    boolean free = entry.getValue() > requiredBlocks.getOrDefault(entry.getKey(), 0);
                    if (usable && free && heldCount > bestCount) {
                        best = entry.getKey();
                        bestCount = heldCount;
                    }
                }
                if (best == null) {
                    // Nothing to top up: ask for the copied stacker itself, so the missing-items list names it.
                    requiredBlocks.merge(wanted, missing, Integer::sum);
                    break;
                }
                requiredBlocks.merge(best, 1, Integer::sum);
                requiredBlocks.merge(new ItemStack(copied.upgradeType()), copied.count() - bestCount, Integer::sum);
            }
        }
    }

    private boolean checkAndConsumeInventory(Player player, Level level, Map<ItemStack, Integer> requiredBlocks) {
        Map<ItemStack, Integer> inventoryCount = countInventory(player);
        Map<ItemStack, Integer> meProvided = IOUtil.isAE2Loaded ? AE2Integration.getMissingFromME(player, level, inventoryCount, requiredBlocks) : stackCounts();

        boolean anyMissing = false;
        for (var entry : requiredBlocks.entrySet()) {
            ItemStack item = entry.getKey();
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
                ItemStack item = entry.getKey();
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

    private static Map<ItemStack, Integer> countInventory(Player player) {
        Map<ItemStack, Integer> inventoryCount = stackCounts();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) inventoryCount.merge(stack.copyWithCount(1), stack.getCount(), Integer::sum);
        }
        return inventoryCount;
    }

    private static void consumeFromInventory(Player player, Map<ItemStack, Integer> requiredBlocks, Map<ItemStack, Integer> meProvided) {
        for (var entry : requiredBlocks.entrySet()) {
            ItemStack item = entry.getKey();
            int totalRequired = entry.getValue();
            int fromMe = meProvided.getOrDefault(item, 0);
            int remainingToConsume = totalRequired - fromMe;

            if (remainingToConsume <= 0) continue;

            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItemSameComponents(stack, item)) {
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

    private static Component formatItemLine(ItemStack item, int count, int color) {
        String name = item.getHoverName().getString();
        return IO.text().multiblockBuilderRequiredItem(count, name).withColor(color);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var map = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        if (isSilkTouch) lookup.get(Enchantments.SILK_TOUCH).ifPresent(h -> map.set(h, 1));
        return map.toImmutable();
    }
}

