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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Vajra extends Item implements DynamicToolItem, ISimpleEnergyItem {
    private final long energyUsagePerBlock = CableTier.MV.getEu() * 5;
    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 4;

    public Vajra(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .component(IOComponents.VAJRA_SPEED, 2)
                .component(IOComponents.SILK_TOUCH, false)
                .component(MIComponents.ENERGY, 0L)
        );
    }

    private static boolean isSilkTouch(ItemStack stack) {
        return stack.getOrDefault(IOComponents.SILK_TOUCH, false);
    }

    private static void setSilkTouch(ItemStack stack, boolean silkTouch) {
        stack.set(IOComponents.SILK_TOUCH, silkTouch);
    }

    public static int getToolSpeed(ItemStack stack) {
        return stack.getOrDefault(IOComponents.VAJRA_SPEED, 2);
    }

    public static void setToolSpeed(ItemStack stack, int speed) {
        speed = Math.clamp(speed, MIN_SPEED, MAX_SPEED);
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
        if (canUse(stack)) {
            useEnergy(stack, energyUsagePerBlock);
            return true;
        }
        return false;
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
        var block = blockState.getBlock();
        var blockEntity = level.getBlockEntity(pos);
        var stack = ctx.getItemInHand();

        // Don't break unbreakable blocks
        if (blockState.getDestroySpeed(level, pos) < 0) return super.useOn(ctx);

        var player = ctx.getPlayer();
        if (player == null) return super.useOn(ctx);

        var energy = stack.getOrDefault(MIComponents.ENERGY, 0L);
        if (energy < energyUsagePerBlock) return super.useOn(ctx);

        if (level.isClientSide && !level.isEmptyBlock(pos)) {
            player.swing(InteractionHand.MAIN_HAND);
        } else {
            block.playerWillDestroy(level, pos, blockState, player);

            if (level.removeBlock(pos, false)) {
                block.destroy(level, pos, blockState);

                block.playerDestroy(level, player, pos, blockState, blockEntity, stack);
                player.level().playSound(null, pos, block.getSoundType(blockState, level, pos, player).getBreakSound(), SoundSource.PLAYERS, 1.0f, 1.0f);
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
        if (state.getDestroySpeed(null, null) < 0) return super.getDestroySpeed(stack, state);

        var baseSpeed = state.getDestroySpeed(null, null);

        switch (getToolSpeed(stack)) {
            case 1 -> {
                return baseSpeed * 4f;
            }
            case 2 -> {
                return baseSpeed * 7.5f;
            }
            case 3 -> {
                return baseSpeed * 29;
            }
            case 4 -> {
                return baseSpeed = Float.MAX_VALUE;
            }
        }

        return baseSpeed;
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return CableTier.HV.getEu() * 100;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return CableTier.HV.getEu();
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
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
            case 1 -> speed = IO.text().vajraSpeedSlow();
            case 2 -> speed = IO.text().vajraSpeedNormal();
            case 3 -> speed = IO.text().vajraSpeedFast();
            case 4 -> speed = IO.text().vajraSpeedInstant();
        }

        tooltip.add(IO.text().vajraSpeedInfo(speed));
        tooltip.add(IO.text().vajraSilkTouchInfo(isSilkTouch(stack) ? IO.text().enabled() : IO.text().disabled()));
        tooltip.add(IO.text().energyInfo(stack.getOrDefault(MIComponents.ENERGY, 0L), getEnergyCapacity(stack)));
    }
}



