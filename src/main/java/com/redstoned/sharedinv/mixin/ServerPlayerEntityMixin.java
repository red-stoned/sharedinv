package com.redstoned.sharedinv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtList;

@Mixin(PlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Redirect(method = "readCustomDataFromNbt",
	at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;readNbt(Lnet/minecraft/nbt/NbtList;)V")
	)
	private static void nothing(PlayerInventory one, NbtList two) {}
}
