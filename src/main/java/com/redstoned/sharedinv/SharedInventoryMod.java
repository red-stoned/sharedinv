package com.redstoned.sharedinv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class SharedInventoryMod implements ModInitializer {
	public static final String MOD_ID = "sharedinv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static HashMap<String, SharedInventory> inventories = new HashMap<>();

	public static void UpdatePlayerSlots(SharedInventory inv, ServerPlayerEntity player) {
		player.getInventory().main = inv.main;
		player.getInventory().armor = inv.armor;
		player.getInventory().offHand = inv.offHand;
		player.getInventory().combinedInventory = inv.combinedInventory;
	}

	public static void ResetPlayerSlots(ServerPlayerEntity player) {
		PlayerInventory i = player.getInventory();
		i.main = DefaultedList.ofSize(36, ItemStack.EMPTY);
		i.armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
		i.offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
		i.combinedInventory = ImmutableList.of(i.main, i.armor, i.offHand);
	}
	

	public static void Save(MinecraftServer server) {
		try {
			NbtCompound root = new NbtCompound();
			NbtList l = new NbtList();
			inventories.forEach((name, inv) -> {
				NbtCompound i = new NbtCompound();
				l.add(inv.toNbt(server.getRegistryManager(), i));
			});

			root.put("i", l);
			
			NbtIo.writeCompressed(root, server.getPath("world/sharedinv.nbt"));
		} catch (Exception e) {
			LOGGER.error("Failed to save Shared Inventory", e);
		}
	}

	private static void Load(MinecraftServer server) {
		Path ipath = server.getPath("world/sharedinv.nbt");
		if (!Files.exists(ipath)) {
			LOGGER.info("Could not find inventory state, starting empty.");
			return;
		};
		
		try {
			NbtCompound nbt = NbtIo.readCompressed(ipath, NbtSizeTracker.ofUnlimitedBytes());
			NbtList nt = nbt.getList("i", 10);
			for (int i = 0; i < nt.size(); ++i) {
				SharedInventory inv = SharedInventory.fromNbt(server.getRegistryManager(), nt.getCompound(i));
				LOGGER.info("[DEBUG] Loaded shared inv: " + inv.name);
				inventories.put(inv.name, inv);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load Shared Inventory", e);
		}
	}

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
			Save(server);
		});
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			Load(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SharedInventory inv = SharedInventory.playerInvs.get(handler.getPlayer().getUuid());
			if (inv == null) return;

			UpdatePlayerSlots(inv, handler.getPlayer());
		});

		SharedInventoryCommand.register();
	}
}