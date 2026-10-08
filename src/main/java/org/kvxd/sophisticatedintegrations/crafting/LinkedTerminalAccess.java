package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.components.WorldPos;
import com.tom.storagemod.item.AdvWirelessTerminalItem;
import com.tom.storagemod.util.PlayerInvUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;

public final class LinkedTerminalAccess {
    private LinkedTerminalAccess() {}

    public static StorageTerminalBlockEntity find(Player player) {
        if (player.level().isClientSide || player.isSpectator() || !IntegrationConfig.BACKPACK_CRAFTING.get()) return null;
        WorldPos link = PlayerInvUtil.findItem(player,
                stack -> stack.getItem() instanceof AdvWirelessTerminalItem && stack.has(Content.boundPosComponent.get()),
                null, stack -> stack.get(Content.boundPosComponent.get()));

        if (link == null) return null;

        ServerLevel level = player.getServer().getLevel(link.dim());
        if (level == null || !level.hasChunkAt(link.pos())) return null;

        return level.getBlockEntity(link.pos()) instanceof StorageTerminalBlockEntity terminal
                && terminal.canInteractWith(player, false) ? terminal : null;
    }
}
