package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.kvxd.sophisticatedintegrations.backpack.BackpackInventories;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

public final class TerminalRecipeTransfer {
    private TerminalRecipeTransfer() {}

    public static List<NetworkIngredient> ingredients(ServerPlayer player, CraftingTerminalBlockEntity terminal) {
        var ingredients = new ArrayList<>(BackpackCraftingSession.snapshot(terminal));
        var grid = terminal.getCraftingInv();
        for (int slot = 0; slot < grid.getContainerSize(); slot++) add(ingredients, grid.getItem(slot));
        if (new TerminalSession(player, terminal).includesBackpacks()) {
            BackpackInventories.snapshot(player).values().forEach(stack ->
                    ingredients.add(new NetworkIngredient(stack.getStack().copyWithCount(1), stack.getQuantity())));
        }
        return ingredients;
    }

    public static boolean canFill(ServerPlayer player, CraftingTerminalBlockEntity terminal, CraftingRecipe recipe) {
        var available = new ArrayList<>(ingredients(player, terminal));
        available.addAll(InventoryRecipeRouting.localIngredients(player.inventoryMenu));
        return RecipeIngredientPlanner.plan(player, recipe, available).isPresent();
    }

    public static boolean fill(ServerPlayer player, CraftingTerminalBlockEntity terminal, CraftingRecipe recipe,
                               boolean maxTransfer, int action) {
        if (!(player.containerMenu instanceof CraftingTerminalMenu menu) || !menu.stillValid(player)
                || ((IntegrationMenu) menu).sophisticatedIntegrations$getSession().terminal() != terminal) return false;
        var available = new ArrayList<>(ingredients(player, terminal));
        available.addAll(InventoryRecipeRouting.localIngredients(player.inventoryMenu));
        var gridSlots = menu.slots.stream().filter(slot -> slot.container == terminal.getCraftingInv())
                .sorted(java.util.Comparator.comparingInt(net.minecraft.world.inventory.Slot::getContainerSlot)).toList();
        if (gridSlots.size() != 9 || gridSlots.stream().anyMatch(slot -> !slot.mayPickup(player))) return false;
        var plan = RecipeIngredientPlanner.planGrid(player, recipe, available, maxTransfer,
                (index, stack) -> gridSlots.get(index).getMaxStackSize(stack));
        if (plan.isEmpty()) return false;
        return TerminalContext.run(((IntegrationMenu) menu).sophisticatedIntegrations$getSession(), () -> {
            var staged = new ArrayList<ItemStack>();
            for (ItemStack requested : plan.get()) {
                ItemStack extracted = extract(player, terminal, requested);
                staged.add(extracted);
                if (extracted.getCount() != requested.getCount()) {
                    staged.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
                    return false;
                }
            }
            // Stage ingredients before cleanup so a full inventory cannot drop ingredients needed by this recipe.
            player.inventoryMenu.removed(player);
            var grid = terminal.getCraftingInv();
            for (int slot = 0; slot < grid.getContainerSize(); slot++) {
                ItemStack previous = grid.removeItemNoUpdate(slot);
                if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
            }
            for (int slot = 0; slot < staged.size(); slot++) grid.setItem(slot, staged.get(slot));
            menu.onCraftMatrixChanged();
            if (action != 0 && recipe.matches(CraftingInput.of(3, 3, staged), player.level()))
                menu.clicked(menu.getCraftingResultSlot().index, 0, action == 1 ? ClickType.PICKUP : ClickType.QUICK_MOVE, player);
            menu.broadcastChanges();
            return true;
        });
    }

    private static ItemStack extract(ServerPlayer player, CraftingTerminalBlockEntity terminal, ItemStack requested) {
        if (requested.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = requested.copyWithCount(0);
        take(terminal.getCraftingInv(), requested, result);
        takeSlots(player, 1, 5, requested, result);
        ItemStack cursor = player.inventoryMenu.getCarried();
        if (ItemStack.isSameItemSameComponents(cursor, requested)) {
            int amount = Math.min(requested.getCount() - result.getCount(), cursor.getCount());
            result.grow(amount);
            cursor.shrink(amount);
            player.inventoryMenu.setCarried(cursor);
        }
        takeSlots(player, 9, 45, requested, result);
        if (result.getCount() < requested.getCount()) {
            StoredItemStack network = terminal.pullStack(new StoredItemStack(requested), requested.getCount() - result.getCount());
            if (network != null) result.grow((int) network.getQuantity());
        }
        return result;
    }

    private static void take(Container inventory, ItemStack requested, ItemStack result) {
        for (int slot = 0; slot < inventory.getContainerSize() && result.getCount() < requested.getCount(); slot++) {
            if (ItemStack.isSameItemSameComponents(inventory.getItem(slot), requested))
                result.grow(inventory.removeItem(slot, requested.getCount() - result.getCount()).getCount());
        }
    }

    private static void takeSlots(ServerPlayer player, int start, int end, ItemStack requested, ItemStack result) {
        for (int index = start; index < end && result.getCount() < requested.getCount(); index++) {
            var slot = player.inventoryMenu.getSlot(index);
            if (ItemStack.isSameItemSameComponents(slot.getItem(), requested))
                result.grow(slot.remove(requested.getCount() - result.getCount()).getCount());
        }
    }

    private static void add(List<NetworkIngredient> ingredients, ItemStack stack) {
        if (!stack.isEmpty()) ingredients.add(new NetworkIngredient(stack.copyWithCount(1), stack.getCount()));
    }
}
