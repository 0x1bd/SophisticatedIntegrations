package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecipeIngredientPlanner {
    private RecipeIngredientPlanner() {
    }

    public static Optional<List<ItemStack>> plan(Player player, StorageContainerMenuBase<?> menu, CraftingRecipe recipe,
                                                 List<NetworkIngredient> network) {
        return plan(player, menu, recipe, network, false);
    }

    public static Optional<List<ItemStack>> plan(Player player, StorageContainerMenuBase<?> menu, CraftingRecipe recipe,
                                                 List<NetworkIngredient> network, boolean maxTransfer) {
        var crafting = menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).orElse(null);
        if (crafting == null) return Optional.empty();
        List<NetworkIngredient> available = new ArrayList<>(inventory(player, menu));
        available.addAll(network);
        List<Slot> grid = crafting.getRecipeSlots();
        if (grid.size() != 9) return Optional.empty();
        return plan(player, recipe, available, (index, stack) -> grid.get(index).getMaxStackSize(stack), maxTransfer);
    }

    public static List<NetworkIngredient> inventory(Player player, StorageContainerMenuBase<?> menu) {
        List<Slot> sources = new ArrayList<>(menu.slots);
        menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).ifPresent(c -> sources.addAll(c.getRecipeSlots()));
        return sources.stream().distinct().filter(slot -> slot.mayPickup(player)).map(Slot::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new NetworkIngredient(stack.copyWithCount(1), stack.getCount())).toList();
    }

    public static Optional<List<ItemStack>> plan(Player player, CraftingRecipe recipe, List<NetworkIngredient> ingredients) {
        return plan(player, recipe, ingredients, (index, stack) -> stack.getMaxStackSize(), false);
    }

    private static Optional<List<ItemStack>> plan(Player player, CraftingRecipe recipe, List<NetworkIngredient> ingredients,
                                                  java.util.function.ToIntBiFunction<Integer, ItemStack> slotLimit, boolean maxTransfer) {
        if (recipe.getIngredients().isEmpty() || recipe.getIngredients().size() > 9) return Optional.empty();
        if (recipe instanceof ShapedRecipe shaped && (shaped.getWidth() > 3 || shaped.getHeight() > 3))
            return Optional.empty();
        Map<StoredItemStack, Long> available = new LinkedHashMap<>();
        for (NetworkIngredient item : ingredients) {
            if (item.quantity() > 0 && !item.template().isEmpty())
                available.merge(new StoredItemStack(item.template()), item.quantity(), Long::sum);
        }
        List<Ingredient> inputs = new ArrayList<>(java.util.Collections.nCopies(9, Ingredient.EMPTY));

        if (recipe instanceof ShapedRecipe shaped) {
            for (int row = 0; row < shaped.getHeight(); row++) {
                for (int col = 0; col < shaped.getWidth(); col++)
                    inputs.set(row * 3 + col, shaped.getIngredients().get(row * shaped.getWidth() + col));
            }
        } else {
            for (int i = 0; i < recipe.getIngredients().size(); i++) inputs.set(i, recipe.getIngredients().get(i));
        }

        List<ItemStack> chosen = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        Map<Integer, List<StoredItemStack>> candidates = new LinkedHashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            Ingredient ingredient = inputs.get(i);
            if (ingredient.isEmpty()) continue;
            List<StoredItemStack> matches = available.keySet().stream().filter(item -> ingredient.test(item.getStack())).toList();
            if (matches.isEmpty()) return Optional.empty();
            candidates.put(i, matches);
        }
        List<Integer> order = candidates.keySet().stream().sorted(java.util.Comparator.comparingInt(i -> candidates.get(i).size())).toList();
        if (order.isEmpty()) return Optional.empty();
        int lower = 1;
        int upper = maxTransfer ? candidates.values().stream().flatMap(List::stream)
                                  .mapToInt(item -> item.getStack().getMaxStackSize()).max().orElse(1) : 1;
        List<ItemStack> best = null;
        while (lower <= upper) {
            int batch = lower + (upper - lower) / 2;
            if (choose(0, order, candidates, chosen, new LinkedHashMap<>(available), recipe, player, slotLimit, batch)) {
                best = List.copyOf(chosen);
                lower = batch + 1;
            } else {
                upper = batch - 1;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean choose(int depth, List<Integer> order, Map<Integer, List<StoredItemStack>> candidates, List<ItemStack> chosen,
                                  Map<StoredItemStack, Long> available, CraftingRecipe recipe, Player player,
                                  java.util.function.ToIntBiFunction<Integer, ItemStack> slotLimit, int batch) {
        if (depth == order.size()) return recipe.matches(CraftingInput.of(3, 3, chosen), player.level());
        int index = order.get(depth);
        List<StoredItemStack> matches = candidates.get(index);
        long remainingSlots = order.subList(depth, order.size()).stream().filter(i -> candidates.get(i).equals(matches)).count();
        long capacity = matches.stream().mapToLong(item -> Math.min(available.get(item) / batch, remainingSlots)).sum();
        if (capacity < remainingSlots) return false;
        for (StoredItemStack candidate : candidates.get(index)) {
            long count = available.get(candidate);
            if (count < batch || batch > slotLimit.applyAsInt(index, candidate.getStack())) continue;
            available.put(candidate, count - batch);
            chosen.set(index, candidate.getStack().copyWithCount(1));
            if (choose(depth + 1, order, candidates, chosen, available, recipe, player, slotLimit, batch)) return true;
            available.put(candidate, count);
        }

        chosen.set(index, ItemStack.EMPTY);
        return false;
    }
}
