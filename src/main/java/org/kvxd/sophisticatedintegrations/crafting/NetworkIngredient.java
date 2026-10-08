package org.kvxd.sophisticatedintegrations.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record NetworkIngredient(ItemStack template, long quantity) {

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkIngredient> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, NetworkIngredient::template,
            ByteBufCodecs.VAR_LONG, NetworkIngredient::quantity, NetworkIngredient::new);
}
