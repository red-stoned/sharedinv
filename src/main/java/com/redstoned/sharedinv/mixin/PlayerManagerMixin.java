package com.redstoned.sharedinv.mixin;

import com.redstoned.sharedinv.SharedInventoryMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("TAIL"))
    void removeOriginalInvOnDisconnect(ServerPlayer player, CallbackInfo ci) {
        //SharedInventoryMod.LOGGER.info("[DEBUG] Removing {}'s original inventory", player.getNameForScoreboard());
        SharedInventoryMod.originalInventories.remove(player.getUUID());
    }
}
