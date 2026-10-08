package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

public record BackpackRecipePayload(int containerId, ResourceLocation recipeId, boolean maxTransfer,
                                    int action) implements CustomPacketPayload {
    public static final Type<BackpackRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "backpack_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackpackRecipePayload::containerId,
            ResourceLocation.STREAM_CODEC, BackpackRecipePayload::recipeId,
            ByteBufCodecs.BOOL, BackpackRecipePayload::maxTransfer,
            ByteBufCodecs.VAR_INT, BackpackRecipePayload::action, BackpackRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
