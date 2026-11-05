package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.core.localization.GuiText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MEHatchAmountScreen extends AEBaseScreen<MEHatchAmountMenu> {
    public static final String STYLE = "/screens/set_stock_amount.json";

    private final NumberEntryWidget amount;
    private boolean amountInitialized;

    public MEHatchAmountScreen(MEHatchAmountMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        AESubScreen.addBackButton(menu, "back", widgets);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        this.amount.setLongValue(1);
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(1);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        // The menu's synced fields arrive after the screen opens.
        if (!amountInitialized) {
            var whatToStock = menu.getWhatToStock();
            if (whatToStock != null && menu.getInitialAmount() > 0) {
                amount.setType(NumberEntryType.of(whatToStock));
                amount.setLongValue(menu.getInitialAmount());
                amount.setMaxValue(menu.getMaxAmount());
                amountInitialized = true;
            }
        }
    }

    private void confirm() {
        amount.getLongValue().ifPresent(menu::confirm);
    }
}
