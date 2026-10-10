package org.kvxd.sophisticatedintegrations.mixin.client;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.client.BackpackCraftingBridge;
import org.kvxd.sophisticatedintegrations.crafting.RecipeIngredientPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiGridMenuInfo")
public abstract class BackpackEmiTransferMixin implements StandardRecipeHandler<StorageContainerMenuBase<?>> {
    @Shadow
    @Final
    private RecipeType<?> recipeType;

    @Inject(method = "getInventory", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$networkInventory(AbstractContainerScreen<? extends StorageContainerMenuBase<?>> screen,
                                                            CallbackInfoReturnable<EmiPlayerInventory> ci) {
        if (recipeType == RecipeType.CRAFTING && screen.getMenu() instanceof BackpackContainer)
            ci.setReturnValue(sophisticatedIntegrations$inventory(screen.getMenu()));
    }

    private EmiPlayerInventory sophisticatedIntegrations$inventory(StorageContainerMenuBase<?> menu) {
        var items = new ArrayList<EmiStack>();
        RecipeIngredientPlanner.inventory(Minecraft.getInstance().player, menu)
                .forEach(item -> items.add(EmiStack.of(item.template(), item.quantity())));
        BackpackCraftingBridge.ingredients(menu).forEach(item -> items.add(EmiStack.of(item.template(), item.quantity())));
        return new EmiPlayerInventory(items);
    }

    private boolean sophisticatedIntegrations$handles(StorageContainerMenuBase<?> menu, EmiRecipe recipe) {
        var player = Minecraft.getInstance().player;
        if (player == null || recipe.getId() == null || recipeType != RecipeType.CRAFTING || !(menu instanceof BackpackContainer)
                || !VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())) return false;
        return player.level().getRecipeManager().byKey(recipe.getId()).map(holder -> holder.value() instanceof CraftingRecipe).orElse(false);
    }

    private EmiCraftContext<StorageContainerMenuBase<?>> sophisticatedIntegrations$freshContext(EmiCraftContext<StorageContainerMenuBase<?>> context) {
        return new EmiCraftContext<>(context.getScreen(), sophisticatedIntegrations$inventory(context.getScreenHandler()),
                context.getType(), context.getDestination(), context.getAmount());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<StorageContainerMenuBase<?>> context) {
        return StandardRecipeHandler.super.getTooltip(recipe, sophisticatedIntegrations$handles(context.getScreenHandler(), recipe)
                ? sophisticatedIntegrations$freshContext(context) : context);
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<StorageContainerMenuBase<?>> context, List<Widget> widgets, GuiGraphics draw) {
        StandardRecipeHandler.super.render(recipe, sophisticatedIntegrations$handles(context.getScreenHandler(), recipe)
                ? sophisticatedIntegrations$freshContext(context) : context, widgets, draw);
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$canCraft(EmiRecipe recipe, EmiCraftContext<? extends StorageContainerMenuBase<?>> context,
                                                    CallbackInfoReturnable<Boolean> ci) {
        if (sophisticatedIntegrations$handles(context.getScreenHandler(), recipe))
            ci.setReturnValue(BackpackCraftingBridge.canFill(context.getScreenHandler(), recipe.getId()));
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$linkedRecipe(EmiRecipe recipe, EmiCraftContext<? extends StorageContainerMenuBase<?>> context,
                                                        CallbackInfoReturnable<Boolean> ci) {
        if (!sophisticatedIntegrations$handles(context.getScreenHandler(), recipe)) return;
        if (!BackpackCraftingBridge.canFill(context.getScreenHandler(), recipe.getId())) {
            ci.setReturnValue(false);
            return;
        }
        int action = switch (context.getDestination()) {
            case NONE -> 0;
            case CURSOR -> 1;
            case INVENTORY -> 2;
        };
        BackpackCraftingBridge.fill(context.getScreenHandler(), recipe.getId(), context.getAmount() > 1, action);
        ci.setReturnValue(true);
    }
}
