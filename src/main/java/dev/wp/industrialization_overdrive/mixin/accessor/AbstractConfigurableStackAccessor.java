package dev.wp.industrialization_overdrive.mixin.accessor;

import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractConfigurableStack.class, remap = false)
public interface AbstractConfigurableStackAccessor {
    @Accessor("playerInsert")
    void io_setPlayerInsert(boolean playerInsert);

    @Accessor("playerExtract")
    void io_setPlayerExtract(boolean playerExtract);

    @Accessor("playerLockable")
    void io_setPlayerLockable(boolean playerLockable);

    @Accessor("playerLocked")
    void io_setPlayerLocked(boolean playerLocked);

    @Accessor("lockedInstance")
    void io_setLockedInstance(Object lockedInstance);

    @Invoker("notifyListeners")
    void io_notifyListeners();
}
