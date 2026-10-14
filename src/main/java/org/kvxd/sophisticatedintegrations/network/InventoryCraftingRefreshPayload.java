package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

public record InventoryCraftingRefreshPayload(int containerId) implements CustomPacketPayload {

    public static final Type<InventoryCraftingRefreshPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            SophisticatedIntegrations.ID, "inventory_crafting_refresh"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryCraftingRefreshPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, InventoryCraftingRefreshPayload::containerId, InventoryCraftingRefreshPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
