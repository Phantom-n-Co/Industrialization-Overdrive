package dev.wp.industrialization_overdrive.item;

import aztech.modern_industrialization.machines.components.UpgradeComponent;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class UpgradeHolder extends Item {
    static final int MAX_STORED = 4096;

    public UpgradeHolder(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack thisStack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (other.isEmpty()) return false;
        if (UpgradeComponent.getExtraEu(other.getItem()) <= 0) return false;

        IOComponents.UpgradeHolderContents contents = thisStack.get(IOComponents.UPGRADE_HOLDER_CONTENTS.get());

        if (contents != null && contents.upgradeType() != other.getItem()) return false;

        int currentCount = contents != null ? contents.count() : 0;
        int space = MAX_STORED - currentCount;
        if (space <= 0) return false;

        int toAdd = Math.min(other.getCount(), space);
        thisStack.set(IOComponents.UPGRADE_HOLDER_CONTENTS.get(),
                new IOComponents.UpgradeHolderContents(other.getItem(), currentCount + toAdd));
        slot.set(thisStack);
        other.shrink(toAdd);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IOComponents.UpgradeHolderContents contents = stack.get(IOComponents.UPGRADE_HOLDER_CONTENTS.get());
        if (contents == null || contents.count() <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                giveContentsToPlayer(stack, contents, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide()) {
            int space = MAX_STORED - contents.count();
            int added = 0;
            for (ItemStack inventoryStack : player.getInventory().items) {
                if (space <= 0) break;
                if (inventoryStack.getItem() != contents.upgradeType()) continue;
                int toAdd = Math.min(inventoryStack.getCount(), space);
                inventoryStack.shrink(toAdd);
                added += toAdd;
                space -= toAdd;
            }
            if (added > 0) {
                stack.set(IOComponents.UPGRADE_HOLDER_CONTENTS.get(),
                        new IOComponents.UpgradeHolderContents(contents.upgradeType(), contents.count() + added));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void giveContentsToPlayer(ItemStack stack, IOComponents.UpgradeHolderContents contents, Player player) {
        int remaining = contents.count();
        Inventory inv = player.getInventory();
        while (remaining > 0) {
            int give = Math.min(remaining, contents.upgradeType().getDefaultMaxStackSize());
            ItemStack giveStack = new ItemStack(contents.upgradeType(), give);
            if (!inv.add(giveStack)) {
                player.drop(giveStack, false);
            }
            remaining -= give;
        }
        stack.remove(IOComponents.UPGRADE_HOLDER_CONTENTS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @NonNull TooltipContext ctx, List<Component> tooltip, @NonNull TooltipFlag flag) {
        IOComponents.UpgradeHolderContents contents = stack.get(IOComponents.UPGRADE_HOLDER_CONTENTS.get());

        tooltip.add(IO.text().upgradeHolderInfo1());
        tooltip.add(IO.text().upgradeHolderInfo2());

        if (contents == null || contents.count() <= 0) {
            tooltip.add(IO.text().empty());
        } else {
            tooltip.add(IO.text().contains(contents.count(), contents.upgradeType().getDescription().getString()));
            tooltip.add(IO.text().totalEuForX(UpgradeComponent.getExtraEu(contents.upgradeType()) * contents.count()));
        }
    }
}
