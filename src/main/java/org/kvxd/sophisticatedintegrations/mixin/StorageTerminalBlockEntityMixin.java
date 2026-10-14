package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.NetworkInventory;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import org.kvxd.sophisticatedintegrations.storage.NetworkStorage;
import org.kvxd.sophisticatedintegrations.storage.TerminalStorageAccess;
import org.kvxd.sophisticatedintegrations.storage.StorageMaintenance;
import org.spongepowered.asm.mixin.Shadow;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = StorageTerminalBlockEntity.class, remap = false)
public abstract class StorageTerminalBlockEntityMixin implements TerminalStorageAccess {
    @Shadow private NetworkInventory itemCache;

    @Override
    public IInventoryAccess sophisticatedIntegrations$networkStorage() {
        var terminal = (StorageTerminalBlockEntity) (Object) this;
        return itemCache.getAccess(terminal.getLevel(), terminal.getBlockPos());
    }

    @WrapOperation(method = "pushStack(Lcom/tom/storagemod/inventory/StoredItemStack;)Lcom/tom/storagemod/inventory/StoredItemStack;",
            at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/inventory/IInventoryAccess;pushStack(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack sophisticatedIntegrations$prioritizeStacks(IInventoryAccess access, ItemStack input, Operation<ItemStack> original) {
        var terminal = (StorageTerminalBlockEntity) (Object) this;
        ItemStack remainder = NetworkStorage.insert(access, terminal.getLevel(), input);
        if (remainder.getCount() < input.getCount()) {
            if (NetworkStorage.consolidateItem(access, terminal.getLevel(), input, terminal::dropItem) > 0)
                StorageMaintenance.schedule(terminal);
        }
        return remainder;
    }

    @WrapOperation(method = "pullStack", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/inventory/IInventoryAccess;pullMatchingStack(Lnet/minecraft/world/item/ItemStack;J)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack sophisticatedIntegrations$drainOverflow(IInventoryAccess access, ItemStack template, long amount, Operation<ItemStack> original) {
        var terminal = (StorageTerminalBlockEntity) (Object) this;
        ItemStack extracted = NetworkStorage.extract(access, terminal.getLevel(), template, amount,
                () -> StorageMaintenance.schedule(terminal));
        return extracted;
    }

    @Inject(method = "getStacks", at = @At("RETURN"), cancellable = true)
    private void sophisticatedIntegrations$personalView(CallbackInfoReturnable<Map<StoredItemStack, TerminalItemStack>> callback) {
        TerminalSession session = TerminalContext.forTerminal((StorageTerminalBlockEntity) (Object) this);
        if (session != null) callback.setReturnValue(session.merge(callback.getReturnValue()));
    }

    @Inject(method = "pullStack", at = @At("RETURN"), cancellable = true)
    private void sophisticatedIntegrations$extract(StoredItemStack stack, long amount, CallbackInfoReturnable<StoredItemStack> callback) {
        TerminalSession session = TerminalContext.forTerminal((StorageTerminalBlockEntity) (Object) this);
        if (session != null) callback.setReturnValue(session.completeExtraction(stack, amount, callback.getReturnValue()));
    }

    @Inject(method = "pushStack(Lcom/tom/storagemod/inventory/StoredItemStack;)Lcom/tom/storagemod/inventory/StoredItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void sophisticatedIntegrations$insert(StoredItemStack stack, CallbackInfoReturnable<StoredItemStack> callback) {
        TerminalSession session = TerminalContext.forTerminal((StorageTerminalBlockEntity) (Object) this);
        if (session != null) callback.setReturnValue(session.completeInsertion(callback.getReturnValue()));
    }
}
