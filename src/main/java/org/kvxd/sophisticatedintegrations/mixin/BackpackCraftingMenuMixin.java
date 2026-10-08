package org.kvxd.sophisticatedintegrations.mixin;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingSession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StorageContainerMenuBase.class)
public abstract class BackpackCraftingMenuMixin implements BackpackCraftingMenu {

    @Shadow @Final protected Player player;
    @Unique private BackpackCraftingSession sophisticatedIntegrations$craftingSession;

    @Override public BackpackCraftingSession sophisticatedIntegrations$craftingSession() {
        if (sophisticatedIntegrations$craftingSession == null) sophisticatedIntegrations$craftingSession =
                new BackpackCraftingSession(player, (StorageContainerMenuBase<?>) (Object) this);
        return sophisticatedIntegrations$craftingSession;
    }

    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void sophisticatedIntegrations$networkSnapshot(CallbackInfo ci) {
        sophisticatedIntegrations$craftingSession().broadcast();
    }
}
