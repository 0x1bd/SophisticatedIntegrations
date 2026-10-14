package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;

public final class StorageMaintenanceJob {

    private final WeakReference<StorageTerminalBlockEntity> terminal;
    private Deque<ItemStack> remaining;
    private boolean repeat;

    public StorageMaintenanceJob(StorageTerminalBlockEntity terminal) {
        this.terminal = new WeakReference<>(terminal);
    }

    public StorageTerminalBlockEntity terminal() {
        return terminal.get();
    }

    public void requestPass() {
        repeat = true;
    }

    public boolean step(MinecraftServer server) {
        var terminal = terminal();
        if (terminal == null || terminal.isRemoved() || terminal.getLevel() == null
                || terminal.getLevel().getServer() != server) return false;
        if (terminal instanceof MountedTerminalAccess mounted) {
            if (!mounted.sophisticatedIntegrations$mountedStorageValid()) return false;
        } else if (!terminal.getLevel().hasChunkAt(terminal.getBlockPos())
                || terminal.getLevel().getBlockEntity(terminal.getBlockPos()) != terminal) return false;
        var access = ((TerminalStorageAccess) terminal).sophisticatedIntegrations$networkStorage();
        if (remaining == null) {
            repeat = false;
            var tracker = access.tracker();
            tracker.getChangeTracker(terminal.getLevel());
            var items = new LinkedHashMap<StoredItemStack, ItemStack>();
            tracker.streamWrappedStacks(false).filter(item -> item.getQuantity() > 0)
                    .forEach(item -> items.putIfAbsent(item, item.getStack().copyWithCount(1)));
            remaining = new ArrayDeque<>(items.values());
            return !remaining.isEmpty();
        }
        if (!remaining.isEmpty() && NetworkStorage.consolidateItem(access, terminal.getLevel(), remaining.removeFirst(), terminal::dropItem) > 0)
            repeat = true;
        if (!remaining.isEmpty()) return true;
        if (repeat) {
            remaining = null;
            return true;
        }
        return false;
    }
}
