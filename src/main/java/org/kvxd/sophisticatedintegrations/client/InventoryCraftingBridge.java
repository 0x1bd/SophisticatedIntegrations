package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.crafting.RecipeIngredientPlanner;
import org.kvxd.sophisticatedintegrations.network.InventoryBackpackRecipePayload;

import java.util.ArrayList;
import java.util.List;

public final class InventoryCraftingBridge {
    private InventoryCraftingBridge() {
    }

    public static boolean canRoute(AbstractContainerMenu menu, ResourceLocation recipeId) {
        var player = Minecraft.getInstance().player;
        if (player == null || recipeId == null || menu != player.inventoryMenu || !(menu instanceof InventoryMenu)
                || player.containerMenu != menu || !((InventoryCraftingAccess) menu).sophisticatedIntegrations$hasCraftingBackpack())
            return false;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        return holder != null && holder.value() instanceof CraftingRecipe recipe && InventoryRecipeRouting.needsBackpack(recipe);
    }

    public static boolean canRoute(ResourceLocation recipeId) {
        var player = Minecraft.getInstance().player;
        return player != null && canRoute(player.containerMenu, recipeId);
    }

    public static List<NetworkIngredient> ingredients(InventoryMenu menu) {
        return ((InventoryCraftingAccess) menu).sophisticatedIntegrations$inventoryCraftingSession().ingredients();
    }

    public static boolean canFill(InventoryMenu menu, ResourceLocation recipeId) {
        if (!canRoute(menu, recipeId)) return false;
        var player = Minecraft.getInstance().player;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElseThrow();
        List<NetworkIngredient> available = new ArrayList<>(ingredients(menu));
        for (int slot = 1; slot <= 4; slot++) add(available, menu.getSlot(slot).getItem());
        for (int slot = 9; slot < 45; slot++) add(available, menu.getSlot(slot).getItem());
        add(available, menu.getCarried());
        return RecipeIngredientPlanner.plan(player, (CraftingRecipe) holder.value(), available).isPresent();
    }

    private static void add(List<NetworkIngredient> available, ItemStack stack) {
        if (!stack.isEmpty()) available.add(new NetworkIngredient(stack.copyWithCount(1), stack.getCount()));
    }

    public static void route(InventoryMenu menu, ResourceLocation recipeId, boolean maxTransfer, int action) {
        PacketDistributor.sendToServer(new InventoryBackpackRecipePayload(menu.containerId, recipeId, maxTransfer, action));
    }
}
