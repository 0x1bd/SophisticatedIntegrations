package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

@EventBusSubscriber(modid = SophisticatedIntegrations.ID, value = Dist.CLIENT)
public final class RecipeViewerLoadingChecks {
    @SubscribeEvent
    public static void joined(ClientPlayerNetworkEvent.LoggingIn event) throws ClassNotFoundException {
        if (ModList.get().isLoaded("jei")) {
            Class.forName("mezz.jei.library.transfer.PlayerRecipeTransferHandler");
            LogUtils.getLogger().info("Recipe viewer loading check passed: JEI inventory transfer handler");
        }
        if (ModList.get().isLoaded("emi")) {
            Class.forName("dev.emi.emi.handler.InventoryRecipeHandler");
            LogUtils.getLogger().info("Recipe viewer loading check passed: EMI inventory transfer handler");
        }
    }
}
