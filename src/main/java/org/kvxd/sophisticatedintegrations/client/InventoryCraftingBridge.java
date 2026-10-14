package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.network.InventoryBackpackRecipePayload;

import java.util.List;

public final class InventoryCraftingBridge {

    private static final InventoryRecipePreview PREVIEW = new InventoryRecipePreview();

    private InventoryCraftingBridge() {
    }

    public static boolean canRoute(AbstractContainerMenu menu, ResourceLocation recipeId) {
        var player = Minecraft.getInstance().player;
        if (player == null || recipeId == null || menu != player.inventoryMenu || !(menu instanceof InventoryMenu)
                || player.containerMenu != menu || !((InventoryCraftingAccess) menu).sophisticatedIntegrations$hasCraftingBackpack())
            return false;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        return holder != null && holder.value() instanceof CraftingRecipe recipe && InventoryRecipeRouting.supportsBackpack(recipe);
    }

    public static boolean canRoute(ResourceLocation recipeId) {
        var player = Minecraft.getInstance().player;
        return player != null && canRoute(player.containerMenu, recipeId);
    }

    public static List<NetworkIngredient> ingredients(InventoryMenu menu) {
        return ((InventoryCraftingAccess) menu).sophisticatedIntegrations$inventoryCraftingSession().ingredients();
    }

    public static List<NetworkIngredient> alternativeIngredients(InventoryMenu menu) {
        return ((InventoryCraftingAccess) menu).sophisticatedIntegrations$inventoryCraftingSession().alternativeIngredients();
    }

    public static boolean canFill(InventoryMenu menu, ResourceLocation recipeId) {
        if (!canRoute(menu, recipeId)) return false;
        var player = Minecraft.getInstance().player;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElseThrow();
        return PREVIEW.canFillAny(player, menu, (CraftingRecipe) holder.value(), ingredients(menu), alternativeIngredients(menu));
    }

    public static boolean shouldOpen(InventoryMenu menu, ResourceLocation recipeId) {
        if (!canRoute(menu, recipeId)) return false;
        var player = Minecraft.getInstance().player;
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(recipeId).orElseThrow().value();
        return InventoryRecipeRouting.needsBackpack(recipe)
                || !PREVIEW.canFill(player, menu, recipe, ingredients(menu), true);
    }

    public static void route(InventoryMenu menu, ResourceLocation recipeId, boolean maxTransfer, int action) {
        PacketDistributor.sendToServer(new InventoryBackpackRecipePayload(menu.containerId, recipeId, maxTransfer, action));
    }
}
