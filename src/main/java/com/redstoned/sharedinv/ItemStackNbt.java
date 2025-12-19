package com.redstoned.sharedinv;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

import java.util.Optional;

public record ItemStackNbt(ItemStack stack) {
    public static Optional<ItemStack> fromNbt(RegistryWrapper.WrapperLookup registries, NbtElement nbt) {
        return ItemStack.CODEC.parse(registries.getOps(NbtOps.INSTANCE), nbt).resultOrPartial(error -> SharedInventoryMod.LOGGER.error("Tried to load invalid item: '{}'", error));
    }

    public NbtElement toNbt(RegistryWrapper.WrapperLookup registries, NbtElement prefix) {
        if (this.stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return ItemStack.CODEC.encode(this.stack, registries.getOps(NbtOps.INSTANCE), prefix).getOrThrow();
        }
    }
}
