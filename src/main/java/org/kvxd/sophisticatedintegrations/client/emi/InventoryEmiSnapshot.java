package org.kvxd.sophisticatedintegrations.client.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InventoryEmiSnapshot extends EmiPlayerInventory {
    private final EmiPlayerInventory preferred;
    private final EmiPlayerInventory alternative;

    public InventoryEmiSnapshot(EmiPlayerInventory preferred, EmiPlayerInventory alternative) {
        super(discovery(preferred, alternative));
        this.preferred = preferred;
        this.alternative = alternative;
    }

    public EmiPlayerInventory preferred() {
        return preferred;
    }

    public EmiPlayerInventory alternative() {
        return alternative;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe) {
        return canCraft(recipe, 1);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, long amount) {
        return preferred.canCraft(recipe, amount) || alternative != null && alternative.canCraft(recipe, amount);
    }

    @Override
    public List<Boolean> getCraftAvailability(EmiRecipe recipe) {
        return !preferred.canCraft(recipe) && alternative != null && alternative.canCraft(recipe)
                ? alternative.getCraftAvailability(recipe) : preferred.getCraftAvailability(recipe);
    }

    @Override
    public boolean isEqual(EmiPlayerInventory other) {
        if (!(other instanceof InventoryEmiSnapshot snapshot) || !preferred.isEqual(snapshot.preferred)) return false;
        return alternative == null ? snapshot.alternative == null
                : snapshot.alternative != null && alternative.isEqual(snapshot.alternative);
    }

    private static List<EmiStack> discovery(EmiPlayerInventory preferred, EmiPlayerInventory alternative) {
        Map<EmiStack, EmiStack> stacks = new LinkedHashMap<>();
        add(stacks, preferred);
        if (alternative != null) add(stacks, alternative);
        // EmiPlayerInventory adds the cursor itself; the prepared destination inventories already include it.
        var screen = EmiApi.getHandledScreen();
        if (screen != null && screen.getMenu() != null) {
            var carried = screen.getMenu().getCarried();
            var cursor = EmiStack.of(carried).comparison(comparison -> Comparison.DEFAULT_COMPARISON);
            var included = stacks.get(cursor);
            if (included != null) included.setAmount(included.getAmount() - carried.getCount());
        }
        var discovered = new ArrayList<EmiStack>(stacks.size());
        for (EmiStack stack : stacks.values()) {
            if (stack.getAmount() > 0) discovered.add(stack);
        }
        return discovered;
    }

    private static void add(Map<EmiStack, EmiStack> stacks, EmiPlayerInventory source) {
        for (EmiStack stack : source.inventory.values()) {
            EmiStack existing = stacks.get(stack);
            if (existing == null) stacks.put(stack, stack.copy());
            else existing.setAmount(Math.max(existing.getAmount(), stack.getAmount()));
        }
    }
}
