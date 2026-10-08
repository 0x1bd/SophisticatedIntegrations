package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.crafting.CraftingBackpackFinder;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;

import java.util.UUID;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class InventoryCraftingTests {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("chest");

    private static ServerPlayer player(GameTestHelper helper) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "routing-test"), false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        new TestPlayerConnection(player, cookie);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        return player;
    }

    private static ItemStack backpack(int planks, boolean crafting) {
        var stack = new ItemStack(ModItems.BACKPACK.get());
        var wrapper = BackpackWrapper.fromStack(stack);
        wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, planks));
        if (crafting) wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
        return stack;
    }

    private static CraftingUpgradeContainer crafting(BackpackContainer menu) {
        return (CraftingUpgradeContainer) menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).orElseThrow();
    }

    @GameTest(template = "empty")
    public static void opensEquippedBackpackWithoutWireless(GameTestHelper helper) {
        var player = player(helper);
        var equipped = backpack(8, true);
        var carried = backpack(8, true);
        player.getInventory().setItem(0, carried);
        player.setItemSlot(EquipmentSlot.CHEST, equipped);
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer, "A 3x3 recipe must open a crafting backpack from inventory");
        var menu = (BackpackContainer) player.containerMenu;
        helper.assertTrue(crafting(menu).isOpen() && crafting(menu).getRecipeSlots().stream().map(Slot::getItem).mapToInt(ItemStack::getCount).sum() == 8,
                "The native crafting tab must open and fill without a wireless terminal");
        helper.assertTrue(BackpackWrapper.fromStack(equipped).getInventoryHandler().getStackInSlot(0).isEmpty()
                        && BackpackWrapper.fromStack(carried).getInventoryHandler().getStackInSlot(0).getCount() == 8,
                "Equipped crafting backpacks must take priority over main inventory backpacks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ignoresEquippedBackpackWithoutCrafting(GameTestHelper helper) {
        var player = player(helper);
        player.setItemSlot(EquipmentSlot.CHEST, backpack(8, false));
        var offhand = backpack(8, true);
        player.setItemSlot(EquipmentSlot.OFFHAND, offhand);
        player.getInventory().setItem(0, backpack(8, true));
        helper.assertTrue(CraftingBackpackFinder.find(player).orElseThrow().stack() == offhand,
                "Selection must skip equipped backpacks without a crafting upgrade and prefer offhand over main inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void maxTransferReturnsInventoryCraftingItems(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, backpack(17, true));
        player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
        player.inventoryMenu.getSlot(1).set(new ItemStack(Items.COBBLESTONE, 3));
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, true, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer, "Maximum transfer must open the backpack");
        var menu = (BackpackContainer) player.containerMenu;
        helper.assertTrue(crafting(menu).getRecipeSlots().stream().map(Slot::getItem).mapToInt(ItemStack::getCount).sum() == 16
                        && menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount() == 1,
                "Maximum transfer must fill complete sets and preserve unused ingredients");
        helper.assertTrue(player.inventoryMenu.getCarried().isEmpty() && !player.inventoryMenu.getSlot(1).hasItem()
                        && player.getInventory().countItem(Items.DIAMOND) == 1 && player.getInventory().countItem(Items.COBBLESTONE) == 3,
                "Opening the backpack must return the inventory cursor and 2x2 ingredients without loss");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsSmallRecipesAndStaleRequests(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, backpack(16, true));
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("crafting_table"), false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Recipes fitting 2x2 must stay in the player inventory");
        InventoryRecipeRouting.openAndFill(player, 99, CHEST, false, 0);
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("missing_recipe"), false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Stale menu ids and unknown recipes must not open a backpack");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Removing the backpack must revoke automatic opening immediately");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void availabilityTracksCraftingUpgrade(GameTestHelper helper) {
        var player = player(helper);
        var access = (InventoryCraftingAccess) player.inventoryMenu;
        player.getInventory().setItem(0, backpack(0, false));
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(!access.sophisticatedIntegrations$hasCraftingBackpack(), "A backpack without a crafting upgrade must not enable routing");
        player.getInventory().setItem(0, backpack(0, true));
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(access.sophisticatedIntegrations$hasCraftingBackpack(), "A crafting upgrade must enable inventory recipe routing");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(!access.sophisticatedIntegrations$hasCraftingBackpack(), "Removing the backpack must clear availability");
        helper.succeed();
    }
}
