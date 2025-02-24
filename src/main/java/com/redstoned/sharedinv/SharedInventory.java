package com.redstoned.sharedinv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;

import static net.minecraft.server.command.CommandManager.*;

public class SharedInventory implements ModInitializer {
	public static final String MOD_ID = "sharedinv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static HashMap<String, SharedInventoryTeam> teams = new HashMap<>();

	public static DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
	public static DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
	public static DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);

	@Override
	public void onInitialize() {
		// ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
		// 	Util.getIoWorkerExecutor().execute(() -> {
		// 		try {
		// 			NbtList l = writeNbt(server, new NbtList());
		// 			NbtCompound n = new NbtCompound();
		// 			n.put("i", l);
					
		// 			NbtIo.writeCompressed(n, server.getPath("world/sharedinv.nbt"));
		// 		} catch (Exception e) {
		// 			LOGGER.error("Failed to save Shared Inventory", e);
		// 		}
		// 	});
		// });
		// ServerLifecycleEvents.SERVER_STARTING.register(server -> {
		// 	Path ipath = server.getPath("world/sharedinv.nbt");
		// 	if (!Files.exists(ipath)) {
		// 		LOGGER.info("Could not find inventory state, starting empty.");
		// 		return;
		// 	};
		// 	Util.getIoWorkerExecutor().execute(() -> {
		// 		try {
		// 			NbtCompound nbt = NbtIo.readCompressed(ipath, NbtSizeTracker.ofUnlimitedBytes());
		// 			readNbt(server, nbt.getList("i", 10));
		// 		} catch (Exception e) {
		// 			LOGGER.error("Failed to load Shared Inventory", e);
		// 		}
		// 	});
		// });

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				literal("sharedinv")
				.then(literal("add")
					.then(argument("name", StringArgumentType.word())
					.executes(context -> {
						String team_name = StringArgumentType.getString(context, "name");
						if (teams.get(team_name) != null) {
							context.getSource().sendError(Text.translatable("commands.team.add.duplicate"));
							return 0;
						}

						SharedInventoryTeam t1 = new SharedInventoryTeam(team_name);
						teams.put(team_name, t1);

						context.getSource().sendFeedback(() -> {
							return Text.translatable("commands.team.add.success", t1.name);
						}, true);

						return 1;
					})						
					)
				)
				.then(literal("join")
					.then(argument("team", StringArgumentType.string()).suggests((context, builder) -> CommandSource.suggestMatching(teams.keySet().toArray(new String[teams.keySet().size()]), builder))
						.executes(context -> {
							String team_name = StringArgumentType.getString(context, "team");
							SharedInventoryTeam t = teams.get(team_name);
							if (t == null) {
								context.getSource().sendError(Text.translatable("team.notFound", team_name));
								return 0;
							}
							t.AddPlayer(context.getSource().getPlayer().getUuid());

							context.getSource().sendFeedback(() -> {
								return Text.translatable("commands.team.join.success.single", context.getSource().getPlayer().getStyledDisplayName(), t.Name());
							}, true);

							return 1;
						})

						.then(argument("player", GameProfileArgumentType.gameProfile())
							.executes(context -> {
								String team_name = StringArgumentType.getString(context, "team");
								if (teams.get(team_name) == null) {
									context.getSource().sendError(Text.translatable("team.notFound", team_name));
									return 0;
								}
								SharedInventoryTeam t = teams.get(team_name);

								Collection<GameProfile> prof = GameProfileArgumentType.getProfileArgument(context, "player");
								
								if (prof.size() == 1) {
									context.getSource().sendFeedback(() -> {
									   return Text.translatable("commands.team.join.success.single", new Object[]{getMemberName(members), t.Name()});
									}, true);
								 } else {
									context.getSource().sendFeedback(() -> {
									   return Text.translatable("commands.team.join.success.multiple", new Object[]{members.size(), t.Name()});
									}, true);
								 }
								return 1;
							})
						)
					)
				)
			);
		});
	}
}