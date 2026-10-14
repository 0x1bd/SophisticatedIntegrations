package org.kvxd.sophisticatedintegrations.mixin;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InventoryHandler.class, remap = false)
public interface SophisticatedInventoryAccessor {

    @Accessor("storageWrapper")
    IStorageWrapper sophisticatedIntegrations$storageWrapper();
}
