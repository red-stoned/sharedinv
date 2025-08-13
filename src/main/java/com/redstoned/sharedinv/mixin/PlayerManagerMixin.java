package com.redstoned.sharedinv.mixin;

import com.redstoned.sharedinv.SharedInventoryMod;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("TAIL"))
    void removeOriginalInvOnDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        //SharedInventoryMod.LOGGER.info("[DEBUG] Removing {}'s original inventory", player.getNameForScoreboard());
        SharedInventoryMod.original_inventories.remove(player.getUuid());
    }
}
