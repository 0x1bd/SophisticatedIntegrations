package org.kvxd.sophisticatedintegrations.mixin.client;

import com.tom.storagemod.menu.StorageTerminalMenu;
import com.tom.storagemod.screen.AbstractStorageTerminalScreen;
import com.tom.storagemod.screen.widget.ToggleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kvxd.sophisticatedintegrations.client.TerminalBackpackButton;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;
import org.kvxd.sophisticatedintegrations.terminal.TerminalSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractStorageTerminalScreen.class, remap = false)
public abstract class TerminalBackpackScreenMixin extends AbstractContainerScreen<StorageTerminalMenu> {
    @Shadow
    protected ToggleButton buttonTallMode;
    @Unique
    private TerminalBackpackButton sophisticatedIntegrations$backpacks;

    protected TerminalBackpackScreenMixin(StorageTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sophisticatedIntegrations$addBackpacks(CallbackInfo callback) {
        sophisticatedIntegrations$backpacks = addRenderableWidget(new TerminalBackpackButton(
                buttonTallMode.getX(), buttonTallMode.getY() + 18, (IntegrationMenu) menu));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sophisticatedIntegrations$updateBackpacks(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                                           CallbackInfo callback) {
        int mode = ((IntegrationMenu) menu).sophisticatedIntegrations$backpackMode();
        sophisticatedIntegrations$backpacks.active = (mode & TerminalSession.BACKPACKS_ALLOWED) != 0;
        sophisticatedIntegrations$backpacks.setState((mode & TerminalSession.BACKPACKS_INCLUDED) != 0);
        sophisticatedIntegrations$backpacks.setPosition(buttonTallMode.getX(), buttonTallMode.getY() + 18);
    }
}
