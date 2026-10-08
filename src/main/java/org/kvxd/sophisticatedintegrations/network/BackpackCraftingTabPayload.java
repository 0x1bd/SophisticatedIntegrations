package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

public record BackpackCraftingTabPayload(int containerId) implements CustomPacketPayload {
    public static final Type<BackpackCraftingTabPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "backpack_crafting_tab"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackCraftingTabPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(BackpackCraftingTabPayload::new, BackpackCraftingTabPayload::containerId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
