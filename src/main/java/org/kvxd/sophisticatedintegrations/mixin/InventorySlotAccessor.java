package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.inventory.InventorySlot;
import com.tom.storagemod.inventory.IInventoryAccess;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InventorySlot.class, remap = false)
public interface InventorySlotAccessor {

    @Accessor("handler")
    IItemHandler sophisticatedIntegrations$handler();

    @Accessor("id")
    int sophisticatedIntegrations$slot();

    @Accessor("notifier")
    IInventoryAccess.IChangeNotifier sophisticatedIntegrations$notifier();
}
