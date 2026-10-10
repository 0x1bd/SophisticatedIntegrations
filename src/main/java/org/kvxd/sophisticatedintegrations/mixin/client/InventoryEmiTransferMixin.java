package org.kvxd.sophisticatedintegrations.mixin.client;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.InventoryMenu;
import org.kvxd.sophisticatedintegrations.client.InventoryCraftingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

@Mixin(targets = "dev.emi.emi.handler.InventoryRecipeHandler")
public abstract class InventoryEmiTransferMixin implements StandardRecipeHandler<InventoryMenu> {
    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipe(EmiRecipe recipe, CallbackInfoReturnable<Boolean> ci) {
        if (InventoryCraftingBridge.canRoute(recipe.getId())) ci.setReturnValue(true);
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$canOpen(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context, CallbackInfoReturnable<Boolean> ci) {
        if (InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId()))
            ci.setReturnValue(InventoryCraftingBridge.canFill(context.getScreenHandler(), recipe.getId()));
        else
            ci.setReturnValue(supportsRecipe(recipe) && sophisticatedIntegrations$inventory(context.getScreen(), false).canCraft(recipe));
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipeTooltip(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context,
                                                              CallbackInfoReturnable<List<ClientTooltipComponent>> ci) {
        if (InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId()))
            ci.setReturnValue(StandardRecipeHandler.super.getTooltip(recipe, sophisticatedIntegrations$freshContext(recipe, context)));
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<InventoryMenu> screen) {
        return sophisticatedIntegrations$inventory(screen, true);
    }

    private EmiPlayerInventory sophisticatedIntegrations$inventory(AbstractContainerScreen<InventoryMenu> screen, boolean backpacks) {
        var items = new ArrayList<>(getInputSources(screen.getMenu()).stream().map(slot -> EmiStack.of(slot.getItem())).toList());
        if (backpacks) InventoryCraftingBridge.ingredients(screen.getMenu())
                .forEach(item -> items.add(EmiStack.of(item.template(), item.quantity())));
        return new EmiPlayerInventory(items);
    }

    private EmiCraftContext<InventoryMenu> sophisticatedIntegrations$freshContext(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        return new EmiCraftContext<>(context.getScreen(), sophisticatedIntegrations$inventory(context.getScreen(),
                InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId())), context.getType(), context.getDestination(), context.getAmount());
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context, List<Widget> widgets, GuiGraphics draw) {
        StandardRecipeHandler.super.render(recipe, sophisticatedIntegrations$freshContext(recipe, context), widgets, draw);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        if (!InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId()))
            return StandardRecipeHandler.super.craft(recipe, context);
        if (!InventoryCraftingBridge.canFill(context.getScreenHandler(), recipe.getId())) return false;
        int action = switch (context.getDestination()) {
            case NONE -> 0;
            case CURSOR -> 1;
            case INVENTORY -> 2;
        };
        InventoryCraftingBridge.route(context.getScreenHandler(), recipe.getId(), context.getAmount() > 1, action);
        return true;
    }
}
