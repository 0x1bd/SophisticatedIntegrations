package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.inventory.InventorySlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.kvxd.sophisticatedintegrations.mixin.InventorySlotAccessor;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public record StorageSlot(InventorySlot slot, int priority, boolean filtered, int order) {

    public IItemHandler handler() {
        return ((InventorySlotAccessor) slot).sophisticatedIntegrations$handler();
    }

    public int index() {
        return ((InventorySlotAccessor) slot).sophisticatedIntegrations$slot();
    }

    public boolean matches(ItemStack template) {
        return ItemStack.isSameItemSameComponents(slot.getStack(), template);
    }

    public int space(ItemStack template) {
        ItemStack stored = slot.getStack();
        if (!stored.isEmpty() && !matches(template)) return 0;
        int probe = Math.max(0, handler().getSlotLimit(index()));
        if (probe == 0) return 0;
        return probe - handler().insertItem(index(), template.copyWithCount(probe), true).getCount();
    }

    public int consolidationSpace(ItemStack template) {
        return StorageSafety.mayVoid(handler(), index(), template) ? 0 : space(template);
    }

    public ItemStack insert(ItemStack input) {
        var backing = StorageSafety.physicalSlot(handler(), index());
        if (backing == null) return input;
        if (StorageSafety.mayVoid(handler(), index(), input)) return slot.insert(input);
        if (!(backing.handler() instanceof InventoryHandler inventory)) return slot.insert(input);
        int accepted = input.getCount() - handler().insertItem(index(), input.copy(), true).getCount();
        if (accepted <= 0) return input;
        ItemStack remainder = input.copy();
        ItemStack uninserted = inventory.insertItemOnlyToSlot(backing.index(), input.copyWithCount(accepted), false);
        remainder.shrink(accepted - uninserted.getCount());
        if (remainder.getCount() < input.getCount())
            ((InventorySlotAccessor) slot).sophisticatedIntegrations$notifier().onSlotChanged(slot);
        return remainder;
    }

    public boolean sameBackingSlot(StorageSlot other) {
        var first = StorageSafety.physicalSlot(handler(), index());
        var second = StorageSafety.physicalSlot(other.handler(), other.index());
        return first != null && second != null && first.handler() == second.handler() && first.index() == second.index();
    }

}
