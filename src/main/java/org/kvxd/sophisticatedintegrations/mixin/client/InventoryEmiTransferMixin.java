package org.kvxd.sophisticatedintegrations.mixin.client;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.InventoryMenu;
import org.kvxd.sophisticatedintegrations.client.InventoryCraftingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "dev.emi.emi.handler.InventoryRecipeHandler")
public abstract class InventoryEmiTransferMixin implements StandardRecipeHandler<InventoryMenu> {
    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipe(EmiRecipe recipe, CallbackInfoReturnable<Boolean> ci) {
        if (InventoryCraftingBridge.canRoute(recipe.getId())) ci.setReturnValue(true);
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$canOpen(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context, CallbackInfoReturnable<Boolean> ci) {
        if (InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId())) ci.setReturnValue(true);
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipeTooltip(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context,
                                                              CallbackInfoReturnable<List<ClientTooltipComponent>> ci) {
        if (InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId())) ci.setReturnValue(List.of());
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        if (!InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId()))
            return StandardRecipeHandler.super.craft(recipe, context);
        int action = switch (context.getDestination()) {
            case NONE -> 0;
            case CURSOR -> 1;
            case INVENTORY -> 2;
        };
        InventoryCraftingBridge.route(context.getScreenHandler(), recipe.getId(), context.getAmount() > 1, action);
        return true;
    }
}
