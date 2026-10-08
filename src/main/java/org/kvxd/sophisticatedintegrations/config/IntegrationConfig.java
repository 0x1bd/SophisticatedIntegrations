package org.kvxd.sophisticatedintegrations.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class IntegrationConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue INSERT_INTO_BACKPACKS;
    public static final ModConfigSpec.BooleanValue BACKPACK_CRAFTING;
    public static final ModConfigSpec.BooleanValue AUTO_OPEN_CRAFTING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("terminal");
        ENABLED = builder.comment("Include the viewing player's carried and equipped Sophisticated Backpacks in Tom's terminals.")
                .define("includeBackpacks", true);
        INSERT_INTO_BACKPACKS = builder.comment("Insert into backpacks when the storage network cannot accept the remaining items.")
                .define("insertIntoBackpacks", true);
        builder.pop();
        builder.push("backpackCrafting");
        BACKPACK_CRAFTING = builder.comment("Use a carried, bound Advanced Wireless Terminal for missing backpack crafting ingredients and refill.")
                .define("useLinkedNetwork", true);
        AUTO_OPEN_CRAFTING = builder.comment("Open an equipped or carried crafting backpack for inventory recipe transfers that need a 3x3 grid.")
                .define("openFromInventory", true);
        builder.pop();
        SPEC = builder.build();
    }

    private IntegrationConfig() {
    }
}
