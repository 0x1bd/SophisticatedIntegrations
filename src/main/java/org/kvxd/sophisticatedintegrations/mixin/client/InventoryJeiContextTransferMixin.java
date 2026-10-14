package org.kvxd.sophisticatedintegrations.mixin.client;

import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.kvxd.sophisticatedintegrations.client.InventoryCraftingBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.library.transfer.PlayerRecipeTransferHandler")
public abstract class InventoryJeiContextTransferMixin {
    @Shadow @Final private IRecipeTransferHandlerHelper handlerHelper;

    @Inject(method = "transferRecipe(Lmezz/jei/api/recipe/transfer/IRecipeTransferContext;Z)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void sophisticatedIntegrations$openBackpack(IRecipeTransferContext<?, ?> context, boolean transfer,
                                                        CallbackInfoReturnable<IRecipeTransferError> ci) {
        if (!(context.getContainer() instanceof InventoryMenu menu)
                || !(context.getRecipe() instanceof RecipeHolder<?> recipe)
                || !InventoryCraftingBridge.canRoute(menu, recipe.id())
                || !InventoryCraftingBridge.shouldOpen(menu, recipe.id())) return;
        if (!InventoryCraftingBridge.canFill(menu, recipe.id())) {
            ci.setReturnValue(handlerHelper.createUserErrorWithTooltip(Component.translatable("gui.sophisticatedintegrations.missing_ingredients")));
            return;
        }
        if (transfer) {
            InventoryCraftingBridge.route(menu, recipe.id(), context.isMaxTransfer(), 0);
            context.completeRecipeTransfer(RecipeTransferResult.SUCCESS);
        }
        ci.setReturnValue(null);
    }
}
