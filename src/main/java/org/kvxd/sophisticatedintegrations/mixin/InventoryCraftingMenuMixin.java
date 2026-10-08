package org.kvxd.sophisticatedintegrations.mixin;

import net.minecraft.world.inventory.InventoryMenu;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InventoryMenu.class)
public abstract class InventoryCraftingMenuMixin implements InventoryCraftingAccess {
    @Unique
    private boolean sophisticatedIntegrations$hasCraftingBackpack;

    @Override
    public boolean sophisticatedIntegrations$hasCraftingBackpack() {
        return sophisticatedIntegrations$hasCraftingBackpack;
    }

    @Override
    public void sophisticatedIntegrations$setCraftingBackpack(boolean available) {
        sophisticatedIntegrations$hasCraftingBackpack = available;
    }
}
