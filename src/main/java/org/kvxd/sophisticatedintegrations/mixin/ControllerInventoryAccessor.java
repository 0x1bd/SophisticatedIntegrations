package org.kvxd.sophisticatedintegrations.mixin;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ControllerBlockEntityBase.class, remap = false)
public interface ControllerInventoryAccessor {
    @Invoker("getIndexForSlot")
    int sophisticatedIntegrations$storageIndex(int slot);

    @Invoker("getHandlerFromIndex")
    IItemHandlerModifiable sophisticatedIntegrations$storageHandler(int index);

    @Invoker("getSlotFromIndex")
    int sophisticatedIntegrations$localSlot(int slot, int index);
}
