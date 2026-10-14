package org.kvxd.sophisticatedintegrations.gametest;

import net.minecraft.gametest.framework.GlobalTestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

@EventBusSubscriber(modid = SophisticatedIntegrations.ID)
public final class TestReportingEvents {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        GlobalTestReporter.replaceWith(new IntegrationTestReporter());
    }
}
