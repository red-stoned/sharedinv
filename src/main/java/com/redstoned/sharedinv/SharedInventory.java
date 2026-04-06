package com.redstoned.sharedinv;

import java.util.*;
import java.util.function.Function;

import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SharedInventory {
	public static final Object2ReferenceMap<UUID, SharedInventory> playerInvs = new Object2ReferenceOpenHashMap<>();

	public final IPlayerInventory.SavedInventory shared = new IPlayerInventory.SavedInventory(
			NonNullList.withSize(36, ItemStack.EMPTY),
			new EntityEquipment()
	);

	public final String name;
	public final Set<UUID> players = new HashSet<>();

	public ListTag writeNbt(HolderLookup.Provider rm, ListTag nbtList) {
		for (int i = 0; i < shared.main().size(); ++i) {
			ItemStack stack = shared.main().get(i);
			if (!stack.isEmpty()) {
				CompoundTag nbtCompound = new CompoundTag();
				nbtCompound.putByte("Slot", (byte) i);
				nbtList.add(new ItemStackNbt(stack).toNbt(rm, nbtCompound));
			}
		}
		for (EquipmentSlot entry : Inventory.EQUIPMENT_SLOT_MAPPING.values()) {
			if (entry == EquipmentSlot.MAINHAND) continue; // This is overwritten for players
			ItemStack stack = shared.equipment().get(entry);
			if (!stack.isEmpty()) {
				CompoundTag nbtCompound = new CompoundTag();
				byte slot;
				if (entry == EquipmentSlot.OFFHAND) slot = (byte) 150;
				else slot = (byte) (entry.getIndex() + 100);
				nbtCompound.putByte("Slot", slot);
				nbtList.add(new ItemStackNbt(stack).toNbt(rm, nbtCompound));
			}
		}
		return nbtList;
   	}

	public CompoundTag toNbt(HolderLookup.Provider rm, CompoundTag nbt) {
		nbt.putString("name", this.name);
		nbt.put("i", this.writeNbt(rm, new ListTag()));
		ListTag nbtPlayerList = new ListTag();
		players.stream()
				.map(NbtOps.INSTANCE.withEncoder(UUIDUtil.CODEC))
				.map(DataResult::getOrThrow)
				.forEach(nbtPlayerList::add);
		nbt.put("players", nbtPlayerList);
		return nbt;
	}

	private final EquipmentSlot[] equipmentSlots;
	public SharedInventory(String name) {
		this.name = name;

		// OFFHAND is handled separately, this is just for armor slots
		// MAINHAND is not handled since PlayerEquipment overwrites that with the selected main inventory slot
		List<EquipmentSlot> list = Arrays.stream(EquipmentSlot.values())
				.filter(s -> s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
				.toList();
		int max = list.stream().mapToInt(EquipmentSlot::getIndex).max().orElseThrow();
		equipmentSlots = new EquipmentSlot[max + 1];
		for (EquipmentSlot slot : list) {
			equipmentSlots[slot.getIndex()] = slot;
		}
	}

	private static final Function<Tag, UUID> parseUuid = NbtOps.INSTANCE
			.withParser(UUIDUtil.CODEC)
			.andThen(DataResult::getOrThrow);
	public static SharedInventory fromNbt(HolderLookup.Provider rm, CompoundTag nbt) {
		SharedInventory t = new SharedInventory(nbt.getString("name").orElseThrow());

		ListTag nbtPlayers = nbt.getList("players").orElseThrow();
		for (Tag ia : nbtPlayers) {
			UUID p = parseUuid.apply(ia);
			t.players.add(p);
			SharedInventory.playerInvs.put(p, t);
		}

		ListTag nbtList = nbt.getList("i").orElseThrow();
		for (Tag element : nbtList) {
			CompoundTag compound = element.asCompound().orElseThrow();
			int j = compound.getByte("Slot").orElseThrow() & 255;
			ItemStack itemStack = ItemStackNbt.fromNbt(rm, compound).orElse(ItemStack.EMPTY);
			if (j >= 0 && j < t.shared.main().size()) {
				t.shared.main().set(j, itemStack);
			} else if (j >= 100 && j < t.equipmentSlots.length + 100) {
				t.shared.equipment().set(t.equipmentSlots[j - 100], itemStack );
			} else if (j == 150) {
				t.shared.equipment().set(EquipmentSlot.OFFHAND, itemStack);
			}
		}
		return t;
	}

	public void AddPlayer(UUID uuid) {
		if (this.players.contains(uuid)) return;

		SharedInventory existingInv = playerInvs.get(uuid);
		if (existingInv != null) {
			existingInv.RemovePlayer(uuid);
		}

		this.players.add(uuid);
		playerInvs.put(uuid, this);
	}

	public void RemovePlayer(UUID uuid) {
		this.players.remove(uuid);
		playerInvs.remove(uuid);
	}
}
