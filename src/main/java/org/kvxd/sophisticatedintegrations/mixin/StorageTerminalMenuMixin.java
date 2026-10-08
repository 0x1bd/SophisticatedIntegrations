package org.kvxd.sophisticatedintegrations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import com.tom.storagemod.menu.StorageTerminalMenu;
import com.tom.storagemod.util.TerminalSyncManager.SlotAction;
import com.tom.storagemod.util.TerminalSyncManager;
import com.tom.storagemod.util.DataSlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = StorageTerminalMenu.class, remap = false)
public abstract class StorageTerminalMenuMixin extends AbstractContainerMenu implements IntegrationMenu {

    @Shadow protected StorageTerminalBlockEntity te;
    @Shadow protected Inventory pinv;
    @Unique private TerminalSession sophisticatedIntegrations$session;
    @Unique private int sophisticatedIntegrations$backpackMode;

    protected StorageTerminalMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lcom/tom/storagemod/block/entity/StorageTerminalBlockEntity;)V", at = @At("TAIL"))
    private void sophisticatedIntegrations$addBackpackMode(CallbackInfo callback) {
        addDataSlot(DataSlots.create(
                mode -> sophisticatedIntegrations$backpackMode = mode, this::sophisticatedIntegrations$backpackMode));
    }

    @Override
    public int sophisticatedIntegrations$backpackMode() {
        if (pinv.player.level().isClientSide) return sophisticatedIntegrations$backpackMode;
        return (IntegrationConfig.ENABLED.get() ? TerminalSession.BACKPACKS_ALLOWED : 0)
                | (sophisticatedIntegrations$getSession().includesBackpacks() ? TerminalSession.BACKPACKS_INCLUDED : 0);
    }

    @Override
    public void sophisticatedIntegrations$setIncludesBackpacks(boolean included) {
        if (pinv.player.level().isClientSide) {
            if ((sophisticatedIntegrations$backpackMode & TerminalSession.BACKPACKS_ALLOWED) == 0) return;
            sophisticatedIntegrations$backpackMode = TerminalSession.BACKPACKS_ALLOWED
                    | (included ? TerminalSession.BACKPACKS_INCLUDED : 0);
            var message = new CompoundTag();
            message.putBoolean(TerminalSession.BACKPACKS_SETTING, included);
            message.putInt("sophisticatedintegrations:menu", ((StorageTerminalMenu) (Object) this).containerId);
            ((StorageTerminalMenu) (Object) this).sendMessage(message);
        } else {
            sophisticatedIntegrations$getSession().setIncludesBackpacks(included);
        }
    }

    @Override
    public TerminalSession sophisticatedIntegrations$getSession() {
        if (sophisticatedIntegrations$session == null) {
            sophisticatedIntegrations$session = new TerminalSession(pinv.player, te);
        }
        return sophisticatedIntegrations$session;
    }

    @WrapMethod(method = "broadcastChanges")
    private void sophisticatedIntegrations$sync(Operation<Void> original) {
        if (pinv.player.containerMenu != (Object) this) return;
        TerminalContext.run(sophisticatedIntegrations$getSession(), original::call);
    }

    @WrapOperation(method = "broadcastChanges", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/util/TerminalSyncManager;update(ILjava/util/Map;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"))
    private void sophisticatedIntegrations$syncRevision(TerminalSyncManager sync, int networkRevision,
            Map<StoredItemStack, TerminalItemStack> items, ServerPlayer player, Consumer<CompoundTag> extra,
            Operation<Void> original) {
        original.call(sync, sophisticatedIntegrations$getSession().revisionFor(items), items, player, extra);
    }

    @WrapMethod(method = "quickMoveStack")
    private ItemStack sophisticatedIntegrations$shiftClick(Player player, int slot, Operation<ItemStack> original) {
        return TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(player, slot));
    }

    @WrapMethod(method = "receive")
    private void sophisticatedIntegrations$receive(CompoundTag message, Operation<Void> original) {
        if (message.contains(TerminalSession.BACKPACKS_SETTING, Tag.TAG_BYTE)
                && message.getInt("sophisticatedintegrations:menu") == ((StorageTerminalMenu) (Object) this).containerId) {
            sophisticatedIntegrations$setIncludesBackpacks(message.getBoolean(TerminalSession.BACKPACKS_SETTING));
        }
        TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(message));
    }

    @WrapMethod(method = "onInteract")
    private void sophisticatedIntegrations$interact(StoredItemStack stack, SlotAction action, boolean modifier, Operation<Void> original) {
        TerminalContext.run(sophisticatedIntegrations$getSession(), () -> original.call(stack, action, modifier));
    }
}
