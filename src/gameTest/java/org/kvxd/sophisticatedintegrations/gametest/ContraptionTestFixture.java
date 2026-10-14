package org.kvxd.sophisticatedintegrations.gametest;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.piston.PistonContraption;
import com.tom.createterminal.behaviour.ContraptionWorld;
import com.tom.createterminal.behaviour.CraftingTerminalInstance;
import com.tom.createterminal.behaviour.StorageTerminalInstance;
import com.tom.createterminal.behaviour.TerminalBehaviour;
import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.crafting.BackpackCraftingSession;
import org.kvxd.sophisticatedintegrations.storage.StorageMaintenanceJob;

public final class ContraptionTestFixture {
    public static void verify(GameTestHelper helper, boolean crafting) {
        var inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 12));
        var contraption = new PistonContraption();
        contraption.bounds = new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1);
        contraption.getStorage().initialize();
        contraption.getStorage().attachExternal(inventory);
        OrientedContraptionEntity.create(helper.getLevel(), contraption, net.minecraft.core.Direction.NORTH);
        var state = (crafting ? Content.craftingTerminal.get() : Content.terminal.get()).defaultBlockState();
        var context = new MovementContext(helper.getLevel(), new StructureBlockInfo(new BlockPos(1, 2, 3), state, null), contraption);
        var world = new ContraptionWorld(new TerminalBehaviour(StorageTerminalInstance::new), context);
        StorageTerminalBlockEntity terminal = crafting ? new CraftingTerminalInstance(world) : new StorageTerminalInstance(world);
        helper.assertTrue(terminal.pushStack(new StoredItemStack(new ItemStack(Items.DIAMOND, 5))) == null
                        && inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).getCount() == 17,
                "Mounted terminal must deposit into its mounted matching stack");
        var extracted = terminal.pullStack(new StoredItemStack(new ItemStack(Items.DIAMOND)), 6);
        helper.assertTrue(extracted != null && extracted.getQuantity() == 6 && inventory.getStackInSlot(1).getCount() == 11,
                "Mounted terminal must withdraw from mounted storage");
        var snapshot = BackpackCraftingSession.snapshot(terminal);
        helper.assertTrue(snapshot.size() == 1 && snapshot.getFirst().template().is(Items.DIAMOND) && snapshot.getFirst().quantity() == 11,
                "Mounted snapshots must use contraption contents rather than a stationary network: " + snapshot);
        var maintenance = new StorageMaintenanceJob(terminal);
        helper.assertTrue(maintenance.step(helper.getLevel().getServer()), "An alive mounted terminal must enter maintenance without a world block entity");
        helper.assertTrue(!maintenance.step(helper.getLevel().getServer()), "Already consolidated mounted storage must finish maintenance");
        contraption.entity.discard();
        helper.assertTrue(!new StorageMaintenanceJob(terminal).step(helper.getLevel().getServer()),
                "Removed contraptions must stop maintenance");
        helper.succeed();
    }

}
