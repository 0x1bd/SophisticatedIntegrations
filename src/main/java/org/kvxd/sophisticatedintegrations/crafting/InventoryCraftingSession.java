package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;
import org.kvxd.sophisticatedintegrations.network.InventoryCraftingAvailabilityPayload;
import org.kvxd.sophisticatedintegrations.backpack.BackpackLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InventoryCraftingSession {
    private boolean available;
    private boolean initialized;
    private List<NetworkIngredient> ingredients = List.of();
    private List<NetworkIngredient> alternativeIngredients = List.of();
    private int revision;
    private int incomingRevision = -1;
    private int nextPart;
    private int incomingParts;
    private List<NetworkIngredient> incoming = new ArrayList<>();
    private List<NetworkIngredient> incomingAlternative = new ArrayList<>();

    public boolean available() {
        return available;
    }

    public List<NetworkIngredient> ingredients() {
        return available ? ingredients : List.of();
    }

    public List<NetworkIngredient> alternativeIngredients() {
        return available ? alternativeIngredients : List.of();
    }

    public void broadcast(ServerPlayer player) {
        broadcast(player, false);
    }

    public void broadcast(ServerPlayer player, boolean force) {
        var location = !player.isSpectator() && IntegrationConfig.AUTO_OPEN_CRAFTING.get()
                ? CraftingBackpackFinder.find(player).orElse(null) : null;
        var terminal = !player.isSpectator() && IntegrationConfig.AUTO_OPEN_CRAFTING.get()
                ? LinkedTerminalAccess.findCrafting(player) : null;
        boolean enabled = terminal != null || location != null;
        List<NetworkIngredient> backpack = backpackIngredients(player, location);
        List<NetworkIngredient> latest = terminal != null ? TerminalRecipeTransfer.ingredients(player, terminal) : backpack;
        List<NetworkIngredient> alternative = terminal != null ? backpack : List.of();
        if (!force && initialized && enabled == available && BackpackCraftingSession.sameContents(ingredients, latest)
                && BackpackCraftingSession.sameContents(alternativeIngredients, alternative)) return;
        initialized = true;
        available = enabled;
        ingredients = latest;
        alternativeIngredients = alternative;
        revision++;
        int primaryParts = Math.max(1, (latest.size() + 63) / 64);
        int alternativeParts = (alternative.size() + 63) / 64;
        int parts = primaryParts + alternativeParts;
        for (int part = 0; part < parts; part++) {
            boolean fallback = part >= primaryParts;
            List<NetworkIngredient> source = fallback ? alternative : latest;
            int sourcePart = fallback ? part - primaryParts : part;
            PacketDistributor.sendToPlayer(player, new InventoryCraftingAvailabilityPayload(revision, part, parts, enabled, fallback,
                    source.subList(Math.min(sourcePart * 64, source.size()), Math.min((sourcePart + 1) * 64, source.size()))));
        }
    }

    public static List<NetworkIngredient> backpackIngredients(ServerPlayer player, BackpackLocation location) {
        Map<StoredItemStack, Long> contents = new LinkedHashMap<>();
        if (location != null) {
            var wrapper = BackpackWrapper.fromStack(location.stack());
            var inventory = wrapper.getInventoryHandler();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (inventory.isSlotAccessible(slot) && !inventory.extractItem(slot, 1, true).isEmpty())
                    add(contents, inventory.getStackInSlot(slot));
            }
            var upgrades = wrapper.getUpgradeHandler().getSlotWrappers();
            var category = wrapper.getSettingsHandler().getGlobalSettingsCategory();
            boolean keepTab = SettingsManager.getSettingValue(player, category.getPlayerSettingsTagName(),
                    category, SettingsManager.KEEP_TAB_OPEN);
            var open = keepTab ? wrapper.getOpenTabId().map(upgrades::get).orElse(null) : null;
            var crafting = open instanceof CraftingUpgradeWrapper c ? c : upgrades.values().stream()
                                                                          .filter(CraftingUpgradeWrapper.class::isInstance).map(CraftingUpgradeWrapper.class::cast).findFirst().orElseThrow();
            for (int slot = 0; slot < crafting.getInventory().getSlots(); slot++)
                add(contents, crafting.getInventory().getStackInSlot(slot));
            for (NetworkIngredient item : BackpackCraftingSession.snapshot(LinkedTerminalAccess.find(player)))
                contents.merge(new StoredItemStack(item.template()), item.quantity(), Long::sum);
        }
        return contents.entrySet().stream()
                .map(entry -> new NetworkIngredient(entry.getKey().getStack().copyWithCount(1), entry.getValue())).toList();
    }

    private static void add(Map<StoredItemStack, Long> contents, ItemStack stack) {
        if (!stack.isEmpty())
            contents.merge(new StoredItemStack(stack.copyWithCount(1)), (long) stack.getCount(), Long::sum);
    }

    public void receive(InventoryCraftingAvailabilityPayload payload) {
        if (payload.parts() < 1 || payload.part() < 0 || payload.part() >= payload.parts()) return;
        if (payload.part() == 0) {
            incomingRevision = payload.revision();
            incomingParts = payload.parts();
            nextPart = 0;
            incoming = new ArrayList<>();
            incomingAlternative = new ArrayList<>();
        }
        if (payload.revision() != incomingRevision || payload.parts() != incomingParts || payload.part() != nextPart)
            return;
        (payload.alternative() ? incomingAlternative : incoming).addAll(payload.ingredients());
        nextPart++;
        if (nextPart == incomingParts) {
            available = payload.available();
            ingredients = List.copyOf(incoming);
            alternativeIngredients = List.copyOf(incomingAlternative);
            initialized = true;
            incoming.clear();
            incomingAlternative.clear();
        }
    }
}
