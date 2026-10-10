package org.kvxd.sophisticatedintegrations.mixin.client;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.kvxd.sophisticatedintegrations.client.InventoryCraftingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "me.shedaniel.rei.plugin.autocrafting.InventoryCraftingTransferHandler")
public abstract class InventoryReiTransferMixin {
    @Inject(method = "checkApplicable", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipe(TransferHandler.Context context, CallbackInfoReturnable<TransferHandler.ApplicabilityResult> ci) {
        if (InventoryCraftingBridge.canRoute(context.getMenu(), context.getDisplay().getDisplayLocation().orElse(null))) {
            ci.setReturnValue(TransferHandler.ApplicabilityResult.createApplicable());
        }
    }

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$openBackpack(TransferHandler.Context context, CallbackInfoReturnable<TransferHandler.Result> ci) {
        var recipeId = context.getDisplay().getDisplayLocation().orElse(null);
        if (!(context.getMenu() instanceof InventoryMenu menu) || !InventoryCraftingBridge.canRoute(menu, recipeId))
            return;
        if (!InventoryCraftingBridge.canFill(menu, recipeId)) {
            ci.setReturnValue(TransferHandler.Result.createFailed(Component.translatable("gui.sophisticatedintegrations.missing_ingredients")));
            return;
        }
        if (context.isActuallyCrafting()) InventoryCraftingBridge.route(menu, recipeId, context.isStackedCrafting(), 0);
        ci.setReturnValue(TransferHandler.Result.createSuccessful());
    }
}
