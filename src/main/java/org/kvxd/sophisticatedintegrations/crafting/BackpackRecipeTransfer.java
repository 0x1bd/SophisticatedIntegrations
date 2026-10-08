package org.kvxd.sophisticatedintegrations.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;

import java.util.List;

public final class BackpackRecipeTransfer {
    private BackpackRecipeTransfer() {}

    public static void fill(Player player, int menuId, ResourceLocation recipeId, boolean maxTransfer, int action) {
        if (player.level().isClientSide || player.isSpectator() || action < 0 || action > 2
                || !(player.containerMenu instanceof BackpackContainer menu) || menu.containerId != menuId
                || !menu.stillValid(player)) return;
        var session = ((BackpackCraftingMenu) menu).sophisticatedIntegrations$craftingSession();
        var terminal = session.terminal();
        if (terminal == null) return;
        var holder = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)) return;
        var crafting = menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).orElse(null);
        if (!(crafting instanceof CraftingUpgradeContainer upgrade)) return;
        var plan = RecipeIngredientPlanner.plan(player, menu, recipe, BackpackCraftingSession.snapshot(terminal));
        if (plan.isEmpty()) return;
        List<Slot> grid = upgrade.getRecipeSlots();
        if (grid.size() != 9 || grid.stream().anyMatch(slot -> !slot.mayPickup(player))) return;
        List<Integer> inventory = menu.slots.stream().filter(slot -> slot.mayPickup(player)).map(slot -> slot.index).toList();
        CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, recipeId, RecipeType.CRAFTING,
                plan.get(), grid.stream().map(slot -> slot.index).toList(), inventory, maxTransfer);
        if (action != 0 && recipe.matches(CraftingInput.of(3, 3, grid.stream().map(Slot::getItem).toList()), player.level())) {
            Slot output = upgrade.getSlots().getLast();
            menu.clicked(output.index, 0, action == 1 ? ClickType.PICKUP : ClickType.QUICK_MOVE, player);
        }
    }
}
