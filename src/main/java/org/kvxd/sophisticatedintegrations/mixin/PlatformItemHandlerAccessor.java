package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.PlatformItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlatformItemHandler.class, remap = false)
public interface PlatformItemHandlerAccessor {
    @Accessor("access")
    IInventoryAccess.IInventory sophisticatedIntegrations$access();
}
