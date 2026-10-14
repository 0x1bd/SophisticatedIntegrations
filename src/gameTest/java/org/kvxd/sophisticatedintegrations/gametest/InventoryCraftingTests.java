package org.kvxd.sophisticatedintegrations.gametest;

import com.mojang.authlib.GameProfile;
import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import com.tom.storagemod.components.WorldPos;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.client.InventoryRecipePreview;
import org.kvxd.sophisticatedintegrations.crafting.CraftingBackpackFinder;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingAccess;
import org.kvxd.sophisticatedintegrations.crafting.InventoryRecipeRouting;
import org.kvxd.sophisticatedintegrations.crafting.InventoryCraftingSession;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;
import org.kvxd.sophisticatedintegrations.crafting.RecipeIngredientPlanner;
import org.kvxd.sophisticatedintegrations.network.InventoryCraftingAvailabilityPayload;
import org.kvxd.sophisticatedintegrations.config.IntegrationConfig;

import java.util.List;
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

    private static boolean canFill(ServerPlayer player, InventoryCraftingSession session) {
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(CHEST).orElseThrow().value();
        return RecipeIngredientPlanner.plan(player, recipe, session.ingredients()).isPresent();
    }

    private static CraftingTerminalBlockEntity linkedCraftingTerminal(GameTestHelper helper, ServerPlayer player, int planks) {
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.BARREL);
        helper.setBlock(new BlockPos(2, 1, 2), Content.craftingTerminal.get());
        ((Container) helper.getBlockEntity(new BlockPos(2, 1, 1))).setItem(0, new ItemStack(Items.OAK_PLANKS, planks));
        var terminal = (CraftingTerminalBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        terminal.onLoad();
        var wireless = new ItemStack(Content.advWirelessTerminal.get());
        wireless.set(Content.boundPosComponent.get(), new WorldPos(player.level().dimension(), terminal.getBlockPos()));
        player.getInventory().setItem(1, wireless);
        return terminal;
    }

    @GameTest(template = "empty")
    public static void linkedCraftingTerminalWorksWithoutCraftingBackpack(GameTestHelper helper) {
        var player = player(helper);
        var terminal = linkedCraftingTerminal(helper, player, 8);
        var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        session.broadcast(player);
        helper.assertTrue(session.available() && canFill(player, session), "A reachable linked crafting terminal must enable inventory previews without a backpack");
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof CraftingTerminalMenu
                && terminal.getCraftingInv().getItems().stream().mapToInt(ItemStack::getCount).sum() == 8
                && terminal.getCraftResult().getItem(0).is(Items.CHEST), "Selecting a 3x3 recipe must open and fill the linked Tom crafting grid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void linkedCraftingTerminalTakesPriorityAndPreservesMaxTransfer(GameTestHelper helper) {
        var player = player(helper);
        var terminal = linkedCraftingTerminal(helper, player, 17);
        var bag = backpack(64, true);
        player.setItemSlot(EquipmentSlot.CHEST, bag);
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, true, 0);
        helper.assertTrue(player.containerMenu instanceof CraftingTerminalMenu
                && terminal.getCraftingInv().getItems().stream().mapToInt(ItemStack::getCount).sum() == 16,
                "The linked Tom grid must take priority and fill two complete chest recipes");
        helper.assertTrue(BackpackWrapper.fromStack(bag).getInventoryHandler().getStackInSlot(0).getCount() == 64
                && ((Container) helper.getBlockEntity(new BlockPos(2, 1, 1))).getItem(0).getCount() == 1,
                "Tom routing must respect the disabled backpack toggle and preserve unused ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void linkedTerminalAndBackpackSavedGridsStaySeparate(GameTestHelper helper) {
        var player = player(helper);
        var terminal = linkedCraftingTerminal(helper, player, 0);
        terminal.getCraftingInv().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        var bag = backpack(0, true);
        player.setItemSlot(EquipmentSlot.CHEST, bag);
        var wrapper = BackpackWrapper.fromStack(bag);
        ((CraftingUpgradeWrapper) wrapper.getUpgradeHandler().getSlotWrappers().get(0)).getInventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        session.broadcast(player);
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(CHEST).orElseThrow().value();
        helper.assertTrue(RecipeIngredientPlanner.plan(player, recipe, session.ingredients()).isEmpty()
                && RecipeIngredientPlanner.plan(player, recipe, session.alternativeIngredients()).isEmpty(),
                "Two inaccessible saved grids must never advertise eight combined chest ingredients");
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "An unfillable recipe must not open or clear either grid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallsBackToBackpackWhenLinkedCraftingGridCannotFill(GameTestHelper helper) {
        var player = player(helper);
        linkedCraftingTerminal(helper, player, 0);
        var bag = backpack(0, true);
        player.setItemSlot(EquipmentSlot.CHEST, bag);
        var wrapper = BackpackWrapper.fromStack(bag);
        ((CraftingUpgradeWrapper) wrapper.getUpgradeHandler().getSlotWrappers().get(0)).getInventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 8));
        var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        session.broadcast(player);
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(CHEST).orElseThrow().value();
        helper.assertTrue(RecipeIngredientPlanner.plan(player, recipe, session.alternativeIngredients()).isPresent(), "The independent backpack snapshot must expose the executable fallback");
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer
                && crafting((BackpackContainer) player.containerMenu).getSlots().getLast().getItem().is(Items.CHEST),
                "An unfillable linked grid must fall back to the independently fillable backpack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalRoutingStagesCursorAndReturnsOccupiedGrids(GameTestHelper helper) {
        var player = player(helper);
        var terminal = linkedCraftingTerminal(helper, player, 6);
        terminal.getCraftingInv().setItem(0, new ItemStack(Items.OAK_PLANKS));
        terminal.getCraftingInv().setItem(4, new ItemStack(Items.DIAMOND, 2));
        player.inventoryMenu.setCarried(new ItemStack(Items.OAK_PLANKS));
        player.inventoryMenu.getSlot(1).set(new ItemStack(Items.COBBLESTONE, 3));
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof CraftingTerminalMenu
                && terminal.getCraftResult().getItem(0).is(Items.CHEST), "Saved Tom grid, cursor and network ingredients must fill the preferred grid together");
        helper.assertTrue(player.inventoryMenu.getCarried().isEmpty() && !player.inventoryMenu.getSlot(1).hasItem()
                && player.getInventory().countItem(Items.DIAMOND) == 2 && player.getInventory().countItem(Items.COBBLESTONE) == 3,
                "Replacing either saved grid must return every unused item without feeding network void upgrades");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalRoutingKeepsLocalSmallRecipesAndRejectsOutOfRangeLinks(GameTestHelper helper) {
        var player = player(helper);
        linkedCraftingTerminal(helper, player, 8);
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("crafting_table"), false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Locally fillable 2x2 recipes must remain in normal inventory");
        player.getInventory().setItem(0, backpack(8, true));
        player.setPos(player.getX() + 10000, player.getY(), player.getZ());
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer, "Out-of-range linked terminals must not prevent the reachable backpack fallback");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeTerminalRoutingDoesNotDependOnBackpackNetworkSetting(GameTestHelper helper) {
        boolean enabled = IntegrationConfig.BACKPACK_CRAFTING.get();
        IntegrationConfig.BACKPACK_CRAFTING.set(false);
        try {
            var player = player(helper);
            var terminal = linkedCraftingTerminal(helper, player, 8);
            var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
            session.broadcast(player);
            helper.assertTrue(session.available() && canFill(player, session), "Disabling backpack network refill must preserve native linked-terminal crafting");
            InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 1);
            helper.assertTrue(player.containerMenu instanceof CraftingTerminalMenu && player.containerMenu.getCarried().is(Items.CHEST)
                    && terminal.getCraftingInv().getItems().stream().allMatch(ItemStack::isEmpty),
                    "The preferred Tom grid must support direct cursor crafting independently of backpack refill configuration");
        } finally {
            IntegrationConfig.BACKPACK_CRAFTING.set(enabled);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preferredAndFallbackSnapshotsPublishAtomically(GameTestHelper helper) {
        var session = new InventoryCraftingSession();
        var preferred = new NetworkIngredient(new ItemStack(Items.COBBLESTONE), 7);
        var fallback = new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 8);
        session.receive(new InventoryCraftingAvailabilityPayload(1, 0, 1, true, List.of(preferred)));
        session.receive(new InventoryCraftingAvailabilityPayload(2, 0, 2, true, false, List.of(preferred)));
        helper.assertTrue(session.alternativeIngredients().isEmpty(), "A partially received revision must retain its previous destination snapshots");
        session.receive(new InventoryCraftingAvailabilityPayload(2, 1, 2, true, true, List.of(fallback)));
        helper.assertTrue(session.ingredients().size() == 1 && session.ingredients().getFirst().template().is(Items.COBBLESTONE)
                && session.alternativeIngredients().size() == 1 && session.alternativeIngredients().getFirst().template().is(Items.OAK_PLANKS),
                "Complete revisions must publish independent sources without merging their ingredient pools");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void previewTracksSelectedBackpackAndSavedGrid(GameTestHelper helper) {
        var player = player(helper);
        var equipped = backpack(7, true);
        player.setItemSlot(EquipmentSlot.CHEST, equipped);
        player.getInventory().setItem(0, backpack(64, true));
        var wrapper = BackpackWrapper.fromStack(equipped);
        var grid = ((CraftingUpgradeWrapper) wrapper.getUpgradeHandler().getSlotWrappers().get(0)).getInventory();
        grid.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));
        grid.setStackInSlot(4, new ItemStack(Items.STRING));
        var access = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(canFill(player, access), "The saved grid and selected backpack must supply eight chest ingredients together");
        helper.assertTrue(access.ingredients().stream().filter(item -> item.template().is(Items.OAK_PLANKS)).mapToLong(NetworkIngredient::quantity).sum() == 8,
                "Ingredients in an unselected crafting backpack must not inflate the preview");
        var client = new InventoryCraftingSession();
        for (var packet : ((TestPlayerConnection) player.connection).packets()) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof InventoryCraftingAvailabilityPayload payload)
                client.receive(payload);
        }
        helper.assertTrue(client.available() && canFill(player, client), "The inventory screen must receive the real ingredient quantities before opening the backpack");
        grid.setStackInSlot(0, ItemStack.EMPTY);
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(!canFill(player, access), "Removing an ingredient must revoke craftability while keeping routing available");
        helper.assertTrue(grid.getStackInSlot(4).is(Items.STRING), "Previewing missing ingredients must preserve the occupied crafting grid");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(canFill(player, access), "Selecting the next crafting backpack must refresh its ingredient snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPreviewTracksWirelessAccess(GameTestHelper helper) {
        var player = player(helper);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.BARREL);
        helper.setBlock(new BlockPos(2, 1, 2), Content.terminal.get());
        ((Container) helper.getBlockEntity(new BlockPos(2, 1, 1))).setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        var terminal = (StorageTerminalBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        terminal.onLoad();
        player.setItemSlot(EquipmentSlot.CHEST, backpack(0, true));
        var wireless = new ItemStack(Content.advWirelessTerminal.get());
        wireless.set(Content.boundPosComponent.get(), new WorldPos(player.level().dimension(), terminal.getBlockPos()));
        player.getInventory().setItem(1, wireless);
        var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(canFill(player, session), "The first inventory preview must refresh the linked terminal without requiring its screen to be opened first");
        player.setPos(player.getX() + 10000, player.getY(), player.getZ());
        InventoryRecipeRouting.broadcast(player);
        helper.assertTrue(!canFill(player, session), "Moving out of Tom's wireless range must revoke preview ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPreviewPublishesCompleteSnapshots(GameTestHelper helper) {
        var player = player(helper);
        var session = new InventoryCraftingSession();
        var planks = new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 7);
        session.receive(new InventoryCraftingAvailabilityPayload(1, 0, 1, true, List.of(planks)));
        helper.assertTrue(!canFill(player, session), "Seven planks must not preview a complete chest recipe");
        session.receive(new InventoryCraftingAvailabilityPayload(2, 0, 2, true, List.of(planks)));
        helper.assertTrue(session.available() && session.ingredients().size() == 1, "The previous snapshot must remain visible until every new chunk arrives");
        session.receive(new InventoryCraftingAvailabilityPayload(2, 1, 2, true, List.of(new NetworkIngredient(new ItemStack(Items.BIRCH_PLANKS), 1))));
        helper.assertTrue(canFill(player, session), "Publishing the complete snapshot must include mixed ingredient variants");
        session.receive(new InventoryCraftingAvailabilityPayload(3, 0, 1, false, List.of()));
        helper.assertTrue(!session.available() && session.ingredients().isEmpty(), "Removing the crafting backpack must clear all preview ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void replacesOccupiedBackpackGridFromInventory(GameTestHelper helper) {
        var player = player(helper);
        var stack = backpack(8, true);
        player.setItemSlot(EquipmentSlot.CHEST, stack);
        var wrapper = BackpackWrapper.fromStack(stack);
        var savedGrid = ((CraftingUpgradeWrapper) wrapper.getUpgradeHandler().getSlotWrappers().get(0)).getInventory();
        savedGrid.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));
        savedGrid.setStackInSlot(4, new ItemStack(Items.DIAMOND, 2));
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer, "The selected recipe must open the equipped backpack");
        var menu = (BackpackContainer) player.containerMenu;
        var upgrade = crafting(menu);
        int planks = upgrade.getRecipeSlots().stream().map(Slot::getItem).filter(s -> s.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(planks == 8 && upgrade.getSlots().getLast().getItem().is(Items.CHEST),
                "Selecting a chest must replace the saved grid; plank count=" + planks + ", grid="
                        + upgrade.getRecipeSlots().stream().map(Slot::getItem).toList());
        int cobble = player.getInventory().countItem(Items.COBBLESTONE);
        int diamonds = player.getInventory().countItem(Items.DIAMOND);
        for (int i = 0; i < wrapper.getInventoryHandler().getSlots(); i++) {
            var item = wrapper.getInventoryHandler().getStackInSlot(i);
            if (item.is(Items.COBBLESTONE)) cobble += item.getCount();
            if (item.is(Items.DIAMOND)) diamonds += item.getCount();
        }
        helper.assertTrue(cobble == 3 && diamonds == 2, "Old grid ingredients must be returned intact to storage or player inventory");
        helper.succeed();
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
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("crafting_table"), false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Small recipes with local ingredients must stay in the player inventory");
        InventoryRecipeRouting.openAndFill(player, 99, CHEST, false, 0);
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("missing_recipe"), false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Stale menu ids and unknown recipes must not open a backpack");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        InventoryRecipeRouting.openAndFill(player, 0, CHEST, false, 0);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Removing the backpack must revoke automatic opening immediately");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smallRecipesCanUseBackpackIngredients(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, backpack(4, true));
        InventoryRecipeRouting.openAndFill(player, 0, ResourceLocation.withDefaultNamespace("crafting_table"), false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer,
                "A small recipe missing local ingredients must be allowed to open the crafting backpack");
        helper.assertTrue(crafting((BackpackContainer) player.containerMenu).getSlots().getLast().getItem().is(Items.CRAFTING_TABLE),
                "The backpack must fill the small recipe from its real ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openingInventoryResendsUnchangedSnapshot(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, backpack(8, true));
        var session = ((InventoryCraftingAccess) player.inventoryMenu).sophisticatedIntegrations$inventoryCraftingSession();
        session.broadcast(player);
        var packets = ((TestPlayerConnection) player.connection).packets();
        int initial = packets.size();
        session.broadcast(player);
        helper.assertTrue(packets.size() == initial, "Idle previews must not resend unchanged ingredients");
        session.broadcast(player, true);
        helper.assertTrue(packets.size() == initial + 1, "Opening inventory must receive a snapshot even if server ingredients are unchanged");
        var freshClient = new InventoryCraftingSession();
        var last = (ClientboundCustomPayloadPacket) packets.getLast();
        freshClient.receive((InventoryCraftingAvailabilityPayload) last.payload());
        helper.assertTrue(canFill(player, freshClient), "A fresh client menu must recover craftability from the forced snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cachedPreviewTracksNetworkLocalAndCursorChanges(GameTestHelper helper) {
        var player = player(helper);
        var preview = new InventoryRecipePreview();
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(CHEST).orElseThrow().value();
        var network = List.of(new NetworkIngredient(new ItemStack(Items.OAK_PLANKS), 7));
        helper.assertTrue(!preview.canFill(player, player.inventoryMenu, recipe, network, false), "Seven external planks cannot fill a chest");
        player.inventoryMenu.setCarried(new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(preview.canFill(player, player.inventoryMenu, recipe, network, false), "A cursor ingredient must invalidate the cached missing result");
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(preview.canFill(player, player.inventoryMenu, recipe, network, false)
                && !preview.canFill(player, player.inventoryMenu, recipe, network, true), "Combined and local craftability must remain separate");
        helper.assertTrue(!preview.canFill(player, player.inventoryMenu, recipe, List.of(), false), "Revoking external access must invalidate cached craftability");
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 8));
        helper.assertTrue(preview.canFill(player, player.inventoryMenu, recipe, List.of(), true), "A local count change must invalidate the cached local result");
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

    @GameTest(template = "empty")
    public static void cursorIngredientsOpenBackpackAndInvalidateLocalPreview(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, backpack(0, true));
        var recipeId = ResourceLocation.withDefaultNamespace("oak_planks");
        var recipe = (CraftingRecipe) player.level().getRecipeManager().byKey(recipeId).orElseThrow().value();
        var preview = new InventoryRecipePreview();
        player.inventoryMenu.setCarried(new ItemStack(Items.OAK_LOG));
        helper.assertTrue(preview.canFill(player, player.inventoryMenu, recipe, List.of(), false)
                && !preview.canFill(player, player.inventoryMenu, recipe, List.of(), true),
                "Cursor-only ingredients must route through the backpack rather than advertise vanilla JEI transfer");
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG));
        helper.assertTrue(preview.canFill(player, player.inventoryMenu, recipe, List.of(), true),
                "Moving an ingredient from cursor to a slot must invalidate the native-local preview even when total ingredients stay equal");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        player.inventoryMenu.setCarried(new ItemStack(Items.OAK_LOG));
        InventoryRecipeRouting.openAndFill(player, 0, recipeId, false, 0);
        helper.assertTrue(player.containerMenu instanceof BackpackContainer
                && crafting((BackpackContainer) player.containerMenu).getSlots().getLast().getItem().is(Items.OAK_PLANKS),
                "Server routing must allow small cursor-dependent recipes and preserve their ingredient through menu cleanup");
        helper.succeed();
    }
}
