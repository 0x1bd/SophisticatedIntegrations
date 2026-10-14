package org.kvxd.sophisticatedintegrations.mixin;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CombinedInvWrapper.class, remap = false)
public interface CombinedInventoryAccessor {
    @Invoker("getIndexForSlot")
    int sophisticatedIntegrations$handlerIndex(int slot);

    @Invoker("getHandlerFromIndex")
    IItemHandlerModifiable sophisticatedIntegrations$handler(int index);

    @Invoker("getSlotFromIndex")
    int sophisticatedIntegrations$localSlot(int slot, int index);
}
