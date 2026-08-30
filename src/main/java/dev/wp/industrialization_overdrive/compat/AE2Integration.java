package dev.wp.industrialization_overdrive.compat;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import dev.wp.industrialization_overdrive.IOItems;
import dev.wp.industrialization_overdrive.item.MultiblockBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Refactored AE2 integration for ME extraction and simulation.
 */
public class AE2Integration {
    public static final IGridLinkableHandler LINKABLE_HANDLER = new LinkableHandler();

    public static void registerItems() {
        GridLinkables.register(IOItems.TERMINAL.get(), LINKABLE_HANDLER);
    }

    public static class LinkableHandler implements IGridLinkableHandler {
        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof MultiblockBuilder;
        }

        @Override
        public void link(ItemStack itemStack, GlobalPos pos) {
            itemStack.set(AEComponents.WIRELESS_LINK_TARGET, pos);
        }

        @Override
        public void unlink(ItemStack itemStack) {
            itemStack.remove(AEComponents.WIRELESS_LINK_TARGET);
        }
    }

    public static GlobalPos getLinkPos(ItemStack stack) {
        return stack.get(AEComponents.WIRELESS_LINK_TARGET);
    }

    /**
     * Tries to fulfill the required items from the ME system for any missing items in the player's inventory.
     * Returns a map of items and their amounts that can be provided by the ME system.
     */
    public static Map<Item, Integer> getMissingFromME(Player player, Level level, Map<Item, Integer> inventoryCount, Map<Item, Integer> requiredBlocks) {
        Map<Item, Integer> meProvided = new HashMap<>();
        MEStorage networkInv = getMEStorage(player, level);
        if (networkInv == null) return meProvided;

        for (var entry : requiredBlocks.entrySet()) {
            Item item = entry.getKey();
            int required = entry.getValue();
            int inInv = inventoryCount.getOrDefault(item, 0);
            int missing = required - inInv;

            if (missing > 0) {
                ItemStack testStack = new ItemStack(item, missing);
                var key = AEItemKey.of(testStack);
                long extracted = networkInv.extract(key, missing, Actionable.SIMULATE, IActionSource.ofPlayer(player));
                if (extracted > 0) meProvided.put(item, (int) extracted);
            }
        }
        return meProvided;
    }

    /**
     * Actually extract items from the ME system.
     */
    public static void extractFromME(Player player, Level level, Map<Item, Integer> toExtract) {
        MEStorage networkInv = getMEStorage(player, level);
        if (networkInv == null) return;
        for (var entry : toExtract.entrySet()) {
            Item item = entry.getKey();
            int needed = entry.getValue();
            ItemStack testStack = new ItemStack(item, needed);
            var key = AEItemKey.of(testStack);
            networkInv.extract(key, needed, Actionable.MODULATE, IActionSource.ofPlayer(player));
        }
    }

    /**
     * Helper to get MEStorage from the player's linked terminal.
     */
    private static MEStorage getMEStorage(Player player, Level level) {
        ItemStack itemStack = player.getMainHandItem();
        GlobalPos linkPos = itemStack.get(AEComponents.WIRELESS_LINK_TARGET);
        if (linkPos == null || level == null) return null;
        BlockEntity be = level.getBlockEntity(linkPos.pos());
        if (!(be instanceof IWirelessAccessPoint accessPoint)) return null;
        IGrid grid = accessPoint.getGrid();
        if (grid == null) return null;
        return grid.getStorageService().getInventory();
    }
}

