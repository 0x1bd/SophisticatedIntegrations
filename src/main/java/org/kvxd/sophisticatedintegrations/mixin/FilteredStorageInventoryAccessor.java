package org.kvxd.sophisticatedintegrations.mixin;

import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FilteredItemHandler.class, remap = false)
public interface FilteredStorageInventoryAccessor {

    @Accessor("inventoryHandler")
    IItemHandler sophisticatedIntegrations$wrappedHandler();
}
