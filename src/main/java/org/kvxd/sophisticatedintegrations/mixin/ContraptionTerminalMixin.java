package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.createterminal.behaviour.ContraptionWorld;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.StoredItemStack;
import org.kvxd.sophisticatedintegrations.storage.HandlerStorageAccess;
import org.kvxd.sophisticatedintegrations.storage.MountedTerminalAccess;
import org.kvxd.sophisticatedintegrations.storage.NetworkStorage;
import org.kvxd.sophisticatedintegrations.storage.StorageMaintenance;
import org.kvxd.sophisticatedintegrations.storage.TerminalStorageAccess;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"com.tom.createterminal.behaviour.StorageTerminalInstance",
        "com.tom.createterminal.behaviour.CraftingTerminalInstance"}, remap = false)
public abstract class ContraptionTerminalMixin implements TerminalStorageAccess, MountedTerminalAccess {
    @Shadow(remap = false) public abstract ContraptionWorld getContraption();
    @Unique private HandlerStorageAccess sophisticatedIntegrations$mountedStorage;

    @Override
    public IInventoryAccess sophisticatedIntegrations$networkStorage() {
        if (sophisticatedIntegrations$mountedStorage == null)
            sophisticatedIntegrations$mountedStorage = new HandlerStorageAccess(
                    () -> getContraption().getContraption().getStorage().getAllItems());
        return sophisticatedIntegrations$mountedStorage;
    }

    @Override
    public boolean sophisticatedIntegrations$mountedStorageValid() {
        return getContraption().isValid();
    }

    @Inject(method = "pushStack", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$insert(StoredItemStack stack, CallbackInfoReturnable<StoredItemStack> ci) {
        if (stack == null) return;
        var terminal = (StorageTerminalBlockEntity) (Object) this;
        var access = sophisticatedIntegrations$networkStorage();
        var input = stack.getActualStack();
        var remainder = NetworkStorage.insert(access, terminal.getLevel(), input);
        if (remainder.getCount() < input.getCount()
                && NetworkStorage.consolidateItem(access, terminal.getLevel(), input, terminal::dropItem) > 0)
            StorageMaintenance.schedule(terminal);
        StoredItemStack result = remainder.isEmpty() ? null : new StoredItemStack(remainder);
        var session = TerminalContext.forTerminal(terminal);
        ci.setReturnValue(session == null ? result : session.completeInsertion(result));
    }

    @Inject(method = "pullStack", at = @At("HEAD"), cancellable = true)
    private void sophisticatedIntegrations$extract(StoredItemStack stack, long amount, CallbackInfoReturnable<StoredItemStack> ci) {
        if (stack == null || amount <= 0) return;
        var terminal = (StorageTerminalBlockEntity) (Object) this;
        var extracted = NetworkStorage.extract(sophisticatedIntegrations$networkStorage(), terminal.getLevel(), stack.getStack(),
                amount, () -> StorageMaintenance.schedule(terminal));
        StoredItemStack result = extracted.isEmpty() ? null : new StoredItemStack(extracted);
        var session = TerminalContext.forTerminal(terminal);
        ci.setReturnValue(session == null ? result : session.completeExtraction(stack, amount, result));
    }
}
