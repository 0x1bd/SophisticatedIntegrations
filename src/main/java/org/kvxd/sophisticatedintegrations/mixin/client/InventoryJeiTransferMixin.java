package org.kvxd.sophisticatedintegrations.mixin.client;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.kvxd.sophisticatedintegrations.client.InventoryCraftingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.library.transfer.PlayerRecipeTransferHandler")
public abstract class InventoryJeiTransferMixin {
    @Inject(method = "transferRecipe(Lnet/minecraft/world/inventory/InventoryMenu;Lnet/minecraft/world/item/crafting/RecipeHolder;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$openBackpack(InventoryMenu menu, RecipeHolder<?> recipe, IRecipeSlotsView slots,
                                                        Player player, boolean max, boolean transfer, CallbackInfoReturnable<IRecipeTransferError> ci) {
        if (!InventoryCraftingBridge.canRoute(menu, recipe.id())) return;
        if (transfer) InventoryCraftingBridge.route(menu, recipe.id(), max, 0);
        ci.setReturnValue(null);
    }
}
