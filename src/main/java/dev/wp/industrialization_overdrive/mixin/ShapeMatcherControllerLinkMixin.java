package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tells an ME hatch which controller matched it (MI's link only passes the casing); the input hatch needs that
 * machine's running recipe. {@code rematch} is inherited by all matchers; the hatch overrides {@code unlink}.
 */
@Mixin(value = ShapeMatcher.class, remap = false)
public abstract class ShapeMatcherControllerLinkMixin {
    @Shadow
    @Final
    protected BlockPos controllerPos;

    @WrapOperation(method = "rematch",
            at = @At(value = "INVOKE",
                    target = "Laztech/modern_industrialization/machines/multiblocks/HatchBlockEntity;link(Laztech/modern_industrialization/machines/models/MachineCasing;)V"))
    private void io_linkMEHatchController(HatchBlockEntity hatch, MachineCasing casing, Operation<Void> original) {
        original.call(hatch, casing);
        if (hatch instanceof MEHatchBlockEntity meHatch) {
            meHatch.linkController(controllerPos);
        }
    }
}
