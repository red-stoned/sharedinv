package com.redstoned.sharedinv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.redstoned.sharedinv.SharedInventory;
import com.redstoned.sharedinv.SharedInventoryMod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtList;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	@Shadow
	PlayerEntity player;

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
			player.getInventory().main = inv.main;
			player.getInventory().armor = inv.armor;
			player.getInventory().offHand = inv.offHand;
			player.getInventory().combinedInventory = inv.combinedInventory;
		}
	}
}