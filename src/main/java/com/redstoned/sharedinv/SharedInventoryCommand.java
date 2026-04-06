package com.redstoned.sharedinv;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import static net.minecraft.commands.Commands.*;

public class SharedInventoryCommand {
	public static SuggestionProvider<CommandSourceStack> inventories_provider = (context, builder) -> {
		return SharedSuggestionProvider.suggest(SharedInventoryMod.inventories.keySet().toArray(String[]::new), builder);
	};

	

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				literal("sharedinv")
				.requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
				.then(literal("help")
				.executes(context -> {
					context.getSource().sendSuccess(() -> {
						return Component.literal("Find help at: ").append(Component.literal("https://modrinth.com/mod/sharedinv").setStyle(Style.EMPTY
								.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/sharedinv")))
								.withUnderlined(true)
								.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the Shared Inventory mod page on Modrinth")))
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
				.then(argument("player", GameProfileArgument.gameProfile())
				.executes(SharedInventoryCommand::executeJoinPlayers)))
				)
				.then(literal("leave")
				.then(argument("player", GameProfileArgument.gameProfile())
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

	public static int executeAdd(CommandContext<CommandSourceStack> context) {
		String inv_name = StringArgumentType.getString(context, "name");
		if (SharedInventoryMod.inventories.get(inv_name) != null) {
			context.getSource().sendFailure(Component.literal("An inventory with that name already exists!"));
			return 0;
		}

		SharedInventory inv = new SharedInventory(inv_name);
		SharedInventoryMod.inventories.put(inv.name, inv);

		context.getSource().sendSuccess(() -> {
			return Component.literal(String.format("Created inventory '%s'", inv.name));
		}, true);

		return 1;
	}

	public static int executeRemove(CommandContext<CommandSourceStack> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		while (!inv.players.isEmpty()) {
			UUID u = inv.players.iterator().next();
			inv.RemovePlayer(u);
			ServerPlayer p = context.getSource().getServer().getPlayerList().getPlayer(u);
			if (p == null) continue;
			SharedInventoryMod.RestorePlayerSlots(p);
		};
		SharedInventoryMod.inventories.remove(inv_name);

		context.getSource().sendSuccess(() -> {
			return Component.literal(String.format("Removed inventory %s", inv_name));
		}, true);

		return 1;
	}

	public static int executeJoinSelf(CommandContext<CommandSourceStack> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}
		
		inv.AddPlayer(context.getSource().getPlayer().getUUID());
		context.getSource().getPlayer().getInventory().sharedinv$updateFrom(inv);

		context.getSource().sendSuccess(new Supplier<Component>() {
			public Component get() {
				return Component.literal(String.format("%s is now sharing the inventory '%s'", context.getSource().getPlayer().getGameProfile().name(), inv.name));
			}
		}, true);

		return 1;
	}

	public static int executeJoinPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "player");
		profiles.forEach(profile -> {
			inv.AddPlayer(profile.id());
			context.getSource().getServer().getPlayerList().getPlayer(profile.id()).getInventory().sharedinv$updateFrom(inv);
		});
		
		if (profiles.size() == 1) {
			context.getSource().sendSuccess(() -> Component.literal(String.format("%s is now sharing the inventory '%s'", profiles.stream().findFirst().orElseThrow().name(), inv.name)), true);
		} else {
			context.getSource().sendSuccess(() -> Component.literal(String.format("Shared inventory '%s' with %d players", inv.name, profiles.size())), true);
		}
		return 1;
	}

	public static int executeLeave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "player");
		profiles.forEach(profile -> {
			SharedInventory inv = SharedInventory.playerInvs.get(profile.id());
			if (inv == null) return;
			inv.RemovePlayer(profile.id());
			ServerPlayer s = context.getSource().getServer().getPlayerList().getPlayer(profile.id());
			SharedInventoryMod.RestorePlayerSlots(s);
		});
		
		if (profiles.size() == 1) {
			context.getSource().sendSuccess(() -> {
				return Component.literal(String.format("%s is no longer sharing an inventory", profiles.stream().findFirst().orElseThrow().name()));
			}, true);
		} else {
			context.getSource().sendSuccess(() -> {
				return Component.literal(String.format("%d players are no longer sharing an inventory", profiles.size()));
			}, true);
		}
		return 1;
	}

	public static int executeListInventories(CommandContext<CommandSourceStack> context) {
		if (SharedInventoryMod.inventories.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("There are no shared inventories"), false);
		} else {
			context.getSource().sendSuccess(() -> {
				Set<String> inv_names = SharedInventoryMod.inventories.keySet();
				return Component.literal(String.format(
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

	public static int executeListInventoryPlayers(CommandContext<CommandSourceStack> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		if (inv.players.isEmpty()) {
			context.getSource().sendSuccess(() -> {
				return Component.literal(String.format("There are no players sharing the inventory '%s'", inv.name));
			}, false);
		} else {
			context.getSource().sendSuccess(() -> Component.literal(String.format(
                "There %s %d player%s sharing the inventory '%s': %s",
                inv.players.size() > 1 ? "are" : "is",
                inv.players.size(),
                inv.players.size() > 1 ? "s" : "",
                inv.name,
                inv.players
					.stream()
					// todo(piz) this probably should use Services.profileResolver
					//  butttttt i really dont like the network request so
					.map(u -> context.getSource().getServer().services().nameToIdCache().get(u).map(NameAndId::name).orElseGet(u::toString))
					.collect(Collectors.joining(", "))
            )), false);
		}

		return 1;
	}

	public static int executeDefaultSet(CommandContext<CommandSourceStack> context) {
		String inv_name = StringArgumentType.getString(context, "inventory");
		SharedInventory inv = SharedInventoryMod.inventories.get(inv_name);
		if (inv == null) {
			context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", inv_name)));
			return 0;
		}

		SharedInventoryMod.default_inv = inv;

		context.getSource().sendSuccess(() -> {
			return Component.literal("Set default shared inventory to: " + inv.name);
		}, true);
		return 1;
	}

	public static int executeDefaultGet(CommandContext<CommandSourceStack> context) {
		if (SharedInventoryMod.default_inv != null) {
			context.getSource().sendSuccess(() -> {
				return Component.literal("The default inventory is: " + SharedInventoryMod.default_inv.name);
			}, false);
		} else {
			context.getSource().sendSuccess(() -> {
				return Component.literal("There is no default inventory.");
			}, false);
		}
		return 1;
	}

	public static int executeDefaultRemove(CommandContext<CommandSourceStack> context) {
		if (SharedInventoryMod.default_inv != null) {
			SharedInventoryMod.default_inv = null;

			context.getSource().sendSuccess(() -> {
				return Component.literal("Removed default shared inventory");
			}, true);
		} else {
			context.getSource().sendFailure(Component.literal("There is no default inventory to remove!"));
		}
		return 1;
	}
}
