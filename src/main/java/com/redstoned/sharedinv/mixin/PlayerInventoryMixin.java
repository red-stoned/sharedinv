package com.redstoned.sharedinv.mixin;

import com.redstoned.sharedinv.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerEquipment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class PlayerInventoryMixin implements IPlayerInventory {
	@Final @Shadow public Player player;
	@Mutable @Final @Shadow private NonNullList<ItemStack> items;
	@Mutable @Shadow @Final private EntityEquipment equipment;

	@Override
	public void sharedinv$updateFrom(SharedInventory inv) {
		sharedinv$restore(new SavedInventory(
				inv.shared.main(),
				EntityEquipmentHollower.wrap(inv.shared.equipment(), player)
		));
	}

	@Override
	public void sharedinv$clear() {
		this.items = NonNullList.withSize(36, ItemStack.EMPTY);
		this.equipment = new PlayerEquipment(player);
	}

	@Override
	public SavedInventory sharedinv$save() {
		return new SavedInventory(this.items, this.equipment);
	}

	@Override
	public void sharedinv$restore(SavedInventory inventory) {
		this.items = inventory.main();
		this.equipment = inventory.equipment();
		((LivingEntity)this.player).setEquipment(inventory.equipment());
	}
}
