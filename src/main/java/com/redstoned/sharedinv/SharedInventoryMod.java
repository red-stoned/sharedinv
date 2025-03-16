package com.redstoned.sharedinv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

public class SharedInventoryMod implements ModInitializer {
	public static final String MOD_ID = "sharedinv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Object2ObjectMap<String, SharedInventory> inventories = new Object2ObjectOpenHashMap<>();
	public static Object2ReferenceMap<UUID, List<DefaultedList<ItemStack>>> original_inventories = new Object2ReferenceOpenHashMap<>();
	public static SharedInventory default_inv = null;

	public static void UpdatePlayerSlots(SharedInventory inv, ServerPlayerEntity player) {
		player.getInventory().main = inv.main;
		player.getInventory().armor = inv.armor;
		player.getInventory().offHand = inv.offHand;
		player.getInventory().combinedInventory = inv.combinedInventory;
	}

	public static void RestorePlayerSlots(PlayerEntity player) {
		PlayerInventory i = player.getInventory();
		var original_inv = original_inventories.get(player.getUuid());
		if (original_inv == null) {
			// LOGGER.info("[DEBUG] has no original inv");
			i.main = DefaultedList.ofSize(36, ItemStack.EMPTY);
			i.armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
			i.offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
			i.combinedInventory = ImmutableList.of(i.main, i.armor, i.offHand);
		} else {
			i.main = original_inv.get(0);
			i.armor = original_inv.get(1);
			i.offHand = original_inv.get(2);
			i.combinedInventory = original_inv;
		}
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
			if (default_inv != null) {
				root.putString("d", default_inv.name);
			}
			
			NbtIo.writeCompressed(root, server.session.getDirectory().path().resolve("sharedinv.nbt"));
		} catch (Exception e) {
			LOGGER.error("Failed to save Shared Inventory", e);
		}
	}

	private static void Load(MinecraftServer server) {
		Path ipath = server.session.getDirectory().path().resolve("sharedinv.nbt");
		if (!Files.exists(ipath)) {
			// LOGGER.info("Could not find inventory state, defaulting to single shared inventory.");

			default_inv = new SharedInventory("group_1");
			SharedInventoryMod.inventories.put(default_inv.name, default_inv);

			return;
		};
		
		try {
			NbtCompound nbt = NbtIo.readCompressed(ipath, NbtSizeTracker.ofUnlimitedBytes());
			NbtList nt = nbt.getList("i", 10);
			for (int i = 0; i < nt.size(); ++i) {
				SharedInventory inv = SharedInventory.fromNbt(server.getRegistryManager(), nt.getCompound(i));
				// LOGGER.info("[DEBUG] Loaded shared inv: " + inv.name);
				inventories.put(inv.name, inv);
			}

			String def_name = nbt.getString("d");
			SharedInventory def = inventories.get(def_name);
			if (def_name != "" && def != null) {
				default_inv = def;
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
			original_inventories.put(handler.getPlayer().getUuid(), handler.getPlayer().getInventory().combinedInventory);

			SharedInventory inv = SharedInventory.playerInvs.get(handler.getPlayer().getUuid());
			if (inv == null) {
				if (default_inv == null) return;
				
				default_inv.AddPlayer(handler.getPlayer().getUuid());
				// LOGGER.info(String.format("[DEBUG] Adding %s to default team %s", handler.getPlayer().getGameProfile().getName(), default_inv.name));
				inv = default_inv;
			}

			UpdatePlayerSlots(inv, handler.getPlayer());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			original_inventories.remove(handler.getPlayer().getUuid());
			// LOGGER.info("saved invs: " + original_inventories.size());
		});

		SharedInventoryCommand.register();
	}
}