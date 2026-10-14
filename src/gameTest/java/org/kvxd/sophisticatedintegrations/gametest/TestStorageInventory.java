package org.kvxd.sophisticatedintegrations.gametest;

import com.tom.storagemod.inventory.InventoryChangeTracker;
import com.tom.storagemod.inventory.PlatformInventoryAccess;
import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.IProxy;
import com.tom.storagemod.util.Priority;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Set;

public final class TestStorageInventory implements PlatformInventoryAccess, Priority.IPriority {

    private final IItemHandler handler;
    private final InventoryChangeTracker tracker;
    private final Priority priority;

    public TestStorageInventory(IItemHandler handler) {
        this(handler, Priority.NORMAL);
    }

    public TestStorageInventory(IItemHandler handler, Priority priority) {
        this.handler = handler;
        this.tracker = new InventoryChangeTracker(handler);
        this.priority = priority;
    }

    @Override public IItemHandler get() { return handler; }
    @Override public IInventoryChangeTracker tracker() { return tracker; }
    @Override public Priority getPriority() { return priority; }
    @Override public IInventoryAccess getRootHandler(Set<IProxy> seen) { return this; }
}
