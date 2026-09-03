package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.machines.guicomponents.SlotPanel;
import dev.wp.industrialization_overdrive.item.UpgradeHolder;
import net.minecraft.world.item.ItemStack;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "aztech.modern_industrialization.machines.guicomponents.SlotPanel$1", remap = false)
public abstract class SlotPanelUpgradeHolderMixin {
    @Shadow @Final SlotPanel this$0;
    @Shadow @Final SlotPanel.SlotType val$type;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void io_allowUpgradeHolder(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (val$type == SlotPanel.SlotType.UPGRADES
                && ((SlotPanelAccessor) this$0).io_getMachine() instanceof AbstractElectricMultipliedCraftingMultiblockBlockEntity
                && stack.getItem() instanceof UpgradeHolder) {
            cir.setReturnValue(true);
        }
    }
}
