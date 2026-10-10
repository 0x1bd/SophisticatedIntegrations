package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.minecraft.world.item.crafting.RecipeType;
import org.kvxd.sophisticatedintegrations.network.BackpackNetworkPayload;

import java.util.ArrayList;
import java.util.List;

public final class BackpackCraftingSession {
    private final Player player;
    private final StorageContainerMenuBase<?> menu;
    private List<NetworkIngredient> ingredients = List.of();
    private List<NetworkIngredient> incoming = new ArrayList<>();
    private StorageTerminalBlockEntity previousTerminal;
    private boolean linked;
    private int revision;
    private int incomingRevision = -1;
    private int nextPart;
    private long nextUpdate;

    public BackpackCraftingSession(Player player, StorageContainerMenuBase<?> menu) {
        this.player = player;
        this.menu = menu;
    }

    public boolean isCurrent() {
        return menu instanceof BackpackContainer && player.containerMenu == menu && !player.isSpectator();
    }

    public StorageTerminalBlockEntity terminal() {
        return isCurrent() && menu.stillValid(player) && menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).isPresent()
                ? LinkedTerminalAccess.find(player) : null;
    }

    public boolean linked() {
        return isCurrent() && linked;
    }

    public List<NetworkIngredient> ingredients() {
        return linked() ? ingredients : List.of();
    }

    public static List<NetworkIngredient> snapshot(StorageTerminalBlockEntity terminal) {
        if (terminal == null) return List.of();
        return terminal.getStacks().values().stream().filter(item -> item.getQuantity() > 0)
                .map(item -> new NetworkIngredient(item.getStack().copyWithCount(1), item.getQuantity())).toList();
    }

    public void broadcast() {
        if (!(player instanceof ServerPlayer serverPlayer) || !isCurrent()) return;
        StorageTerminalBlockEntity terminal = terminal();
        long now = player.level().getGameTime();
        if (terminal == previousTerminal && now < nextUpdate) return;
        nextUpdate = now + 10;
        List<NetworkIngredient> latest = snapshot(terminal);
        if (terminal == previousTerminal && linked == (terminal != null) && sameContents(ingredients, latest)) return;
        previousTerminal = terminal;
        linked = terminal != null;
        ingredients = latest;
        revision++;
        int parts = Math.max(1, (latest.size() + 63) / 64);
        for (int part = 0; part < parts; part++) {
            PacketDistributor.sendToPlayer(serverPlayer, new BackpackNetworkPayload(menu.containerId, revision, part, parts, linked,
                    latest.subList(Math.min(part * 64, latest.size()), Math.min((part + 1) * 64, latest.size()))));
        }
    }

    static boolean sameContents(List<NetworkIngredient> first, List<NetworkIngredient> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).quantity() != second.get(i).quantity()
                    || !ItemStack.isSameItemSameComponents(first.get(i).template(), second.get(i).template()))
                return false;
        }
        return true;
    }

    public void receive(BackpackNetworkPayload payload) {
        if (!player.level().isClientSide || !isCurrent() || payload.containerId() != menu.containerId) return;
        if (payload.part() == 0) {
            incomingRevision = payload.revision();
            nextPart = 0;
            incoming = new ArrayList<>();
            linked = false;
            ingredients = List.of();
        }
        if (payload.revision() != incomingRevision || payload.part() != nextPart) return;
        incoming.addAll(payload.ingredients());
        nextPart++;
        if (nextPart == payload.parts()) {
            ingredients = List.copyOf(incoming);
            linked = payload.linked();
            incoming.clear();
        }
    }
}
