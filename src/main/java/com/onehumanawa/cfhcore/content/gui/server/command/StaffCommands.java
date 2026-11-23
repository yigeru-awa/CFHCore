package com.onehumanawa.cfhcore.content.gui.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerCard;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.service.Moderation;
import com.onehumanawa.cfhcore.content.gui.server.service.Msg;
import com.onehumanawa.cfhcore.content.gui.server.service.OpResult;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerService;
import com.onehumanawa.cfhcore.content.gui.server.service.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class StaffCommands {
	static void build(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		moderation(out);
		tools(out);
		words(out);
		out.add(role());
	}

	private static void moderation(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(punishing("mute", Nodes.MOD_MUTE, Punishment.Kind.MUTE));
		out.add(pardoning("unmute", Nodes.MOD_MUTE, Punishment.Kind.MUTE));
		out.add(punishing("freeze", Nodes.MOD_FREEZE, Punishment.Kind.FREEZE));
		out.add(pardoning("unfreeze", Nodes.MOD_FREEZE, Punishment.Kind.FREEZE));
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tempban", Nodes.MOD_BAN, true).executes(c -> Cmd.usage(c, "tempban")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "tempban")))
						.then(
							((RequiredArgumentBuilder)Cmd.word("time").suggests(Cmd.TIMES).executes(c -> punish(c, Punishment.Kind.BAN, Cmd.text(c, "time"), true)))
								.then(Cmd.greedy("reason").executes(c -> punish(c, Punishment.Kind.BAN, Cmd.text(c, "time") + " " + Cmd.text(c, "reason"), true)))
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("ban", Nodes.MOD_BAN_PERMANENT, true).executes(c -> Cmd.usage(c, "ban")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> punish(c, Punishment.Kind.BAN, "", false)))
						.then(Cmd.greedy("reason").executes(c -> punish(c, Punishment.Kind.BAN, Cmd.text(c, "reason"), false)))
				)
		);
		out.add(pardoning("unban", Nodes.MOD_BAN, Punishment.Kind.BAN));
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("kick", Nodes.MOD_KICK, true).executes(c -> Cmd.usage(c, "kick")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> punish(c, Punishment.Kind.KICK, "", false)))
						.then(Cmd.greedy("reason").executes(c -> punish(c, Punishment.Kind.KICK, Cmd.text(c, "reason"), false)))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("warn", Nodes.MOD_WARN, true).executes(c -> Cmd.usage(c, "warn")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "warn")))
						.then(Cmd.greedy("reason").executes(c -> punish(c, Punishment.Kind.WARN, Cmd.text(c, "reason"), false)))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("history", Nodes.MOD_HISTORY, true).executes(c -> Cmd.usage(c, "history")))
				.then(Cmd.word("player").suggests(Cmd.KNOWN).executes(StaffCommands::history))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("note", Nodes.MOD_HISTORY, true).executes(c -> Cmd.usage(c, "note")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "note")))
						.then(
							Cmd.greedy("text")
								.executes(
									c -> Cmd.act(
											c,
											(s, a) -> s.moderation().noteAdd(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "text")),
											r -> Cmd.say("note_added", r.arg(0))
										)
								)
						)
				)
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> punishing(String name, Node node, Punishment.Kind kind) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command(name, node, true).executes(c -> Cmd.usage(c, name)))
			.then(
				((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> punish(c, kind, "", true)))
					.then(Cmd.greedy("rest").suggests(Cmd.TIMES).executes(c -> punish(c, kind, Cmd.text(c, "rest"), true)))
			);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> pardoning(String name, Node node, Punishment.Kind kind) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command(name, node, true).executes(c -> Cmd.usage(c, name)))
			.then(
				Cmd.word("player")
					.suggests(Cmd.KNOWN)
					.executes(
						c -> Cmd.act(
								c,
								(s, a) -> s.moderation().pardon(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), kind),
								r -> Cmd.say("pardoned_" + kind.key(), r.arg(0))
							)
					)
			);
	}

	private static int punish(CommandContext<CommandSourceStack> c, Punishment.Kind kind, String rest, boolean timed) throws CommandSyntaxException {
		String trimmed = rest.trim();
		int seconds = 0;
		String reason = trimmed;
		if (timed && !trimmed.isEmpty()) {
			int space = trimmed.indexOf(32);
			String first = space < 0 ? trimmed : trimmed.substring(0, space);
			int parsed = Text.seconds(first);
			if (parsed > 0) {
				seconds = parsed;
				reason = space < 0 ? "" : trimmed.substring(space + 1).trim();
			} else if (kind == Punishment.Kind.BAN) {
				((CommandSourceStack)c.getSource()).sendFailure(Cmd.say("time_invalid", first));
				return 0;
			}
		}

		int length = seconds;
		String why = reason;
		return Cmd.act(c, (s, a) -> s.moderation().punish(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), kind, why, length), r -> {
			MutableComponent line = kind.lasts() ? Cmd.say("punished_" + kind.key(), r.arg(0), Cmd.length((long)length)) : Cmd.say("punished_" + kind.key(), r.arg(0));
			return why.isEmpty() ? line : line.append(" ").append(Cmd.dim(Component.literal("(" + r.arg(3) + ")")));
		});
	}

	private static int history(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		return Cmd.act(
			c,
			(s, a) -> {
				OpResult r = s.moderation().history(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"));
				Moderation.History h = r.data(Moderation.History.class);
				if (h == null) {
					return r;
				} else {
					long now = s.now();
					List<Component> lines = new ArrayList<>();
					lines.add(Cmd.head(Cmd.say("history_header", h.name(), h.punishments().size(), h.notes().size())));
					if (h.punishments().isEmpty() && h.notes().isEmpty()) {
						lines.add(Cmd.dim(Cmd.say("history_none")));
					}

					int from = Math.max(0, h.punishments().size() - 12);
					if (from > 0) {
						lines.add(Cmd.dim(Cmd.say("history_older", from)));
					}

					for (int i = from; i < h.punishments().size(); i++) {
						Punishment p = h.punishments().get(i);
						MutableComponent line = Component.literal(" " + Text.date(p.at) + "  ")
							.withStyle(style -> style.withColor(11184810))
							.append(Msg.tr("cfhgui_server.kind." + p.kind.key()).withStyle(style -> style.withColor(p.active(now) ? 15026253 : 16777215)))
							.append(Cmd.dim(Component.literal(" ")).append(Cmd.say("history_by", p.byName)));
						if (p.kind.lasts()) {
							line.append(
								Cmd.dim(Component.literal(", "))
									.append(
										p.liftedAt != 0L
											? Cmd.say("history_lifted", p.liftedByName)
											: (
												p.expiresAt == 0L
													? Cmd.say("forever")
													: (p.active(now) ? Cmd.say("history_until", Text.date(p.expiresAt)) : Cmd.say("history_ended", Text.date(p.expiresAt)))
											)
									)
							);
						}

						if (!p.reason.isEmpty()) {
							line.append(Component.literal(": " + p.reason).withStyle(style -> style.withColor(14540253)));
						}

						lines.add(line);
					}

					for (PlayerRecord.Note n : h.notes()) {
						lines.add(
							Component.literal(" " + Text.date(n.at) + "  ")
								.withStyle(style -> style.withColor(11184810))
								.append(Cmd.say("history_note", n.byName).withStyle(style -> style.withColor(14722136)))
								.append(Component.literal(": " + n.text).withStyle(style -> style.withColor(14540253)))
						);
					}

					for (Component linex : lines) {
						source.sendSuccess(() -> linex, false);
					}

					return OpResult.OK;
				}
			},
			null
		);
	}

	private static void tools(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(onPlayer("heal", Nodes.MOD_HEAL, "heal"));
		out.add(onPlayer("feed", Nodes.MOD_HEAL, "feed"));
		if (Moderation.thirstHere()) {
			out.add(onPlayer("drink", Nodes.MOD_HEAL, "drink"));
		}

		out.add(onPlayer("fly", Nodes.MOD_FLY, "fly"));
		out.add(onPlayer("god", Nodes.MOD_GOD, "god"));
		out.add(onPlayer("vanish", Nodes.MOD_VANISH, "vanish"));
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("speed", Nodes.MOD_SPEED, true).executes(c -> Cmd.usage(c, "speed")))
				.then(
					((RequiredArgumentBuilder)Commands.argument("speed", IntegerArgumentType.integer(0, 10))
							.executes(c -> does(c, "speed", null, Integer.toString(IntegerArgumentType.getInteger(c, "speed")))))
						.then(
							Cmd.word("player")
								.suggests(Cmd.ONLINE)
								.executes(c -> does(c, "speed", Cmd.text(c, "player"), Integer.toString(IntegerArgumentType.getInteger(c, "speed"))))
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("gm", Nodes.MOD_GAMEMODE, true).executes(c -> Cmd.usage(c, "gm")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("mode")
							.suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"survival", "creative", "adventure", "spectator"}, b))
							.executes(c -> does(c, "gamemode", null, mode(Cmd.text(c, "mode")))))
						.then(Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> does(c, "gamemode", Cmd.text(c, "player"), mode(Cmd.text(c, "mode")))))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("invsee", Nodes.MOD_INVSEE, true).executes(c -> Cmd.usage(c, "invsee")))
				.then(Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> does(c, "invsee", Cmd.text(c, "player"), null)))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("endersee", Nodes.MOD_INVSEE, true).executes(c -> Cmd.usage(c, "endersee")))
				.then(Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> does(c, "endersee", Cmd.text(c, "player"), null)))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tphere", Nodes.MOD_TELEPORT, true).executes(c -> Cmd.usage(c, "tphere")))
				.then(Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> does(c, "bring", Cmd.text(c, "player"), null)))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tpo", Nodes.MOD_TELEPORT, true).executes(c -> Cmd.usage(c, "tpo")))
				.then(Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> does(c, "goto", Cmd.text(c, "player"), null)))
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> onPlayer(String name, Node node, String what) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command(name, node, true).executes(c -> does(c, what, null, null)))
			.then(Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> does(c, what, Cmd.text(c, "player"), null)));
	}

	private static String mode(String typed) {
		String var1 = typed.toLowerCase(Locale.ROOT);

		return switch (var1) {
			case "s", "0" -> "survival";
			case "c", "1" -> "creative";
			case "a", "2" -> "adventure";
			case "sp", "3" -> "spectator";
			default -> typed;
		};
	}

	private static int does(CommandContext<CommandSourceStack> c, String what, @Nullable String player, @Nullable String value) throws CommandSyntaxException {
		return Cmd.act(c, (s, a) -> {
			ServerService.Target t = player == null ? s.target(a.id(), null) : s.target(null, player);
			return player == null && t == null ? OpResult.fail("player_only") : s.moderation().playerDo(a, t, player == null ? a.name() : player, what, value);
		}, Cmd.journey(r -> {
			return switch (what) {
				case "invsee", "endersee" -> null;
				case "fly", "god", "vanish" -> Cmd.say("did_" + what + "_" + r.arg(1), r.arg(0));
				case "speed", "gamemode" -> Cmd.say("did_" + what, r.arg(0), r.arg(1));
				default -> Cmd.say("did_" + what, r.arg(0));
			};
		}));
	}

	private static void words(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		Consumer<String> broadcast = name -> out.add(
				(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)(name.equals("broadcast")
							? Cmd.command(name, Nodes.MOD_BROADCAST, true)
							: Cmd.alias(name, Nodes.MOD_BROADCAST))
						.executes(c -> Cmd.usage(c, "broadcast")))
					.then(Cmd.greedy("text").executes(c -> Cmd.act(c, (s, a) -> s.moderation().broadcast(a, Cmd.text(c, "text")), null)))
			);
		broadcast.accept("broadcast");
		broadcast.accept("bc");
		Consumer<String> staff = name -> out.add(
				(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)(name.equals("staffchat")
							? Cmd.command(name, Nodes.MOD_STAFF_CHAT, true)
							: Cmd.alias(name, Nodes.MOD_STAFF_CHAT))
						.executes(c -> Cmd.usage(c, "staffchat")))
					.then(
						Cmd.greedy("text")
							.executes(
								c -> Cmd.act(
										c,
										(s, a) -> s.moderation().staffChat(a, Cmd.text(c, "text")),
										r -> ((CommandSourceStack)c.getSource()).getEntity() instanceof ServerPlayer ? null : Cmd.say("staffchat_sent", r.arg(0))
									)
							)
					)
			);
		staff.accept("staffchat");
		staff.accept("sc");
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Cmd.command("maintenance", Nodes.ADMIN_MAINTENANCE, true)
						.executes(c -> Cmd.act(c, (s, a) -> {
								((CommandSourceStack)c.getSource()).sendSuccess(() -> Cmd.say(s.rules().maintenance ? "maintenance_is_on" : "maintenance_is_off"), false);
								return OpResult.OK;
							}, null)))
					.then(Cmd.alias("on", null).executes(c -> Cmd.act(c, (s, a) -> s.admin().ruleSet(a, "maintenance", "true"), r -> Cmd.say("maintenance_on")))))
				.then(Cmd.alias("off", null).executes(c -> Cmd.act(c, (s, a) -> s.admin().ruleSet(a, "maintenance", "false"), r -> Cmd.say("maintenance_off"))))
		);
		out.add((LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("admin", Nodes.ADMIN_MENU, true).executes(c -> {
			CommandSourceStack source = (CommandSourceStack)c.getSource();
			ServerService service = Cmd.service();
			return service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "admin", "") ? 1 : ServerCommands.help(source, true, 1);
		}));
	}

	static void kitAdmin(LiteralArgumentBuilder<CommandSourceStack> kit) {
		kit.then(
			((LiteralArgumentBuilder)Cmd.alias("create", Nodes.ADMIN_KITS).executes(c -> Cmd.usage(c, "kit")))
				.then(((RequiredArgumentBuilder)Cmd.word("id").executes(c -> kitCreate(c, 0))).then(Cmd.word("cooldown").suggests(Cmd.TIMES).executes(c -> {
					int seconds = Text.seconds(Cmd.text(c, "cooldown"));
					if (seconds <= 0) {
						((CommandSourceStack)c.getSource()).sendFailure(Cmd.say("time_invalid", Cmd.text(c, "cooldown")));
						return 0;
					} else {
						return kitCreate(c, seconds);
					}
				})))
		);
		kit.then(
			((LiteralArgumentBuilder)Cmd.alias("delete", Nodes.ADMIN_KITS).executes(c -> Cmd.usage(c, "kit")))
				.then(
					Cmd.word("id")
						.suggests(Cmd.ALL_KITS)
						.executes(c -> Cmd.act(c, (s, a) -> s.admin().kitDelete(a, Cmd.text(c, "id")), r -> Cmd.say("kit_deleted", r.arg(0))))
				)
		);
		kit.then(
			((LiteralArgumentBuilder)Cmd.alias("give", Nodes.ADMIN_KITS).executes(c -> Cmd.usage(c, "kit")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.ONLINE).executes(c -> Cmd.usage(c, "kit")))
						.then(
							Cmd.word("id")
								.suggests(Cmd.ALL_KITS)
								.executes(
									c -> Cmd.act(
											c,
											(s, a) -> s.admin().kitGive(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "id")),
											r -> Cmd.say("kit_given", r.arg(0), r.arg(1))
										)
								)
						)
				)
		);
	}

	private static int kitCreate(CommandContext<CommandSourceStack> c, int cooldownSeconds) throws CommandSyntaxException {
		return Cmd.act(c, (s, a) -> {
			Kit old = s.kits().get(Cmd.text(c, "id"));
			Kit kit = old == null ? new Kit() : (Kit)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(old), Kit.class);
			kit.id = Cmd.text(c, "id");
			if (old == null) {
				kit.name = Cmd.text(c, "id");
			}

			if (cooldownSeconds > 0 || old == null) {
				kit.cooldownSeconds = cooldownSeconds;
			}

			return s.admin().kitSave(a, kit, true);
		}, r -> Cmd.say("kit_saved", r.arg(0), r.arg(1)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> role() {
		LiteralArgumentBuilder<CommandSourceStack> role = (LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("role", Nodes.ADMIN_ROLES, true)
			.executes(c -> roleList(c));
		role.then(Cmd.alias("list", null).executes(StaffCommands::roleList));
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("info", null).executes(c -> Cmd.usage(c, "role")))
				.then(Cmd.word("role").suggests(Cmd.ROLES).executes(StaffCommands::roleInfo))
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("create", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").executes(c -> roleCreate(c, 10)))
						.then(Commands.argument("priority", IntegerArgumentType.integer(-1000, 1000)).executes(c -> roleCreate(c, IntegerArgumentType.getInteger(c, "priority"))))
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("delete", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					Cmd.word("role")
						.suggests(Cmd.ROLES)
						.executes(c -> Cmd.act(c, (s, a) -> s.admin().roleDelete(a, Cmd.text(c, "role")), r -> Cmd.say("role_deleted", r.arg(0))))
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("give", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "role")))
						.then(
							((RequiredArgumentBuilder)Cmd.word("role")
									.suggests(Cmd.ROLES)
									.executes(
										c -> Cmd.act(
												c,
												(s, a) -> s.admin().roleGrant(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "role"), 0),
												r -> Cmd.say("role_given", r.arg(0), r.arg(1))
											)
									))
								.then(
									Cmd.word("time")
										.suggests(Cmd.TIMES)
										.executes(
											c -> {
												int seconds = Text.seconds(Cmd.text(c, "time"));
												if (seconds <= 0) {
													((CommandSourceStack)c.getSource()).sendFailure(Cmd.say("time_invalid", Cmd.text(c, "time")));
													return 0;
												} else {
													return Cmd.act(
														c,
														(s, a) -> s.admin().roleGrant(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "role"), seconds),
														r -> Cmd.say("role_given_for", r.arg(0), r.arg(1), r.arg(2))
													);
												}
											}
										)
								)
						)
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("take", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "role")))
						.then(
							Cmd.word("role")
								.suggests(Cmd.ROLES)
								.executes(
									c -> Cmd.act(
											c,
											(s, a) -> s.admin().roleRevoke(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "role")),
											r -> Cmd.say("role_taken", r.arg(0), r.arg(1))
										)
								)
						)
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("player", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					Cmd.word("player")
						.suggests(Cmd.KNOWN)
						.executes(
							c -> Cmd.act(
									c,
									(s, a) -> s.people().card(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player")),
									r -> r.data(PlayerCard.class) == null ? null : PlayerCommands.roles(Cmd.actor((CommandSourceStack)c.getSource()), r.data(PlayerCard.class))
								)
						)
				)
		);

		for (String verb : new String[]{"allow", "deny", "unset"}) {
			role.then(
				((LiteralArgumentBuilder)Cmd.alias(verb, null).executes(c -> Cmd.usage(c, "role")))
					.then(
						((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
							.then(Cmd.greedy("node").suggests(Cmd.NODES).executes(c -> roleEdit(c, r -> {
									String node = Cmd.text(c, "node").trim().toLowerCase(Locale.ROOT);
									r.allow.remove(node);
									r.deny.remove(node);
									if (verb.equals("allow")) {
										r.allow.add(node);
									}

									if (verb.equals("deny")) {
										r.deny.add(node);
									}
								}, r -> Cmd.say("role_" + verb, Cmd.text(c, "node").trim().toLowerCase(Locale.ROOT), r.arg(0)))))
					)
			);
		}

		role.then(
			((LiteralArgumentBuilder)Cmd.alias("number", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
						.then(
							((RequiredArgumentBuilder)((RequiredArgumentBuilder)Cmd.word("node").suggests(Cmd.NUMBERS).executes(c -> Cmd.usage(c, "role")))
									.then(
										Cmd.alias("unset", null)
											.executes(
												c -> roleEdit(
														c, r -> r.numbers.remove(Cmd.text(c, "node").toLowerCase(Locale.ROOT)), r -> Cmd.say("role_number_unset", Cmd.text(c, "node"), r.arg(0))
													)
											)
									))
								.then(
									Commands.argument("value", IntegerArgumentType.integer(0, 1000000))
										.executes(
											c -> roleEdit(
													c,
													r -> r.numbers.put(Cmd.text(c, "node").toLowerCase(Locale.ROOT), IntegerArgumentType.getInteger(c, "value")),
													r -> Cmd.say("role_number", Cmd.text(c, "node"), IntegerArgumentType.getInteger(c, "value"), r.arg(0))
												)
										)
								)
						)
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("prefix", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
						.then(
							Cmd.greedy("prefix")
								.suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"none"}, b))
								.executes(
									c -> roleEdit(
											c,
											r -> r.prefix = Cmd.text(c, "prefix").equalsIgnoreCase("none") ? "" : Cmd.text(c, "prefix"),
											r -> Cmd.text(c, "prefix").equalsIgnoreCase("none")
													? Cmd.say("role_prefix_none", r.arg(0))
													: Cmd.say("role_prefix", r.arg(0), Cmd.text(c, "prefix"))
										)
								)
						)
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("name", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
						.then(Cmd.greedy("name").executes(c -> roleEdit(c, r -> r.name = Cmd.text(c, "name"), r -> Cmd.say("role_named", r.arg(0), Cmd.text(c, "name")))))
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("color", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
						.then(
							Cmd.greedy("color")
								.suggests(
									(c, b) -> SharedSuggestionProvider.suggest(
											new String[]{"red", "gold", "yellow", "green", "aqua", "blue", "light_purple", "white", "gray", "#E8553A"}, b
										)
								)
								.executes(
									c -> {
										int rgb = Text.color(Cmd.text(c, "color"));
										if (rgb < 0) {
											((CommandSourceStack)c.getSource()).sendFailure(Cmd.say("color_invalid", Cmd.text(c, "color")));
											return 0;
										} else {
											return roleEdit(
												c, r -> r.color = rgb, r -> Cmd.say("role_color", r.arg(0), Component.literal(Text.hex(rgb)).withStyle(style -> style.withColor(rgb)))
											);
										}
									}
								)
						)
				)
		);
		role.then(
			((LiteralArgumentBuilder)Cmd.alias("priority", null).executes(c -> Cmd.usage(c, "role")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
						.then(
							Commands.argument("priority", IntegerArgumentType.integer(-1000, 1000))
								.executes(
									c -> roleEdit(
											c,
											r -> r.priority = IntegerArgumentType.getInteger(c, "priority"),
											r -> Cmd.say("role_priority", r.arg(0), IntegerArgumentType.getInteger(c, "priority"))
										)
								)
						)
				)
		);

		for (String verb : new String[]{"inherit", "disinherit"}) {
			role.then(
				((LiteralArgumentBuilder)Cmd.alias(verb, null).executes(c -> Cmd.usage(c, "role")))
					.then(
						((RequiredArgumentBuilder)Cmd.word("role").suggests(Cmd.ROLES).executes(c -> Cmd.usage(c, "role")))
							.then(Cmd.word("other").suggests(Cmd.ROLES).executes(c -> roleEdit(c, r -> {
									String other = Cmd.text(c, "other").toLowerCase(Locale.ROOT);
									r.inherits.remove(other);
									if (verb.equals("inherit")) {
										r.inherits.add(other);
									}
								}, r -> Cmd.say("role_" + verb, r.arg(0), Cmd.text(c, "other").toLowerCase(Locale.ROOT)))))
					)
			);
		}

		return role;
	}

	private static int roleCreate(CommandContext<CommandSourceStack> c, int priority) throws CommandSyntaxException {
		return Cmd.act(c, (s, a) -> {
			String id = Text.id(Cmd.text(c, "role"));
			if (id == null) {
				return OpResult.fail("name_invalid");
			} else if (s.roles().get(id) != null) {
				return OpResult.fail("name_taken");
			} else {
				Role role = new Role();
				role.id = id;
				role.name = Cmd.text(c, "role").substring(0, 1).toUpperCase(Locale.ROOT) + Cmd.text(c, "role").substring(1);
				role.priority = priority;
				role.inherits.add("default");
				return s.admin().roleSave(a, role);
			}
		}, r -> Cmd.say("role_created", r.arg(0), priority));
	}

	private static int roleEdit(CommandContext<CommandSourceStack> c, Consumer<Role> change, Function<OpResult, Component> said) throws CommandSyntaxException {
		return Cmd.act(c, (s, a) -> {
			Role old = s.roles().get(Cmd.text(c, "role").toLowerCase(Locale.ROOT));
			if (old == null) {
				return OpResult.fail("no_such_role", Cmd.text(c, "role"));
			} else {
				Role copy = (Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(old), Role.class);
				change.accept(copy);
				return s.admin().roleSave(a, copy);
			}
		}, said);
	}

	private static int roleList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		return Cmd.act(
			c,
			(s, a) -> {
				OpResult r = s.admin().roles(a);
				AdminData.RoleSet set = r.data(AdminData.RoleSet.class);
				if (set == null) {
					return r;
				} else {
					List<Component> lines = new ArrayList<>();
					lines.add(Cmd.head(Cmd.say("roles_header", set.roles.size())));

					for (int i = 0; i < set.roles.size(); i++) {
						Role role = set.roles.get(i);
						MutableComponent line = Component.literal(" ")
							.append(Cmd.link(role.id, "/role info " + role.id))
							.append("  ")
							.append(Cmd.role(new Role.Tag(role)))
							.append(Cmd.dim(Component.literal("  ")).append(Cmd.say("roles_entry", role.priority, i < set.holders.size() ? set.holders.get(i) : 0)));
						if (role.everyone) {
							line.append(" ").append(Cmd.dim(Cmd.say("role_everyone")));
						}

						if (role.operators) {
							line.append(" ").append(Cmd.dim(Cmd.say("role_operators")));
						}

						lines.add(line);
					}

					if (!set.externalPermissions.isEmpty()) {
						lines.add(Cmd.dim(Cmd.say("roles_external", set.externalPermissions)));
					}

					for (Component linex : lines) {
						source.sendSuccess(() -> linex, false);
					}

					return OpResult.OK;
				}
			},
			null
		);
	}

	private static int roleInfo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		return Cmd.act(c, (s, a) -> {
			if (!s.may(a, Nodes.ADMIN_ROLES)) {
				return OpResult.fail("no_permission");
			} else {
				Role role = s.roles().get(Cmd.text(c, "role").toLowerCase(Locale.ROOT));
				if (role == null) {
					return OpResult.fail("no_such_role", Cmd.text(c, "role"));
				} else {
					List<Component> lines = new ArrayList<>();
					lines.add(Cmd.head(Cmd.say("role_info_header", role.id)).append(" ").append(Cmd.role(new Role.Tag(role))));
					lines.add(Component.literal(" ").append(Cmd.say("role_info_priority", role.priority, Text.hex(role.color), role.prefix.isEmpty() ? "-" : role.prefix)));
					if (!role.inherits.isEmpty()) {
						lines.add(Component.literal(" ").append(Cmd.say("role_info_inherits", String.join(", ", role.inherits))));
					}

					if (role.everyone) {
						lines.add(Component.literal(" ").append(Cmd.dim(Cmd.say("role_everyone"))));
					}

					if (role.operators) {
						lines.add(Component.literal(" ").append(Cmd.dim(Cmd.say("role_operators"))));
					}

					for (String node : role.allow) {
						lines.add(Component.literal(" + " + node).withStyle(style -> style.withColor(6074742)));
					}

					for (String node : role.deny) {
						lines.add(Component.literal(" - " + node).withStyle(style -> style.withColor(15026253)));
					}

					for (Entry<String, Integer> e : role.numbers.entrySet()) {
						lines.add(Component.literal(" # " + e.getKey() + " = " + e.getValue()).withStyle(style -> style.withColor(14540253)));
					}

					if (role.allow.isEmpty() && role.deny.isEmpty() && role.numbers.isEmpty()) {
						lines.add(Cmd.dim(Cmd.say("role_info_silent")));
					}

					for (Component line : lines) {
						source.sendSuccess(() -> line, false);
					}

					return OpResult.OK;
				}
			}
		}, null);
	}

	private StaffCommands() {
	}
}
