package dev.wp.industrialization_overdrive.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.guicomponents.SlotPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotPanel.class)
public interface SlotPanelAccessor {
    @Accessor("machine")
    MachineBlockEntity io_getMachine();
}
