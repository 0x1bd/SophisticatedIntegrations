package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;

public record InventoryBackpackRecipePayload(int containerId, ResourceLocation recipeId, boolean maxTransfer,
                                             int action) implements CustomPacketPayload {
    public static final Type<InventoryBackpackRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "inventory_backpack_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryBackpackRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InventoryBackpackRecipePayload::containerId,
            ResourceLocation.STREAM_CODEC, InventoryBackpackRecipePayload::recipeId,
            ByteBufCodecs.BOOL, InventoryBackpackRecipePayload::maxTransfer,
            ByteBufCodecs.VAR_INT, InventoryBackpackRecipePayload::action, InventoryBackpackRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
