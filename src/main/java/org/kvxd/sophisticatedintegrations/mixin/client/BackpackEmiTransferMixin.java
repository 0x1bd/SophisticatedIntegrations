package org.kvxd.sophisticatedintegrations.mixin.client;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.client.BackpackCraftingBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiGridMenuInfo")
public abstract class BackpackEmiTransferMixin {
    @Shadow
    @Final
    private RecipeType<?> recipeType;

    @Inject(method = "getInventory", at = @At("RETURN"), cancellable = true)
    private void sophisticatedIntegrations$networkInventory(AbstractContainerScreen<? extends StorageContainerMenuBase<?>> screen,
                                                            CallbackInfoReturnable<EmiPlayerInventory> ci) {
        if (recipeType != RecipeType.CRAFTING) return;
        var ingredients = BackpackCraftingBridge.ingredients(screen.getMenu());
        if (ingredients.isEmpty()) return;
        var items = new ArrayList<>(ci.getReturnValue().inventory.values());
        ingredients.forEach(item -> items.add(EmiStack.of(item.template(), item.quantity())));
        ci.setReturnValue(new EmiPlayerInventory(items));
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$canCraft(EmiRecipe recipe, EmiCraftContext<? extends StorageContainerMenuBase<?>> context,
                                                    CallbackInfoReturnable<Boolean> ci) {
        if (recipeType == RecipeType.CRAFTING && VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())
                && BackpackCraftingBridge.canFill(context.getScreenHandler(), recipe.getId())) ci.setReturnValue(true);
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$linkedRecipe(EmiRecipe recipe, EmiCraftContext<? extends StorageContainerMenuBase<?>> context,
                                                        CallbackInfoReturnable<Boolean> ci) {
        if (recipeType != RecipeType.CRAFTING || !VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())
                || !BackpackCraftingBridge.canFill(context.getScreenHandler(), recipe.getId())) return;
        int action = switch (context.getDestination()) {
            case NONE -> 0;
            case CURSOR -> 1;
            case INVENTORY -> 2;
        };
        BackpackCraftingBridge.fill(context.getScreenHandler(), recipe.getId(), context.getAmount() > 1, action);
        ci.setReturnValue(true);
    }
}
