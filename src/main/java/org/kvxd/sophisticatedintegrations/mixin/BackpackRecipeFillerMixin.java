package org.kvxd.sophisticatedintegrations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(CraftingContainerRecipeTransferHandlerServer.class)
public abstract class BackpackRecipeFillerMixin {
    @WrapMethod(method = "removeItemsFromInventory")
    private static Map<Integer, ItemStack> sophisticatedIntegrations$reservations(Player player, StorageContainerMenuBase<?> menu,
            Map<Integer, ItemStack> required, List<Integer> crafting, List<Integer> inventory, boolean max,
            Operation<Map<Integer, ItemStack>> original) {
        var upgrade = menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).orElse(null);
        if (upgrade == null || crafting.size() != 9 || !new java.util.HashSet<>(crafting)
                .equals(upgrade.getRecipeSlots().stream().map(slot -> slot.index).collect(java.util.stream.Collectors.toSet()))) {
            return original.call(player, menu, required, crafting, inventory, max);
        }
        return BackpackCraftingContext.transfer(player, menu, () -> original.call(player, menu, required, crafting, inventory, max));
    }

    @Inject(method = "getSlotWithStack(Lnet/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/world/inventory/Slot;",
            at = @At("RETURN"), cancellable = true)
    private static void sophisticatedIntegrations$missingIngredient(StorageContainerMenuBase<?> menu, ItemStack stack,
                                                                    List<Integer> crafting, List<Integer> inventory, CallbackInfoReturnable<Slot> ci) {
        if (ci.getReturnValue() == null) ci.setReturnValue(BackpackCraftingContext.reserve(menu, stack));
    }
}
