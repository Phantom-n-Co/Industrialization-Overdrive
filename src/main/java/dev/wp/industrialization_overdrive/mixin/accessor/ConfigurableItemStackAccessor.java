package dev.wp.industrialization_overdrive.mixin.accessor;

import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ConfigurableItemStack.class, remap = false)
public interface ConfigurableItemStackAccessor {
    @Accessor("adjustedCapacity")
    void io_setAdjustedCapacity(int adjustedCapacity);
}
