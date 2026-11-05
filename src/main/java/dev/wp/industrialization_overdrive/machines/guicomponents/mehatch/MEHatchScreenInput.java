package dev.wp.industrialization_overdrive.machines.guicomponents.mehatch;

import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.client.machines.gui.MachineScreen;
import dev.wp.industrialization_overdrive.machines.components.MEHatchConfigComponent;
import dev.wp.industrialization_overdrive.network.packet.OpenMEHatchEditorPacket;
import dev.wp.industrialization_overdrive.network.packet.SetMEHatchAmountPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Cancelling the {@code Pre} event bypasses the screen's handler, so MI's own scroll-to-adjust-capacity does not also fire on the same notch. */
public final class MEHatchScreenInput {
    private MEHatchScreenInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, MEHatchScreenInput::onInit);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseScrolled.Pre.class, MEHatchScreenInput::onScroll);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, MEHatchScreenInput::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, MEHatchScreenInput::onKey);
    }

    /** MI's screen only takes buttons from its components, so the interval field is added here. */
    private static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof MachineScreen screen) {
            MEHatchGuiClient component = screen.getMenu().getComponent(MEHatchGuiClient.class);
            if (component != null) {
                event.addListener(new MEHatchGuiClient.IntervalField(screen, component));
            }
        }
    }

    private static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        MEHatchGuiClient.IntervalField field = intervalField(event.getScreen());
        if (field != null && field.isMouseOver(event.getMouseX(), event.getMouseY()) && event.getScrollDeltaY() != 0.0D) {
            event.setCanceled(true);
            int step = Screen.hasShiftDown() ? 20 : 1;
            field.adjust(event.getScrollDeltaY() > 0.0D ? step : -step);
            return;
        }
        Slot cell = hoveredEditableCell(event.getScreen());
        if (cell != null) {
            event.setCanceled(true);
            scrollAmount(event.getScreen(), cell, event.getScrollDeltaY());
        }
    }

    private static void onMouseButton(ScreenEvent.MouseButtonPressed.Pre event) {
        // A click anywhere but the interval field ends its edit; vanilla only moves focus between widgets.
        MEHatchGuiClient.IntervalField field = intervalField(event.getScreen());
        if (field != null && field.isFocused() && !field.isMouseOver(event.getMouseX(), event.getMouseY())) {
            event.getScreen().setFocused(null);
        }
        if (Minecraft.getInstance().options.keyPickItem.matchesMouse(event.getButton())) {
            openEditor(event);
        }
    }

    /** While the interval field is edited it takes every key but escape, so typing cannot close the screen or trigger a recipe viewer. */
    private static void onKey(ScreenEvent.KeyPressed.Pre event) {
        MEHatchGuiClient.IntervalField field = intervalField(event.getScreen());
        if (field != null && field.isFocused() && event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) {
            event.setCanceled(true);
            if (event.getKeyCode() == GLFW.GLFW_KEY_ENTER || event.getKeyCode() == GLFW.GLFW_KEY_KP_ENTER) {
                event.getScreen().setFocused(null);
            } else {
                field.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            }
            return;
        }
        if (Minecraft.getInstance().options.keyPickItem.matches(event.getKeyCode(), event.getScanCode())) {
            openEditor(event);
        }
    }

    private static @Nullable MEHatchGuiClient.IntervalField intervalField(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof MEHatchGuiClient.IntervalField field) {
                return field;
            }
        }
        return null;
    }

    private static <E extends ScreenEvent & ICancellableEvent> void openEditor(E event) {
        Slot cell = hoveredEditableCell(event.getScreen());
        if (cell != null) {
            event.setCanceled(true);
            openAmountEditor(event.getScreen(), cell);
        }
    }

    private static @Nullable Slot hoveredEditableCell(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return null;
        }
        Slot hovered = containerScreen.getSlotUnderMouse();
        return isEditableCell(hovered) ? hovered : null;
    }

    private static boolean isEditableCell(Slot slot) {
        return slot instanceof MEHatchConfigSlot configSlot
                && configSlot.isInputChannel()
                && configSlot.displayedConfig() != null;
    }

    private static void scrollAmount(Screen screen, Slot slot, double scrollDelta) {
        if (!isEditableCell(slot) || scrollDelta == 0.0D) {
            return;
        }
        MEHatchConfigSlot configSlot = (MEHatchConfigSlot) slot;
        GenericStack current = configSlot.displayedConfig();
        if (current == null) {
            return;
        }

        boolean isFluid = configSlot.isFluid();
        long target = Math.clamp(
                target(current.amount(), isFluid, Screen.hasShiftDown(), Screen.hasControlDown(), scrollDelta > 0.0D),
                1L, MEHatchConfigComponent.maxAmount(isFluid));
        if (target == current.amount()) {
            return;
        }

        new SetMEHatchAmountPacket(containerId(screen), configSlot.getChannel(), isFluid, target).sendToServer();
    }

    // Only the server can open the editor menu.
    private static void openAmountEditor(Screen screen, Slot slot) {
        if (isEditableCell(slot)) {
            MEHatchConfigSlot configSlot = (MEHatchConfigSlot) slot;
            OpenMEHatchEditorPacket.amount(containerId(screen), configSlot.getChannel(), configSlot.isFluid()).sendToServer();
        }
    }

    private static int containerId(Screen screen) {
        return ((AbstractContainerScreen<?>) screen).getMenu().containerId;
    }

    /** Ctrl alone doubles or halves; otherwise the result snaps to a multiple of the notch, so up from 17 with shift is 32, not 33. */
    static long target(long amount, boolean isFluid, boolean shift, boolean ctrl, boolean increase) {
        if (ctrl && !shift) {
            return increase ? amount * 2L : amount / 2L;
        }

        long step = (isFluid ? 1000L : 1L) * (shift ? (ctrl ? 64L : 16L) : 1L);
        return increase
                ? (amount / step + 1L) * step
                : (amount + step - 1L) / step * step - step;
    }
}
