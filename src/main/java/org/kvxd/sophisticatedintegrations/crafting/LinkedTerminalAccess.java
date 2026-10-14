package org.kvxd.sophisticatedintegrations.crafting;

import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.components.WorldPos;
import com.tom.storagemod.item.AdvWirelessTerminalItem;
import com.tom.storagemod.util.PlayerInvUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;

public final class LinkedTerminalAccess {
    private LinkedTerminalAccess() {}

    public static CraftingTerminalBlockEntity findCrafting(Player player) {
        return findBound(player) instanceof CraftingTerminalBlockEntity crafting ? crafting : null;
    }

    public static boolean openCrafting(Player player, CraftingTerminalBlockEntity terminal) {
        if (findCrafting(player) != terminal) return false;
        ItemStack wireless = PlayerInvUtil.findItem(player,
                stack -> stack.getItem() instanceof AdvWirelessTerminalItem && stack.has(Content.boundPosComponent.get())
                        && new WorldPos(terminal.getLevel().dimension(), terminal.getBlockPos()).equals(stack.get(Content.boundPosComponent.get())),
                ItemStack.EMPTY, stack -> stack);
        if (wireless.isEmpty()) return false;
        // Use Tom's normal remote activation so block-use permission hooks remain authoritative.
        AdvWirelessTerminalItem.activateTerminal(player.level(), wireless, player, InteractionHand.MAIN_HAND);
        return player.containerMenu instanceof com.tom.storagemod.menu.CraftingTerminalMenu menu
                && ((org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu) menu).sophisticatedIntegrations$getSession().terminal() == terminal;
    }

    public static StorageTerminalBlockEntity find(Player player) {
        return IntegrationConfig.BACKPACK_CRAFTING.get() ? findBound(player) : null;
    }

    private static StorageTerminalBlockEntity findBound(Player player) {
        if (player.level().isClientSide || player.isSpectator()) return null;
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
