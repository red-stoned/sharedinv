package com.redstoned.sharedinv;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public record ItemStackNbt(ItemStack stack) {
    public static Optional<ItemStack> fromNbt(HolderLookup.Provider registries, Tag nbt) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt).resultOrPartial(error -> SharedInventoryMod.LOGGER.error("Tried to load invalid item: '{}'", error));
    }

    public Tag toNbt(HolderLookup.Provider registries, Tag prefix) {
        if (this.stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return ItemStack.CODEC.encode(this.stack, registries.createSerializationContext(NbtOps.INSTANCE), prefix).getOrThrow();
        }
    }
}
