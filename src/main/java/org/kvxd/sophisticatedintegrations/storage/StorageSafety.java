package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.inventory.PlatformFilteredInventoryAccess;
import com.tom.storagemod.inventory.PlatformItemHandler;
import com.tom.storagemod.inventory.PlatformProxyInventoryAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import org.kvxd.sophisticatedintegrations.mixin.CachedStorageInventoryAccessor;
import org.kvxd.sophisticatedintegrations.mixin.FilteredStorageInventoryAccessor;
import org.kvxd.sophisticatedintegrations.mixin.SophisticatedInventoryAccessor;
import org.kvxd.sophisticatedintegrations.mixin.PlatformItemHandlerAccessor;
import org.kvxd.sophisticatedintegrations.mixin.ProxyInventoryAccessor;
import org.kvxd.sophisticatedintegrations.mixin.ControllerInventoryAccessor;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

import java.util.Collections;
import java.util.IdentityHashMap;

public final class StorageSafety {

    private StorageSafety() {
    }

    public static boolean mayVoid(IItemHandler handler, int slot, ItemStack template) {
        var backing = physicalSlot(handler, slot);
        if (backing == null) return true;
        if (!(backing.handler() instanceof InventoryHandler inventory)) return false;
        var wrapper = ((SophisticatedInventoryAccessor) inventory).sophisticatedIntegrations$storageWrapper();
        return wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).stream()
                .anyMatch(upgrade -> upgrade.getFilterLogic().matchesFilter(template));
    }

    public static StorageBackingSlot physicalSlot(IItemHandler handler, int slot) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<IItemHandler, Boolean>());
        while (handler != null && seen.add(handler)) {
            if (handler instanceof PlatformFilteredInventoryAccess filtered) {
                handler = filtered.getActualInventory().getPlatformHandler();
            } else if (handler instanceof PlatformItemHandler) {
                handler = ((PlatformItemHandlerAccessor) handler).sophisticatedIntegrations$access().getInventoryAccess().getPlatformHandler();
            } else if (handler instanceof PlatformProxyInventoryAccess) {
                handler = ((ProxyInventoryAccessor) handler).sophisticatedIntegrations$access().getPlatformHandler();
            } else if (handler instanceof CachedFailedInsertInventoryHandler<?>) {
                handler = ((CachedStorageInventoryAccessor) handler).sophisticatedIntegrations$wrappedHandler().get();
            } else if (handler instanceof FilteredItemHandler<?>) {
                handler = ((FilteredStorageInventoryAccessor) handler).sophisticatedIntegrations$wrappedHandler();
            } else if (handler instanceof ContentsFilteredStorageAccess filtered) {
                handler = filtered.sophisticatedIntegrations$wrappedHandler().get();
            } else if (handler instanceof ControllerBlockEntityBase) {
                var controller = (ControllerInventoryAccessor) handler;
                int index = controller.sophisticatedIntegrations$storageIndex(slot);
                if (index < 0) return null;
                slot = controller.sophisticatedIntegrations$localSlot(slot, index);
                handler = controller.sophisticatedIntegrations$storageHandler(index);
            } else if (handler instanceof net.neoforged.neoforge.items.wrapper.CombinedInvWrapper) {
                var combined = (org.kvxd.sophisticatedintegrations.mixin.CombinedInventoryAccessor) handler;
                int index = combined.sophisticatedIntegrations$handlerIndex(slot);
                if (index < 0) return null;
                slot = combined.sophisticatedIntegrations$localSlot(slot, index);
                handler = combined.sophisticatedIntegrations$handler(index);
            } else return new StorageBackingSlot(handler, slot);
        }
        return null;
    }
}
