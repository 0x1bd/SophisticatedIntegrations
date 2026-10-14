package org.kvxd.sophisticatedintegrations.mixin;

import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.kvxd.sophisticatedintegrations.storage.ContentsFilteredStorageAccess;

import java.util.function.Supplier;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.block.ContentsFilteredItemHandler", remap = false)
public interface ContentsFilteredInventoryAccessor extends ContentsFilteredStorageAccess {
    @Accessor("itemHandlerGetter")
    Supplier<ITrackedContentsItemHandler> sophisticatedIntegrations$wrappedHandler();
}
