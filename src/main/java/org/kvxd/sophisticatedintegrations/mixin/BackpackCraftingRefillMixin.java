package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingUpgradeWrapper.class)
public abstract class BackpackCraftingRefillMixin {

    @Inject(method = "extractFromStorageOrPlayer", at = @At("RETURN"), cancellable = true)
    private void sophisticatedIntegrations$refill(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue() || !(player.containerMenu instanceof BackpackCraftingMenu menu)) return;
        var storage = (net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase<?>) player.containerMenu;
        if (storage.getOpenOrFirstCraftingContainer(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                .filter(upgrade -> upgrade.getUpgradeWrapper() == (Object) this).isEmpty()) return;

        var terminal = menu.sophisticatedIntegrations$craftingSession().terminal();
        if (terminal == null) return;
        StoredItemStack extracted = terminal.pullStack(new StoredItemStack(stack.copyWithCount(1)), 1);
        ci.setReturnValue(extracted != null && extracted.getQuantity() == 1);
    }
}
