package org.kvxd.sophisticatedintegrations.gametest;

import com.tom.storagemod.inventory.IInventoryAccess;
import com.tom.storagemod.inventory.PlatformFilteredInventoryAccess;
import com.tom.storagemod.inventory.PlatformItemHandler;
import com.tom.storagemod.inventory.PlatformProxyInventoryAccess;
import com.tom.storagemod.inventory.filter.IFilter;
import com.tom.storagemod.inventory.filter.ItemPredicate;
import com.tom.storagemod.inventory.filter.ItemFilter;
import com.tom.storagemod.Content;
import com.tom.storagemod.util.Priority;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.storage.NetworkStorage;
import org.kvxd.sophisticatedintegrations.storage.StorageMaintenance;
import org.kvxd.sophisticatedintegrations.storage.StorageMaintenanceJob;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class StoragePriorityTests {

    private static IInventoryAccess cobbleFilter(ItemStackHandler inventory, boolean keepLast) {
        return new PlatformFilteredInventoryAccess(new TestStorageInventory(inventory), new IFilter() {
            @Override public ItemPredicate getItemPred() { return item -> item.getStack().is(Items.COBBLESTONE); }
            @Override public boolean isKeepLast() { return keepLast; }
            @Override public Priority getPriority() { return Priority.NORMAL; }
        });
    }

    private static void resort(GameTestHelper helper, IInventoryAccess network) {
        NetworkStorage.consolidate(network, helper.getLevel(), stack -> {
            throw new AssertionError("Unexpected recovery: " + stack);
        });
    }

    @GameTest(template = "empty")
    public static void insertionFillsExistingStacksAcrossInventories(GameTestHelper helper) {
        var first = new ItemStackHandler(2);
        var second = new ItemStackHandler(2);
        second.setStackInSlot(1, new ItemStack(Items.DIAMOND, 12));
        var network = new TestStorageNetwork(new TestStorageInventory(first), new TestStorageInventory(second));
        ItemStack input = new ItemStack(Items.DIAMOND, 5);
        helper.assertTrue(NetworkStorage.insert(network, helper.getLevel(), input).isEmpty()
                && input.getCount() == 5 && first.getStackInSlot(0).isEmpty() && second.getStackInSlot(1).getCount() == 17,
                "Fill a matching stack before an earlier empty inventory without mutating input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void resortCombinesSplitStacksAndIsIdempotent(GameTestHelper helper) {
        var first = new ItemStackHandler(2);
        var second = new ItemStackHandler(2);
        first.setStackInSlot(0, new ItemStack(Items.DIAMOND, 12));
        second.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        var network = new TestStorageNetwork(new TestStorageInventory(first), new TestStorageInventory(second));
        resort(helper, network);
        helper.assertTrue(first.getStackInSlot(0).getCount() == 17 && second.getStackInSlot(0).isEmpty(),
                "Twelve and five diamonds must become one stack of seventeen");
        helper.assertTrue(NetworkStorage.consolidate(network, helper.getLevel(), stack -> {}) == 0,
                "An already consolidated network must not move items again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void explicitFiltersWinOverGenericMatchingStacks(GameTestHelper helper) {
        var generic = new ItemStackHandler(2);
        var designated = new ItemStackHandler(2);
        generic.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 12));
        var network = new TestStorageNetwork(new TestStorageInventory(generic), cobbleFilter(designated, false));
        NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(designated.getStackInSlot(0).getCount() == 5 && generic.getStackInSlot(0).getCount() == 12,
                "An empty chest designated for cobble must win over matching cobble in generic storage");
        resort(helper, network);
        helper.assertTrue(designated.getStackInSlot(0).getCount() == 17 && generic.getStackInSlot(0).isEmpty(),
                "Re-sort must move old cobble into its designated chest");
        NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(generic.getStackInSlot(0).is(Items.DIAMOND) && designated.getStackInSlot(1).isEmpty(),
                "Diamonds must not enter a cobble-only chest");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void configuredPrioritiesAndKeepLastArePreserved(GameTestHelper helper) {
        var designated = new ItemStackHandler(1);
        var high = new ItemStackHandler(1);
        designated.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        var network = new TestStorageNetwork(cobbleFilter(designated, true), new TestStorageInventory(high, Priority.HIGH));
        NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.COBBLESTONE, 2));
        resort(helper, network);
        helper.assertTrue(designated.getStackInSlot(0).getCount() == 1 && high.getStackInSlot(0).getCount() == 9,
                "Explicit higher priority wins, but keep-last must leave the protected item behind");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradedHandlersUseTheirRealCapacity(GameTestHelper helper) {
        var vanilla = new ItemStackHandler(2);
        var wrapper = BackpackWrapper.fromStack(new ItemStack(ModItems.BACKPACK.get()));
        var upgraded = wrapper.getInventoryHandler();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        var network = new TestStorageNetwork(new TestStorageInventory(vanilla), new TestStorageInventory(upgraded));
        helper.assertTrue(NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.DIAMOND, 200)).isEmpty()
                && upgraded.getStackInSlot(0).getCount() == 200 && vanilla.getStackInSlot(0).isEmpty(),
                "An empty Sophisticated slot must accept two hundred diamonds before vanilla slots");
        vanilla.setStackInSlot(0, new ItemStack(Items.DIAMOND, 12));
        upgraded.setStackInSlot(1, new ItemStack(Items.DIAMOND, 5));
        resort(helper, network);
        helper.assertTrue(upgraded.getStackInSlot(0).getCount() == 217
                && upgraded.getStackInSlot(1).isEmpty() && vanilla.getStackInSlot(0).isEmpty(),
                "Re-sort must keep counts above sixty-four and combine all 217 items");
        // Existing matches still come first during ordinary insertion
        upgraded.setStackInSlot(0, ItemStack.EMPTY);
        vanilla.setStackInSlot(0, new ItemStack(Items.DIAMOND, 12));
        NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.DIAMOND, 5));
        helper.assertTrue(vanilla.getStackInSlot(0).getCount() == 17 && upgraded.getStackInSlot(0).isEmpty(),
                "Ordinary deposits must prefer an existing stack over a larger empty slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void resortDoesNotFeedExistingItemsIntoVoidUpgrades(GameTestHelper helper) {
        var vanilla = new ItemStackHandler(1);
        vanilla.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 17));
        var wrapper = BackpackWrapper.fromStack(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));
        var inventory = wrapper.getInventoryHandler();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.VOID_UPGRADE.get()));
        var voidUpgrade = wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).getFirst();
        voidUpgrade.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        voidUpgrade.setVoidType(VoidType.SLOT_OVERFLOW);
        wrapper.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.FILTER_UPGRADE.get()));
        int capacity = inventory.getSlotLimit(0);
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, capacity));
        var cached = new net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler<>(
                () -> wrapper.getInventoryForInputOutput(), () -> 0L);
        var proxy = new PlatformProxyInventoryAccess(new TestStorageInventory(cached));
        var capability = new PlatformItemHandler(() -> proxy);
        var designated = new PlatformFilteredInventoryAccess(new TestStorageInventory(capability), new IFilter() {
            @Override public ItemPredicate getItemPred() { return item -> item.getStack().is(Items.COBBLESTONE); }
            @Override public boolean isKeepLast() { return true; }
            @Override public Priority getPriority() { return Priority.NORMAL; }
        });
        var network = new TestStorageNetwork(new TestStorageInventory(vanilla), designated);
        helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.COBBLESTONE, 17), true).isEmpty(),
                "The overflow void upgrade must simulate accepting items despite the full slot");
        resort(helper, network);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == capacity && vanilla.getStackInSlot(0).getCount() == 17,
                "Re-sort must preserve all items instead of feeding a matching void destination, including filtered wrappers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aliasedStorageDoesNotMoveItemsBackIntoItself(GameTestHelper helper) {
        var inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        var network = new TestStorageNetwork(cobbleFilter(inventory, false), cobbleFilter(inventory, false));
        long moved = NetworkStorage.consolidate(network, helper.getLevel(), stack -> {
            throw new AssertionError("Unexpected recovery: " + stack);
        });
        helper.assertTrue(moved == 0 && inventory.getStackInSlot(0).getCount() == 10,
                "Two views of one physical slot must not report a move back into that same slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sophisticatedPartialStacksConsolidateIntoTheChosenSlot(GameTestHelper helper) {
        var wrapper = BackpackWrapper.fromStack(new ItemStack(ModItems.BACKPACK.get()));
        var inventory = wrapper.getInventoryHandler();
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 30));
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 50));
        var network = new TestStorageInventory(wrapper.getInventoryForInputOutput());
        resort(helper, network);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 16 && inventory.getStackInSlot(1).getCount() == 64,
                "Re-sort must fill the chosen larger stack instead of depositing back into the source slot");
        helper.assertTrue(NetworkStorage.consolidate(network, helper.getLevel(), stack -> {
            throw new AssertionError("Unexpected recovery: " + stack);
        }) == 0, "A consolidated Sophisticated inventory must require no further maintenance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void componentsAndUnstackableItemsStaySeparate(GameTestHelper helper) {
        var inventory = new ItemStackHandler(8);
        var named = new ItemStack(Items.DIAMOND, 5);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named diamonds"));
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 12));
        inventory.setStackInSlot(1, named);
        inventory.setStackInSlot(2, new ItemStack(Items.IRON_SWORD));
        inventory.setStackInSlot(3, new ItemStack(Items.IRON_SWORD));
        var network = new TestStorageInventory(inventory);
        resort(helper, network);
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 12 && inventory.getStackInSlot(1).getCount() == 5
                && inventory.getStackInSlot(1).has(DataComponents.CUSTOM_NAME)
                && inventory.getStackInSlot(2).getCount() == 1 && inventory.getStackInSlot(3).getCount() == 1,
                "Components must never merge and unstackable items must obey their item limit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractionRestrictionsAndInsertionRemaindersArePreserved(GameTestHelper helper) {
        var locked = new ItemStackHandler(1) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        };
        var destination = new ItemStackHandler(1);
        destination.setStackInSlot(0, new ItemStack(Items.DIAMOND, 60));
        locked.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        var network = new TestStorageNetwork(new TestStorageInventory(destination), new TestStorageInventory(locked));
        resort(helper, network);
        helper.assertTrue(locked.getStackInSlot(0).getCount() == 3 && destination.getStackInSlot(0).getCount() == 60,
                "Re-sort must not extract output-locked items");
        ItemStack remainder = NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.DIAMOND, 100));
        helper.assertTrue(remainder.getCount() == 35 && destination.getStackInSlot(0).getCount() == 64
                && locked.getStackInSlot(0).getCount() == 64, "Every item that cannot fit must be returned");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void failedInsertionRestoresExtractedItems(GameTestHelper helper) {
        var origin = new ItemStackHandler(1);
        origin.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        var changing = new ItemStackHandler(1) {
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return simulate ? super.insertItem(slot, stack, true) : stack.copy();
            }
        };
        var network = new TestStorageNetwork(new TestStorageInventory(changing, Priority.HIGH), new TestStorageInventory(origin));
        resort(helper, network);
        helper.assertTrue(origin.getStackInSlot(0).getCount() == 5 && changing.getStackInSlot(0).isEmpty(),
                "If a target stops accepting after simulation, all extracted items must return to their source");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void failedMoveFromVoidStorageRecoversInsteadOfVoiding(GameTestHelper helper) {
        var wrapper = BackpackWrapper.fromStack(new ItemStack(ModItems.DIAMOND_BACKPACK.get()));
        var origin = wrapper.getInventoryHandler();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.VOID_UPGRADE.get()));
        var voidUpgrade = wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).getFirst();
        voidUpgrade.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        voidUpgrade.setVoidType(VoidType.SLOT_OVERFLOW);
        int capacity = origin.getSlotLimit(0);
        origin.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, capacity));
        origin.setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 5));
        var changing = new ItemStackHandler(1) {
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return simulate ? super.insertItem(slot, stack, true) : stack.copy();
            }
        };
        var network = new TestStorageNetwork(new TestStorageInventory(changing, Priority.HIGH), new TestStorageInventory(origin));
        var recovered = new java.util.ArrayList<ItemStack>();
        NetworkStorage.consolidate(network, helper.getLevel(), stack -> recovered.add(stack.copy()));
        int remaining = java.util.stream.IntStream.range(0, origin.getSlots()).map(slot -> origin.getStackInSlot(slot).getCount()).sum();
        int rescued = recovered.stream().mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(rescued > 0 && remaining + rescued == capacity + 5 && changing.getStackInSlot(0).isEmpty(),
                "A failed destination must recover every extracted item without feeding the source void upgrade");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unrestrictedCompositeFiltersDoNotOverrideDesignatedStorage(GameTestHelper helper) {
        var generic = new ItemStackHandler(1);
        generic.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 12));
        var unrestricted = new IFilter() {
            @Override public ItemPredicate getItemPred() { return ItemFilter.TRUE; }
            @Override public boolean isKeepLast() { return false; }
            @Override public Priority getPriority() { return Priority.NORMAL; }
        };
        var designated = new ItemStackHandler(1);
        var network = new TestStorageNetwork(new PlatformFilteredInventoryAccess(new TestStorageInventory(generic),
                new IFilter.MultiFilter(unrestricted, unrestricted)), cobbleFilter(designated, false));
        NetworkStorage.insert(network, helper.getLevel(), new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(generic.getStackInSlot(0).getCount() == 12 && designated.getStackInSlot(0).getCount() == 5,
                "Combining unrestricted connector and chest filters must not make generic storage designated");
        resort(helper, network);
        helper.assertTrue(generic.getStackInSlot(0).isEmpty() && designated.getStackInSlot(0).getCount() == 17,
                "Re-sort must also prefer the actual cobble filter over the unrestricted composite");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sparseResortDoesNotScanEveryPairOfEmptySlots(GameTestHelper helper) {
        int slots = 2_000;
        int[] simulations = {0};
        var inventory = new ItemStackHandler(slots) {
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (simulate) simulations[0]++;
                return super.insertItem(slot, stack, simulate);
            }
        };
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 12));
        inventory.setStackInSlot(slots - 1, new ItemStack(Items.DIAMOND, 5));
        resort(helper, new TestStorageInventory(inventory));
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 17 && inventory.getStackInSlot(slots - 1).isEmpty(),
                "Sparse storage must still consolidate its occupied slots");
        helper.assertTrue(simulations[0] < slots * 8, "Empty slots must not cause quadratic insertion simulations: " + simulations[0]);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void withdrawalsDrainSmallOverflowBeforeUpgradedStorage(GameTestHelper helper) {
        var vanilla = new ItemStackHandler(1);
        vanilla.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        var wrapper = BackpackWrapper.fromStack(new ItemStack(ModItems.BACKPACK.get()));
        var upgraded = wrapper.getInventoryHandler();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        upgraded.insertItem(0, new ItemStack(Items.DIAMOND, 256), false);
        var network = new TestStorageNetwork(new TestStorageInventory(upgraded), new TestStorageInventory(vanilla));
        var withdrawn = NetworkStorage.extract(network, helper.getLevel(), new ItemStack(Items.DIAMOND), 64);
        helper.assertTrue(withdrawn.getCount() == 64 && vanilla.getStackInSlot(0).isEmpty() && upgraded.getStackInSlot(0).getCount() == 197,
                "Withdrawals must drain the five overflow diamonds first so the remaining 197 fit in one upgraded stack");
        helper.assertTrue(NetworkStorage.consolidate(network, helper.getLevel(), stack -> {}) == 0,
                "Withdrawal must leave nothing for manual re-sort to consolidate");
        helper.succeed();
    }

    private static TestStorageTerminal maintenanceTerminal(GameTestHelper helper, IInventoryAccess network) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, Content.terminal.get());
        var terminal = new TestStorageTerminal(helper.absolutePos(relative), helper.getBlockState(relative), network);
        helper.getLevel().setBlockEntity(terminal);
        return terminal;
    }

    @GameTest(template = "empty")
    public static void newlyFreedCapacityIsConsolidatedWithoutManualResort(GameTestHelper helper) {
        var vanilla = new ItemStackHandler(3);
        vanilla.setStackInSlot(0, new ItemStack(Items.DIAMOND, 64));
        vanilla.setStackInSlot(1, new ItemStack(Items.DIAMOND, 5));
        vanilla.setStackInSlot(2, new ItemStack(Items.IRON_INGOT));
        var upgraded = new ItemStackHandler(1) {
            @Override public int getSlotLimit(int slot) { return 512; }
            @Override protected int getStackLimit(int slot, ItemStack stack) { return 512; }
        };
        upgraded.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        var network = new TestStorageNetwork(new TestStorageInventory(vanilla), new TestStorageInventory(upgraded));
        var terminal = maintenanceTerminal(helper, network);
        StorageMaintenance.schedule(terminal);
        var tick = new ServerTickEvent.Post(() -> true, helper.getLevel().getServer());
        StorageMaintenance.tick(tick); // Capture item types.
        StorageMaintenance.tick(tick); // Diamonds cannot move while gold occupies the upgraded slot.
        helper.assertTrue(vanilla.getStackInSlot(1).getCount() == 5, "Gold must initially block the upgraded destination");
        NetworkStorage.extract(network, helper.getLevel(), new ItemStack(Items.GOLD_INGOT), 1, () -> StorageMaintenance.schedule(terminal));
        for (int i = 0; i < 20; i++) StorageMaintenance.tick(tick);
        helper.assertTrue(upgraded.getStackInSlot(0).is(Items.DIAMOND) && upgraded.getStackInSlot(0).getCount() == 69
                && vanilla.getStackInSlot(0).isEmpty() && vanilla.getStackInSlot(1).isEmpty(),
                "A request during an existing pass must revisit diamonds after gold withdrawal frees their larger destination");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void maintenanceRevisitsItemsWhenAnotherItemFreesStorage(GameTestHelper helper) {
        var vanilla = new ItemStackHandler(4);
        for (int slot = 0; slot < 3; slot++) vanilla.setStackInSlot(slot, new ItemStack(Items.DIAMOND, 64));
        vanilla.setStackInSlot(3, new ItemStack(Items.DIAMOND, 8));
        var upgraded = new ItemStackHandler(2) {
            @Override public int getSlotLimit(int slot) { return 512; }
            @Override protected int getStackLimit(int slot, ItemStack stack) { return 512; }
        };
        upgraded.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        upgraded.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT));
        var network = new TestStorageNetwork(new TestStorageInventory(vanilla), new TestStorageInventory(upgraded));
        var job = new StorageMaintenanceJob(maintenanceTerminal(helper, network));
        boolean pending = true;
        for (int step = 0; step < 20 && pending; step++) pending = job.step(helper.getLevel().getServer());
        helper.assertTrue(!pending && upgraded.getStackInSlot(0).getCount() + upgraded.getStackInSlot(1).getCount() == 202,
                "Maintenance must reach a stable state after gold consolidation frees upgraded diamond storage");
        helper.assertTrue(vanilla.getStackInSlot(0).isEmpty() && vanilla.getStackInSlot(3).isEmpty(),
                "Earlier item types must be revisited when a later type frees larger capacity");
        helper.succeed();
    }
}
