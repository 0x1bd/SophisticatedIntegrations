package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.crafting.RecipeIngredientPlanner;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class InventoryRecipePreview {

    private InventoryMenu menu;
    private List<NetworkIngredient> network;
    private List<NetworkIngredient> local = List.of();
    private List<NetworkIngredient> nativeLocal = List.of();
    private List<NetworkIngredient> combined = List.of();
    private final Map<CraftingRecipe, Boolean> craftable = new IdentityHashMap<>();
    private final Map<CraftingRecipe, Boolean> locallyCraftable = new IdentityHashMap<>();
    private InventoryRecipePreview alternativePreview;

    public boolean canFillAny(Player player, InventoryMenu menu, CraftingRecipe recipe,
                              List<NetworkIngredient> preferred, List<NetworkIngredient> alternative) {
        if (canFill(player, menu, recipe, preferred, false)) return true;
        if (alternative.isEmpty()) return false;
        if (alternativePreview == null) alternativePreview = new InventoryRecipePreview();
        return alternativePreview.canFill(player, menu, recipe, alternative, false);
    }

    public boolean canFill(Player player, InventoryMenu menu, CraftingRecipe recipe, List<NetworkIngredient> network, boolean localOnly) {
        List<NetworkIngredient> latestLocal = InventoryRecipeRouting.localIngredients(menu);
        List<NetworkIngredient> latestNativeLocal = latestLocal.subList(0,
                latestLocal.size() - (menu.getCarried().isEmpty() ? 0 : 1));
        boolean localChanged = this.menu != menu || !sameLocal(local, latestLocal) || !sameLocal(nativeLocal, latestNativeLocal);
        if (localChanged) {
            this.menu = menu;
            local = latestLocal;
            nativeLocal = latestNativeLocal;
            locallyCraftable.clear();
        }
        if (localChanged || this.network != network) {
            this.network = network;
            combined = new ArrayList<>(network);
            combined.addAll(local);
            craftable.clear();
        }
        return (localOnly ? locallyCraftable : craftable).computeIfAbsent(recipe,
                value -> RecipeIngredientPlanner.plan(player, value, localOnly ? nativeLocal : combined).isPresent());
    }

    private static boolean sameLocal(List<NetworkIngredient> first, List<NetworkIngredient> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).quantity() != second.get(i).quantity()
                    || !ItemStack.isSameItemSameComponents(first.get(i).template(), second.get(i).template()))
                return false;
        }
        return true;
    }
}
