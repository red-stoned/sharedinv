package com.redstoned.sharedinv;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;

import static net.minecraft.server.command.CommandManager.*;

public class SharedInventoryCommand {
	public static SuggestionProvider<ServerCommandSource> inventories_provider = (context, builder) -> {
		return CommandSource.suggestMatching(SharedInventoryMod.inventories.keySet().toArray(String[]::new), builder);
	};

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				literal("sharedinv")
				.requires(source -> source.hasPermissionLevel(2))
				.then(literal("help")
				.executes(context -> {
					context.getSource().sendFeedback(() -> {
						return Text.literal("Find help at: ").append(Text.literal("https://modrinth.com/mod/sharedinv").setStyle(Style.EMPTY
								.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/sharedinv")))
								.withUnderline(true)
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to open the Shared Inventory mod page on Modrinth")))
						));
					}, false);
					return 1;
				}))
				.then(literal("group")
				.then(literal("add").then(argument("name", StringArgumentType.word())
				.executes(SharedInventoryCommand::executeAdd))
				)
				.then(literal("remove").then(argument("inventory", StringArgumentType.word()).suggests(inventories_provider)
				.executes(SharedInventoryCommand::executeRemove))
				)
				.then(literal("list")
				.executes(SharedInventoryCommand::executeListInventories)
				.then(argument("inventory", StringArgumentType.string()).suggests(inventories_provider)
				.executes(SharedInventoryCommand::executeListInventoryPlayers))
				))
				.then(literal("join")
				.then(argument("inventory", StringArgumentType.string()).suggests(inventories_provider)
				.executes(SharedInventoryCommand::executeJoinSelf)
				.then(argument("player", GameProfileArgumentType.gameProfile())
				.executes(SharedInventoryCommand::executeJoinPlayers)))
				)
				.then(literal("leave")
				.then(argument("player", GameProfileArgumentType.gameProfile())
				.executes(SharedInventoryCommand::executeLeave))
				)
				.then(literal("default")
					.then(literal("set")
					.then(argument("inventory", StringArgumentType.string()).suggests(inventories_provider)
					.executes(SharedInventoryCommand::executeDefaultSet))
					)
					.then(literal("get")
					.executes(SharedInventoryCommand::executeDefaultGet)
					)
					.then(literal("clear")
					.executes(SharedInventoryCommand::executeDefaultRemove)
					)
				)
			);
		});
	}

	public static int executeAdd(CommandContext<ServerCommandSource> context) {
		String inv_name = StringArgumentType.getString(context, "name");
		if (SharedInventoryMod.inventories.get(inv_name) != null) {
			context.getSource().sendError(Text.literal("An inventory with that name already exists!"));
			return 0;
		}

		SharedInventory inv = new SharedInventory(inv_name);
		SharedInventoryMod.inventories.put(inv.name, inv);

		context.getSource().sendFeedback(() -> {
			return Text.literal(String.format("Created inventory '%s'", inv.name));
		}, true);

		return 1;
	}

	public static int executeRemove(CommandContext<ServerCommandSource> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendError(Text.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		while (!inv.players.isEmpty()) {
			UUID u = inv.players.iterator().next();
			inv.RemovePlayer(u);
			ServerPlayerEntity p = context.getSource().getServer().getPlayerManager().getPlayer(u);
			if (p == null) continue;
			SharedInventoryMod.RestorePlayerSlots(p);
		};
		SharedInventoryMod.inventories.remove(inv_name);

		context.getSource().sendFeedback(() -> {
			return Text.literal(String.format("Removed inventory %s", inv_name));
		}, true);

		return 1;
	}

	public static int executeJoinSelf(CommandContext<ServerCommandSource> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendError(Text.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}
		
		inv.AddPlayer(context.getSource().getPlayer().getUuid());
		context.getSource().getPlayer().getInventory().sharedinv$updateFrom(inv);

		context.getSource().sendFeedback(() -> {
			return Text.literal(String.format("%s is now sharing the inventory '%s'", context.getSource().getPlayer().getGameProfile().getName(), inv.name));
		}, true);

		return 1;
	}

	public static int executeJoinPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendError(Text.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
		profiles.forEach(profile -> {
			inv.AddPlayer(profile.getId());
			context.getSource().getServer().getPlayerManager().getPlayer(profile.getId()).getInventory().sharedinv$updateFrom(inv);
		});
		
		if (profiles.size() == 1) {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format("%s is now sharing the inventory '%s'", profiles.stream().findFirst().orElseThrow().getName(), inv.name));
			}, true);
		} else {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format("Shared inventory '%s' with %d players", inv.name, profiles.size()));
			}, true);
		}
		return 1;
	}

	public static int executeLeave(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
		profiles.forEach(profile -> {
			SharedInventory inv = SharedInventory.playerInvs.get(profile.getId());
			if (inv == null) return;
			inv.RemovePlayer(profile.getId());
			ServerPlayerEntity s = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());
			SharedInventoryMod.RestorePlayerSlots(s);
		});
		
		if (profiles.size() == 1) {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format("%s is no longer sharing an inventory", profiles.stream().findFirst().orElseThrow().getName()));
			}, true);
		} else {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format("%d players are no longer sharing an inventory", profiles.size()));
			}, true);
		}
		return 1;
	}

	public static int executeListInventories(CommandContext<ServerCommandSource> context) {
		if (SharedInventoryMod.inventories.isEmpty()) {
			context.getSource().sendFeedback(() -> {
				return Text.literal("There are no shared inventories");
			}, false);
		} else {
			context.getSource().sendFeedback(() -> {
				Set<String> inv_names = SharedInventoryMod.inventories.keySet();
				return Text.literal(String.format(
					"There %s %d shared %s: %s",
					inv_names.size() > 1 ? "are" : "is",
					inv_names.size(),
					inv_names.size() > 1 ? "inventories" : "inventory",
						String.join(", ", inv_names)
				));
			}, false);
		}
		return 1;
	}

	public static int executeListInventoryPlayers(CommandContext<ServerCommandSource> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendError(Text.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		if (inv.players.isEmpty()) {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format("There are no players sharing the inventory '%s'", inv.name));
			}, false);
		} else {
			context.getSource().sendFeedback(() -> {
				return Text.literal(String.format(
					"There %s %d player%s sharing the inventory '%s': %s",
					inv.players.size() > 1 ? "are" : "is",
					inv.players.size(),
					inv.players.size() > 1 ? "s" : "",
					inv.name,
					inv.players.stream().map(u -> {
						Optional<GameProfile> profile = context.getSource().getServer().getUserCache().getByUuid(u);
						return profile.isPresent() ? profile.get().getName() : u.toString();
					}).collect(Collectors.joining(", "))
				));
			}, false);
		}

		return 1;
	}

	public static int executeDefaultSet(CommandContext<ServerCommandSource> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendError(Text.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		SharedInventoryMod.default_inv = inv;

		context.getSource().sendFeedback(() -> {
			return Text.literal("Set default shared inventory to: " + inv.name);
		}, true);
		return 1;
	}

	public static int executeDefaultGet(CommandContext<ServerCommandSource> context) {
		if (SharedInventoryMod.default_inv != null) {
			context.getSource().sendFeedback(() -> {
				return Text.literal("The default inventory is: " + SharedInventoryMod.default_inv.name);
			}, false);
		} else {
			context.getSource().sendFeedback(() -> {
				return Text.literal("There is no default inventory.");
			}, false);
		}
		return 1;
	}

	public static int executeDefaultRemove(CommandContext<ServerCommandSource> context) {
		if (SharedInventoryMod.default_inv != null) {
			SharedInventoryMod.default_inv = null;

			context.getSource().sendFeedback(() -> {
				return Text.literal("Removed default shared inventory");
			}, true);
		} else {
			context.getSource().sendError(Text.literal("There is no default inventory to remove!"));
		}
		return 1;
	}
}
