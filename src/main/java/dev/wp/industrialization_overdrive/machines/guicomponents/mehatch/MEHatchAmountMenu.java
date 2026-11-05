package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.InaccessibleSlot;
import appeng.util.inv.AppEngInternalInventory;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchBlockEntity;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Not AE2's {@code SetStockAmountMenu}: that only opens on an interface host and takes an {@code int}. */
public final class MEHatchAmountMenu extends AEBaseMenu implements ISubMenu {
    public static final MenuType<MEHatchAmountMenu> TYPE = MenuTypeBuilder
            .create(MEHatchAmountMenu::new, MEHatchBlockEntity.class)
            .buildUnregistered(IO.id("me_hatch_amount"));

    private static final String ACTION_SET_AMOUNT = "setAmount";

    private final MEHatchBlockEntity host;

    private final Slot shownResource;

    @GuiSync(1)
    private long initialAmount = -1;

    @GuiSync(2)
    private long maxAmount = -1;

    private boolean fluid;
    private int channel;
    private @Nullable AEKey configured;

    public MEHatchAmountMenu(int id, Inventory playerInventory, MEHatchBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_SET_AMOUNT, Long.class, this::confirm);
        this.shownResource = new InaccessibleSlot(new AppEngInternalInventory(1), 0);
        this.addSlot(shownResource, SlotSemantics.MACHINE_OUTPUT);
    }

    public static void open(ServerPlayer player, MEHatchBlockEntity hatch, boolean fluid, int channel) {
        GenericStack config = hatch.getConfig(fluid, channel);
        if (!hatch.isInput() || config == null) {
            return;
        }
        if (hatch.openEditor(player, TYPE) && player.containerMenu instanceof MEHatchAmountMenu menu) {
            menu.fluid = fluid;
            menu.channel = channel;
            menu.configured = config.what();
            menu.initialAmount = config.amount();
            menu.maxAmount = MEHatchConfigComponent.maxAmount(fluid);
            menu.shownResource.set(config.what().wrapForDisplayOrFilter());
            menu.broadcastChanges();
        }
    }

    public void confirm(long amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_AMOUNT, amount);
            return;
        }

        // Only if the channel was not reconfigured since the editor opened.
        GenericStack current = host.getConfig(fluid, channel);
        if (amount > 0 && current != null && Objects.equals(current.what(), configured)) {
            host.setInputTarget(fluid, channel, amount);
        }
        host.returnToMainMenu(getPlayer(), this);
    }

    public @Nullable AEKey getWhatToStock() {
        GenericStack stack = GenericStack.fromItemStack(shownResource.getItem());
        return stack != null ? stack.what() : null;
    }

    public long getInitialAmount() {
        return initialAmount;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }
}
