package org.kvxd.sophisticatedintegrations.storage;

import net.neoforged.neoforge.items.IItemHandler;

public record StorageBackingSlot(IItemHandler handler, int index) {
}
