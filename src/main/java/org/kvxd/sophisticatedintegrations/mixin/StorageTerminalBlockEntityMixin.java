package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
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
public abstract class StorageTerminalBlockEntityMixin {

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
