package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.crafting.RecipeIngredientPlanner;
import org.kvxd.sophisticatedintegrations.network.BackpackRecipePayload;

import java.util.List;

public final class BackpackCraftingBridge {
    private BackpackCraftingBridge() {
    }

    public static List<NetworkIngredient> ingredients(StorageContainerMenuBase<?> menu) {
        return menu instanceof BackpackContainer && menu instanceof BackpackCraftingMenu integration
                ? integration.sophisticatedIntegrations$craftingSession().ingredients() : List.of();
    }

    public static boolean canFill(StorageContainerMenuBase<?> menu, ResourceLocation recipeId) {
        var player = Minecraft.getInstance().player;
        if (player == null || recipeId == null || !(menu instanceof BackpackContainer)
                || !((BackpackCraftingMenu) menu).sophisticatedIntegrations$craftingSession().linked()) return false;
        var recipe = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        return recipe != null && recipe.value() instanceof CraftingRecipe crafting
                && menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).isPresent()
                && RecipeIngredientPlanner.plan(player, menu, crafting, ingredients(menu)).isPresent();
    }

    public static void fill(StorageContainerMenuBase<?> menu, ResourceLocation recipeId, boolean maxTransfer, int action) {
        BackpackCraftingTabs.open(menu);
        PacketDistributor.sendToServer(new BackpackRecipePayload(menu.containerId, recipeId, maxTransfer, action));
    }
}
