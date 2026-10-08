package org.kvxd.sophisticatedintegrations.client;

import com.tom.storagemod.screen.widget.ToggleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.kvxd.sophisticatedintegrations.terminal.IntegrationMenu;

public final class TerminalBackpackButton extends ToggleButton {
    private static final ResourceLocation OFF = ResourceLocation.withDefaultNamespace("container/beacon/cancel");
    private static final ResourceLocation ON = ResourceLocation.withDefaultNamespace("container/beacon/confirm");
    private final ItemStack backpack = new ItemStack(ModItems.BACKPACK.get());

    public TerminalBackpackButton(int x, int y, IntegrationMenu menu) {
        super(x, y, Component.translatable("gui.sophisticatedintegrations.backpacks"), OFF, ON,
                menu::sophisticatedIntegrations$setIncludesBackpacks);
        setMessage(Component.translatable("gui.sophisticatedintegrations.backpacks"));
        setTooltip(Tooltip.create(Component.translatable("gui.sophisticatedintegrations.backpacks.off")),
                Tooltip.create(Component.translatable("gui.sophisticatedintegrations.backpacks.on")));
    }

    @Override
    protected void drawIcon(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + 2, getY() + 1, 0);
        graphics.pose().scale(.75f, .75f, 1);
        graphics.renderItem(backpack, 0, 0);
        graphics.pose().popPose();
        graphics.blitSprite(getIcon(),
                getX() + 8, getY() + 8, 8, 8);
    }
}
