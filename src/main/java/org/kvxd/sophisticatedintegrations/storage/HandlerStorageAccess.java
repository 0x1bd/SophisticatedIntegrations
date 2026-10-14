package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.IProxy;
import com.tom.storagemod.inventory.PlatformInventoryAccess;
import com.tom.storagemod.util.WorldStates;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Set;
import java.util.function.Supplier;

public final class HandlerStorageAccess implements PlatformInventoryAccess {
    private final Supplier<IItemHandler> handler;

    public HandlerStorageAccess(Supplier<IItemHandler> handler) {
        this.handler = handler;
    }

    @Override public IItemHandler get() { return handler.get(); }
    @Override public IInventoryChangeTracker tracker() { return WorldStates.getTracker(get()); }
    @Override public IInventoryAccess getRootHandler(Set<IProxy> seen) { return this; }
}
