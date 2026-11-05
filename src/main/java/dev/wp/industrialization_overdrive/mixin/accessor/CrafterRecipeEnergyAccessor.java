package dev.wp.industrialization_overdrive.mixin.accessor;

import aztech.modern_industrialization.machines.components.CrafterComponent;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.AbstractModularCrafterComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A crafter's cycle energy, in MI's crafter and Tesseract's copy of it: the total its running cycle costs, and the
 * per-tick rate it would run at for a given efficiency — through Tesseract's efficiency hooks, so modded speed-ups
 * are included. Together they give the shortest a cycle can get, efficiency, upgrades and batch size included.
 */
@Mixin(value = {CrafterComponent.class, AbstractModularCrafterComponent.class}, remap = false)
public interface CrafterRecipeEnergyAccessor {
    @Accessor("recipeEnergy")
    long io_getRecipeEnergy();

    @Invoker("getRecipeMaxEu")
    long io_invokeGetRecipeMaxEu(long recipeEu, long totalEu, int efficiencyTicks);
}
