package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;

import java.util.List;

public record BackpackNetworkPayload(int containerId, int revision, int part, int parts, boolean linked,
                                     List<NetworkIngredient> ingredients) implements CustomPacketPayload {
    public static final Type<BackpackNetworkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "backpack_network"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackNetworkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackpackNetworkPayload::containerId,
            ByteBufCodecs.VAR_INT, BackpackNetworkPayload::revision,
            ByteBufCodecs.VAR_INT, BackpackNetworkPayload::part,
            ByteBufCodecs.VAR_INT, BackpackNetworkPayload::parts,
            ByteBufCodecs.BOOL, BackpackNetworkPayload::linked,
            NetworkIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(64)), BackpackNetworkPayload::ingredients, BackpackNetworkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
