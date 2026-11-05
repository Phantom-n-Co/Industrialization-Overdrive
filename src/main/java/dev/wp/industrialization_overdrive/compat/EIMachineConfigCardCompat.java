package dev.wp.industrialization_overdrive.compat;

import dev.wp.industrialization_overdrive.IOComponents;
import dev.wp.industrialization_overdrive.IOUtil;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.components.craft.MultiProcessingArrayMachineComponent;
import dev.wp.industrialization_overdrive.machines.guicomponents.multiprocessingarraymachineslot.MultiProcessingArrayMachineSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.swedz.extended_industrialization.item.machineconfig.MachineConfigCardItem;
import net.swedz.extended_industrialization.item.machineconfig.MachineConfigPanel;
import net.swedz.extended_industrialization.item.machineconfig.event.MachineConfigCardApplyEvent;
import net.swedz.extended_industrialization.item.machineconfig.event.MachineConfigCardClearEvent;
import net.swedz.extended_industrialization.item.machineconfig.event.MachineConfigCardSaveEvent;
import org.jetbrains.annotations.Nullable;

// Carries an ME hatch's channel configuration on EI's machine config card, as a component of ours next to EI's.
public final class EIMachineConfigCardCompat {
    private EIMachineConfigCardCompat() {
    }

    public static void register() {
        if (IOUtil.isAE2Loaded) {
            NeoForge.EVENT_BUS.addListener(MachineConfigCardSaveEvent.class, EIMachineConfigCardCompat::onSave);
            NeoForge.EVENT_BUS.addListener(MachineConfigCardClearEvent.class, event -> event.getItem().remove(IOComponents.ME_HATCH_CARD_CONFIG));
            NeoForge.EVENT_BUS.addListener(MachineConfigCardApplyEvent.class, EIMachineConfigCardCompat::onApply);
        }
        MachineConfigPanel.register("multi_processing_array_machines", MultiProcessingArrayMachineComponent.class,
                (player, target, component, holder, slotItem, item, simulation) ->
                        MultiProcessingArrayMachineSlot.isMachine(item)
                                && MachineConfigPanel.ComponentTypeHandler.insertStack(player, target, component, slotItem, item, simulation));
    }

    private static void onSave(MachineConfigCardSaveEvent event) {
        if (event.getTargetMachine() instanceof MEHatchBlockEntity hatch) {
            event.getItem().set(IOComponents.ME_HATCH_CARD_CONFIG, hatch.saveConfiguration(hatch.getLevel().registryAccess()));
        } else {
            event.getItem().remove(IOComponents.ME_HATCH_CARD_CONFIG);
        }
    }

    // Cancelling the simulation makes EI refuse the whole card, so a config the hatch can't take is never half-applied.
    private static void onApply(MachineConfigCardApplyEvent event) {
        if (!(event.getTargetMachine() instanceof MEHatchBlockEntity hatch)) {
            return;
        }
        ItemStack card = heldCard(event.getEntity());
        var configuration = card == null ? null : card.get(IOComponents.ME_HATCH_CARD_CONFIG);
        if (configuration == null) {
            return;
        }
        boolean loaded = hatch.loadConfiguration(configuration, hatch.getLevel().registryAccess(), event.isSimulating());
        if (!loaded && event.isSimulating()) {
            event.setCanceled(true);
        }
    }

    // The event carries no stack: EI applies from the used hand, or the offhand when placing (main hand holds the block).
    private static @Nullable ItemStack heldCard(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MachineConfigCardItem) {
                return stack;
            }
        }
        return null;
    }
}
