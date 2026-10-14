package org.kvxd.sophisticatedintegrations.storage;

import net.minecraft.world.item.ItemStack;

public record StorageDestination(StorageSlot slot, boolean matching, long capacity, int space, int count) {

    public static StorageDestination forItem(StorageSlot slot, ItemStack template) {
        return forItem(slot, template, false);
    }

    public static StorageDestination forConsolidation(StorageSlot slot, ItemStack template) {
        return forItem(slot, template, true);
    }

    private static StorageDestination forItem(StorageSlot slot, ItemStack template, boolean consolidation) {
        boolean matching = slot.matches(template);
        int count = matching ? slot.slot().getStack().getCount() : 0;
        int space = consolidation ? slot.consolidationSpace(template) : slot.space(template);
        return new StorageDestination(slot, matching, (long) count + space, space, count);
    }
}
