package dev.wp.industrialization_overdrive.item;

import aztech.modern_industrialization.MIComponents;
import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.items.DynamicToolItem;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Vajra extends Item implements DynamicToolItem, ISimpleEnergyItem {
    private final long maxEnergy = CableTier.HV.getEu() * 10000;
    private final long energyUsagePerBlock = maxEnergy / 6000;

    public enum Speed {
        SLOW, NORMAL, FAST, INSTANT
    }

    private static final ThreadLocal<List<ItemStack>> TOTAL_DROPS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RECURSIVE_MINE_BLOCK = ThreadLocal.withInitial(() -> false);

    static {
        NeoForge.EVENT_BUS.addListener(Vajra::pickupDrops);
        NeoForge.EVENT_BUS.addListener(Vajra::pickupSpawnedDrop);
    }

    public Vajra(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .component(IOComponents.HIDE_BAR, false)
                .component(MIComponents.SILK_TOUCH, false)
                .component(IOComponents.VAJRA_SPEED, Speed.NORMAL)
                .component(MIComponents.ENERGY, 0L)
        );
    }

    private static boolean isSilkTouch(ItemStack stack) {
        return stack.getOrDefault(MIComponents.SILK_TOUCH, false);
    }

    private static void setSilkTouch(ItemStack stack, boolean silkTouch) {
        stack.set(MIComponents.SILK_TOUCH, silkTouch);
    }

    private static void pickupDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer) || !(event.getTool().getItem() instanceof Vajra)) return;

        List<ItemStack> totalDrops = TOTAL_DROPS.get();
        if (totalDrops == null) return;

        for (ItemEntity entity : event.getDrops()) {
            if (entity.getItem().isEmpty()) continue;
            boolean merged = false;
            for (ItemStack drop : totalDrops) {
                if (ItemStack.isSameItemSameComponents(entity.getItem(), drop)) {
                    drop.grow(entity.getItem().getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) totalDrops.add(entity.getItem());
        }
        event.getDrops().clear();
    }

    private static void pickupSpawnedDrop(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity drop)) return;

        List<ItemStack> totalDrops = TOTAL_DROPS.get();
        if (totalDrops != null) {
            totalDrops.add(drop.getItem().copy());
            event.setCanceled(true);
        }
    }

    private static boolean destroyWithPickup(ServerPlayer player, Level level, BlockPos pos) {
        TOTAL_DROPS.set(new ArrayList<>());
        RECURSIVE_MINE_BLOCK.set(true);
        boolean destroyed;
        try {
            destroyed = player.gameMode.destroyBlock(pos);
            if (!player.isCreative()) {
                TOTAL_DROPS.get().forEach(drop -> {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop);
                    itemEntity.setNoPickUpDelay();
                    NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, itemEntity));
                    if (!itemEntity.isRemoved()) ItemHandlerHelper.giveItemToPlayer(player, itemEntity.getItem());
                });
            }
        } finally {
            RECURSIVE_MINE_BLOCK.set(false);
            TOTAL_DROPS.remove();
        }
        return destroyed;
    }

    public static Speed getToolSpeed(ItemStack stack) {
        return stack.getOrDefault(IOComponents.VAJRA_SPEED, Speed.NORMAL);
    }

    public static void setToolSpeed(ItemStack stack, Speed speed) {
        stack.set(IOComponents.VAJRA_SPEED, speed);
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !newStack.is(this) || slotChanged;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !newStack.is(this) || !canUse(newStack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!(miner instanceof ServerPlayer player) || RECURSIVE_MINE_BLOCK.get()) return false;
        if (!canUse(stack)) return false;

        useEnergy(stack, energyUsagePerBlock);
        destroyWithPickup(player, world, pos);
        return true;
    }

    private void useEnergy(ItemStack stack, long amount) {
        var energy = stack.getOrDefault(MIComponents.ENERGY, 0L);
        stack.set(MIComponents.ENERGY, Math.max(0, energy - amount));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {
            var stack = player.getItemInHand(hand);

            setSilkTouch(stack, !isSilkTouch(stack));
            if (level.isClientSide) {
                player.displayClientMessage(IO.text().vajraSilkTouchInfo(isSilkTouch(stack) ? IO.text().enabled() : IO.text().disabled()), true);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        var pos = ctx.getClickedPos();
        var level = ctx.getLevel();
        var blockState = level.getBlockState(pos);
        var stack = ctx.getItemInHand();

        // Don't break unbreakable blocks
        if (blockState.getDestroySpeed(level, pos) < 0) return super.useOn(ctx);

        Player player = ctx.getPlayer();
        if (player == null) return super.useOn(ctx);

        if (!canUse(stack)) return super.useOn(ctx);

        if (level.isClientSide) {
            if (!level.isEmptyBlock(pos)) player.swing(InteractionHand.MAIN_HAND);
        } else if (player instanceof ServerPlayer serverPlayer) {
            useEnergy(stack, energyUsagePerBlock);
            if (destroyWithPickup(serverPlayer, level, pos)) {
                level.levelEvent(2001, pos, Block.getId(blockState));
            }
        }


        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isSilkTouch(stack);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var map = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        if (isSilkTouch(stack)) lookup.get(Enchantments.SILK_TOUCH).ifPresent(h -> map.set(h, 1));
        return map.toImmutable();
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return canUse(stack);
    }

    private boolean canUse(ItemStack stack) {
        var energy = stack.getOrDefault(MIComponents.ENERGY, 0L);
        return energy >= energyUsagePerBlock;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canUse(stack)) return 0;

        var baseSpeed = state.getDestroySpeed(null, null);
        if (baseSpeed <= 0) return super.getDestroySpeed(stack, state);

        switch (getToolSpeed(stack)) {
            case SLOW -> baseSpeed *= 4f;
            case NORMAL -> baseSpeed *= 7.5f;
            case FAST -> baseSpeed *= 29f;
            case INSTANT -> baseSpeed = Float.MAX_VALUE;
        }
        return baseSpeed;
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return maxEnergy;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return maxEnergy;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return maxEnergy;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return !stack.getOrDefault(IOComponents.HIDE_BAR, false);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return (int) Math.round(this.getStoredEnergy(stack) / (double) this.getEnergyCapacity(stack) * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0xFF0000;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext ctx, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        Component speed = null;
        switch (getToolSpeed(stack)) {
            case SLOW -> speed = IO.text().vajraSpeedSlow();
            case NORMAL -> speed = IO.text().vajraSpeedNormal();
            case FAST -> speed = IO.text().vajraSpeedFast();
            case INSTANT -> speed = IO.text().vajraSpeedInstant();
        }

        tooltip.add(IO.text().vajraSpeedInfo(speed));
        tooltip.add(IO.text().vajraSilkTouchInfo(isSilkTouch(stack) ? IO.text().enabled() : IO.text().disabled()));
        tooltip.add(IO.text().energyInfo(stack.getOrDefault(MIComponents.ENERGY, 0L), getEnergyCapacity(stack)));
    }
}
