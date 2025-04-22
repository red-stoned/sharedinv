package com.redstoned.sharedinv.mixin;

import com.redstoned.sharedinv.IPlayerInventory;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.redstoned.sharedinv.SharedInventory;
import com.redstoned.sharedinv.SharedInventoryMod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtList;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventory {
	@Final @Shadow public PlayerEntity player;
	@Mutable @Final @Shadow private DefaultedList<ItemStack> main;
	@Mutable @Shadow @Final private EntityEquipment equipment;

	@Inject(method = "writeNbt", at = @At("HEAD"))
	private void inplaceOriginalInventoryOnWrite(NbtList nbtList, CallbackInfoReturnable<NbtList> ci) {
		if (SharedInventory.playerInvs.containsKey(player.getUuid())) {
			// SharedInventoryMod.LOGGER.info("[DEBUG] Player is in team at begin write time, resetting their inv to point to the original");
			SharedInventoryMod.RestorePlayerSlots(player);
		}
	}

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void rejoinTeamAfterWriteInventory(NbtList nbtList, CallbackInfoReturnable<NbtList> ci) {
		SharedInventory inv = SharedInventory.playerInvs.get(player.getUuid());
		if (inv != null) {
			// SharedInventoryMod.LOGGER.info("[DEBUG] Player is in team at end write time, resetting their inv to point to the shared");
			player.getInventory().sharedinv$updateFrom(inv);
		}
	}

	@Override
	public void sharedinv$updateFrom(SharedInventory inv) {
		sharedinv$restore(inv.shared);
	}

	@Override
	public void sharedinv$clear() {
		this.main = DefaultedList.ofSize(36, ItemStack.EMPTY);
		this.equipment = new EntityEquipment();
	}

	@Override
	public SavedInventory sharedinv$save() {
		return new SavedInventory(this.main, this.equipment);
	}

	@Override
	public void sharedinv$restore(SavedInventory inventory) {
		this.main = inventory.main();
		this.equipment = inventory.equipment();
	}
}
