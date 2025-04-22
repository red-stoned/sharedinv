package com.redstoned.sharedinv;

import java.util.*;
import java.util.function.Function;

import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;

public class SharedInventory {
	public static Object2ReferenceMap<UUID, SharedInventory> playerInvs = new Object2ReferenceOpenHashMap<>();

    public IPlayerInventory.SavedInventory shared = new IPlayerInventory.SavedInventory(
            DefaultedList.ofSize(36, ItemStack.EMPTY),
            new EntityEquipment()
    );

	public String name;
	public Set<UUID> players = new HashSet<>();

	public NbtList writeNbt(RegistryWrapper.WrapperLookup rm, NbtList nbtList) {
		for (int i = 0; i < shared.main().size(); ++i) {
            ItemStack stack = shared.main().get(i);
			if (!stack.isEmpty()) {
                NbtCompound nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte) i);
				nbtList.add(stack.toNbt(rm, nbtCompound));
			}
		}
        for (EquipmentSlot entry : PlayerInventory.EQUIPMENT_SLOTS.values()) {
            ItemStack stack = shared.equipment().get(entry);
            if (!stack.isEmpty()) {
                NbtCompound nbtCompound = new NbtCompound();
                byte slot;
                if (entry == EquipmentSlot.OFFHAND) slot = (byte) 150;
                else slot = (byte) (entry.getEntitySlotId() + 100);
                nbtCompound.putByte("Slot", slot);
                nbtList.add(stack.toNbt(rm, nbtCompound));
            }
        }
		return nbtList;
   	}

	public NbtCompound toNbt(RegistryWrapper.WrapperLookup rm, NbtCompound nbt) {
		nbt.putString("name", this.name);
		nbt.put("i", this.writeNbt(rm, new NbtList()));
		NbtList nbtPlayerList = new NbtList();
        players.stream()
                .map(NbtOps.INSTANCE.withEncoder(Uuids.INT_STREAM_CODEC))
                .map(DataResult::getOrThrow)
                .forEach(nbtPlayerList::add);
		nbt.put("players", nbtPlayerList);
		return nbt;
	}

    private final EquipmentSlot[] equipmentSlots;
	public SharedInventory(String name) {
		this.name = name;

        List<EquipmentSlot> list = Arrays.stream(EquipmentSlot.values())
                .filter(s -> s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                .toList();
        int max = list.stream().mapToInt(EquipmentSlot::getEntitySlotId).max().orElseThrow();
        equipmentSlots = new EquipmentSlot[max + 1];
        for (EquipmentSlot slot : list) {
            equipmentSlots[slot.getEntitySlotId()] = slot;
        }
    }

    private static final Function<NbtElement, UUID> parseUuid = NbtOps.INSTANCE
            .withParser(Uuids.INT_STREAM_CODEC)
            .andThen(DataResult::getOrThrow);
	public static SharedInventory fromNbt(RegistryWrapper.WrapperLookup rm, NbtCompound nbt) {
		SharedInventory t = new SharedInventory(nbt.getString("name").orElseThrow());

		NbtList nbtPlayers = nbt.getList("players").orElseThrow();
        for (NbtElement ia : nbtPlayers) {
            UUID p = parseUuid.apply(ia);
            t.players.add(p);
            SharedInventory.playerInvs.put(p, t);
        }

		NbtList nbtList = nbt.getList("i").orElseThrow();
        for (NbtElement element : nbtList) {
            NbtCompound compound = element.asCompound().orElseThrow();
            int j = compound.getByte("Slot").orElseThrow() & 255;
            ItemStack itemStack = ItemStack.fromNbt(rm, compound).orElse(ItemStack.EMPTY);
            if (j >= 0 && j < t.shared.main().size()) {
                t.shared.main().set(j, itemStack);
            } else if (j >= 100 && j < t.equipmentSlots.length + 100) {
                t.shared.equipment().put(t.equipmentSlots[j - 100], itemStack );
            } else if (j == 150) {
                t.shared.equipment().put(EquipmentSlot.OFFHAND, itemStack);
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
