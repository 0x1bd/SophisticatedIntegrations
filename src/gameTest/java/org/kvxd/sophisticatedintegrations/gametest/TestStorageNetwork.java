package org.kvxd.sophisticatedintegrations.gametest;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.PlatformMultiInventoryAccess;

import java.util.List;

public final class TestStorageNetwork extends PlatformMultiInventoryAccess {

    public TestStorageNetwork(IInventoryAccess... inventories) {
        connected.addAll(List.of(inventories));
        refresh();
    }
}
