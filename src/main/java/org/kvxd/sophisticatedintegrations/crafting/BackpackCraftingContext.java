package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class BackpackCraftingContext {
    private static final ThreadLocal<BackpackCraftingContext> CURRENT = new ThreadLocal<>();
    private final Player player;
    private final StorageContainerMenuBase<?> menu;
    private final List<NetworkIngredientSlot> reservations = new ArrayList<>();

    private BackpackCraftingContext(Player player, StorageContainerMenuBase<?> menu) {
        this.player = player;
        this.menu = menu;
    }

    public static <T> T transfer(Player player, StorageContainerMenuBase<?> menu, Supplier<T> action) {
        BackpackCraftingContext previous = CURRENT.get();
        BackpackCraftingContext context = new BackpackCraftingContext(player, menu);
        CURRENT.set(context);
        try {
            T result = action.get();
            context.reservations.forEach(NetworkIngredientSlot::commit);
            return result;
        } finally {
            context.reservations.forEach(NetworkIngredientSlot::restore);
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static Slot reserve(StorageContainerMenuBase<?> menu, ItemStack template) {
        BackpackCraftingContext context = CURRENT.get();
        if (context == null || context.menu != menu || !(menu instanceof BackpackCraftingMenu integration)) return null;
        StorageTerminalBlockEntity terminal = integration.sophisticatedIntegrations$craftingSession().terminal();
        if (terminal == null) return null;
        StoredItemStack extracted = terminal.pullStack(new StoredItemStack(template.copyWithCount(1)), 1);
        if (extracted == null || extracted.getQuantity() <= 0) return null;
        ItemStack stack = extracted.getActualStack();
        NetworkIngredientSlot reservation = new NetworkIngredientSlot(context.player, terminal, stack);
        context.reservations.add(reservation);
        if (!ItemStack.isSameItemSameComponents(stack, template) || stack.getCount() != 1) {
            reservation.restore();
            return null;
        }
        return reservation;
    }
}
