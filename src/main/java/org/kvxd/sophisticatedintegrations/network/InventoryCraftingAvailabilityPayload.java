package org.kvxd.sophisticatedintegrations.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.kvxd.sophisticatedintegrations.SophisticatedIntegrations;
import org.kvxd.sophisticatedintegrations.crafting.NetworkIngredient;

import java.util.List;

public record InventoryCraftingAvailabilityPayload(int revision, int part, int parts, boolean available, boolean alternative,
                                                   List<NetworkIngredient> ingredients) implements CustomPacketPayload {
    public InventoryCraftingAvailabilityPayload(int revision, int part, int parts, boolean available, List<NetworkIngredient> ingredients) {
        this(revision, part, parts, available, false, ingredients);
    }
    public static final Type<InventoryCraftingAvailabilityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedIntegrations.ID, "inventory_crafting_available"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryCraftingAvailabilityPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, InventoryCraftingAvailabilityPayload::revision,
                    ByteBufCodecs.VAR_INT, InventoryCraftingAvailabilityPayload::part,
                    ByteBufCodecs.VAR_INT, InventoryCraftingAvailabilityPayload::parts,
                    ByteBufCodecs.BOOL, InventoryCraftingAvailabilityPayload::available,
                    ByteBufCodecs.BOOL, InventoryCraftingAvailabilityPayload::alternative,
                    NetworkIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(64)), InventoryCraftingAvailabilityPayload::ingredients,
                    InventoryCraftingAvailabilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
