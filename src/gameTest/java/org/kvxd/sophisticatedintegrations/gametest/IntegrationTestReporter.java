package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.LogTestReporter;

public final class IntegrationTestReporter extends LogTestReporter {
    @Override
    public void onTestSuccess(GameTestInfo test) {
        LogUtils.getLogger().info("Integration GameTest passed: {}", test.getTestName());
    }
}
