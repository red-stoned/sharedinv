package com.redstoned.sharedinv.mixin;

import com.google.common.collect.ImmutableList;
import com.redstoned.sharedinv.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(Inventory.class)
public class PlayerInventoryMixin implements IPlayerInventory {
	@Final @Shadow public Player player;
	@Mutable @Final @Shadow public NonNullList<ItemStack> items;
	@Mutable @Final @Shadow public NonNullList<ItemStack> armor;
	@Mutable @Final @Shadow public NonNullList<ItemStack> offhand;
	@Mutable @Final @Shadow private List<NonNullList<ItemStack>> compartments;

	@Override
	public void sharedinv$updateFrom(SharedInventory inv) {
		sharedinv$restore(new SavedInventory(
			inv.shared.items(),
			inv.shared.armor(),
			inv.shared.offhand()
		));
	}

	@Override
	public void sharedinv$clear() {
		this.items = NonNullList.withSize(36, ItemStack.EMPTY);
		this.armor = NonNullList.withSize(4, ItemStack.EMPTY);
		this.offhand = NonNullList.withSize(4, ItemStack.EMPTY);
		this.compartments = ImmutableList.of(this.items, this.armor, this.offhand);
	}

	@Override
	public SavedInventory sharedinv$save() {
		return new SavedInventory(this.items, this.armor, this.offhand);
	}

	@Override
	public void sharedinv$restore(SavedInventory inventory) {
		this.items = inventory.items();
		this.armor = inventory.armor();
		this.offhand = inventory.offhand();
		this.compartments = ImmutableList.of(this.items, this.armor, this.offhand);
	}
}
