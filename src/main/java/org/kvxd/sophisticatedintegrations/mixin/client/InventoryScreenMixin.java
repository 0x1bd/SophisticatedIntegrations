package org.kvxd.sophisticatedintegrations.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kvxd.sophisticatedintegrations.network.InventoryCraftingRefreshPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void sophisticatedIntegrations$refreshIngredients(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu == player.inventoryMenu)
            PacketDistributor.sendToServer(new InventoryCraftingRefreshPayload(player.inventoryMenu.containerId));
    }
}
