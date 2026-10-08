package org.kvxd.sophisticatedintegrations.mixin.client;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.client.BackpackCraftingBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiCraftingContainerTransferHandler")
public abstract class BackpackReiTransferMixin {
    @Shadow
    @Final
    private RecipeType<?> recipeType;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$linkedRecipe(TransferHandler.Context context, CallbackInfoReturnable<TransferHandler.Result> ci) {
        if (recipeType != RecipeType.CRAFTING || !(context.getMenu() instanceof StorageContainerMenuBase<?> menu))
            return;
        var recipeId = context.getDisplay().getDisplayLocation().orElse(null);
        if (!BackpackCraftingBridge.canFill(menu, recipeId)) return;
        if (context.isActuallyCrafting()) BackpackCraftingBridge.fill(menu, recipeId, context.isStackedCrafting(), 0);
        ci.setReturnValue(TransferHandler.Result.createSuccessful());
    }
}
