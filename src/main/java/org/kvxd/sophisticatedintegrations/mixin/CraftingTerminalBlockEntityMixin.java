package org.kvxd.sophisticatedintegrations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import net.minecraft.world.entity.player.Player;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftingTerminalBlockEntity.class, remap = false)
public abstract class CraftingTerminalBlockEntityMixin {

    @WrapMethod(method = {"craft", "clear"})
    private void sophisticatedIntegrations$craft(Player player, Operation<Void> original) {
        TerminalSession session = player.containerMenu instanceof IntegrationMenu menu ? menu.sophisticatedIntegrations$getSession() : null;
        if (session != null && session.terminal() != (Object) this) session = null;
        TerminalContext.run(session, () -> original.call(player));
    }
}
