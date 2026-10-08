package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

public record InventoryCraftingAvailabilityPayload(boolean available) implements CustomPacketPayload {
    public static final Type<InventoryCraftingAvailabilityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "inventory_crafting_available"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryCraftingAvailabilityPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(InventoryCraftingAvailabilityPayload::new, InventoryCraftingAvailabilityPayload::available).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
