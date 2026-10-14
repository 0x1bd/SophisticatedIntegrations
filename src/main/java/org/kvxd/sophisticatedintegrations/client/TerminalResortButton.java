package org.kvxd.sophisticatedintegrations.client;

import com.tom.storagemod.screen.widget.IconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;

public final class TerminalResortButton extends IconButton {

    public TerminalResortButton(int x, int y, IntegrationMenu menu) {
        super(x, y, Component.translatable("gui.sophisticatedintegrations.resort"),
                ResourceLocation.fromNamespaceAndPath("toms_storage", "icons/sort_space_efficiency"), button -> menu.sophisticatedIntegrations$resort());
        setMessage(Component.translatable("gui.sophisticatedintegrations.resort"));
        setTooltip(Tooltip.create(Component.translatable("gui.sophisticatedintegrations.resort.tooltip")));
    }
}
