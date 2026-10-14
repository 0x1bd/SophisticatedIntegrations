package org.kvxd.sophisticatedintegrations.gametest;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.IInventoryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.kvxd.sophisticatedintegrations.storage.TerminalStorageAccess;

public final class TestStorageTerminal extends StorageTerminalBlockEntity implements TerminalStorageAccess {

    private final IInventoryAccess access;

    public TestStorageTerminal(BlockPos pos, BlockState state, IInventoryAccess access) {
        super(pos, state);
        this.access = access;
    }

    @Override
    public IInventoryAccess sophisticatedIntegrations$networkStorage() {
        return access;
    }
}
