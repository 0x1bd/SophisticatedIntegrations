package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.authlib.GameProfile;
import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.components.WorldPos;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingContext;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingMenu;
import org.kvxd.sophisticatedintegrations.crafting.BackpackRecipeTransfer;
import org.kvxd.sophisticatedintegrations.crafting.LinkedTerminalAccess;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.network.BackpackNetworkPayload;

import java.util.List;
import java.util.UUID;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class BackpackCraftingTests {
    private static final BlockPos TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos BARREL = new BlockPos(2, 1, 1);
    private static final ResourceLocation RECIPE = ResourceLocation.withDefaultNamespace("crafting_table");

    private static ServerPlayer player(GameTestHelper helper) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "backpack-crafting-test"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        new TestPlayerConnection(player, cookie);
        BlockPos pos = helper.absolutePos(TERMINAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        return player;
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper, int planks) {
        helper.setBlock(BARREL, Blocks.BARREL);
        helper.setBlock(TERMINAL, Content.terminal.get());
        ((Container) helper.getBlockEntity(BARREL)).setItem(0, new ItemStack(Items.OAK_PLANKS, planks));
        var terminal = (StorageTerminalBlockEntity) helper.getBlockEntity(TERMINAL);
        terminal.onLoad();
        terminal.getStacks();
        terminal.updateServer();
        return terminal;
    }

    private static BackpackContainer menu(ServerPlayer player, StorageTerminalBlockEntity terminal, int localPlanks) {
        ItemStack stack = new ItemStack(ModItems.BACKPACK.get());
        player.getInventory().setItem(0, stack);
        var wrapper = BackpackWrapper.fromStack(stack);
        wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, localPlanks));
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
        ItemStack wireless = new ItemStack(Content.advWirelessTerminal.get());
        wireless.set(Content.boundPosComponent.get(), new WorldPos(player.level().dimension(), terminal.getBlockPos()));
        player.getInventory().setItem(1, wireless);
        BackpackContainer menu = new BackpackContainer(7, player, new BackpackContext.Item("main", 0));
        player.containerMenu = menu;
        crafting(menu).setIsOpen(true);
        menu.setOpenTabId(crafting(menu).getUpgradeContainerId());
        return menu;
    }

    private static CraftingUpgradeContainer crafting(BackpackContainer menu) {
        return (CraftingUpgradeContainer) menu.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).orElseThrow();
    }

    private static int gridCount(BackpackContainer menu) {
        return crafting(menu).getRecipeSlots().stream().map(Slot::getItem).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty")
    public static void mixedIngredientsAndNativeRefill(GameTestHelper helper) {
        var terminal = terminal(helper, 6);
        var player = player(helper);
        var menu = menu(player, terminal, 2);
        BackpackRecipeTransfer.fill(player, 7, RECIPE, false, 0);
        helper.assertTrue(gridCount(menu) == 4 && ((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 4
                        && menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).isEmpty(),
                "Backpack crafting must combine local ingredients with the bound network, using local ingredients first");
        var upgrade = crafting(menu);
        upgrade.setRefillCraftingGrid(true);
        player.containerMenu.clicked(upgrade.getSlots().getLast().index, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.CRAFTING_TABLE) && gridCount(menu) == 4
                        && ((Container) helper.getBlockEntity(BARREL)).getItem(0).isEmpty(),
                "The native crafting result and enabled refill must consume exactly four more linked network ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void maxTransferKeepsCompleteSets(GameTestHelper helper) {
        var terminal = terminal(helper, 17);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        BackpackRecipeTransfer.fill(player, 7, RECIPE, true, 0);
        helper.assertTrue(gridCount(menu) == 16 && crafting(menu).getRecipeSlots().stream().filter(Slot::hasItem)
                        .allMatch(slot -> slot.getItem().getCount() == 4)
                        && ((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 1,
                "Maximum transfer must stop after complete sets and return the last incomplete network reservation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void refillToggleRemainsOptional(GameTestHelper helper) {
        var terminal = terminal(helper, 8);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        BackpackRecipeTransfer.fill(player, 7, RECIPE, false, 0);
        var upgrade = crafting(menu);
        upgrade.setRefillCraftingGrid(false);
        menu.clicked(upgrade.getSlots().getLast().index, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.CRAFTING_TABLE) && gridCount(menu) == 0
                        && ((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 4,
                "Disabling Sophisticated's refill toggle must leave the remaining network ingredients untouched");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialNativeTransferRestoresNetwork(GameTestHelper helper) {
        var terminal = terminal(helper, 3);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        var grid = crafting(menu).getRecipeSlots();
        List<ItemStack> required = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> i == 0 || i == 1 || i == 3 || i == 4 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY).toList();
        CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, RECIPE, RecipeType.CRAFTING, required,
                grid.stream().map(slot -> slot.index).toList(), menu.slots.stream().filter(slot -> slot.mayPickup(player)).map(slot -> slot.index).toList(), false);
        helper.assertTrue(gridCount(menu) == 0 && ((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 3,
                "The native helper must restore network ingredients when a full recipe cannot be completed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void liveLinkAndRangeChecks(GameTestHelper helper) {
        var terminal = terminal(helper, 8);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        ItemStack wireless = player.getInventory().getItem(1);
        player.getInventory().setItem(1, ItemStack.EMPTY);
        BackpackRecipeTransfer.fill(player, 7, RECIPE, false, 0);
        helper.assertTrue(gridCount(menu) == 0 && LinkedTerminalAccess.find(player) == null, "Removing the carried wireless terminal must revoke access");
        player.getInventory().setItem(1, wireless);
        player.setPos(player.getX() + 10000, player.getY(), player.getZ());
        BackpackRecipeTransfer.fill(player, 7, RECIPE, false, 0);
        helper.assertTrue(gridCount(menu) == 0 && LinkedTerminalAccess.find(player) == null, "Backpack crafting must obey Tom's wireless range");
        var pos = helper.absolutePos(TERMINAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        BackpackRecipeTransfer.fill(player, 8, RECIPE, false, 0);
        helper.assertTrue(gridCount(menu) == 0, "An old or incorrect menu id must not transfer items");
        player.containerMenu = player.inventoryMenu;
        BackpackRecipeTransfer.fill(player, 7, RECIPE, false, 0);
        helper.assertTrue(((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 8, "Closing the backpack must revoke crafting access");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reservationRestoresOnException(GameTestHelper helper) {
        var terminal = terminal(helper, 1);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        try {
            BackpackCraftingContext.transfer(player, menu, () -> {
                var reservation = BackpackCraftingContext.reserve(menu, new ItemStack(Items.OAK_PLANKS));
                helper.assertTrue(reservation != null && reservation.remove(1).getCount() == 1, "Reserve a real network item");
                throw new IllegalStateException("intentional test failure");
            });
        } catch (IllegalStateException expected) {
            helper.assertTrue(((Container) helper.getBlockEntity(BARREL)).getItem(0).getCount() == 1,
                    "An exception must return the real network item even after the reservation was consumed");
        }
        helper.assertTrue(BackpackCraftingContext.reserve(menu, new ItemStack(Items.OAK_PLANKS)) == null, "Recipe context must not leak after a failure");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rollbackPreservesItemsWhenNetworkFills(GameTestHelper helper) {
        var terminal = terminal(helper, 1);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        Container barrel = (Container) helper.getBlockEntity(BARREL);
        try {
            BackpackCraftingContext.transfer(player, menu, () -> {
                var reservation = BackpackCraftingContext.reserve(menu, new ItemStack(Items.OAK_PLANKS));
                helper.assertTrue(reservation != null, "Reserve a real network item before the network fills");
                reservation.remove(1);
                for (int i = 0; i < barrel.getContainerSize(); i++) barrel.setItem(i, new ItemStack(Items.STONE, 64));
                throw new IllegalStateException("intentional test failure");
            });
        } catch (IllegalStateException expected) {
            helper.assertTrue(player.getInventory().items.stream().filter(stack -> stack.is(Items.OAK_PLANKS))
                            .mapToInt(ItemStack::getCount).sum() == 1,
                    "If the origin cannot accept rollback, return the reserved item to the player instead of losing it");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void snapshotUsesLongCountsAndClearsAccess(GameTestHelper helper) {
        var terminal = terminal(helper, 8);
        var player = player(helper);
        var menu = menu(player, terminal, 0);
        menu.broadcastChanges();
        var packets = ((TestPlayerConnection) player.connection).packets();
        helper.assertTrue(packets.stream().anyMatch(packet -> packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof BackpackNetworkPayload payload && payload.linked()
                        && payload.ingredients().stream().anyMatch(item -> item.template().is(Items.OAK_PLANKS) && item.quantity() == 8)),
                "The backpack menu must synchronize available network ingredients to recipe viewers");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        menu.broadcastChanges();
        helper.assertTrue(packets.stream().anyMatch(packet -> packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof BackpackNetworkPayload payload && !payload.linked() && payload.ingredients().isEmpty()),
                "Losing wireless access must immediately send an empty network snapshot");
        ItemStack named = new ItemStack(Items.COPPER_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Network copper"));
        BackpackNetworkPayload original = new BackpackNetworkPayload(7, 2, 0, 1, true, List.of(new NetworkIngredient(named, 10_000_000_000L)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            BackpackNetworkPayload.STREAM_CODEC.encode(buffer, original);
            var decoded = BackpackNetworkPayload.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.ingredients().getFirst().quantity() == 10_000_000_000L
                            && ItemStack.isSameItemSameComponents(named, decoded.ingredients().getFirst().template()),
                    "The recipe-viewer snapshot codec must preserve long quantities and item components");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
