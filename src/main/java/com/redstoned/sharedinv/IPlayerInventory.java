package com.redstoned.sharedinv;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface IPlayerInventory {
	default void sharedinv$updateFrom(SharedInventory inv) {
		throw new UnsupportedOperationException("This method should be overridden in the mixin");
	}
	default void sharedinv$clear() {
		throw new UnsupportedOperationException("This method should be overridden in the mixin");
	}
	default SavedInventory sharedinv$save() {
		throw new UnsupportedOperationException("This method should be overridden in the mixin");
	}
	default void sharedinv$restore(SavedInventory inventory) {
		throw new UnsupportedOperationException("This method should be overridden in the mixin");
	}

	record SavedInventory(
        NonNullList<ItemStack> items,
        NonNullList<ItemStack> armor,
        NonNullList<ItemStack> offhand
	) {
		public static SavedInventory empty() {
			NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
			NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
			NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);

			return new SavedInventory(items, armor, offhand);
		}
	}
}
