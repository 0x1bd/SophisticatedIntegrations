package org.kvxd.sophisticatedintegrations.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class IntegrationMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("InventoryJeiContextTransferMixin")) {
            try {
                org.spongepowered.asm.service.MixinService.getService().getBytecodeProvider()
                        .getClassNode("mezz.jei.api.recipe.transfer.IRecipeTransferContext", true);
            } catch (ClassNotFoundException | java.io.IOException unavailable) {
                return false;
            }
        }
        String mod = mixinClassName.endsWith("ContraptionTerminalMixin") ? "createcontraptionterminals"
                : mixinClassName.endsWith("ContentsFilteredInventoryAccessor") ? "sophisticatedstorage"
                : mixinClassName.endsWith("JeiTransferMixin") || mixinClassName.endsWith("JeiContextTransferMixin") ? "jei"
                : mixinClassName.endsWith("EmiTransferMixin") ? "emi"
                  : mixinClassName.endsWith("ReiTransferMixin") ? "roughlyenoughitems" : null;
        return mod == null || FMLLoader.getLoadingModList().getModFileById(mod) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {
    }

    @Override
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {
    }
}
