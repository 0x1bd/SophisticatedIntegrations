package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.menu.TerminalCraftingFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.kvxd.sophisticatedintegrations.backpack.BackpackInventories;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TerminalCraftingFiller.class, remap = false)
public abstract class TerminalCraftingFillerMixin {

    @Shadow private Player player;
    @Shadow public abstract void accountStack(ItemStack stack);

    @Inject(method = "placeRecipe", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/util/TerminalSyncManager;fillCraftingFiller(Lcom/tom/storagemod/menu/TerminalCraftingFiller;)V",
            shift = At.Shift.AFTER))
    private void sophisticatedIntegrations$freshIngredients(Recipe<?> recipe, CallbackInfo callback) {
        if (player.containerMenu instanceof IntegrationMenu menu && menu.sophisticatedIntegrations$getSession().canAccess()) {
            BackpackInventories.snapshot(player).values().forEach(value -> accountStack(value.getStack()));
        }
    }
}
