package org.kvxd.sophisticatedintegrations.mixin;

import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(value = CachedFailedInsertInventoryHandler.class, remap = false)
public interface CachedStorageInventoryAccessor {

    @Accessor("wrappedHandlerGetter")
    Supplier<? extends IItemHandler> sophisticatedIntegrations$wrappedHandler();
}
