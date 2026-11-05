package dev.wp.industrialization_overdrive.compat;

import appeng.api.integrations.igtooltip.BaseClassRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.blocks.MEHatchBlock;

/** Grid tooltip for ME hatches; AE2 attaches its providers to every registered base class. Loaded via ServiceLoader. */
public final class AE2Tooltips implements TooltipProvider {
    @Override
    public void registerBlockEntityBaseClasses(BaseClassRegistration registration) {
        registration.addBaseBlockEntity(MEHatchBlockEntity.class, MEHatchBlock.class);
    }
}
