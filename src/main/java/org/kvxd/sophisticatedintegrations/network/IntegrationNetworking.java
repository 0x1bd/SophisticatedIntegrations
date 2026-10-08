package org.kvxd.sophisticatedintegrations.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.kvxd.sophisticatedintegrations.crafting.BackpackRecipeTransfer;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

public final class IntegrationNetworking {
    private IntegrationNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(BackpackNetworkPayload.TYPE, BackpackNetworkPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof BackpackCraftingMenu menu)
                menu.sophisticatedIntegrations$craftingSession().receive(payload);
        });
        registrar.playToServer(BackpackRecipePayload.TYPE, BackpackRecipePayload.STREAM_CODEC,
                (payload, context) -> BackpackRecipeTransfer.fill(context.player(), payload.containerId(), payload.recipeId(), payload.maxTransfer(), payload.action()));
        registrar.playToClient(InventoryCraftingAvailabilityPayload.TYPE, InventoryCraftingAvailabilityPayload.STREAM_CODEC,
                (payload, context) -> ((InventoryCraftingAccess) context.player().inventoryMenu).sophisticatedIntegrations$setCraftingBackpack(payload.available()));
        registrar.playToClient(BackpackCraftingTabPayload.TYPE, BackpackCraftingTabPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof BackpackContainer menu && menu.containerId == payload.containerId()) {
                BackpackRecipeTransfer.openCraftingTab(menu);
            }
        });
        registrar.playToServer(InventoryBackpackRecipePayload.TYPE, InventoryBackpackRecipePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) InventoryRecipeRouting.openAndFill(player,
                    payload.containerId(), payload.recipeId(), payload.maxTransfer(), payload.action());
        });
    }
}
