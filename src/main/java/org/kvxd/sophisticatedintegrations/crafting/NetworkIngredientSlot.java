package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class NetworkIngredientSlot extends Slot {
    private final Player player;
    private final StorageTerminalBlockEntity origin;
    private final ItemStack reserved;
    private boolean settled;

    public NetworkIngredientSlot(Player player, StorageTerminalBlockEntity origin, ItemStack reserved) {
        super(new SimpleContainer(reserved.copy()), 0, -100, -100);
        this.player = player;
        this.origin = origin;
        this.reserved = reserved.copy();
    }

    @Override public boolean mayPickup(Player player) { return player == this.player && !settled; }
    @Override public boolean mayPlace(ItemStack stack) { return false; }

    @Override public void set(ItemStack original) { restore(); }

    public void commit() {
        if (!getItem().isEmpty()) restore();
        settled = true;
    }

    public void restore() {
        if (settled) return;
        settled = true;
        container.setItem(0, ItemStack.EMPTY);
        StoredItemStack remainder = origin.isRemoved() ? new StoredItemStack(reserved.copy()) : origin.pushStack(new StoredItemStack(reserved.copy()));
        if (remainder != null) {
            ItemStack remaining = remainder.getActualStack();
            if (!player.getInventory().add(remaining)) player.drop(remaining, false);
        }
    }
}
