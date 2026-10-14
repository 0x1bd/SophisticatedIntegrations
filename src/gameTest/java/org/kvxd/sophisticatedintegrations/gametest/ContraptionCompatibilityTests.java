package org.kvxd.sophisticatedintegrations.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class ContraptionCompatibilityTests {
    private static void verify(GameTestHelper helper, boolean crafting) {
        if (ModList.get().isLoaded("createcontraptionterminals")) ContraptionTestFixture.verify(helper, crafting);
        else helper.succeed();
    }

    @GameTest(template = "empty")
    public static void movingStorageTerminalUsesMountedItems(GameTestHelper helper) { verify(helper, false); }

    @GameTest(template = "empty")
    public static void movingCraftingTerminalUsesMountedItems(GameTestHelper helper) { verify(helper, true); }
}
