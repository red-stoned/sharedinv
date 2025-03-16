package com.redstoned.sharedinv;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

public class SharedInventory {
	public static Object2ReferenceMap<UUID, SharedInventory> playerInvs = new Object2ReferenceOpenHashMap<>();

	public DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
	public DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
	public DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
	public List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);

	public String name;
	public Set<UUID> players = new HashSet<>();

	// both methods yoinked from PlayerInventory.class
	public NbtList writeNbt(RegistryWrapper.WrapperLookup rm, NbtList nbtList) {
		int i;
		NbtCompound nbtCompound;
		for(i = 0; i < main.size(); ++i) {
			if (!((ItemStack)main.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)i);
				nbtList.add(((ItemStack)main.get(i)).toNbt(rm, nbtCompound));
			}
		}

		for(i = 0; i < armor.size(); ++i) {
			if (!((ItemStack)armor.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)(i + 100));
				nbtList.add(((ItemStack)armor.get(i)).toNbt(rm, nbtCompound));
			}
		}

		for(i = 0; i < offHand.size(); ++i) {
			if (!((ItemStack)offHand.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)(i + 150));
				nbtList.add(((ItemStack)offHand.get(i)).toNbt(rm, nbtCompound));
			}
		}
		return nbtList;
   	}

	public NbtCompound toNbt(RegistryWrapper.WrapperLookup rm, NbtCompound nbt) {
		nbt.putString("name", this.name);
		nbt.put("i", this.writeNbt(rm, new NbtList()));
		NbtList nbtPlayerList = new NbtList();
		players.stream().map(t -> NbtHelper.fromUuid(t)).forEach(t -> nbtPlayerList.add(t));
		nbt.put("players", nbtPlayerList);
		return nbt;
	}

	public SharedInventory(String name) {
		this.name = name;
	}


	public static SharedInventory fromNbt(RegistryWrapper.WrapperLookup rm, NbtCompound nbt) {
		SharedInventory t = new SharedInventory(nbt.getString("name"));

		NbtList nbtPlayers = nbt.getList("players", 11);
		for (int i = 0; i < nbtPlayers.size(); ++i) {
			NbtElement ia = nbtPlayers.get(i);
			UUID p = NbtHelper.toUuid(ia);
			t.players.add(p);
			SharedInventory.playerInvs.put(p, t);
		}

		NbtList nbtList = nbt.getList("i", 10);
		for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			int j = nbtCompound.getByte("Slot") & 255;
			ItemStack itemStack = (ItemStack)ItemStack.fromNbt(rm, nbtCompound).orElse(ItemStack.EMPTY);
			if (j >= 0 && j < t.main.size()) {
				t.main.set(j, itemStack);
			} else if (j >= 100 && j < t.armor.size() + 100) {
				t.armor.set(j - 100, itemStack);
			} else if (j >= 150 && j < t.offHand.size() + 150) {
				t.offHand.set(j - 150, itemStack);
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
