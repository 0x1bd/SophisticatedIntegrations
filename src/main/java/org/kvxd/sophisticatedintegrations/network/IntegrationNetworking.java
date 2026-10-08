package org.kvxd.sophisticatedintegrations.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.kvxd.sophisticatedintegrations.crafting.BackpackRecipeTransfer;

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
    }
}
