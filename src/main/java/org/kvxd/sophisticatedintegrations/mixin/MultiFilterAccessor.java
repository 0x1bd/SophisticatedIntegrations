package org.kvxd.sophisticatedintegrations.mixin;

import com.tom.storagemod.inventory.filter.IFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = IFilter.MultiFilter.class, remap = false)
public interface MultiFilterAccessor {

    @Accessor("filters")
    List<IFilter> sophisticatedIntegrations$filters();
}
