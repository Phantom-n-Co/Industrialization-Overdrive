package dev.wp.industrialization_overdrive.mixin.client;

import aztech.modern_industrialization.client.machines.gui.MachineMenuClient;
import aztech.modern_industrialization.client.machines.gui.MachineScreen;
import aztech.modern_industrialization.client.screen.MIHandledScreen;
import aztech.modern_industrialization.inventory.BackgroundRenderedSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wp.industrialization_overdrive.machines.guicomponents.mehatch.MEHatchGuiClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AE2-style rendering of ME hatch slots on MI's machine screen; inert for every other machine. */
@Mixin(MachineScreen.class)
public abstract class MachineScreenMEHatchMixin extends MIHandledScreen<MachineMenuClient> {
    private MachineScreenMEHatchMixin(MachineMenuClient menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (MEHatchGuiClient.isMEHatchMenu(getMenu())) {
            MEHatchGuiClient.renderSlotHighlight(guiGraphics, slot);
        } else {
            super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
        }
    }
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void io_renderMEHatchSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        MachineScreen self = (MachineScreen) (Object) this;
        if (!MEHatchGuiClient.isHandledSlot(self.getMenu(), slot)) {
            return;
        }
        ci.cancel();
        MEHatchGuiClient.renderSlot(guiGraphics, self.getMenu(), slot);
    }

    @WrapOperation(method = "renderConfigurableSlotBackgrounds", remap = false,
            at = @At(value = "INVOKE",
                    target = "Laztech/modern_industrialization/inventory/BackgroundRenderedSlot;getBackgroundAtlasLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation io_meHatchSlotBackground(BackgroundRenderedSlot slot, Operation<ResourceLocation> original) {
        MachineScreen self = (MachineScreen) (Object) this;
        return MEHatchGuiClient.backgroundAtlas(self.getMenu(), slot, original.call(slot));
    }
}
