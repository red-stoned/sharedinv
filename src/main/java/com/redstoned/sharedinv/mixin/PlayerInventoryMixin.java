package com.redstoned.sharedinv.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.redstoned.sharedinv.SharedInventory;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	@Shadow
	public final DefaultedList<ItemStack> main = SharedInventory.main;
	@Shadow
	public final DefaultedList<ItemStack> armor = SharedInventory.armor;
	@Shadow
	public final DefaultedList<ItemStack> offHand = SharedInventory.offHand;
}