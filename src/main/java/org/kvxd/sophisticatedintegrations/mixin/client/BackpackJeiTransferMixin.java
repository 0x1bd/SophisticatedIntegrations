package org.kvxd.sophisticatedintegrations.mixin.client;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.client.BackpackCraftingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase")
public abstract class BackpackJeiTransferMixin {
    @Inject(method = "transferRecipe(Lnet/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase;Lnet/minecraft/world/item/crafting/RecipeHolder;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$linkedRecipe(StorageContainerMenuBase<?> menu, RecipeHolder<?> recipe,
                                                        IRecipeSlotsView slots, Player player, boolean max, boolean transfer, CallbackInfoReturnable<IRecipeTransferError> ci) {
        if (!BackpackCraftingBridge.canFill(menu, recipe.id())) return;
        if (transfer) BackpackCraftingBridge.fill(menu, recipe.id(), max, 0);
        ci.setReturnValue(null);
    }
}
