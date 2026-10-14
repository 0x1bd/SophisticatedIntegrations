package org.kvxd.sophisticatedintegrations.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;
import org.kvxd.sophisticatedintegrations.network.BackpackCraftingTabPayload;

import java.util.ArrayList;
import java.util.List;

public final class InventoryRecipeRouting {
    private InventoryRecipeRouting() {
    }

    public static boolean needsBackpack(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() <= 3 && shaped.getHeight() <= 3 && (shaped.getWidth() > 2 || shaped.getHeight() > 2);
        }
        long ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).count();
        return ingredients > 4 && ingredients <= 9;
    }

    public static boolean supportsBackpack(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped && (shaped.getWidth() > 3 || shaped.getHeight() > 3)) return false;
        return !recipe.getIngredients().isEmpty() && recipe.getIngredients().size() <= 9;
    }

    public static List<NetworkIngredient> localIngredients(InventoryMenu menu) {
        return localIngredients(menu, true);
    }

    public static List<NetworkIngredient> localIngredients(InventoryMenu menu, boolean includeCursor) {
        var available = new ArrayList<NetworkIngredient>();
        for (int slot = 1; slot <= 4; slot++) add(available, menu.getSlot(slot).getItem());
        for (int slot = 9; slot < 45; slot++) add(available, menu.getSlot(slot).getItem());
        if (includeCursor) add(available, menu.getCarried());
        return available;
    }

    private static void add(List<NetworkIngredient> available, ItemStack stack) {
        if (!stack.isEmpty()) available.add(new NetworkIngredient(stack.copyWithCount(1), stack.getCount()));
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) broadcast(player);
    }

    public static void broadcast(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) return;
        ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession().broadcast(player);
    }

    public static void openAndFill(ServerPlayer player, int menuId, ResourceLocation recipeId, boolean maxTransfer, int action) {
        if (player.isSpectator() || !IntegrationConfig.AUTO_OPEN_CRAFTING.get() || action < 0 || action > 2
                || player.containerMenu != player.inventoryMenu || player.inventoryMenu.containerId != menuId) return;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe) || !supportsBackpack(recipe)) return;
        if (!needsBackpack(recipe) && RecipeIngredientPlanner.plan(player, recipe, localIngredients(player.inventoryMenu, false)).isPresent())
            return;
        var terminal = LinkedTerminalAccess.findCrafting(player);
        if (terminal != null && TerminalRecipeTransfer.canFill(player, terminal, recipe)) {
            if (!LinkedTerminalAccess.openCrafting(player, terminal)) return;
            TerminalRecipeTransfer.fill(player, terminal, recipe, maxTransfer, action);
            return;
        }
        var location = CraftingBackpackFinder.find(player).orElse(null);
        if (location == null) return;
        var available = new ArrayList<>(InventoryCraftingSession.backpackIngredients(player, location));
        available.addAll(localIngredients(player.inventoryMenu));
        if (RecipeIngredientPlanner.plan(player, recipe, available).isEmpty()) return;
        var context = location.context();
        // Use the normal inventory-close cleanup for its cursor and 2x2 crafting ingredients
        player.inventoryMenu.removed(player);
        player.openMenu(new SophisticatedMenuProvider((id, inventory, owner) -> {
            BackpackContainer menu = new BackpackContainer(id, owner, context);
            BackpackRecipeTransfer.openCraftingTab(menu);
            return menu;
        }, context.getDisplayName(player), false), buffer -> context.toBuffer(buffer, player));
        if (!(player.containerMenu instanceof BackpackContainer menu)) return;
        BackpackRecipeTransfer.fill(player, menu.containerId, recipeId, maxTransfer, action);
        menu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BackpackCraftingTabPayload(menu.containerId));
    }
}
