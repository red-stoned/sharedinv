package com.redstoned.sharedinv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.redstoned.sharedinv.SharedInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(ServerPlayer.class)
abstract public class ServerPlayerMixin extends Player {

	public ServerPlayerMixin(Level world, GameProfile gameProfile) {
		super(world, gameProfile);
		//TODO Auto-generated constructor stub
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	public void resetInvRefs(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		var inv = SharedInventory.playerInvs.get(oldPlayer.getUUID());
		if (inv != null) {
			getInventory().sharedinv$updateFrom(inv);
		}
	}
}
