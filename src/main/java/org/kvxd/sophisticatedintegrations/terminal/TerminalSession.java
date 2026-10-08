package org.kvxd.sophisticatedintegrations.terminal;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.kvxd.sophisticatedintegrations.backpack.BackpackInventories;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;

import java.util.HashMap;
import java.util.Map;

public final class TerminalSession {
    private final Player player;
    private final StorageTerminalBlockEntity terminal;
    private Map<StoredItemStack, TerminalItemStack> previous;
    private int revision;

    public TerminalSession(Player player, StorageTerminalBlockEntity terminal) {
        this.player = player;
        this.terminal = terminal;
    }

    public Player player() {
        return player;
    }

    public StorageTerminalBlockEntity terminal() {
        return terminal;
    }

    public int revision() {
        return revision;
    }

    public boolean canAccess() {
        return terminal != null && !player.level().isClientSide && !player.isSpectator()
                && player.containerMenu instanceof IntegrationMenu menu && menu.sophisticatedIntegrations$getSession() == this
                && IntegrationConfig.ENABLED.get() && terminal.canInteractWith(player, true);
    }

    public Map<StoredItemStack, TerminalItemStack> merge(Map<StoredItemStack, TerminalItemStack> network) {
        Map<StoredItemStack, TerminalItemStack> combined = new HashMap<>();
        network.values().forEach(value -> {
            TerminalItemStack copy = new TerminalItemStack(value.getStack().copyWithCount(1), value.getQuantity());
            copy.setUsedSlotCount(value.getUsedSlotCount());
            combined.put(copy, copy);
        });
        BackpackInventories.snapshot(player).values().forEach(value -> combined.merge(value, value, TerminalItemStack::merge));
        if (previous == null || previous.size() != combined.size() || combined.entrySet().stream().anyMatch(entry -> {
            TerminalItemStack old = previous.get(entry.getKey());
            return old == null || !entry.getValue().equalDetails(old);
        })) {
            revision++;
            previous = combined;
        }
        return combined;
    }

    public StoredItemStack completeExtraction(StoredItemStack request, long amount, StoredItemStack fromNetwork) {
        if (request == null || amount <= 0) return fromNetwork;
        int limit = (int) Math.min(amount, request.getMaxStackSize());
        int alreadyExtracted = fromNetwork == null ? 0 : (int) fromNetwork.getQuantity();
        ItemStack fromBackpacks = BackpackInventories.extract(player, request.getStack(), limit - alreadyExtracted);
        if (fromBackpacks.isEmpty()) return fromNetwork;
        fromBackpacks.grow(alreadyExtracted);
        return new StoredItemStack(fromBackpacks);
    }

    public StoredItemStack completeInsertion(StoredItemStack remainder) {
        if (remainder == null || !IntegrationConfig.INSERT_INTO_BACKPACKS.get()) return remainder;
        ItemStack uninserted = BackpackInventories.insert(player, remainder.getActualStack());
        return uninserted.isEmpty() ? null : new StoredItemStack(uninserted);
    }
}
