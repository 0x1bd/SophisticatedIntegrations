package org.kvxd.sophisticatedintegrations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftingTerminalMenu.class, remap = false)
public abstract class CraftingTerminalMenuMixin {

    @WrapMethod(method = "receive")
    private void sophisticatedIntegrations$recipeTransfer(CompoundTag message, Operation<Void> original) {
        TerminalContext.run(((IntegrationMenu) this).sophisticatedIntegrations$getSession(), () -> original.call(message));
    }

    @WrapMethod(method = "handlePlacement")
    private void sophisticatedIntegrations$recipeBook(boolean all, RecipeHolder<?> recipe, ServerPlayer player, Operation<Void> original) {
        TerminalContext.run(((IntegrationMenu) this).sophisticatedIntegrations$getSession(), () -> original.call(all, recipe, player));
    }

    @WrapMethod(method = "shiftClickItems")
    private ItemStack sophisticatedIntegrations$craftingShiftClick(Player player, int slot, Operation<ItemStack> original) {
        return TerminalContext.run(((IntegrationMenu) this).sophisticatedIntegrations$getSession(), () -> original.call(player, slot));
    }

    @WrapMethod(method = "clickMenuButton")
    private boolean sophisticatedIntegrations$clearGrid(Player player, int id, Operation<Boolean> original) {
        return TerminalContext.run(((IntegrationMenu) this).sophisticatedIntegrations$getSession(), () -> original.call(player, id));
    }
}
