package dev.wp.industrialization_overdrive.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchDemand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Locks a recipe on a batching (multiplied) crafter with its maximum batch size in effect, so ME input channels
 * the lock configures stock a full batch; see {@link MEHatchDemand}.
 */
@Mixin(value = MultipliedCrafterComponent.class, remap = false)
public abstract class MultipliedCrafterComponentRecipeLockMixin {
    @WrapMethod(method = "lockRecipe")
    private void io_lockRecipeForFullBatch(ResourceLocation recipeId, Inventory inventory, Operation<Void> original) {
        int maxBatchSize = ((MultipliedCrafterComponent) (Object) this).getMaxMultiplier();
        MEHatchDemand.withBatchSize(maxBatchSize, () -> original.call(recipeId, inventory));
    }
}
