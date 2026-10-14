package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.PlatformProxyInventoryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlatformProxyInventoryAccess.class, remap = false)
public interface ProxyInventoryAccessor {
    @Accessor("access")
    IInventoryAccess sophisticatedIntegrations$access();
}
