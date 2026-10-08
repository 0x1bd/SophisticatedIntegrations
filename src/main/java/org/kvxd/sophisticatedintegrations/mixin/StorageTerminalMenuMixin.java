package org.kvxd.sophisticatedintegrations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.menu.StorageTerminalMenu;
import com.tom.storagemod.util.TerminalSyncManager.SlotAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = StorageTerminalMenu.class, remap = false)
public abstract class StorageTerminalMenuMixin implements IntegrationMenu {

    @Shadow protected StorageTerminalBlockEntity te;
    @Shadow protected Inventory pinv;
    @Unique private TerminalSession sophisticatedIntegrations$session;

    @Override
    public TerminalSession sophisticatedIntegrations$getSession() {
        if (sophisticatedIntegrations$session == null) {
            sophisticatedIntegrations$session = new TerminalSession(pinv.player, te);
        }
        return sophisticatedIntegrations$session;
    }

    @WrapMethod(method = "broadcastChanges")
    private void sophisticatedIntegrations$sync(Operation<Void> original) {
        TerminalContext.run(sophisticatedIntegrations$getSession(), original::call);
    }

    @WrapMethod(method = "quickMoveStack")
    private ItemStack sophisticatedIntegrations$shiftClick(Player player, int slot, Operation<ItemStack> original) {
        return TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(player, slot));
    }

    @WrapMethod(method = "receive")
    private void sophisticatedIntegrations$receive(CompoundTag message, Operation<Void> original) {
        TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(message));
    }

    @WrapMethod(method = "onInteract")
    private void sophisticatedIntegrations$interact(StoredItemStack stack, SlotAction action, boolean modifier, Operation<Void> original) {
        TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(stack, action, modifier));
    }
}
