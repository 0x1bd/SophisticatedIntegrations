package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import org.kvxd.sophisticatedintegrations.network.InventoryBackpackRecipePayload;

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

    public static void route(InventoryMenu menu, ResourceLocation recipeId, boolean maxTransfer, int action) {
        PacketDistributor.sendToServer(new InventoryBackpackRecipePayload(menu.containerId, recipeId, maxTransfer, action));
    }
}
