package com.redstoned.sharedinv.mixin;

import com.redstoned.sharedinv.SharedInventory;
import com.redstoned.sharedinv.SharedInventoryMod;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void inplaceOriginalInventoryOnWrite(Player player, CallbackInfo ci) {
        if (SharedInventory.playerInvs.containsKey(player.getUUID())) {
            // SharedInventoryMod.LOGGER.info("[DEBUG] Player is in team at begin write time, resetting their inv to point to the original");
            SharedInventoryMod.RestorePlayerSlots(player);
        }
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void rejoinTeamAfterWriteInventory(Player player, CallbackInfo ci) {
        SharedInventory inv = SharedInventory.playerInvs.get(player.getUUID());
        if (inv != null) {
            // SharedInventoryMod.LOGGER.info("[DEBUG] Player is in team at end write time, resetting their inv to point to the shared");
            player.getInventory().sharedinv$updateFrom(inv);
        }
    }
}

