package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

public final class StorageMaintenance {

    private static final Map<StorageTerminalBlockEntity, StorageMaintenanceJob> PENDING = new WeakHashMap<>();
    private static final Deque<StorageMaintenanceJob> JOBS = new ArrayDeque<>();

    private StorageMaintenance() {}

    public static void schedule(StorageTerminalBlockEntity terminal) {
        var previous = PENDING.get(terminal);
        if (previous != null) {
            previous.requestPass();
            return;
        }
        var job = new StorageMaintenanceJob(terminal);
        PENDING.put(terminal, job);
        JOBS.addLast(job);
    }

    public static void tick(ServerTickEvent.Post event) {
        long deadline = System.nanoTime() + 2_000_000;
        int count = JOBS.size();
        while (count-- > 0 && !JOBS.isEmpty()) {
            var job = JOBS.removeFirst();
            if (job.step(event.getServer())) JOBS.addLast(job);
            else PENDING.remove(job.terminal());
            if (System.nanoTime() >= deadline) break;
        }
    }

    public static void stopped(ServerStoppedEvent event) {
        PENDING.clear();
        JOBS.clear();
    }
}
