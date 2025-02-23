package com.redstoned.sharedinv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedInventory implements ModInitializer {
	public static final String MOD_ID = "sharedinv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
	public static DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
	public static DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);

	// both methods yoinked from PlayerInventory.class
	public static NbtList writeNbt(MinecraftServer server, NbtList nbtList) {
		int i;
		NbtCompound nbtCompound;
		for(i = 0; i < main.size(); ++i) {
			if (!((ItemStack)main.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)i);
				nbtList.add(((ItemStack)main.get(i)).toNbt(server.getRegistryManager(), nbtCompound));
			}
		}

		for(i = 0; i < armor.size(); ++i) {
			if (!((ItemStack)armor.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)(i + 100));
				nbtList.add(((ItemStack)armor.get(i)).toNbt(server.getRegistryManager(), nbtCompound));
			}
		}

		for(i = 0; i < offHand.size(); ++i) {
			if (!((ItemStack)offHand.get(i)).isEmpty()) {
				nbtCompound = new NbtCompound();
				nbtCompound.putByte("Slot", (byte)(i + 150));
				nbtList.add(((ItemStack)offHand.get(i)).toNbt(server.getRegistryManager(), nbtCompound));
			}
		}

		return nbtList;
   	}

	public void readNbt(MinecraftServer server, NbtList nbtList) {
		main.clear();
		armor.clear();
		offHand.clear();
  
		for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			int j = nbtCompound.getByte("Slot") & 255;
			ItemStack itemStack = (ItemStack)ItemStack.fromNbt(server.getRegistryManager(), nbtCompound).orElse(ItemStack.EMPTY);
			if (j >= 0 && j < main.size()) {
				main.set(j, itemStack);
			} else if (j >= 100 && j < armor.size() + 100) {
				armor.set(j - 100, itemStack);
			} else if (j >= 150 && j < offHand.size() + 150) {
				offHand.set(j - 150, itemStack);
			}
		}
	}

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
			Util.getIoWorkerExecutor().execute(() -> {
				try {
					NbtList l = writeNbt(server, new NbtList());
					NbtCompound n = new NbtCompound();
					n.put("i", l);
					NbtIo.writeCompressed(n, server.getPath("world/sharedinv.nbt"));
				} catch (Exception e) {
					LOGGER.error("Failed to save Shared Inventory", e);
				}
			});
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			Path ipath = server.getPath("world/sharedinv.nbt");
			if (!Files.exists(ipath)) {
				LOGGER.info("Could not find inventory state, starting empty.");
				return;
			};
			Util.getIoWorkerExecutor().execute(() -> {
				try {
					NbtCompound nbt = NbtIo.readCompressed(ipath, NbtSizeTracker.ofUnlimitedBytes());
					readNbt(server, nbt.getList("i", 10));
				} catch (Exception e) {
					LOGGER.error("Failed to load Shared Inventory", e);
				}
			});
		});
	}
}