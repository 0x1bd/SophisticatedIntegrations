package org.kvxd.sophisticatedintegrations.gametest;

import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import com.tom.storagemod.menu.StorageTerminalMenu;
import com.tom.storagemod.network.DataPacket;
import com.tom.storagemod.util.TerminalSyncManager;
import com.tom.storagemod.util.TerminalSyncManager.SlotAction;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.backpack.BackpackInventories;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalContext;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;

import java.util.Map;
import java.util.UUID;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class TerminalIntegrationTests {
    private static final BlockPos TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos BARREL = new BlockPos(2, 1, 1);

    private static ServerPlayer player(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "integration-test"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        new TestPlayerConnection(player, cookie);
        BlockPos pos = helper.absolutePos(TERMINAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        return player;
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper, boolean crafting) {
        helper.setBlock(BARREL, Blocks.BARREL);
        helper.setBlock(TERMINAL, crafting ? Content.craftingTerminal.get() : Content.terminal.get());
        StorageTerminalBlockEntity terminal = (StorageTerminalBlockEntity) helper.getBlockEntity(TERMINAL);
        terminal.onLoad();
        return terminal;
    }

    private static StorageTerminalMenu menu(ServerPlayer player, StorageTerminalBlockEntity terminal) {
        StorageTerminalMenu menu = terminal instanceof CraftingTerminalBlockEntity crafting
                ? new CraftingTerminalMenu(1, player.getInventory(), crafting)
                : new StorageTerminalMenu(1, player.getInventory(), terminal);
        player.containerMenu = menu;
        return menu;
    }

    private static IBackpackWrapper backpack(ServerPlayer player) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());
        player.getInventory().setItem(0, backpack);
        return BackpackWrapper.fromStack(backpack);
    }

    private static TerminalSession session(StorageTerminalMenu menu) {
        return ((IntegrationMenu) menu).sophisticatedIntegrations$getSession();
    }

    private static Map<StoredItemStack, TerminalItemStack> view(StorageTerminalMenu menu, StorageTerminalBlockEntity terminal) {
        return TerminalContext.run(session(menu), terminal::getStacks);
    }

    private static long quantity(Map<? extends StoredItemStack, TerminalItemStack> items, ItemStack template) {
        return items.values().stream().filter(s -> ItemStack.isSameItemSameComponents(s.getStack(), template))
                .mapToLong(StoredItemStack::getQuantity).sum();
    }

    private static long clientQuantity(ServerPlayer player, ItemStack template) {
        TerminalSyncManager client = new TerminalSyncManager(player.registryAccess());
        for (var packet : ((TestPlayerConnection) player.connection).packets()) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof DataPacket data) {
                client.receiveUpdate(player.registryAccess(), data.tag());
            }
        }
        return client.getAsList().stream().filter(s -> ItemStack.isSameItemSameComponents(s.getStack(), template))
                .mapToLong(StoredItemStack::getQuantity).sum();
    }

    @GameTest(template = "empty")
    public static void viewerIsolationAndNetworkAggregation(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ((Container) helper.getBlockEntity(BARREL)).setItem(0, new ItemStack(Items.COPPER_INGOT, 20));
        terminal.getStacks();
        terminal.updateServer();
        ServerPlayer alice = player(helper);
        ServerPlayer bob = player(helper);
        backpack(alice).getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 7));
        backpack(bob).getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        StorageTerminalMenu aliceMenu = menu(alice, terminal);
        StorageTerminalMenu bobMenu = menu(bob, terminal);
        helper.assertTrue(quantity(view(aliceMenu, terminal), new ItemStack(Items.COPPER_INGOT)) == 27,
                "Alice's menu must aggregate the network and her own backpack");
        helper.assertTrue(quantity(view(bobMenu, terminal), new ItemStack(Items.COPPER_INGOT)) == 20
                        && quantity(view(bobMenu, terminal), new ItemStack(Items.DIAMOND)) == 3,
                "Bob's menu must never contain Alice's personal inventory");
        helper.assertTrue(quantity(terminal.getStacks(), new ItemStack(Items.COPPER_INGOT)) == 20
                        && quantity(terminal.getStacks(), new ItemStack(Items.DIAMOND)) == 0,
                "The shared block entity must remain network-only outside the viewer's operation");
        aliceMenu.broadcastChanges();
        bobMenu.broadcastChanges();
        helper.assertTrue(clientQuantity(alice, new ItemStack(Items.COPPER_INGOT)) == 27
                        && clientQuantity(bob, new ItemStack(Items.COPPER_INGOT)) == 20
                        && clientQuantity(alice, new ItemStack(Items.DIAMOND)) == 0,
                "Upstream sync packets must send each client only its personal combined item list");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalClicksExtractFromLiveBackpacks(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 9));
        StorageTerminalMenu menu = menu(player, terminal);
        menu.onInteract(new StoredItemStack(new ItemStack(Items.COPPER_INGOT)), SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().getCount() == 9 && backpack.getInventoryHandler().getStackInSlot(0).isEmpty(),
                "Native terminal click must extract actual backpack items without a custom menu");
        menu.setCarried(ItemStack.EMPTY);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        ItemStack equippedBackpack = player.getInventory().getItem(0);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        menu.onInteract(new StoredItemStack(new ItemStack(Items.DIAMOND)), SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().isEmpty() && backpack.getInventoryHandler().getStackInSlot(0).getCount() == 4,
                "An unequipped backpack must not remain accessible through a cached handler");
        player.getInventory().setItem(0, equippedBackpack);
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!session(menu).canAccess(), "A closed terminal menu must lose personal backpack access");
        helper.assertTrue(TerminalContext.run(session(menu), () -> terminal.pullStack(new StoredItemStack(new ItemStack(Items.DIAMOND)), 1)) == null,
                "A stale menu operation must not extract from a still-equipped backpack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractionSpansNetworkAndBackpack(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        Container barrel = (Container) helper.getBlockEntity(BARREL);
        barrel.setItem(0, new ItemStack(Items.COPPER_INGOT, 40));
        terminal.getStacks();
        terminal.updateServer();
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 40));
        StorageTerminalMenu menu = menu(player, terminal);
        menu.onInteract(new StoredItemStack(new ItemStack(Items.COPPER_INGOT)), SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().getCount() == 64 && barrel.getItem(0).isEmpty()
                        && backpack.getInventoryHandler().getStackInSlot(0).getCount() == 16,
                "Take one legal stack, exhausting network storage before using the backpack; carried=" + menu.getCarried()
                        + ", network=" + barrel.getItem(0) + ", backpack=" + backpack.getInventoryHandler().getStackInSlot(0));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void insertionFallsBackAndPreservesRemainders(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        Container barrel = (Container) helper.getBlockEntity(BARREL);
        for (int i = 0; i < barrel.getContainerSize(); i++) barrel.setItem(i, new ItemStack(Items.STONE, 64));
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        var inventory = backpack.getInventoryHandler();
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, new ItemStack(Items.STONE, 64));
        inventory.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 63));
        StorageTerminalMenu menu = menu(player, terminal);
        menu.setCarried(new ItemStack(Items.COPPER_INGOT, 10));
        menu.onInteract(null, SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().getCount() == 9 && inventory.getStackInSlot(0).getCount() == 64,
                "Full storage must return every item that cannot fit in the backpack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeViewerTransferUsesBackpackIngredients(GameTestHelper helper) {
        CraftingTerminalBlockEntity terminal = (CraftingTerminalBlockEntity) terminal(helper, true);
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 8));
        CraftingTerminalMenu menu = (CraftingTerminalMenu) menu(player, terminal);
        CompoundTag transfer = new CompoundTag();
        transfer.putString("fill", "minecraft:crafting_table");
        menu.receive(transfer);
        helper.assertTrue(terminal.getCraftResult().getItem(0).is(Items.CRAFTING_TABLE)
                        && backpack.getInventoryHandler().getStackInSlot(0).getCount() == 4,
                "Upstream recipe transfer must find fresh backpack ingredients and fill the native crafting grid");
        terminal.craft(player);
        helper.assertTrue(backpack.getInventoryHandler().getStackInSlot(0).isEmpty()
                        && terminal.getCraftResult().getItem(0).is(Items.CRAFTING_TABLE),
                "Taking the crafting result must refill the recipe from the player's backpack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void backpackFiltersAreHonored(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 7));
        backpack.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.FILTER_UPGRADE.get()));
        var filter = backpack.getUpgradeHandler().getTypeWrappers(FilterUpgradeItem.TYPE).getFirst();
        filter.getFilterLogic().setDepositFilterType(ContentsFilterType.BLOCK);
        filter.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
        StorageTerminalMenu menu = menu(player, terminal);
        helper.assertTrue(quantity(view(menu, terminal), new ItemStack(Items.COPPER_INGOT)) == 0,
                "Output-filtered items must not be advertised to terminals or recipe viewers");
        menu.onInteract(new StoredItemStack(new ItemStack(Items.COPPER_INGOT)), SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().isEmpty() && backpack.getInventoryHandler().getStackInSlot(0).getCount() == 7,
                "Terminal extraction must not bypass a backpack's output filter");
        helper.assertTrue(BackpackInventories.insert(player, new ItemStack(Items.COPPER_INGOT, 2)).getCount() == 2,
                "Terminal insertion must preserve items rejected by the input filter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void countsUpdateWithoutNetworkChanges(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 10));
        StorageTerminalMenu menu = menu(player, terminal);
        view(menu, terminal);
        int before = session(menu).revision();
        view(menu, terminal);
        helper.assertTrue(session(menu).revision() == before, "Unchanged contents must not resend the item list");
        backpack.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 9));
        view(menu, terminal);
        helper.assertTrue(session(menu).revision() > before, "A backpack-only count change must update native terminal sync");
        menu.broadcastChanges();
        helper.assertTrue(clientQuantity(player, new ItemStack(Items.COPPER_INGOT)) == 9,
                "Native packet decoding must expose the exact backpack count to every recipe viewer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void componentVariantsAndSharedBackpacks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        ItemStack named = new ItemStack(Items.COPPER_INGOT, 4);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special copper"));
        backpack.getInventoryHandler().setStackInSlot(0, named);
        backpack.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 3));
        player.getInventory().setItem(1, player.getInventory().getItem(0).copy());
        var items = BackpackInventories.snapshot(player);
        helper.assertTrue(items.size() == 2 && quantity(items, named) == 4
                        && quantity(items, new ItemStack(Items.COPPER_INGOT)) == 3,
                "Components must remain distinct and the same backpack UUID must only be counted once");
        ItemStack input = new ItemStack(Items.COPPER_INGOT, 2);
        BackpackInventories.insert(player, input);
        helper.assertTrue(input.getCount() == 2, "Capability insertion must not mutate the caller's input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void contextIsRestoredOnFailure(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ServerPlayer player = player(helper);
        backpack(player).getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
        StorageTerminalMenu menu = menu(player, terminal);
        try {
            TerminalContext.run(session(menu), () -> {
                throw new IllegalStateException("intentional test failure");
            });
        } catch (IllegalStateException expected) {
            helper.assertTrue(TerminalContext.forTerminal(terminal) == null,
                    "An exception must never leave a player's backpack accessible to a later server operation");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void backpackCannotInsertIntoItself(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        ItemStack bag = player.getInventory().getItem(0);
        helper.assertTrue(BackpackInventories.insert(player, bag).getCount() == 1
                        && backpack.getInventoryHandler().getStackInSlot(0).isEmpty(),
                "A terminal must not create a self-containing backpack or duplicate its UUID");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stackUpgradesKeepExactCounts(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = terminal(helper, false);
        ServerPlayer player = player(helper);
        IBackpackWrapper backpack = backpack(player);
        backpack.getInventoryHandler();
        backpack.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        ItemStack remainder = backpack.getInventoryHandler().insertItem(0, new ItemStack(Items.COPPER_INGOT, 200), false);
        helper.assertTrue(remainder.isEmpty(), "Sophisticated's stack upgrade should accept more than a vanilla stack");
        StorageTerminalMenu menu = menu(player, terminal);
        menu.broadcastChanges();
        helper.assertTrue(clientQuantity(player, new ItemStack(Items.COPPER_INGOT)) == 200,
                "Tom's native client sync must retain the exact upgraded backpack count rather than cap it to 64");
        menu.onInteract(new StoredItemStack(new ItemStack(Items.COPPER_INGOT)), SlotAction.PULL_OR_PUSH_STACK, false);
        helper.assertTrue(menu.getCarried().getCount() == 64 && backpack.getInventoryHandler().getStackInSlot(0).getCount() == 136,
                "A withdrawal must return one legal stack while preserving the remaining upgraded storage count");
        helper.succeed();
    }
}
