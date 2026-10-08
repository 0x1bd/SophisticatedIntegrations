package org.kvxd.sophisticatedintegrations;

import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;
import org.kvxd.sophisticatedintegrations.network.IntegrationNetworking;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SophisticatedIntegrations.ID)
public final class SophisticatedIntegrations {
    public static final String ID = "sophisticatedintegrations";

    public SophisticatedIntegrations(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, IntegrationConfig.SPEC);
        bus.addListener(IntegrationNetworking::register);
        NeoForge.EVENT_BUS.addListener(InventoryRecipeRouting::tick);
    }
}
