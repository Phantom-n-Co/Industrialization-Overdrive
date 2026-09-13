package dev.wp.industrialization_overdrive.mixin;

import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractMultipliedCraftingMultiblockBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractMultipliedCraftingMultiblockBlockEntity.class)
public interface AbstractMultipliedCraftingMultiblockAccessor {
    @Accessor("crafter")
    MultipliedCrafterComponent io_getCrafter();
}
