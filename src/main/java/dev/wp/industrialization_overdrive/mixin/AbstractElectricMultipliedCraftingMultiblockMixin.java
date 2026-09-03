package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.item.UpgradeHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractMultipliedCraftingMultiblockBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractElectricMultipliedCraftingMultiblockBlockEntity.class, remap = false)
public abstract class AbstractElectricMultipliedCraftingMultiblockMixin {

    @Final @Shadow protected UpgradeComponent upgrades;

    @Inject(method = "getMaxRecipeEuBonus", at = @At("HEAD"), cancellable = true)
    private void io_batchUpgradeEuBonus(CallbackInfoReturnable<Long> cir) {
        ItemStack slot = upgrades.getDrop();
        if (!(slot.getItem() instanceof UpgradeHolder)) return;
        IOComponents.UpgradeHolderContents contents = slot.get(IOComponents.UPGRADE_HOLDER_CONTENTS.get());
        if (contents == null || contents.count() <= 0) {
            cir.setReturnValue(0L);
            return;
        }
        int batchCount = Math.max(1, ((AbstractMultipliedCraftingMultiblockBlockEntity) (Object) this).getMaxMultiplier());
        long effective = Math.min(contents.count(), 64L * batchCount);
        cir.setReturnValue(effective * UpgradeComponent.getExtraEu(contents.upgradeType()));
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void io_batchUpgradeInsertion(Player player, InteractionHand hand, Direction face,
                                          CallbackInfoReturnable<ItemInteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof UpgradeHolder)) return;
        MachineBlockEntity be = (MachineBlockEntity) (Object) this;
        Level level = be.getLevel();
        if (!upgrades.getDrop().isEmpty()) {
            cir.setReturnValue(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
            return;
        }
        if (level != null && !level.isClientSide()) {
            upgrades.setStackServer(be, stack.copyWithCount(1));
            if (!player.isCreative()) stack.shrink(1);
        }
        cir.setReturnValue(ItemInteractionResult.sidedSuccess(level != null && level.isClientSide()));
    }
}
