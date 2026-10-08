package org.kvxd.sophisticatedintegrations.client;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.kvxd.sophisticatedintegrations.crafting.BackpackRecipeTransfer;

public final class BackpackCraftingTabs {
    private BackpackCraftingTabs() {}

    public static void open(StorageContainerMenuBase<?> menu) {
        BackpackRecipeTransfer.openCraftingTab(menu);
        if (!(Minecraft.getInstance().screen instanceof StorageScreenBase<?> screen) || screen.getMenu() != menu) return;

        var control = screen.getUpgradeSettingsControl();
        for (var child : control.children()) {
            if (child instanceof UpgradeSettingsTab<?> tab && control.getOpenTab().orElse(null) != tab) {
                tab.onAfterInit();
            }
        }
    }
}
