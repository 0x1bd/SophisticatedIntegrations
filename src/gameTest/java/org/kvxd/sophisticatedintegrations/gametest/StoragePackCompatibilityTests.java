package org.kvxd.sophisticatedintegrations.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.tom.storagemod.util.Priority;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.storage.NetworkStorage;
import org.kvxd.sophisticatedintegrations.storage.StorageSafety;

@GameTestHolder(SophisticatedIntegrations.ID)
@PrefixGameTestTemplate(false)
public final class StoragePackCompatibilityTests {
    private static boolean installed(GameTestHelper helper) {
        if (ModList.get().isLoaded("sophisticatedstorage")) return true;
        helper.succeed();
        return false;
    }

    private static IStorageWrapper chest(GameTestHelper helper, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "diamond_chest"));
        helper.setBlock(pos, block);
        return ((IControllableStorage) helper.getBlockEntity(pos)).getStorageWrapper();
    }

    @GameTest(template = "empty")
    public static void actualSophisticatedChestUsesChosenSlot(GameTestHelper helper) {
        if (!installed(helper)) return;
        var wrapper = chest(helper, BlockPos.ZERO);
        var inventory = wrapper.getInventoryHandler();
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 30));
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 50));
        var access = new TestStorageInventory(wrapper.getInventoryForInputOutput());
        NetworkStorage.consolidate(access, helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); });
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 16 && inventory.getStackInSlot(1).getCount() == 64,
                "Actual chest input/output must consolidate into the selected slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradedSophisticatedChestDrainsLargeSourcesIntoOneSlot(GameTestHelper helper) {
        if (!installed(helper)) return;
        var source = chest(helper, BlockPos.ZERO);
        var target = chest(helper, new BlockPos(1, 0, 0));
        var sourceInventory = source.getInventoryHandler();
        var targetInventory = target.getInventoryHandler();
        var upgrade = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "stack_upgrade_tier_3"));
        source.getUpgradeHandler().setStackInSlot(0, new ItemStack(upgrade));
        target.getUpgradeHandler().setStackInSlot(0, new ItemStack(upgrade));
        helper.assertTrue(sourceInventory.getSlotLimit(0) >= 350 && targetInventory.getSlotLimit(0) >= 350,
                "Real storage stack upgrades must be active");
        sourceInventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 200));
        sourceInventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 150));
        var network = new TestStorageNetwork(new TestStorageInventory(source.getInventoryForInputOutput()),
                new TestStorageInventory(target.getInventoryForInputOutput(), Priority.HIGH));
        long moved = NetworkStorage.consolidate(network, helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); });
        helper.assertTrue(moved == 350 && targetInventory.getStackInSlot(0).getCount() == 350
                && targetInventory.getStackInSlot(1).isEmpty()
                && sourceInventory.getStackInSlot(0).isEmpty() && sourceInventory.getStackInSlot(1).isEmpty(),
                "Actual upgraded chests must consolidate large sources into one slot in one pass, moved=" + moved);
        helper.assertTrue(NetworkStorage.consolidate(network, helper.getLevel(), stack -> {}) == 0,
                "Actual upgraded chest consolidation must be stable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedSophisticatedChestDoesNotVoidExistingStock(GameTestHelper helper) throws ReflectiveOperationException {
        if (!installed(helper)) return;
        var wrapper = chest(helper, BlockPos.ZERO);
        var inventory = wrapper.getInventoryHandler();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
        wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.VOID_UPGRADE.get()));
        var voidUpgrade = wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).getFirst();
        voidUpgrade.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        voidUpgrade.setVoidType(VoidType.SLOT_OVERFLOW);
        int capacity = inventory.getSlotLimit(0);
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, capacity));
        var entity = helper.getBlockEntity(BlockPos.ZERO);
        entity.getClass().getMethod("toggleLock").invoke(entity);
        var io = wrapper.getInventoryForInputOutput();
        helper.assertTrue(StorageSafety.mayVoid(io, 0, new ItemStack(Items.COBBLESTONE)),
                "Locked storage's ContentsFilteredItemHandler must expose its void upgrade to the safeguard");
        var vanilla = new ItemStackHandler(1);
        vanilla.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 17));
        NetworkStorage.consolidate(new TestStorageNetwork(new TestStorageInventory(io), new TestStorageInventory(vanilla)),
                helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); });
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == capacity && vanilla.getStackInSlot(0).getCount() == 17,
                "Locked storage must not consume existing stock during re-sort");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sophisticatedControllerUsesRealBackingSlots(GameTestHelper helper) throws ReflectiveOperationException {
        if (!installed(helper)) return;
        helper.setBlock(BlockPos.ZERO, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "controller")));
        var controller = (ControllerBlockEntityBase) helper.getBlockEntity(BlockPos.ZERO);
        var wrapper = chest(helper, new BlockPos(1, 0, 0));
        var inventory = wrapper.getInventoryHandler();
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 30));
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 50));
        controller.addStorage(helper.absolutePos(new BlockPos(1, 0, 0)));
        var io = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(BlockPos.ZERO), null);
        var access = new TestStorageInventory(io);
        helper.assertTrue(io.getSlots() >= 2, "The controller must expose the connected chest");
        NetworkStorage.consolidate(access, helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); });
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 16 && inventory.getStackInSlot(1).getCount() == 64,
                "Controller insertion must target the selected backing slot");
        helper.assertTrue(NetworkStorage.consolidate(access, helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); }) == 0,
                "Controller-backed maintenance must converge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradedSophisticatedControllerCombinesLargeStacks(GameTestHelper helper) {
        if (!installed(helper)) return;
        helper.setBlock(BlockPos.ZERO, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "controller")));
        var controller = (ControllerBlockEntityBase) helper.getBlockEntity(BlockPos.ZERO);
        var wrapper = chest(helper, new BlockPos(1, 0, 0));
        var inventory = wrapper.getInventoryHandler();
        var upgrade = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "stack_upgrade_tier_3"));
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(upgrade));
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 200));
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 150));
        controller.addStorage(helper.absolutePos(new BlockPos(1, 0, 0)));
        var io = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(BlockPos.ZERO), null);
        var access = new TestStorageInventory(io);
        NetworkStorage.consolidate(access, helper.getLevel(), stack -> { throw new AssertionError("Unexpected recovery"); });
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 350 && inventory.getStackInSlot(1).isEmpty(),
                "Controller access must repeatedly extract into the same upgraded backing slot");
        helper.assertTrue(NetworkStorage.consolidate(access, helper.getLevel(), stack -> {}) == 0,
                "Upgraded controller consolidation must be stable");
        helper.succeed();
    }
}
