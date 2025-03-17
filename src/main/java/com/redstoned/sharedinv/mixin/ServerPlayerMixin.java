package com.redstoned.sharedinv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.redstoned.sharedinv.SharedInventory;
import com.redstoned.sharedinv.SharedInventoryMod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
abstract public class ServerPlayerMixin extends PlayerEntity {

	public ServerPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Inject(method = "copyFrom", at = @At("TAIL"))
	public void resetInvRefs(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		var inv = SharedInventory.playerInvs.get(oldPlayer.getUuid());
		if (inv != null) {
			SharedInventoryMod.UpdatePlayerSlots(inv, ((ServerPlayerEntity)(Object)this));
		}
	}
}
