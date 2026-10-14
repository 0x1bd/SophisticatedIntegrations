package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.InventorySlot;
import com.tom.storagemod.inventory.MultiInventoryAccess;
import com.tom.storagemod.inventory.PlatformFilteredInventoryAccess;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.util.Priority;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class NetworkStorage {

    private NetworkStorage() {
    }

    public static List<StorageSlot> slots(IInventoryAccess access, Level level) {
        List<StorageSlot> slots = new ArrayList<>();
        collect(access, level, slots, Collections.newSetFromMap(new IdentityHashMap<>()));
        return slots;
    }

    private static void collect(IInventoryAccess access, Level level, List<StorageSlot> slots, Set<IInventoryAccess> seen) {
        if (!seen.add(access)) return;
        if (access instanceof MultiInventoryAccess multi) {
            for (IInventoryAccess child : multi.getConnected()) collect(child, level, slots, seen);
            return;
        }
        var tracker = access.tracker();
        tracker.getChangeTracker(level);
        boolean filtered = access instanceof PlatformFilteredInventoryAccess f
                && StorageFilters.restrictsItems(f.getFilter());
        InventorySlot previous = null;
        while ((previous = tracker.findSlotAfter(previous, item -> true, true, false)) != null) {
            slots.add(new StorageSlot(previous, Priority.IPriority.get(access).getSorting(), filtered, slots.size()));
        }
    }

    private static Comparator<StorageDestination> preferences() {
        return Comparator.comparingInt((StorageDestination destination) -> destination.slot().priority()).reversed()
                .thenComparing(Comparator.comparing((StorageDestination destination) -> destination.slot().filtered()).reversed());
    }

    private static Comparator<StorageDestination> consolidationOrder() {
        return preferences()
                .thenComparing(Comparator.comparingLong(StorageDestination::capacity).reversed())
                .thenComparing(Comparator.comparing(StorageDestination::matching).reversed())
                .thenComparing(Comparator.comparingInt(StorageDestination::count).reversed())
                .thenComparingInt(destination -> destination.slot().order());
    }

    public static ItemStack extract(IInventoryAccess access, Level level, ItemStack template, long amount) {
        return extract(access, level, template, amount, () -> {
        });
    }

    public static ItemStack extract(IInventoryAccess access, Level level, ItemStack template, long amount, Runnable slotEmptied) {
        if (template.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int requested = (int) Math.min(amount, Integer.MAX_VALUE);
        List<StorageDestination> sources = slots(access, level).stream().filter(slot -> slot.matches(template))
                .map(slot -> StorageDestination.forItem(slot, template)).sorted(consolidationOrder().reversed()).toList();
        ItemStack result = template.copyWithCount(0);
        for (StorageDestination source : sources) {
            ItemStack extracted = source.slot().slot().extract(requested - result.getCount());
            if (!extracted.isEmpty()) {
                result.grow(extracted.getCount());
                if (source.slot().slot().getStack().isEmpty()) slotEmptied.run();
            }
            if (result.getCount() >= requested) break;
        }
        return result;
    }

    public static ItemStack insert(IInventoryAccess access, Level level, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        List<StorageDestination> candidates = slots(access, level).stream().map(slot -> StorageDestination.forItem(slot, input))
                .filter(destination -> destination.space() > 0)
                .sorted(preferences()
                        .thenComparing(Comparator.comparing(StorageDestination::matching).reversed())
                        .thenComparing(Comparator.comparingLong(StorageDestination::capacity).reversed())
                        .thenComparing(Comparator.comparingInt(StorageDestination::space).reversed())
                        .thenComparingInt(destination -> destination.slot().order())).toList();
        ItemStack remainder = input.copy();
        for (StorageDestination destination : candidates) {
            remainder = destination.slot().insert(remainder);
            if (remainder.isEmpty()) break;
        }
        return remainder;
    }

    public static long consolidate(IInventoryAccess access, Level level, Consumer<ItemStack> recovery) {
        List<StorageSlot> slots = slots(access, level);
        var items = new LinkedHashMap<StoredItemStack, ItemStack>();
        for (StorageSlot slot : slots) {
            ItemStack stack = slot.slot().getStack();
            if (!stack.isEmpty()) items.putIfAbsent(new StoredItemStack(stack), stack.copyWithCount(1));
        }
        return consolidate(slots, items.values(), recovery);
    }

    public static long consolidateItem(IInventoryAccess access, Level level, ItemStack template, Consumer<ItemStack> recovery) {
        List<StorageSlot> slots = slots(access, level);
        List<StorageSlot> occupied = slots.stream().filter(slot -> slot.matches(template)).toList();
        if (occupied.isEmpty()) return 0;
        if (occupied.size() == 1) {
            StorageSlot source = occupied.getFirst();
            boolean designatedDestination = slots.stream().anyMatch(slot ->
                    (slot.priority() > source.priority() || slot.priority() == source.priority() && slot.filtered() && !source.filtered())
                            && slot.consolidationSpace(template) > 0);
            if (!designatedDestination) return 0;
        }
        return consolidate(slots, List.of(template), recovery);
    }

    private static long consolidate(List<StorageSlot> slots, Iterable<ItemStack> items, Consumer<ItemStack> recovery) {
        long moved = 0;
        for (ItemStack template : items) {
            List<StorageSlot> ranked = slots.stream().map(slot -> StorageDestination.forConsolidation(slot, template))
                    .filter(destination -> destination.matching() || destination.space() > 0)
                    .sorted(consolidationOrder()).map(StorageDestination::slot).toList();
            List<Integer> sources = new ArrayList<>();
            for (int index = 0; index < ranked.size(); index++) {
                if (ranked.get(index).matches(template)) sources.add(index);
            }
            for (int destination = 0; destination < ranked.size(); destination++) {
                while (!sources.isEmpty() && !ranked.get(sources.getLast()).matches(template)) sources.removeLast();
                if (sources.isEmpty() || sources.getLast() <= destination) break;
                StorageSlot to = ranked.get(destination);
                for (int source = sources.size() - 1; source >= 0 && sources.get(source) > destination; source--) {
                    int space = to.consolidationSpace(template);
                    if (space <= 0) break;
                    StorageSlot from = ranked.get(sources.get(source));
                    if (!from.matches(template) || from.sameBackingSlot(to))
                        continue;
                    int amount = Math.min(space, from.slot().getStack().getCount());
                    ItemStack simulated = from.handler().extractItem(from.index(), amount, true);
                    if (simulated.isEmpty() || !ItemStack.isSameItemSameComponents(template, simulated)) continue;
                    int accepted = simulated.getCount() - to.handler().insertItem(to.index(), simulated.copy(), true).getCount();
                    if (accepted <= 0) continue;
                    ItemStack extracted = from.slot().extract(accepted);
                    if (extracted.isEmpty()) continue;
                    ItemStack remainder = to.insert(extracted);
                    moved += extracted.getCount() - remainder.getCount();
                    if (!remainder.isEmpty()) {
                        if (!StorageSafety.mayVoid(from.handler(), from.index(), remainder)) remainder = from.insert(remainder);
                        if (!remainder.isEmpty()) recovery.accept(remainder);
                    }
                }
            }
        }
        return moved;
    }
}
