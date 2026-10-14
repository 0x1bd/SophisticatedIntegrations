package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.client.InventoryRecipePreview;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;

import java.util.List;
import java.util.UUID;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class InventoryRecipePreviewTests {
    @GameTest(template = "empty")
    public static void alternativeDestinationsStayIndependentAndInvalidateCaches(GameTestHelper helper) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "preview-test"), false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        new TestPlayerConnection(player, cookie);
        var recipe = (CraftingRecipe) player.level().getRecipeManager()
                .byKey(ResourceLocation.withDefaultNamespace("chest")).orElseThrow().value();
        var preview = new InventoryRecipePreview();
        var primary = List.of(new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 4));
        var partialFallback = List.of(new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 4));
        var fullFallback = List.of(new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 8));
        helper.assertTrue(!preview.canFillAny(player, player.inventoryMenu, recipe, primary, partialFallback),
                "Separate crafting grids must not combine their unavailable ingredient sources");
        helper.assertTrue(preview.canFillAny(player, player.inventoryMenu, recipe, primary, fullFallback),
                "A complete fallback must remain craftable when the preferred destination cannot fill the recipe");
        helper.assertTrue(!preview.canFillAny(player, player.inventoryMenu, recipe, primary, List.of()),
                "Revoking the fallback must invalidate its previously craftable result");
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
        helper.assertTrue(preview.canFillAny(player, player.inventoryMenu, recipe, primary, List.of()),
                "Local ingredients must still supplement each destination independently");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        helper.assertTrue(!preview.canFillAny(player, player.inventoryMenu, recipe, primary, partialFallback),
                "Removing local ingredients must invalidate both destination caches");
        helper.succeed();
    }
}
