package org.kvxd.sophisticatedintegrations.crafting;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.kvxd.sophisticatedintegrations.backpack.BackpackLocation;
import org.kvxd.sophisticatedintegrations.backpack.PlayerBackpacks;

import java.util.Optional;

public final class CraftingBackpackFinder {
    private CraftingBackpackFinder() {
    }

    public static Optional<BackpackLocation> find(Player player) {
        return PlayerBackpacks.find(player).stream().filter(location -> BackpackWrapper.fromStack(location.stack())
                .getUpgradeHandler().getSlotWrappers().values().stream().anyMatch(CraftingUpgradeWrapper.class::isInstance)).findFirst();
    }
}
