package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.inventory.ConfigurableScreenHandler;
import dev.wp.industrialization_overdrive.machines.guicomponents.mehatch.MEHatchConfigSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes every click on an ME hatch config cell to channel configuration; the cell holds no real item, so no vanilla item movement (dupe risk). */
@Mixin(ConfigurableScreenHandler.class)
public abstract class ConfigurableScreenHandlerMEHatchMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void io_handleMEHatchConfigClick(int slotIndex, int button, ClickType actionType,
            Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);
        if (!(slot instanceof MEHatchConfigSlot configSlot)) {
            return;
        }

        // Cancel unconditionally: every click type must be inert as an inventory operation.
        ci.cancel();

        // Only the server copy owns the hatch.
        if (!configSlot.isServerSide()) {
            return;
        }

        if (button == 1 && actionType == ClickType.PICKUP) {
            configSlot.clearConfiguration();
        } else if (button == 0 && actionType == ClickType.PICKUP) {
            configSlot.assignFromCarried(menu.getCarried());
        }
    }
}
