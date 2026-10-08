package org.kvxd.sophisticatedintegrations.backpack;

import com.tom.storagemod.inventory.TerminalItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BackpackInventories {

    public static List<IItemHandler> find(Player player) {
        List<IItemHandler> handlers = new ArrayList<>();
        Set<UUID> contentsIds = new HashSet<>();
        Set<IItemHandler> instances = Collections.newSetFromMap(new IdentityHashMap<>());

        for (BackpackLocation location : PlayerBackpacks.find(player)) {
            var wrapper = BackpackWrapper.fromStack(location.stack());
            var handler = wrapper.getInventoryForInputOutput();
            var contentsId = wrapper.getContentsUuid();
            if (contentsId.isPresent() && !contentsIds.add(contentsId.get())) continue;
            if (instances.add(handler)) handlers.add(handler);
        }

        return handlers;
    }

    public static Map<TerminalItemStack, TerminalItemStack> snapshot(Player player) {
        Map<TerminalItemStack, TerminalItemStack> items = new LinkedHashMap<>();
        for (IItemHandler handler : find(player)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stored = handler.getStackInSlot(slot);
                if (stored.isEmpty()) continue;
                // Output filters can forbid extracting a stack that is otherwise visible in the backpack.
                ItemStack available = handler.extractItem(slot, 1, true);
                if (available.isEmpty() || !ItemStack.isSameItemSameComponents(stored, available)) continue;

                TerminalItemStack entry = new TerminalItemStack(stored.copyWithCount(1), stored.getCount());
                items.merge(entry, entry, TerminalItemStack::merge);
            }
        }
        return items;
    }

    public static ItemStack extract(Player player, ItemStack template, int amount) {
        if (template.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = template.copyWithCount(0);
        for (IItemHandler handler : find(player)) {
            for (int slot = 0; slot < handler.getSlots() && result.getCount() < amount; slot++) {
                if (!ItemStack.isSameItemSameComponents(template, handler.getStackInSlot(slot))) continue;
                ItemStack extracted = handler.extractItem(slot, amount - result.getCount(), false);

                if (!extracted.isEmpty()) result.grow(extracted.getCount());
            }
            if (result.getCount() >= amount) break;
        }

        if (!result.isEmpty()) player.getInventory().setChanged();

        return result;
    }

    public static ItemStack insert(Player player, ItemStack input) {
        if (input.isEmpty() || input.getItem() instanceof BackpackItem) return input.copy();

        ItemStack remainder = input.copy();
        List<IItemHandler> handlers = find(player);

        for (boolean emptySlots : new boolean[]{false, true}) {
            for (IItemHandler handler : handlers) {
                for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
                    if (handler.getStackInSlot(slot).isEmpty() != emptySlots) continue;
                    remainder = handler.insertItem(slot, remainder, false);
                }
            }
        }

        if (remainder.getCount() != input.getCount()) player.getInventory().setChanged();
        return remainder;
    }

    private BackpackInventories() {
    }
}
