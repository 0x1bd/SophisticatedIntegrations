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
import org.kvxd.sophisticatedintegrations.client.emi.InventoryEmiSnapshot;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

@Mixin(targets = "dev.emi.emi.handler.InventoryRecipeHandler")
public abstract class InventoryEmiTransferMixin implements StandardRecipeHandler<InventoryMenu> {
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$lastCraftableInventory;
    @Unique
    private InventoryMenu sophisticatedIntegrations$lastCraftableMenu;
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$localCraftableInventory;
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$lastAlternativeContextInventory;
    @Unique
    private InventoryMenu sophisticatedIntegrations$lastAlternativeMenu;
    @Unique
    private List<NetworkIngredient> sophisticatedIntegrations$lastAlternativeIngredients;
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$alternativeCraftableInventory;
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$lastPreferredContextInventory;
    @Unique
    private InventoryMenu sophisticatedIntegrations$lastPreferredMenu;
    @Unique
    private List<NetworkIngredient> sophisticatedIntegrations$lastPreferredIngredients;
    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$preferredCraftableInventory;

    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipe(EmiRecipe recipe, CallbackInfoReturnable<Boolean> ci) {
        if (InventoryCraftingBridge.canRoute(recipe.getId())) ci.setReturnValue(true);
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$canOpen(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context, CallbackInfoReturnable<Boolean> ci) {
        boolean backpack = InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId());
        EmiPlayerInventory inventory = context.getType() == EmiCraftContext.Type.CRAFTABLE
                ? backpack ? sophisticatedIntegrations$preferredSnapshot(context) : sophisticatedIntegrations$localSnapshot(context)
                : sophisticatedIntegrations$inventory(context.getScreen(), backpack);
        if (!backpack && !supportsRecipe(recipe)) {
            ci.setReturnValue(false);
            return;
        }
        boolean craftable = inventory.canCraft(recipe);
        if (!craftable && backpack) {
            var alternative = sophisticatedIntegrations$alternativeSnapshot(context);
            craftable = alternative != null && alternative.canCraft(recipe);
        }
        ci.setReturnValue(craftable);
    }

    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$preferredSnapshot(EmiCraftContext<InventoryMenu> context) {
        if (context.getInventory() instanceof InventoryEmiSnapshot snapshot) return snapshot.preferred();
        var ingredients = InventoryCraftingBridge.ingredients(context.getScreenHandler());
        if (sophisticatedIntegrations$lastPreferredContextInventory != context.getInventory()
                || sophisticatedIntegrations$lastPreferredMenu != context.getScreenHandler()
                || sophisticatedIntegrations$lastPreferredIngredients != ingredients) {
            sophisticatedIntegrations$lastPreferredContextInventory = context.getInventory();
            sophisticatedIntegrations$lastPreferredMenu = context.getScreenHandler();
            sophisticatedIntegrations$lastPreferredIngredients = ingredients;
            sophisticatedIntegrations$preferredCraftableInventory = sophisticatedIntegrations$inventory(context.getScreen(), ingredients);
        }
        return sophisticatedIntegrations$preferredCraftableInventory;
    }

    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$alternativeSnapshot(EmiCraftContext<InventoryMenu> context) {
        if (context.getType() == EmiCraftContext.Type.CRAFTABLE && context.getInventory() instanceof InventoryEmiSnapshot snapshot)
            return snapshot.alternative();
        var ingredients = InventoryCraftingBridge.alternativeIngredients(context.getScreenHandler());
        if (ingredients.isEmpty()) return null;
        if (sophisticatedIntegrations$lastAlternativeContextInventory != context.getInventory()
                || sophisticatedIntegrations$lastAlternativeMenu != context.getScreenHandler()
                || sophisticatedIntegrations$lastAlternativeIngredients != ingredients) {
            sophisticatedIntegrations$lastAlternativeContextInventory = context.getInventory();
            sophisticatedIntegrations$lastAlternativeMenu = context.getScreenHandler();
            sophisticatedIntegrations$lastAlternativeIngredients = ingredients;
            sophisticatedIntegrations$alternativeCraftableInventory = sophisticatedIntegrations$inventory(context.getScreen(), ingredients);
        }
        return sophisticatedIntegrations$alternativeCraftableInventory;
    }

    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$localSnapshot(EmiCraftContext<InventoryMenu> context) {
        if (sophisticatedIntegrations$lastCraftableInventory != context.getInventory()
                || sophisticatedIntegrations$lastCraftableMenu != context.getScreenHandler()) {
            sophisticatedIntegrations$lastCraftableInventory = context.getInventory();
            sophisticatedIntegrations$lastCraftableMenu = context.getScreenHandler();
            sophisticatedIntegrations$localCraftableInventory = sophisticatedIntegrations$inventory(context.getScreen(), false);
        }
        return sophisticatedIntegrations$localCraftableInventory;
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$largeRecipeTooltip(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context,
                                                              CallbackInfoReturnable<List<ClientTooltipComponent>> ci) {
        if (InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId()))
            ci.setReturnValue(StandardRecipeHandler.super.getTooltip(recipe, context));
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<InventoryMenu> screen) {
        var preferred = sophisticatedIntegrations$inventory(screen, true);
        var alternative = InventoryCraftingBridge.alternativeIngredients(screen.getMenu());
        return new InventoryEmiSnapshot(preferred,
                alternative.isEmpty() ? null : sophisticatedIntegrations$inventory(screen, alternative));
    }

    private EmiPlayerInventory sophisticatedIntegrations$inventory(AbstractContainerScreen<InventoryMenu> screen, boolean backpacks) {
        return sophisticatedIntegrations$inventory(screen, backpacks ? InventoryCraftingBridge.ingredients(screen.getMenu()) : List.of());
    }

    @Unique
    private EmiPlayerInventory sophisticatedIntegrations$inventory(AbstractContainerScreen<InventoryMenu> screen, List<NetworkIngredient> ingredients) {
        var items = new ArrayList<>(getInputSources(screen.getMenu()).stream().map(slot -> EmiStack.of(slot.getItem())).toList());
        ingredients.forEach(item -> items.add(EmiStack.of(item.template(), item.quantity())));
        return new EmiPlayerInventory(items);
    }

    private EmiCraftContext<InventoryMenu> sophisticatedIntegrations$freshContext(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        boolean routed = InventoryCraftingBridge.canRoute(context.getScreenHandler(), recipe.getId());
        var inventory = sophisticatedIntegrations$inventory(context.getScreen(), routed);
        if (routed && !inventory.canCraft(recipe)) {
            var alternative = sophisticatedIntegrations$alternativeSnapshot(context);
            if (alternative != null && alternative.canCraft(recipe)) inventory = alternative;
        }
        return new EmiCraftContext<>(context.getScreen(), inventory, context.getType(), context.getDestination(), context.getAmount());
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context, List<Widget> widgets, GuiGraphics draw) {
        StandardRecipeHandler.super.render(recipe, sophisticatedIntegrations$freshContext(recipe, context), widgets, draw);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        if (!InventoryCraftingBridge.shouldOpen(context.getScreenHandler(), recipe.getId()))
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
