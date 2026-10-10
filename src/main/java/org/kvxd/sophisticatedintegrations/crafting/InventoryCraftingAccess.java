package org.kvxd.sophisticatedintegrations.crafting;

public interface InventoryCraftingAccess {
    InventoryCraftingSession sophisticatedIntegrations$inventoryCraftingSession();

    default boolean sophisticatedIntegrations$hasCraftingBackpack() {
        return sophisticatedIntegrations$inventoryCraftingSession().available();
    }
}
