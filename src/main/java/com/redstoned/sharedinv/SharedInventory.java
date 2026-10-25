package com.redstoned.sharedinv;

import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

public class SharedInventory {
	public static final Object2ReferenceMap<UUID, SharedInventory> playerInvs = new Object2ReferenceOpenHashMap<>();

	public final IPlayerInventory.SavedInventory shared = IPlayerInventory.SavedInventory.empty();
	public final String name;
	public final Set<UUID> players = new HashSet<>();

	// Lnet/minecraft/world/entity/player/Inventory;save(Lnet/minecraft/nbt/ListTag;)Lnet/minecraft/nbt/ListTag;
	public ListTag writeNbt(ListTag listTag) {
		for (int i = 0; i < shared.items().size(); i++) {
			if (!shared.items().get(i).isEmpty()) {
				CompoundTag compoundTag = new CompoundTag();
				compoundTag.putByte("Slot", (byte)i);
				shared.items().get(i).save(compoundTag);
				listTag.add(compoundTag);
			}
		}

		for (int i = 0; i < shared.armor().size(); i++) {
			if (!shared.armor().get(i).isEmpty()) {
				CompoundTag compoundTag = new CompoundTag();
				compoundTag.putByte("Slot", (byte)(i + 100));
				shared.armor().get(i).save(compoundTag);
				listTag.add(compoundTag);
			}
		}

		for (int i = 0; i < shared.offhand().size(); i++) {
			if (!shared.offhand().get(i).isEmpty()) {
				CompoundTag compoundTag = new CompoundTag();
				compoundTag.putByte("Slot", (byte)(i + 150));
				shared.offhand().get(i).save(compoundTag);
				listTag.add(compoundTag);
			}
		}

		return listTag;
	}

	public CompoundTag toNbt(HolderLookup.Provider rm, CompoundTag nbt) {
		nbt.putString("name", this.name);
		nbt.put("i", this.writeNbt(new ListTag()));
		ListTag nbtPlayerList = new ListTag();
		players.stream()
				.map(NbtOps.INSTANCE.withEncoder(UUIDUtil.CODEC))
				.map(a -> a.getOrThrow(false, null))
				.forEach(nbtPlayerList::add);
		nbt.put("players", nbtPlayerList);
		return nbt;
	}

	public SharedInventory(String name) {
		this.name = name;
	}

	private static final Function<Tag, UUID> parseUuid = NbtOps.INSTANCE
			.withParser(UUIDUtil.CODEC)
			.andThen(u -> u.getOrThrow(false, null));

	public static SharedInventory fromNbt(HolderLookup.Provider rm, CompoundTag nbt) {
		SharedInventory t = new SharedInventory(nbt.getString("name"));

		ListTag nbtPlayers = nbt.getList("players", Tag.TAG_INT_ARRAY);
		for (Tag ia : nbtPlayers) {
			UUID p = parseUuid.apply(ia);
			t.players.add(p);
			SharedInventory.playerInvs.put(p, t);
		}

		// Lnet/minecraft/world/entity/player/Inventory;load(Lnet/minecraft/nbt/ListTag;)V
		ListTag nbtList = nbt.getList("i", Tag.TAG_COMPOUND);
		for (int i = 0; i < nbtList.size(); i++) {
			CompoundTag compoundTag = nbtList.getCompound(i);
			int j = compoundTag.getByte("Slot") & 255;
			ItemStack itemStack = ItemStack.of(compoundTag);
			if (!itemStack.isEmpty()) {
				if (j >= 0 && j < t.shared.items().size()) {
					t.shared.items().set(j, itemStack);
				} else if (j >= 100 && j < t.shared.armor().size() + 100) {
					t.shared.armor().set(j - 100, itemStack);
				} else if (j >= 150 && j < t.shared.offhand().size() + 150) {
					t.shared.offhand().set(j - 150, itemStack);
				}
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
