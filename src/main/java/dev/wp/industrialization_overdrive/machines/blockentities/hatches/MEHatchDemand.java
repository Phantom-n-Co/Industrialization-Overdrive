package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.api.machine.component.CrafterAccess;
import aztech.modern_industrialization.api.machine.holder.CrafterComponentHolder;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import dev.wp.industrialization_overdrive.mixin.accessor.CrafterRecipeEnergyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.swedz.tesseract.neoforge.compat.mi.api.ActiveRecipeHolder;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Smart pull: per-channel stock sized to the controller's running recipe until the next transfer. Cycles are timed
 * at full-efficiency speed (efficiency can rise between transfers) and batching crafters at their max batch; the
 * configured amount stays the minimum, and the last targets are held a few transfers after the recipe stops.
 */
public final class MEHatchDemand {
    private static int batchSize = 1;

    private final long[] itemDemand = new long[MEHatchConfigComponent.CHANNEL_COUNT];
    private final long[] fluidDemand = new long[MEHatchConfigComponent.CHANNEL_COUNT];

    private long holdUntil = Long.MIN_VALUE;

    /**
     * Batch size a controller's recipe lock is applied for, so a lock-configured ME input channel stocks a whole batch.
     * MI's lock loop has no notion of batching; the batching crafter publishes its max here during {@code lockRecipe}
     * (server thread only).
     */
    public static void withBatchSize(int maxBatchSize, Runnable lock) {
        int previous = batchSize;
        batchSize = Math.max(1, maxBatchSize);
        try {
            lock.run();
        } finally {
            batchSize = previous;
        }
    }

    static int batchSize() {
        return batchSize;
    }

    long target(boolean fluid, int index, GenericStack config) {
        return Math.max(config.amount(), (fluid ? fluidDemand : itemDemand)[index]);
    }

    void update(Level level, @Nullable BlockPos controllerPos, MEHatchConfigComponent configuration) {
        if (!configuration.isSmartPull()) {
            clear();
            return;
        }
        int interval = configuration.getTransferInterval();
        Cycle cycle = runningCycle(level, controllerPos);
        long now = level.getGameTime();
        if (cycle == null) {
            if (now >= holdUntil) {
                clear();
            }
            return;
        }
        holdUntil = now + (long) IO.config().meHatches().smartPullHoldIntervals() * interval;

        // The cycles that start before the next transfer, plus the one waiting for it.
        long cycles = Math.ceilDiv(interval, cycle.ticks()) + 1;
        for (int index = 0; index < MEHatchConfigComponent.CHANNEL_COUNT; index++) {
            itemDemand[index] = demand(cycle, configuration.getConfig(false, index), cycles, false);
            fluidDemand[index] = demand(cycle, configuration.getConfig(true, index), cycles, true);
        }
    }

    private void clear() {
        Arrays.fill(itemDemand, 0L);
        Arrays.fill(fluidDemand, 0L);
        holdUntil = Long.MIN_VALUE;
    }

    private static long demand(Cycle cycle, @Nullable GenericStack config, long cycles, boolean fluid) {
        if (config == null) {
            return 0L;
        }
        long consumed = 0L;
        long kept = 0L;
        AEKey key = config.what();
        if (!fluid && key instanceof AEItemKey item) {
            ItemStack stack = item.toStack();
            for (MachineRecipe.ItemInput input : cycle.recipe().itemInputs) {
                if (input.matches(stack)) {
                    if (input.probability() == 0F) {
                        kept += input.amount();
                    } else {
                        consumed += input.amount();
                    }
                }
            }
        } else if (fluid && key instanceof AEFluidKey fluidKey) {
            for (MachineRecipe.FluidInput input : cycle.recipe().fluidInputs) {
                if (input.fluid().test(fluidKey.toStack(1))) {
                    if (input.probability() == 0F) {
                        kept += input.amount();
                    } else {
                        consumed += input.amount();
                    }
                }
            }
        }
        long perBatch = saturatingMultiply(consumed, cycles) + kept;
        return Math.min(saturatingMultiply(perBatch, cycle.batch()), MEHatchConfigComponent.maxAmount(fluid));
    }

    private static @Nullable Cycle runningCycle(Level level, @Nullable BlockPos controllerPos) {
        if (controllerPos == null || !level.isLoaded(controllerPos)
                || !(level.getBlockEntity(controllerPos) instanceof CrafterComponentHolder holder)) {
            return null;
        }
        CrafterAccess crafter = holder.getCrafterComponent();
        if (crafter == null || !crafter.hasActiveRecipe()
                || !(crafter instanceof ActiveRecipeHolder<?> recipes)
                || !(crafter instanceof CrafterRecipeEnergyAccessor energy)
                || !(recipes.getActiveRecipe() instanceof RecipeHolder<?> recipeHolder)
                || !(recipeHolder.value() instanceof MachineRecipe recipe)) {
            return null;
        }
        long total = energy.io_getRecipeEnergy();
        long peakPerTick = Math.max(crafter.getCurrentRecipeEu(),
                energy.io_invokeGetRecipeMaxEu(recipeEuCost(recipes), total, crafter.getMaxEfficiencyTicks()));
        if (peakPerTick <= 0 || total <= 0) {
            return null;
        }
        int batch = crafter instanceof MultipliedCrafterComponent multiplied ? Math.max(1, multiplied.getMaxMultiplier()) : 1;
        return new Cycle(recipe, Math.ceilDiv(total, peakPerTick), batch);
    }

    private static <R> long recipeEuCost(ActiveRecipeHolder<R> recipes) {
        return recipes.getRecipeEuCost(recipes.getActiveRecipe());
    }

    private record Cycle(MachineRecipe recipe, long ticks, int batch) {
    }

    private static long saturatingMultiply(long left, long right) {
        long high = Math.multiplyHigh(left, right);
        long low = left * right;
        return (high == 0 && low >= 0) ? low : Long.MAX_VALUE;
    }
}
