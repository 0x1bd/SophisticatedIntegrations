package org.kvxd.sophisticatedintegrations.mixin;

import net.minecraft.world.inventory.InventoryMenu;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InventoryMenu.class)
public abstract class InventoryCraftingMenuMixin implements InventoryCraftingAccess {
    @Unique
    private final InventoryCraftingSession sophisticatedIntegrations$inventoryCraftingSession = new InventoryCraftingSession();

    @Override
    public InventoryCraftingSession sophisticatedIntegrations$inventoryCraftingSession() {
        return sophisticatedIntegrations$inventoryCraftingSession;
    }
}
