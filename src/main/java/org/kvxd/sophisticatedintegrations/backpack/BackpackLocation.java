package org.kvxd.sophisticatedintegrations.backpack;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public record BackpackLocation(ItemStack stack, String handler, String identifier, int slot) {
    public int priority() {
        if (PlayerInventoryProvider.MAIN_INVENTORY.equals(handler)) return 2;
        if (PlayerInventoryProvider.OFFHAND_INVENTORY.equals(handler)) return 1;
        return 0;
    }

    public BackpackContext.Item context() {
        return new BackpackContext.Item(handler, identifier, slot, true);
    }
}
