package org.kvxd.sophisticatedintegrations.backpack;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlayerBackpacks {
    private PlayerBackpacks() {}

    public static List<BackpackLocation> find(Player player) {
        List<BackpackLocation> backpacks = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (stack, handler, identifier, slot) -> {
            backpacks.add(new BackpackLocation(stack, handler, identifier, slot));
            return false;
        });
        backpacks.sort(Comparator.comparingInt(BackpackLocation::priority));
        return backpacks;
    }
}
