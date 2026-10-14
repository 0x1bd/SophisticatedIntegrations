package org.kvxd.sophisticatedintegrations.storage;

import com.tom.storagemod.inventory.filter.IFilter;
import com.tom.storagemod.inventory.filter.ItemFilter;
import org.kvxd.sophisticatedintegrations.mixin.MultiFilterAccessor;

public final class StorageFilters {

    private StorageFilters() {}

    public static boolean restrictsItems(IFilter filter) {
        if (filter instanceof IFilter.MultiFilter multi)
            return ((MultiFilterAccessor) multi).sophisticatedIntegrations$filters().stream().anyMatch(StorageFilters::restrictsItems);
        return filter.getItemPred() != ItemFilter.TRUE;
    }
}
