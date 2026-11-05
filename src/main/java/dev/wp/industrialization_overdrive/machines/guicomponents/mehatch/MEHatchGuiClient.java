package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.AETextField;
import aztech.modern_industrialization.client.machines.gui.ClientComponentRenderer;
import aztech.modern_industrialization.client.machines.gui.GuiComponentClient;
import aztech.modern_industrialization.client.machines.gui.MachineScreen;
import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.BackgroundRenderedSlot;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.gui.GuiComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.util.FluidHelper;
import aztech.modern_industrialization.util.TextHelper;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MEHatchInventories;
import dev.wp.industrialization_overdrive.machines.blockentities.hatches.MENetworkTransfer;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import net.minecraft.client.renderer.RenderType;
import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;
import dev.wp.industrialization_overdrive.network.packet.OpenMEHatchEditorPacket;
import dev.wp.industrialization_overdrive.network.packet.SetMEHatchSettingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MEHatchGuiClient extends GuiComponentClient<Boolean, MEHatchGui.Data> {
    private static final int LABEL_COLOUR = 0x404040;

    private static final ResourceLocation ITEM_BACKGROUND = IO.id("textures/gui/container/me_item_hatch.png");
    private static final ResourceLocation FLUID_BACKGROUND = IO.id("textures/gui/container/me_fluid_hatch.png");
    private static final ResourceLocation MIXED_BACKGROUND = IO.id("textures/gui/container/me_mixed_hatch.png");

    public MEHatchGuiClient(Boolean params, MEHatchGui.Data data) {
        super(params, data);
    }

    public int transferInterval() {
        return data.transferInterval();
    }

    public boolean storageMode() {
        return !params && data.storageMode();
    }

    static final int SMART_PULL_X = 8;
    static final int SMART_PULL_BOX = 10;
    private static final int SMART_PULL_LABEL_GAP = 3;

    private static final int PRIORITY_BUTTON_X = 152;
    private static final int PRIORITY_BUTTON_Y = -5;
    private static final int PRIORITY_BUTTON_SIZE = 20;
    private static final int DISABLED_COLOUR = 0x878FA5;
    private static final int WARNING_COLOUR = 0xCE2401;

    @Override
    public void setupMenu(GuiComponent.MenuFacade menu) {
        MIInventory inventory = menu.getMachineInventory();
        boolean hasItems = inventory.getItemStacks().size() == MEHatchGui.Data.CHANNEL_COUNT;
        boolean hasFluids = inventory.getFluidStacks().size() == MEHatchGui.Data.CHANNEL_COUNT;
        boolean mixed = MEHatchGui.isMixed(inventory.getItemStacks().size(), inventory.getFluidStacks().size());

        if (hasItems) {
            MEHatchConfigSlot.addRow(menu, null, false, params, mixed, i -> config(false, i), i -> stock(false, i));
        }
        if (hasFluids) {
            MEHatchConfigSlot.addRow(menu, null, true, params, mixed, i -> config(true, i), i -> stock(true, i));
        }

        // The stock row is drawn by the render mixin, which recognises ME hatch menus from their own slots so it survives MI's re-syncs.
    }

    // Derived from the live slot list: MI replaces stack list entries on every sync, so cached references go stale.
    public static boolean isMEHatchMenu(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot instanceof MEHatchConfigSlot) {
                return true;
            }
        }
        return false;
    }

    private @Nullable GenericStack config(boolean fluid, int index) {
        return fluid ? data.fluidConfig().get(index) : data.itemConfig().get(index);
    }

    private @Nullable GenericStack stock(boolean fluid, int index) {
        return fluid ? data.fluidStock().get(index) : data.itemStock().get(index);
    }

    private long target(boolean fluid, int index, GenericStack config) {
        return Math.max(config.amount(), (fluid ? data.fluidTarget() : data.itemTarget()).get(index));
    }

    // An output hatch with none configured accepts any output (see MEHatchBlockEntity.isOpenOutput).
    private boolean anyConfigured() {
        return data.itemConfig().stream().anyMatch(Objects::nonNull)
                || data.fluidConfig().stream().anyMatch(Objects::nonNull);
    }

    private boolean hasItems(MachineScreen screen) {
        return screen.getMenu().getMachineInventory().getItemStacks().size() == MEHatchGui.Data.CHANNEL_COUNT;
    }

    private boolean hasFluids(MachineScreen screen) {
        return screen.getMenu().getMachineInventory().getFluidStacks().size() == MEHatchGui.Data.CHANNEL_COUNT;
    }

    private boolean mixed(MachineScreen screen) {
        return MEHatchGui.isMixed(
                screen.getMenu().getMachineInventory().getItemStacks().size(),
                screen.getMenu().getMachineInventory().getFluidStacks().size());
    }

    @Override
    public ClientComponentRenderer createRenderer(MachineScreen machineScreen) {
        return new ClientComponentRenderer() {
            @Override
            public void addButtons(ButtonContainer container) {
                boolean mixed = mixed(machineScreen);
                if (params) {
                    addToggle(container, IO.text().meHatchSmartPullLabel(), () -> data.smartPull(),
                            syncId -> SetMEHatchSettingPacket.of(syncId, SetMEHatchSettingPacket.Setting.SMART_PULL, !data.smartPull()).sendToServer(),
                            () -> List.of(
                                    IO.text().meHatchSmartPullState(data.smartPull() ? IO.text().enabled() : IO.text().disabled()),
                                    IO.text().meHatchSmartPullDescription(),
                                    IO.text().meHatchSmartPullMinimum()),
                            () -> false);
                } else {
                    addToggle(container, IO.text().meHatchStorageModeLabel(), () -> data.storageMode(),
                            syncId -> SetMEHatchSettingPacket.of(syncId, SetMEHatchSettingPacket.Setting.STORAGE_MODE, !data.storageMode()).sendToServer(),
                            this::storageModeTooltip,
                            () -> data.storageMode() && !data.hasCell());
                }
                addPriorityButton(container);
                if (hasItems(machineScreen)) {
                    addChannelButtons(container, false, mixed);
                }
                if (hasFluids(machineScreen)) {
                    addChannelButtons(container, true, mixed);
                }
            }

            /** {@code state} is read every frame: MI replaces {@code data} on each sync, so do not bind it as a method reference. */
            private void addToggle(ButtonContainer container, Component label, BooleanSupplier state,
                    Consumer<Integer> onClick, Supplier<List<Component>> tooltip, BooleanSupplier warning) {
                Font font = Minecraft.getInstance().font;
                int y = IntervalField.Y + (IntervalField.HEIGHT - SMART_PULL_BOX) / 2;
                int width = SMART_PULL_BOX + SMART_PULL_LABEL_GAP + font.width(label);
                container.addButton(
                        SMART_PULL_X,
                        y,
                        width,
                        SMART_PULL_BOX,
                        onClick,
                        tooltip,
                        (screen, button, graphics, mouseX, mouseY, partialTick) -> {
                            int x = button.getX();
                            int top = button.getY();
                            graphics.fill(x, top, x + SMART_PULL_BOX, top + SMART_PULL_BOX,
                                    button.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
                            graphics.fill(x + 1, top + 1, x + SMART_PULL_BOX - 1, top + SMART_PULL_BOX - 1, 0xFF000000);
                            if (state.getAsBoolean()) {
                                graphics.fill(x + 3, top + 3, x + SMART_PULL_BOX - 3, top + SMART_PULL_BOX - 3, 0xFF55FF55);
                            }
                            graphics.drawString(font, label, x + SMART_PULL_BOX + SMART_PULL_LABEL_GAP,
                                    top + (SMART_PULL_BOX - font.lineHeight) / 2 + 1,
                                    warning.getAsBoolean() ? WARNING_COLOUR : LABEL_COLOUR, false);
                        },
                        () -> true);
            }

            private List<Component> storageModeTooltip() {
                List<Component> lines = new ArrayList<>();
                lines.add(IO.text().meHatchStorageModeState(data.storageMode() ? IO.text().enabled() : IO.text().disabled()));
                lines.add(IO.text().meHatchStorageModeDescription());
                lines.add(IO.text().meHatchStorageModeExtractOnly());
                if (!data.hasCell()) {
                    lines.add(IO.text().meHatchStorageModeNeedsCell());
                }
                return lines;
            }

            private void addPriorityButton(ButtonContainer container) {
                container.addButton(
                        PRIORITY_BUTTON_X,
                        PRIORITY_BUTTON_Y,
                        PRIORITY_BUTTON_SIZE,
                        PRIORITY_BUTTON_SIZE,
                        syncId -> OpenMEHatchEditorPacket.priority(syncId).sendToServer(),
                        () -> params
                                ? List.of(
                                        IO.text().meHatchPriority(data.priority()),
                                        IO.text().meHatchPriorityInputDescription())
                                : List.of(
                                        IO.text().meHatchPriority(data.priority()),
                                        IO.text().meHatchPriorityDescription(),
                                        IO.text().meHatchPriorityStorageOnly()),
                        (screen, button, graphics, mouseX, mouseY, partialTick) -> {
                            (button.isHoveredOrFocused() ? Icon.TAB_BUTTON_BACKGROUND_FOCUS : Icon.TAB_BUTTON_BACKGROUND)
                                    .getBlitter().dest(button.getX(), button.getY()).blit(graphics);
                            Icon.PRIORITY.getBlitter().dest(button.getX() + 2, button.getY() + 1).blit(graphics);
                        },
                        () -> true);
            }

            private void addChannelButtons(ButtonContainer container, boolean fluid, boolean mixed) {
                for (int index = 0; index < MEHatchGui.Data.CHANNEL_COUNT; index++) {
                    int channel = index;
                    int x = MEHatchConfigSlot.channelX(index);
                    int y = MEHatchConfigSlot.configY(fluid, mixed);

                    // The cog sits in a strip above the cell, not inside it, so it does not fight the cell for clicks.
                    if (params) {
                        container.addButton(
                                x,
                                y - MEHatchGui.COG_STRIP_HEIGHT,
                                16,
                                16,
                                syncId -> OpenMEHatchEditorPacket.amount(syncId, channel, fluid).sendToServer(),
                                () -> List.of(ButtonToolTips.InterfaceSetStockAmount.text()),
                                (screen, button, graphics, mouseX, mouseY, partialTick) ->
                                        (button.isHoveredOrFocused() ? Icon.COG : Icon.COG_DISABLED).getBlitter()
                                                .dest(button.getX(), button.getY()).blit(graphics),
                                () -> config(fluid, channel) != null);
                    }

                    // Assignment and clearing are not buttons: a Button only takes left clicks; they go through ConfigurableScreenHandlerMEHatchMixin.
                }
            }

            @Override
            public void renderBackground(GuiGraphics graphics, int leftPos, int topPos) {
                boolean mixed = mixed(machineScreen);
                ResourceLocation background = mixed ? MIXED_BACKGROUND
                        : hasFluids(machineScreen) ? FLUID_BACKGROUND : ITEM_BACKGROUND;
                MachineGuiParameters params = machineScreen.getGuiParams();
                graphics.blit(background, leftPos, topPos, 0, 0, params.backgroundWidth, params.backgroundHeight);

                Font font = Minecraft.getInstance().font;
                int labelY = topPos + IntervalField.Y + (IntervalField.HEIGHT - font.lineHeight) / 2 + 1;
                Component label = IO.text().meHatchIntervalLabel();
                int colour = storageMode() ? DISABLED_COLOUR : LABEL_COLOUR;
                graphics.drawString(font, label, leftPos + IntervalField.X - 3 - font.width(label), labelY,
                        colour, false);
                graphics.drawString(font, IO.text().meHatchIntervalUnit(),
                        leftPos + IntervalField.X + IntervalField.WIDTH + 3, labelY, colour, false);
            }

            @Override
            public boolean renderTooltip(MachineScreen screen, Font font, GuiGraphics graphics,
                    int leftPos, int topPos, int cursorX, int cursorY) {
                if (cursorX >= leftPos + IntervalField.X && cursorX < leftPos + IntervalField.X + IntervalField.WIDTH
                        && cursorY >= topPos + IntervalField.Y && cursorY < topPos + IntervalField.Y + IntervalField.HEIGHT) {
                    return showTooltip(graphics, font, storageMode()
                            ? List.of(IO.text().meHatchIntervalDescription(), IO.text().meHatchIntervalStorageMode())
                            : List.of(
                                    IO.text().meHatchIntervalDescription(),
                                    IO.text().meHatchIntervalControlType(),
                                    IO.text().meHatchIntervalControlScroll()), cursorX, cursorY);
                }
                boolean mixed = mixed(screen);
                if (hasItems(screen)) {
                    for (int index = 0; index < MEHatchGui.Data.CHANNEL_COUNT; index++) {
                        if (isHovering(leftPos, topPos, index, MEHatchConfigSlot.configY(false, mixed), cursorX, cursorY)) {
                            return showTooltip(graphics, font,
                                    configurationTooltip(false, index), cursorX, cursorY);
                        }
                        if (isHovering(leftPos, topPos, index, MEHatchInventories.stockY(false, mixed), cursorX, cursorY)) {
                            return showTooltip(graphics, font,
                                    stockTooltip(false, index), cursorX, cursorY);
                        }
                    }
                }
                if (hasFluids(screen)) {
                    for (int index = 0; index < MEHatchGui.Data.CHANNEL_COUNT; index++) {
                        if (isHovering(leftPos, topPos, index, MEHatchConfigSlot.configY(true, mixed), cursorX, cursorY)) {
                            return showTooltip(graphics, font,
                                    configurationTooltip(true, index), cursorX, cursorY);
                        }
                        if (isHovering(leftPos, topPos, index, MEHatchInventories.stockY(true, mixed), cursorX, cursorY)) {
                            return showTooltip(graphics, font,
                                    stockTooltip(true, index), cursorX, cursorY);
                        }
                    }
                }
                return false;
            }


            private boolean isHovering(int leftPos, int topPos, int index, int y,
                    double cursorX, double cursorY) {
                int x = leftPos + MEHatchConfigSlot.channelX(index);
                int absoluteY = topPos + y;
                return cursorX >= x - 1 && cursorX < x + 16 + 1
                        && cursorY >= absoluteY - 1 && cursorY < absoluteY + 16 + 1;
            }

            private boolean showTooltip(GuiGraphics graphics, Font font,
                    List<Component> lines, int cursorX, int cursorY) {
                graphics.renderTooltip(font, lines, Optional.empty(), cursorX, cursorY);
                return true;
            }

            private List<Component> configurationTooltip(boolean fluid, int index) {
                GenericStack current = config(fluid, index);
                List<Component> lines = new ArrayList<>();
                if (current == null) {
                    lines.add(fluid ? IO.text().meHatchNoFluidSet() : IO.text().meHatchNoItemSet());
                    lines.add(IO.text().meHatchSetHint());
                    return lines;
                }

                boolean input = params;
                lines.add(current.what().getDisplayName());
                lines.add(input
                        ? IO.text().meHatchKeepsStocked(formatAmount(fluid, current.amount()))
                        : IO.text().meHatchOutputOnlyThis());

                if (!Screen.hasShiftDown()) {
                    lines.add(IO.text().meHatchHoldShiftForControls());
                    return lines;
                }
                if (input) {
                    lines.add(IO.text().meHatchControlScroll());
                    lines.add(IO.text().meHatchControlScrollModifiers());
                    lines.add(IO.text().meHatchControlEnterAmount("pickItem"));
                }
                lines.add(IO.text().meHatchControlReplace());
                lines.add(IO.text().meHatchControlClear());
                return lines;
            }

            private List<Component> stockTooltip(boolean fluid, int index) {
                GenericStack stock = stock(fluid, index);
                GenericStack config = config(fluid, index);
                boolean input = params;
                long amount = stock == null ? 0L : stock.amount();
                AEKey reserved = reservedFor(fluid, index);

                List<Component> lines = new ArrayList<>();
                if (input) {
                    GenericStack named = stock != null ? stock : config;
                    lines.add(named == null ? IO.text().meHatchStockEmpty() : named.what().getDisplayName());
                    if (config != null) {
                        lines.add(formatAmount(fluid, amount, target(fluid, index, config)));
                    } else if (stock != null) {
                        lines.add(formatAmount(fluid, amount));
                    }
                } else if (stock != null && amount > 0) {
                    lines.add(IO.text().meHatchNameWithAmount(stock.what().getDisplayName(), formatCount(fluid, amount)));
                } else if (reserved != null) {
                    lines.add(reserved.getDisplayName());
                } else {
                    lines.add(config == null ? IO.text().meHatchStockEmpty() : config.what().getDisplayName());
                }

                if (config == null) {
                    if (input) {
                        lines.add(IO.text().meHatchInputUnconfigured());
                    } else {
                        lines.add(anyConfigured() ? IO.text().meHatchOutputUnconfigured() : IO.text().meHatchOutputOpen());
                    }
                } else {
                    lines.add(input ? IO.text().meHatchStockedFromNetwork() : IO.text().meHatchSentToNetwork());
                    if (input && target(fluid, index, config) > config.amount()) {
                        lines.add(IO.text().meHatchSmartPullRaised(formatAmount(fluid, config.amount())));
                    }
                }
                if (reserved != null) {
                    lines.add(IO.text().meHatchReservedForRecipe());
                }
                return lines;
            }

            private @Nullable AEKey reservedFor(boolean fluid, int index) {
                MIInventory inventory = machineScreen.getMenu().getMachineInventory();
                List<? extends AbstractConfigurableStack<?, ?>> stacks = fluid
                        ? inventory.getFluidStacks()
                        : inventory.getItemStacks();
                return index < stacks.size() ? reservedResource(stacks.get(index)) : null;
            }
        };
    }

    private static Component formatCount(boolean fluid, long amount) {
        return fluid ? formatAmount(true, amount) : IO.text().meHatchItemCount(formatAmount(false, amount));
    }

    private static Component formatAmount(boolean fluid, long amount) {
        if (fluid) {
            return FluidHelper.getFluidAmount(amount);
        }
        if (amount < 100_000L || Screen.hasShiftDown()) {
            return Component.literal(String.format("%,d", amount));
        }
        TextHelper.Amount abbreviated = TextHelper.getAmount(amount);
        return Component.literal(abbreviated.digit() + " " + abbreviated.unit());
    }

    private static Component formatAmount(boolean fluid, long amount, long max) {
        if (fluid) {
            return FluidHelper.getFluidAmount(amount, max);
        }
        if (max < 100_000L || Screen.hasShiftDown()) {
            return Component.literal(String.format("%,d / %,d", amount, max));
        }
        TextHelper.MaxedAmount abbreviated = TextHelper.getMaxedAmount(amount, max);
        return Component.literal(abbreviated.digit() + " / " + abbreviated.maxDigit() + " " + abbreviated.unit());
    }

    // ---------------------------------------------------------------- stock rendering

    /** Membership is decided per menu, not by slot/stack identity: MI replaces the entries of its stack lists on sync, so identity registries go stale. */
    public static boolean isHandledSlot(@Nullable AbstractContainerMenu menu, Slot slot) {
        if (slot instanceof MEHatchConfigSlot) {
            return true;
        }
        if (menu == null || !isMEHatchMenu(menu)) {
            return false;
        }
        return slot instanceof ConfigurableItemStack.ConfigurableItemSlot
                || slot instanceof ConfigurableFluidStack.ConfigurableFluidSlot;
    }

    // Drawn in the slot pass to avoid double painting; the amount comes from MI's backing stack since a rendered ItemStack count saturates.
    public static void renderSlot(GuiGraphics graphics, @Nullable AbstractContainerMenu menu, Slot slot) {
        renderLockFrame(graphics, menu, slot);
        GenericStack shown;
        if (slot instanceof MEHatchConfigSlot configSlot) {
            shown = configSlot.displayedConfig();
        } else {
            shown = stockOf(menu, slot);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (shown == null || shown.amount() <= 0) {
            AEKey ghost = slot instanceof MEHatchConfigSlot ? null : ghostResource(menu, slot);
            if (ghost != null) {
                AEKeyRendering.drawInGui(minecraft, graphics, slot.x, slot.y, ghost);
                StackSizeRenderer.renderSizeLabel(graphics, minecraft.font, slot.x, slot.y, "0", false);
            }
            return;
        }

        AEKey what = shown.what();
        AEKeyRendering.drawInGui(minecraft, graphics, slot.x, slot.y, what);
        StackSizeRenderer.renderSizeLabel(graphics, minecraft.font, slot.x, slot.y,
                what.formatAmount(shown.amount(), AmountFormat.SLOT), false);
    }

    public static void renderSlotHighlight(GuiGraphics graphics, Slot slot) {
        int x = slot.x;
        int y = slot.y;
        graphics.hLine(x, x + 16, y - 1, 0xFFDAFFFF);
        graphics.hLine(x - 1, x + 16, y + 16, 0xFFDAFFFF);
        graphics.vLine(x - 1, y - 2, y + 16, 0xFFDAFFFF);
        graphics.vLine(x + 16, y - 2, y + 16, 0xFFDAFFFF);
        graphics.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0x669CD3FF, 0x669CD3FF, 0);
    }

    // MI's slot_atlas lock-frame ring colours: player lock, machine lock.
    private static final int PLAYER_LOCK_COLOUR = 0xFFC44E26;
    private static final int MACHINE_LOCK_COLOUR = 0xFFA0731F;

    /** Drawn here, not via MI's atlas, so the hatch background's slot fill stays; MI would also rank the player lock above a reservation. */
    private static void renderLockFrame(GuiGraphics graphics, @Nullable AbstractContainerMenu menu, Slot slot) {
        if (slot instanceof MEHatchConfigSlot) {
            return;
        }
        int colour = configuredResource(menu, slot) != null ? PLAYER_LOCK_COLOUR
                : reservedResource(slot) != null ? MACHINE_LOCK_COLOUR : 0;
        if (colour != 0) {
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, colour);
        }
    }

    /** None: the hatch background already draws every frame (see the renderer above). */
    public static @Nullable ResourceLocation backgroundAtlas(AbstractContainerMenu menu, BackgroundRenderedSlot slot,
            @Nullable ResourceLocation original) {
        return slot instanceof Slot menuSlot && isHandledSlot(menu, menuSlot) ? null : original;
    }

    private static @Nullable AEKey ghostResource(@Nullable AbstractContainerMenu menu, Slot slot) {
        AEKey reserved = reservedResource(slot);
        return reserved != null ? reserved : configuredResource(menu, slot);
    }

    private static @Nullable AEKey configuredResource(@Nullable AbstractContainerMenu menu, Slot slot) {
        MEHatchConfigSlot configSlot = configSlotOf(menu, slot);
        GenericStack config = configSlot == null ? null : configSlot.displayedConfig();
        return config == null ? null : config.what();
    }

    private static @Nullable AEKey reservedResource(Slot slot) {
        if (slot instanceof ConfigurableItemStack.ConfigurableItemSlot itemSlot) {
            return reservedResource(itemSlot.getConfStack());
        } else if (slot instanceof ConfigurableFluidStack.ConfigurableFluidSlot fluidSlot) {
            return reservedResource(fluidSlot.getConfStack());
        }
        return null;
    }

    static @Nullable AEKey reservedResource(AbstractConfigurableStack<?, ?> stack) {
        return stack.isMachineLocked()
                ? MENetworkTransfer.keyOfLockInstance(stack instanceof ConfigurableFluidStack, stack.getLockedInstance())
                : null;
    }

    /** Located via the config cell: MI's slot cannot be the source, {@code getCapacity} clamps to the item's max stack size. */
    private static @Nullable GenericStack stockOf(@Nullable AbstractContainerMenu menu, Slot slot) {
        MEHatchConfigSlot configSlot = configSlotOf(menu, slot);
        return configSlot == null ? null : configSlot.displayedStock();
    }

    private static @Nullable MEHatchConfigSlot configSlotOf(@Nullable AbstractContainerMenu menu, Slot slot) {
        if (menu == null) {
            return null;
        }
        boolean fluid = slot instanceof ConfigurableFluidStack.ConfigurableFluidSlot;
        int channel = 0;
        for (Slot other : menu.slots) {
            if (other == slot) {
                break;
            }
            if (isStockSlotOfKind(other, fluid)) {
                channel++;
            }
        }

        for (Slot other : menu.slots) {
            if (other instanceof MEHatchConfigSlot configSlot
                    && configSlot.isFluid() == fluid
                    && configSlot.getChannel() == channel) {
                return configSlot;
            }
        }
        return null;
    }

    private static boolean isStockSlotOfKind(Slot slot, boolean fluid) {
        return fluid
                ? slot instanceof ConfigurableFluidStack.ConfigurableFluidSlot
                : slot instanceof ConfigurableItemStack.ConfigurableItemSlot;
    }

    // ---------------------------------------------------------------- interval field

    public static final class IntervalField extends AETextField {
        static final int X = 128;
        static final int Y = 17;
        static final int WIDTH = 34;
        static final int HEIGHT = 12;

        // AE2's TEXTFIELD_TEXT and TEXTFIELD_ERROR palette colours.
        private static final int VALID_COLOUR = 0xF2F2F2;
        private static final int INVALID_COLOUR = 0xFF1900;

        private final MachineScreen screen;
        private final MEHatchGuiClient component;

        /** The synced value last written into the field, so a stale sync does not overwrite a fresher edit. */
        private int shown = -1;

        public IntervalField(MachineScreen screen, MEHatchGuiClient component) {
            // Any AE2 screen style works: the field only reads the shared palette from it.
            super(StyleManager.loadStyleDoc("/screens/priority.json"), Minecraft.getInstance().font,
                    screen.x() + X, screen.y() + Y, WIDTH, HEIGHT);
            setBordered(false);
            setMessage(IO.text().meHatchIntervalLabel());
            this.screen = screen;
            this.component = component;
            setMaxLength(Integer.toString(MEHatchConfigComponent.MAX_TRANSFER_INTERVAL).length());
            setFilter(text -> text.chars().allMatch(Character::isDigit));
            setResponder(text -> setTextColor(isValid(parse(text)) ? VALID_COLOUR : INVALID_COLOUR));
            followServer();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setEditable(!component.storageMode());
            followServer();
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            super.setFocused(focused);
            if (wasFocused && !focused) {
                apply();
            }
        }

        public void adjust(int delta) {
            if (component.storageMode()) {
                return;
            }
            int current = parse(getValue());
            send((isValid(current) ? current : component.transferInterval()) + delta);
        }

        private void apply() {
            int typed = parse(getValue());
            if (isValid(typed)) {
                send(typed);
            } else {
                shown = -1;
                followServer();
            }
        }

        private void send(int ticks) {
            int clamped = Math.clamp(ticks, 1, MEHatchConfigComponent.MAX_TRANSFER_INTERVAL);
            // Keep showing the new value until the server's sync replaces the old one.
            shown = component.transferInterval();
            setValue(Integer.toString(clamped));
            if (clamped != component.transferInterval()) {
                new SetMEHatchSettingPacket(screen.getMenu().containerId, SetMEHatchSettingPacket.Setting.INTERVAL, clamped)
                        .sendToServer();
            }
        }

        private void followServer() {
            int synced = component.transferInterval();
            if (!isFocused() && synced != shown) {
                shown = synced;
                setValue(Integer.toString(synced));
            }
        }

        private static boolean isValid(int ticks) {
            return ticks > 0;
        }

        private static int parse(String text) {
            // The filter allows digits only, and the length limit keeps the number well inside an int.
            return text.isEmpty() ? -1 : Integer.parseInt(text);
        }
    }
}
