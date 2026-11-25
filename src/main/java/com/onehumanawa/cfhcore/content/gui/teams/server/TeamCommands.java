package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

public final class TeamCommands {
	private static final Predicate<CommandSourceStack> OPERATOR = s -> s.hasPermission(2);
	private static final String[] COLORS = new String[]{"red", "gold", "yellow", "green", "aqua", "blue", "light_purple", "white", "#D9805E"};
	private static final SuggestionProvider<CommandSourceStack> ALL_TEAMS = names(c -> teams(c, (s, t) -> true));
	private static final SuggestionProvider<CommandSourceStack> OTHER_TEAMS = names(c -> {
		TeamData mine = own(c);
		return teams(c, (s, t) -> t != mine && (mine == null || !mine.isAlly(t.id)));
	});
	private static final SuggestionProvider<CommandSourceStack> ALLIES = names(c -> {
		TeamData mine = own(c);
		return teams(c, (s, t) -> mine != null && mine.isAlly(t.id));
	});
	private static final SuggestionProvider<CommandSourceStack> JOINABLE = names(c -> {
		UUID me = actor((CommandSourceStack)c.getSource()).id();
		List<String> out = teams(c, (s, t) -> t.privacy != Privacy.INVITE || s.store.invitersOf(me).contains(t.id));
		if (((CommandSourceStack)c.getSource()).hasPermission(2)) {
			out.addAll(((CommandSourceStack)c.getSource()).getAllTeams());
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> INVITERS = names(c -> {
		UUID me = actor((CommandSourceStack)c.getSource()).id();
		return teams(c, (s, t) -> s.store.invitersOf(me).contains(t.id));
	});
	private static final SuggestionProvider<CommandSourceStack> ASKED = names(c -> {
		UUID me = actor((CommandSourceStack)c.getSource()).id();
		return teams(c, (s, t) -> s.store.askedBy(me).contains(t.id));
	});
	private static final SuggestionProvider<CommandSourceStack> MEMBERS = names(c -> {
		TeamData mine = own(c);
		List<String> out = new ArrayList<>();
		if (mine != null) {
			for (TeamData.Member m : mine.members) {
				out.add(m.name);
			}
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> INVITED = names(c -> {
		TeamData mine = own(c);
		List<String> out = new ArrayList<>();
		if (mine != null) {
			for (TeamData.Invite i : mine.invites) {
				out.add(i.playerName);
			}
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> ASKING = names(c -> {
		TeamData mine = own(c);
		List<String> out = new ArrayList<>();
		if (mine != null) {
			for (TeamData.Request r : mine.requests) {
				out.add(r.playerName);
			}
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> INVITABLE = names(c -> {
		TeamData mine = own(c);
		List<String> out = new ArrayList<>();

		for (ServerPlayer p : ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()) {
			if (mine == null || !mine.isMember(p.getUUID())) {
				out.add(p.getGameProfile().getName());
			}
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> WAYPOINTS = names(c -> {
		TeamData mine = own(c);
		List<String> out = new ArrayList<>();
		if (mine != null) {
			for (TeamData.Waypoint w : mine.waypoints) {
				out.add(w.name);
			}
		}

		return out;
	});
	private static final SuggestionProvider<CommandSourceStack> RULE_VALUES = (c, b) -> {
		String rule = LandPins.canonical(StringArgumentType.getString(c, "rule"));
		if (rule == null) {
			return b.buildFuture();
		} else {
			return switch (rule) {
				case "build", "interact", "containers", "entities" -> SharedSuggestionProvider.suggest(new String[]{"members", "allies", "everyone"}, b);
				case "claimRank" -> SharedSuggestionProvider.suggest(new String[]{"member", "moderator", "admin", "owner"}, b);
				default -> SharedSuggestionProvider.suggest(new String[]{"true", "false"}, b);
			};
		}
	};
	private static final SuggestionProvider<CommandSourceStack> LAND_RULE_VALUES = (c, b) -> {
		String rule = LandPins.canonical(StringArgumentType.getString(c, "rule"));
		if (rule == null) {
			return b.buildFuture();
		} else {
			return switch (rule) {
				case "build", "interact", "containers", "entities" -> SharedSuggestionProvider.suggest(new String[]{"everyone", "nobody"}, b);
				default -> SharedSuggestionProvider.suggest(new String[]{"true", "false"}, b);
			};
		}
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		buildSubcommand(context).ifPresent(dispatcher::register);
	}

	public static Optional<LiteralArgumentBuilder<CommandSourceStack>> buildSubcommand(CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> team = Commands.literal("teams");
		ours(team, null, null);

		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cfhcore");
		root.then(team);
		root.then(Commands.literal("list").executes(c -> list((CommandSourceStack)c.getSource())));
		root.then(((LiteralArgumentBuilder)Commands.literal("reload").requires(OPERATOR)).executes(c -> reload((CommandSourceStack)c.getSource())));
		root.then(((LiteralArgumentBuilder)Commands.literal("selftest").requires(OPERATOR)).executes(c -> selfTest((CommandSourceStack)c.getSource())));
		root.then(admin());
		root.then(land());
		root.then(expiry());

		return Optional.of(root);
	}

	private static void ours(
		LiteralArgumentBuilder<CommandSourceStack> team, @Nullable CommandDispatcher<CommandSourceStack> game, @Nullable CommandNode<CommandSourceStack> vanilla
	) {
		team.executes(c -> {
			CommandSourceStack src = (CommandSourceStack)c.getSource();
			TeamService s = service();
			return s != null && s.teamOf(actor(src).id()) != null ? info(src, null) : hint(src);
		});
		team.then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> {
			int[] land = new int[]{0};
			return act(c, (s, a) -> {
				TeamData own = s.personalOf(a.id());
				land[0] = own == null ? 0 : s.claims.count(own.id);
				return s.create(a, text(c, "name"), "", null);
			}, r -> land[0] > 0 ? say("created_land", teamName(r.args().get(0)), land[0]) : say("created", teamName(r.args().get(0))));
		})));
		team.then(
			((LiteralArgumentBuilder)Commands.literal("disband").executes(c -> ask(c, Permission.OWN, "disband")))
				.then(Commands.literal("confirm").executes(c -> act(c, (s, a) -> s.disband(a), r -> gaveUp(r) ? null : say("disbanded", r.args().get(0)))))
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("leave")
					.executes(c -> ownLand(c) ? ask(c, null, "leave") : act(c, (s, a) -> s.leave(a), r -> gaveUp(r) ? null : say("left", r.args().get(0)))))
				.then(Commands.literal("confirm").executes(c -> act(c, (s, a) -> s.leave(a), r -> gaveUp(r) ? null : say("left", r.args().get(0)))))
		);
		team.then(
			Commands.literal("invite")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.suggests(INVITABLE)
						.executes(c -> act(c, (s, a) -> s.invite(a, null, text(c, "player")), r -> say("invited", r.args().get(0), service().cfg().inviteMinutes)))
				)
		);
		team.then(
			Commands.literal("uninvite")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.suggests(INVITED)
						.executes(c -> act(c, (s, a) -> s.cancelInvite(a, invitedId(s, a, text(c, "player"))), r -> say("uninvited", text(c, "player"))))
				)
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("accept").executes(c -> answer(c, null, true)))
				.then(Commands.argument("team", StringArgumentType.greedyString()).suggests(INVITERS).executes(c -> answer(c, text(c, "team"), true)))
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("decline").executes(c -> answer(c, null, false)))
				.then(Commands.argument("team", StringArgumentType.greedyString()).suggests(INVITERS).executes(c -> answer(c, text(c, "team"), false)))
		);
		team.then(Commands.literal("join").then(Commands.argument("team", StringArgumentType.greedyString()).suggests(JOINABLE).executes(c -> join(c, game))));
		team.then(
			Commands.literal("cancel")
				.then(
					Commands.argument("team", StringArgumentType.greedyString())
						.suggests(ASKED)
						.executes(c -> act(c, (s, a) -> s.cancelRequest(a, teamId(s, text(c, "team"))), r -> say("request_cancelled", r.args().get(0))))
				)
		);
		team.then(Commands.literal("requests").executes(c -> requests((CommandSourceStack)c.getSource())));
		team.then(
			Commands.literal("admit")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.suggests(ASKING)
						.executes(c -> act(c, (s, a) -> s.acceptRequest(a, askingId(s, a, text(c, "player"))), r -> say("admitted", r.args().get(0))))
				)
		);
		team.then(
			Commands.literal("deny")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.suggests(ASKING)
						.executes(c -> act(c, (s, a) -> s.denyRequest(a, askingId(s, a, text(c, "player"))), r -> say("denied", text(c, "player"))))
				)
		);
		team.then(
			Commands.literal("kick")
				.then(
					Commands.argument("member", StringArgumentType.word())
						.suggests(MEMBERS)
						.executes(c -> act(c, (s, a) -> s.kick(a, memberId(s, a, text(c, "member"))), r -> say("kicked", r.args().get(0))))
				)
		);
		team.then(
			Commands.literal("promote")
				.then(
					Commands.argument("member", StringArgumentType.word())
						.suggests(MEMBERS)
						.executes(c -> act(c, (s, a) -> s.promote(a, memberId(s, a, text(c, "member"))), r -> say("ranked", r.args().get(0), rank(r.args().get(1)))))
				)
		);
		team.then(
			Commands.literal("demote")
				.then(
					Commands.argument("member", StringArgumentType.word())
						.suggests(MEMBERS)
						.executes(c -> act(c, (s, a) -> s.demote(a, memberId(s, a, text(c, "member"))), r -> say("ranked", r.args().get(0), rank(r.args().get(1)))))
				)
		);
		team.then(
			Commands.literal("transfer")
				.then(
					((RequiredArgumentBuilder)Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS).executes(c -> ask(c, Permission.OWN, "transfer")))
						.then(
							Commands.literal("confirm").executes(c -> act(c, (s, a) -> s.transfer(a, memberId(s, a, text(c, "member"))), r -> say("transferred", r.args().get(0))))
						)
				)
		);
		team.then(
			Commands.literal("ally")
				.then(
					Commands.argument("team", StringArgumentType.greedyString())
						.suggests(OTHER_TEAMS)
						.executes(c -> act(c, (s, a) -> s.ally(a, teamId(s, text(c, "team"))), r -> say("allied", r.args().get(0))))
				)
		);
		team.then(
			Commands.literal("unally")
				.then(
					Commands.argument("team", StringArgumentType.greedyString())
						.suggests(ALLIES)
						.executes(c -> act(c, (s, a) -> s.unally(a, teamId(s, text(c, "team"))), r -> say("unallied", r.args().get(0))))
				)
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("info").executes(c -> info((CommandSourceStack)c.getSource(), null)))
				.then(
					Commands.argument("team", StringArgumentType.greedyString()).suggests(ALL_TEAMS).executes(c -> info((CommandSourceStack)c.getSource(), text(c, "team")))
				)
		);
		Command<CommandSourceStack> gameList = vanilla != null && vanilla.getChild("list") != null ? vanilla.getChild("list").getCommand() : null;
		team.then(Commands.literal("list").executes(c -> {
			CommandSourceStack src = (CommandSourceStack)c.getSource();
			boolean operator = gameList != null && src.hasPermission(2);
			if (operator && !(src.getEntity() instanceof ServerPlayer)) {
				return gameList.run(c);
			} else {
				int n = list(src);
				if (operator && otherScoreboardTeams(src)) {
					gameList.run(c);
				}

				return n;
			}
		}));
		team.then(
			Commands.literal("chat")
				.then(Commands.argument("message", StringArgumentType.greedyString()).executes(c -> act(c, (s, a) -> s.chat(a, text(c, "message")), r -> null)))
		);
		team.then(Commands.literal("home").executes(c -> act(c, (s, a) -> s.goHome(a), r -> null)));
		team.then(Commands.literal("sethome").executes(c -> act(c, (s, a) -> s.setHome(a), r -> say("home_set"))));
		team.then(Commands.literal("delhome").executes(c -> act(c, (s, a) -> s.clearHome(a), r -> say("home_cleared"))));
		team.then(Commands.literal("stash").executes(c -> act(c, (s, a) -> s.openStash(a), r -> null)));
		team.then(
			((LiteralArgumentBuilder)Commands.literal("claim").executes(c -> claim(c, 0)))
				.then(Commands.argument("radius", IntegerArgumentType.integer(0, 15)).executes(c -> claim(c, IntegerArgumentType.getInteger(c, "radius"))))
		);
		team.then(
			((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("unclaim")
						.executes(c -> act(c, (s, a) -> s.unclaim(a, null, around(a, 0)), r -> say("unclaimed", r.number(0)))))
					.then(
						Commands.argument("radius", IntegerArgumentType.integer(0, 15))
							.executes(c -> act(c, (s, a) -> s.unclaim(a, null, around(a, IntegerArgumentType.getInteger(c, "radius"))), r -> say("unclaimed", r.number(0))))
					))
				.then(
					((LiteralArgumentBuilder)Commands.literal("all").executes(c -> ask(c, null, "unclaim_all")))
						.then(Commands.literal("confirm").executes(c -> act(c, (s, a) -> s.unclaimAll(a), r -> say("unclaimed", r.number(0)))))
				)
		);
		team.then(
			((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("forceload").executes(c -> forceLoad(c, true)))
					.then(Commands.literal("on").executes(c -> forceLoad(c, true))))
				.then(Commands.literal("off").executes(c -> forceLoad(c, false)))
		);
		team.then(
			((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("waypoint")
							.executes(c -> waypoints((CommandSourceStack)c.getSource())))
						.then(Commands.literal("list").executes(c -> waypoints((CommandSourceStack)c.getSource()))))
					.then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> act(c, (s, a) -> {
							ServerPlayer px = ((CommandSourceStack)c.getSource()).getPlayerOrException();
							return s.addWaypoint(a, text(c, "name"), px.getBlockX(), px.getBlockY(), px.getBlockZ(), null, null, null);
						}, r -> say("waypoint_added", r.args().get(0)))))))
				.then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.greedyString()).suggests(WAYPOINTS).executes(c -> act(c, (s, a) -> {
						TeamData t = s.teamOf(a.id());
						TeamData.Waypoint w = t == null ? null : s.waypointNamed(t, text(c, "name"));
						return s.removeWaypoint(a, w == null ? null : w.id);
					}, r -> say("waypoint_removed", r.args().get(0))))))
		);
		team.then(
			Commands.literal("rename")
				.then(
					Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> act(c, (s, a) -> s.rename(a, text(c, "name")), r -> say("renamed", r.args().get(0))))
				)
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("tag").executes(c -> act(c, (s, a) -> s.setTag(a, ""), r -> say("tag_cleared"))))
				.then(
					Commands.argument("tag", StringArgumentType.word()).executes(c -> act(c, (s, a) -> s.setTag(a, text(c, "tag")), r -> say("tag_set", r.args().get(0))))
				)
		);
		team.then(
			Commands.literal("color")
				.then(
					Commands.argument("color", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(COLORS, b))
						.executes(c -> act(c, (s, a) -> {
								Integer rgb = color(text(c, "color"));
								return rgb == null ? OpResult.fail("invalid") : s.setColor(a, rgb);
							}, r -> {
								Integer rgb = color(text(c, "color"));
								String hex = String.format(Locale.ROOT, "#%06X", rgb == null ? 0 : rgb);
								return say("color_set", Component.literal(hex).withStyle(Style.EMPTY.withColor(rgb == null ? 16777215 : rgb)));
							}))
				)
		);
		LiteralArgumentBuilder<CommandSourceStack> privacy = Commands.literal("privacy");

		for (Privacy p : Privacy.values()) {
			privacy.then(
				Commands.literal(p.key()).executes(c -> act(c, (s, a) -> s.setPrivacy(a, p), r -> say("privacy_set", Msg.tr("cfhgui_teams.privacy." + p.key()))))
			);
		}

		team.then(privacy);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("motto").executes(c -> act(c, (s, a) -> s.setMotto(a, ""), r -> say("motto_cleared"))))
				.then(Commands.argument("motto", StringArgumentType.greedyString()).executes(c -> act(c, (s, a) -> s.setMotto(a, text(c, "motto")), r -> say("motto_set"))))
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("friendlyfire")
					.then(Commands.literal("on").executes(c -> act(c, (s, a) -> s.setFriendlyFire(a, true), r -> say("friendly_fire_on")))))
				.then(Commands.literal("off").executes(c -> act(c, (s, a) -> s.setFriendlyFire(a, false), r -> say("friendly_fire_off"))))
		);
		team.then(
			((LiteralArgumentBuilder)Commands.literal("rule").executes(c -> rules((CommandSourceStack)c.getSource())))
				.then(
					Commands.argument("rule", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(LandPins.RULES, b))
						.then(
							Commands.argument("value", StringArgumentType.word())
								.suggests(RULE_VALUES)
								.executes(
									c -> act(
											c, (s, a) -> s.setLandRule(a, text(c, "rule"), text(c, "value")), r -> say("rule_set", r.args().get(0), r.args().get(1).toLowerCase(Locale.ROOT))
										)
								)
						)
				)
		);
	}

	@Nullable
	private static TeamService service() {
		return TeamsServer.service();
	}

	private static Actor actor(CommandSourceStack src) {
		return src.getEntity() instanceof ServerPlayer p ? Actor.of(p) : Actor.console(src.getTextName());
	}

	private static String text(CommandContext<CommandSourceStack> c, String name) {
		return StringArgumentType.getString(c, name);
	}

	private static int act(CommandContext<CommandSourceStack> c, TeamCommands.Op op, Function<OpResult, Component> said) throws CommandSyntaxException {
		CommandSourceStack src = (CommandSourceStack)c.getSource();
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else {
			OpResult r = op.run(s, actor(src));
			s.sync.flush();
			if (!r.ok()) {
				src.sendFailure(r.message());
				return 0;
			} else {
				Component line = said.apply(r);
				if (line != null) {
					src.sendSuccess(() -> line, false);
				}

				return 1;
			}
		}
	}

	private static MutableComponent say(String key, Object... args) {
		return Msg.tr("cfhgui_teams.command." + key, args);
	}

	private static Component claimed(OpResult r) {
		return r.number(1) > 0 ? say("claimed_some", r.number(0), r.number(1)) : say("claimed", r.number(0));
	}

	private static int claim(CommandContext<CommandSourceStack> c, int radius) throws CommandSyntaxException {
		boolean[] first = new boolean[]{false};
		return act(c, (s, a) -> {
			first[0] = s.holderOf(a.id()) == null;
			return s.claim(a, null, around(a, radius));
		}, r -> (Component)(first[0] ? say("claimed_own", r.number(0)) : claimed(r)));
	}

	private static boolean ownLand(CommandContext<CommandSourceStack> c) {
		TeamService s = service();
		return s != null && s.personalOf(actor((CommandSourceStack)c.getSource()).id()) != null;
	}

	private static boolean gaveUp(OpResult r) {
		return r.args().size() > 1;
	}

	private static int hint(CommandSourceStack src) {
		TeamService s = service();
		TeamData own = s == null ? null : s.personalOf(actor(src).id());
		if (own != null) {
			int held = s.claims.count(own.id);
			int limit = s.claimLimit(own);
			src.sendSuccess(() -> say("hint_solo", held, limit), false);
			return 1;
		} else {
			src.sendSuccess(() -> say("hint"), false);
			return 1;
		}
	}

	private static Component rank(String key) {
		return Msg.tr("cfhgui_teams.rank." + key);
	}

	private static Component teamName(String name) {
		TeamService s = service();
		TeamData t = s == null ? null : s.store.named(name);
		return t == null ? Component.literal(name) : Msg.team(t);
	}

	private static int ask(CommandContext<CommandSourceStack> c, @Nullable Permission needs, String what) {
		CommandSourceStack src = (CommandSourceStack)c.getSource();
		TeamService s = service();
		UUID me = actor(src).id();
		TeamData t = s == null ? null : s.holderOf(me);
		boolean own = t != null && t.solo;
		if (t != null && (!own || !what.equals("transfer"))) {
			Rank rank = t.rankOf(me);
			if (needs != null ? needs.allows(rank) : rank != null && rank.atLeast(s.pins().claimRank(t))) {
				String command = "/" + c.getInput().trim() + " confirm";
				MutableComponent go = Component.literal(command)
					.withStyle(
						Style.EMPTY
							.withColor(ChatFormatting.YELLOW)
							.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, command))
							.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
					);
				int held = s.claims.count(t.id);
				src.sendSuccess(() -> own ? say("give_up_land_ask", held, go) : say(what + "_ask", Msg.team(t), go), false);
				return 1;
			} else {
				src.sendFailure(OpResult.fail("no_permission").message());
				return 0;
			}
		} else {
			src.sendFailure(OpResult.fail("not_in_team").message());
			return 0;
		}
	}

	@Nullable
	private static UUID teamId(TeamService s, String name) {
		TeamData t = find(s, name);
		return t == null ? null : t.id;
	}

	@Nullable
	private static TeamData find(TeamService s, String name) {
		TeamData byName = s.store.named(name);
		return byName != null ? byName : s.store.tagged(name.trim());
	}

	@Nullable
	private static UUID memberId(TeamService s, Actor a, String name) {
		TeamData t = s.teamOf(a.id());
		if (t == null) {
			return null;
		} else {
			for (TeamData.Member m : t.members) {
				if (m.name.equalsIgnoreCase(name)) {
					return m.id;
				}
			}

			return null;
		}
	}

	@Nullable
	private static UUID invitedId(TeamService s, Actor a, String name) {
		TeamData t = s.teamOf(a.id());
		if (t == null) {
			return null;
		} else {
			for (TeamData.Invite i : t.invites) {
				if (i.playerName.equalsIgnoreCase(name)) {
					return i.player;
				}
			}

			return null;
		}
	}

	@Nullable
	private static UUID askingId(TeamService s, Actor a, String name) {
		TeamData t = s.teamOf(a.id());
		if (t == null) {
			return null;
		} else {
			for (TeamData.Request r : t.requests) {
				if (r.playerName.equalsIgnoreCase(name)) {
					return r.player;
				}
			}

			return null;
		}
	}

	private static long[] around(Actor a, int radius) {
		List<long[]> cells = new ArrayList<>();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				cells.add(new long[]{ChunkPos.asLong(a.chunkX() + dx, a.chunkZ() + dz), (long)Math.max(Math.abs(dx), Math.abs(dz))});
			}
		}

		cells.sort((x, y) -> Long.compare(x[1], y[1]));
		long[] out = new long[cells.size()];

		for (int i = 0; i < out.length; i++) {
			out[i] = cells.get(i)[0];
		}

		return out;
	}

	@Nullable
	static Integer color(String raw) {
		String s = raw.trim().toLowerCase(Locale.ROOT);
		String hex = s.startsWith("#") ? s.substring(1) : s;
		if (hex.length() == 6) {
			try {
				return Integer.parseInt(hex, 16) & 16777215;
			} catch (NumberFormatException var5) {
			}
		}

		ChatFormatting f = ChatFormatting.getByName(s);
		if (f != null && f.isColor() && f.getColor() != null) {
			return f.getColor();
		} else {
			DyeColor dye = DyeColor.byName(s, null);
			return dye == null ? null : dye.getTextureDiffuseColor() & 16777215;
		}
	}

	private static int answer(CommandContext<CommandSourceStack> c, @Nullable String name, boolean accept) throws CommandSyntaxException {
		return act(c, (s, a) -> {
			UUID id = name == null ? null : teamId(s, name);
			if (name == null) {
				List<InviteView> open = s.invitesOf(a.id());
				if (open.size() > 1) {
					return OpResult.fail("several_invites");
				}

				if (open.size() == 1) {
					id = open.get(0).team;
				}
			}

			if (id == null) {
				return OpResult.fail(name == null ? "not_invited" : "no_such_team");
			} else {
				return accept ? s.acceptInvite(a, id) : s.declineInvite(a, id);
			}
		}, r -> accept ? say("joined", teamName(r.args().get(1))) : say("declined", r.args().get(0)));
	}

	private static int join(CommandContext<CommandSourceStack> c, @Nullable CommandDispatcher<CommandSourceStack> game) throws CommandSyntaxException {
		CommandSourceStack src = (CommandSourceStack)c.getSource();
		String raw = text(c, "team");
		if (game != null && src.hasPermission(2)) {
			String first = raw.trim().split(" ", 2)[0];
			boolean theirs = src.getServer().getScoreboard().getPlayerTeam(first) != null;
			if (theirs || !(src.getEntity() instanceof ServerPlayer)) {
				return game.execute("team join " + raw, src);
			}
		}

		return act(
			c,
			(s, a) -> s.join(a, teamId(s, raw)),
			r -> r.args().get(0).equals("requested") ? say("requested", teamName(r.args().get(1))) : say("joined", teamName(r.args().get(1)))
		);
	}

	private static int forceLoad(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
		return act(c, (s, a) -> {
			OpResult r = s.forceLoad(a, null, around(a, 0), on);
			return r.ok() && r.number(0) == 0 ? OpResult.fail(on ? "already_loaded" : "not_loaded") : r;
		}, r -> say(on ? "forceload_on" : "forceload_off"));
	}

	private static int list(CommandSourceStack src) {
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else {
			List<TeamData> teams = s.teams();
			if (teams.isEmpty()) {
				src.sendSuccess(() -> say("list_empty"), false);
				return 0;
			} else {
				teams.sort((x, y) -> y.members.size() != x.members.size() ? Integer.compare(y.members.size(), x.members.size()) : x.name.compareToIgnoreCase(y.name));
				src.sendSuccess(() -> say("list_header", teams.size()), false);
				int shown = Math.min(teams.size(), 40);

				for (int i = 0; i < shown; i++) {
					TeamData t = teams.get(i);
					MutableComponent name = Msg.team(t)
						.withStyle(
							st -> st.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/team info " + t.name))
									.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("/team info " + t.name)))
						);
					src.sendSuccess(
						() -> say("list_entry", name, t.tag.isEmpty() ? "" : "[" + t.tag + "] ", t.members.size(), t.level, Msg.tr("cfhgui_teams.privacy." + t.privacy.key())),
						false
					);
				}

				if (shown < teams.size()) {
					int more = teams.size() - shown;
					src.sendSuccess(() -> say("list_more", more), false);
				}

				return teams.size();
			}
		}
	}

	private static int info(CommandSourceStack src, @Nullable String name) {
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else {
			Actor a = actor(src);
			TeamData t = name == null ? s.teamOf(a.id()) : find(s, name);
			if (t == null && name == null && s.personalOf(a.id()) != null) {
				return hint(src);
			} else if (t == null) {
				src.sendFailure(OpResult.fail(name == null ? "not_in_team" : "no_such_team").message());
				return 0;
			} else {
				boolean own = t.isMember(a.id());
				List<Component> lines = new ArrayList<>();
				lines.add(say("info_head", Msg.team(t), t.tag.isEmpty() ? "" : " [" + t.tag + "]"));
				if (!t.motto.isEmpty()) {
					lines.add(Component.literal("  \"" + t.motto + "\"").withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY}));
				}

				lines.add(say("info_level", t.level, t.xp, TeamData.xpForLevel(t.level)));
				MutableComponent members = Component.empty();
				boolean first = true;

				for (Rank rank : new Rank[]{Rank.OWNER, Rank.ADMIN, Rank.MODERATOR, Rank.MEMBER}) {
					for (TeamData.Member m : t.members) {
						if (m.rank == rank) {
							if (!first) {
								members.append(", ");
							}

							first = false;
							boolean online = s.online(m.id) != null;
							members.append(Component.literal(m.name).withStyle(online ? ChatFormatting.WHITE : ChatFormatting.GRAY));
							if (rank != Rank.MEMBER) {
								members.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
									.append(rank(rank.key()).copy().withStyle(ChatFormatting.DARK_GRAY))
									.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
							}
						}
					}
				}

				lines.add(say("info_members", t.members.size(), s.cfg().maxMembers, members));
				lines.add(say("info_land", s.claims.count(t.id), s.claimLimit(t), s.claims.forced(t.id), s.forceLimit(t)));
				if (!t.allies.isEmpty()) {
					MutableComponent allies = Component.empty();
					boolean any = false;

					for (UUID id : t.allies) {
						TeamData ally = s.store.get(id);
						if (ally != null) {
							if (any) {
								allies.append(", ");
							}

							any = true;
							allies.append(Msg.team(ally));
							if (ally.isAlly(t.id)) {
								allies.append(Component.literal(" ↔").withStyle(ChatFormatting.DARK_GRAY));
							}
						}
					}

					if (any) {
						lines.add(say("info_allies", allies));
					}
				}

				lines.add(say("info_privacy", Msg.tr("cfhgui_teams.privacy." + t.privacy.key())));
				if (own && t.home != null) {
					lines.add(say("info_home", (int)Math.floor(t.home.x), (int)Math.floor(t.home.y), (int)Math.floor(t.home.z), t.home.dimension));
				}

				for (Component line : lines) {
					src.sendSuccess(() -> line, false);
				}

				return 1;
			}
		}
	}

	private static int requests(CommandSourceStack src) {
		TeamService s = service();
		TeamData t = s == null ? null : s.teamOf(actor(src).id());
		if (t == null) {
			src.sendFailure(OpResult.fail("not_in_team").message());
			return 0;
		} else if (t.requests.isEmpty() && t.invites.isEmpty()) {
			src.sendSuccess(() -> say("requests_none"), false);
			return 0;
		} else {
			for (TeamData.Request r : t.requests) {
				MutableComponent yes = button("cfhgui_teams.invite.accept", 6074742, "/team admit " + r.playerName);
				MutableComponent no = button("cfhgui_teams.invite.decline", 15026253, "/team deny " + r.playerName);
				src.sendSuccess(() -> say("requests_entry", r.playerName, yes, no), false);
			}

			for (TeamData.Invite i : t.invites) {
				src.sendSuccess(() -> say("requests_invited", i.playerName, i.byName), false);
			}

			return t.requests.size();
		}
	}

	private static MutableComponent button(String key, int color, String command) {
		return Msg.tr(key)
			.withStyle(
				Style.EMPTY
					.withColor(color)
					.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
					.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
			);
	}

	private static int waypoints(CommandSourceStack src) {
		TeamService s = service();
		TeamData t = s == null ? null : s.teamOf(actor(src).id());
		if (t == null) {
			src.sendFailure(OpResult.fail("not_in_team").message());
			return 0;
		} else if (t.waypoints.isEmpty()) {
			src.sendSuccess(() -> say("waypoints_none"), false);
			return 0;
		} else {
			for (TeamData.Waypoint w : t.waypoints) {
				src.sendSuccess(
					() -> say("waypoints_entry", Component.literal(w.name).withStyle(Style.EMPTY.withColor(w.color & 16777215)), w.x, w.y, w.z, w.dimension), false
				);
			}

			return t.waypoints.size();
		}
	}

	private static int rules(CommandSourceStack src) {
		TeamService s = service();
		TeamData t = s == null ? null : s.holderOf(actor(src).id());
		if (t == null) {
			src.sendFailure(OpResult.fail("not_in_team").message());
			return 0;
		} else {
			LandRules rules = s.pins().effective(t);

			for (String rule : LandPins.RULES) {
				String value = LandPins.get(rules, rule).toLowerCase(Locale.ROOT);
				src.sendSuccess(() -> say(s.pins().pins(rule) ? "rule_pinned" : "rule_entry", rule, value), false);
			}

			return LandPins.RULES.size();
		}
	}

	private static boolean otherScoreboardTeams(CommandSourceStack src) {
		for (PlayerTeam pt : src.getServer().getScoreboard().getPlayerTeams()) {
			if (!pt.getName().startsWith("cfhgui_")) {
				return true;
			}
		}

		return false;
	}

	private static int reload(CommandSourceStack src) {
		TeamsServer.reload();
		src.sendSuccess(() -> say("reloaded"), true);
		return 1;
	}

	private static int selfTest(CommandSourceStack src) {
		SelfTest.Outcome outcome = SelfTest.run(src.getServer(), src.getPlayer());
		Component line = say("selftest", outcome.passed(), outcome.failed());
		if (outcome.failed() == 0) {
			src.sendSuccess(() -> line, true);
		} else {
			src.sendFailure(line);
		}

		return outcome.failed() == 0 ? 1 : 0;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> admin() {
		LiteralArgumentBuilder<CommandSourceStack> admin = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("admin").requires(OPERATOR);
		admin.then(Commands.literal("disband").then(teamArgument().executes(c -> admin(c, (s, a) -> {
				TeamData t = find(s, text(c, "team"));
				if (t == null) {
					return OpResult.fail("no_such_team");
				} else {
					s.dissolve(t, a);
					return OpResult.ok(t.name);
				}
			}, r -> say("disbanded", r.args().get(0))))));
		admin.then(
			Commands.literal("add")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getOnlinePlayerNames(), b))
						.then(teamArgument().executes(c -> admin(c, (s, a) -> s.adminAdd(text(c, "player"), find(s, text(c, "team"))), r -> say("admitted", r.args().get(0)))))
				)
		);
		admin.then(
			Commands.literal("remove")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.executes(c -> admin(c, (s, a) -> s.adminRemove(text(c, "player"), a), r -> say("kicked", r.args().get(0))))
				)
		);
		admin.then(
			Commands.literal("owner")
				.then(
					Commands.argument("player", StringArgumentType.word())
						.executes(c -> admin(c, (s, a) -> s.adminOwner(text(c, "player"), a), r -> say("transferred", r.args().get(0))))
				)
		);
		admin.then(Commands.literal("claim").then(teamArgument().executes(c -> admin(c, (s, a) -> {
				TeamData t = find(s, text(c, "team"));
				return t == null ? OpResult.fail("no_such_team") : s.claimFor(t, a, null, around(a, 0));
			}, TeamCommands::claimed))));
		admin.then(
			((LiteralArgumentBuilder)Commands.literal("unclaim").executes(c -> admin(c, (s, a) -> s.adminUnclaim(a, around(a, 0)), r -> say("unclaimed", r.number(0)))))
				.then(
					Commands.argument("radius", IntegerArgumentType.integer(0, 15))
						.executes(c -> admin(c, (s, a) -> s.adminUnclaim(a, around(a, IntegerArgumentType.getInteger(c, "radius"))), r -> say("unclaimed", r.number(0))))
				)
		);
		admin.then(Commands.literal("unclaimall").then(teamArgument().executes(c -> admin(c, (s, a) -> {
				TeamData t = find(s, text(c, "team"));
				return t == null ? OpResult.fail("no_such_team") : s.adminUnclaimAll(t, a);
			}, r -> say("unclaimed", r.number(0))))));
		admin.then(
			Commands.literal("xp").then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000)).then(teamArgument().executes(c -> admin(c, (s, a) -> {
					TeamData t = find(s, text(c, "team"));
					if (t == null) {
						return OpResult.fail("no_such_team");
					} else {
						s.addXp(t, (long)IntegerArgumentType.getInteger(c, "amount"));
						return OpResult.ok(t.name, t.level);
					}
				}, r -> say("xp_given", r.args().get(0), r.args().get(1))))))
		);
		return admin;
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> teamArgument() {
		return Commands.argument("team", StringArgumentType.greedyString()).suggests(ALL_TEAMS);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> land() {
		LiteralArgumentBuilder<CommandSourceStack> land = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("land").requires(OPERATOR);
		land.executes(c -> landInfo((CommandSourceStack)c.getSource()));
		land.then(Commands.literal("info").executes(c -> landInfo((CommandSourceStack)c.getSource())));
		land.then(
			((LiteralArgumentBuilder)Commands.literal("claim").executes(c -> admin(c, (s, a) -> s.claimServerLand(a, null, around(a, 0)), TeamCommands::landClaimed)))
				.then(
					Commands.argument("radius", IntegerArgumentType.integer(0, 15))
						.executes(c -> admin(c, (s, a) -> s.claimServerLand(a, null, around(a, IntegerArgumentType.getInteger(c, "radius"))), TeamCommands::landClaimed))
				)
		);
		land.then(
			((LiteralArgumentBuilder)Commands.literal("unclaim")
					.executes(c -> admin(c, (s, a) -> s.unclaimServerLand(a, null, around(a, 0)), r -> say("land_unclaimed", r.number(0)))))
				.then(
					Commands.argument("radius", IntegerArgumentType.integer(0, 15))
						.executes(
							c -> admin(c, (s, a) -> s.unclaimServerLand(a, null, around(a, IntegerArgumentType.getInteger(c, "radius"))), r -> say("land_unclaimed", r.number(0)))
						)
				)
		);
		land.then(
			Commands.literal("name")
				.then(
					Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> admin(c, (s, a) -> s.nameServerLand(a, text(c, "name")), r -> say("land_named", landName())))
				)
		);

		for (String word : new String[]{"colour", "color"}) {
			land.then(
				Commands.literal(word)
					.then(
						Commands.argument("colour", StringArgumentType.greedyString())
							.suggests((c, b) -> SharedSuggestionProvider.suggest(COLORS, b))
							.executes(c -> admin(c, (s, a) -> {
									Integer rgb = color(text(c, "colour"));
									return rgb == null ? OpResult.fail("invalid") : s.colorServerLand(a, rgb);
								}, r -> say("land_color_set", landName())))
					)
			);
		}

		land.then(
			((LiteralArgumentBuilder)Commands.literal("rule").executes(c -> landInfo((CommandSourceStack)c.getSource())))
				.then(
					Commands.argument("rule", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(ServerLand.RULES, b))
						.then(
							Commands.argument("value", StringArgumentType.word())
								.suggests(LAND_RULE_VALUES)
								.executes(c -> admin(c, (s, a) -> s.ruleServerLand(a, text(c, "rule"), text(c, "value")), r -> say("land_rule_set", r.args().get(0), r.args().get(1))))
						)
				)
		);
		return land;
	}

	private static Component landClaimed(OpResult r) {
		return r.number(1) > 0 ? say("land_claimed_some", r.number(0), r.number(1)) : say("land_claimed", r.number(0));
	}

	private static Component landName() {
		TeamService s = service();
		return s == null ? Component.empty() : Msg.team(s.serverLand().data);
	}

	private static int landInfo(CommandSourceStack src) {
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else {
			int chunks = s.claims.count(TeamSummary.SERVER_LAND);
			List<Component> lines = new ArrayList<>();
			lines.add(say("land_info_head", landName(), chunks));
			if (chunks == 0) {
				lines.add(say("land_info_none"));
			}

			for (String rule : ServerLand.RULES) {
				lines.add(say("rule_entry", rule, s.serverLandRule(rule)));
			}

			if (src.getEntity() instanceof ServerPlayer p) {
				TeamData here = s.ownerOf(p.level().dimension(), p.chunkPosition().x, p.chunkPosition().z);
				lines.add(here == null ? say("land_here_free") : (s.isServerLand(here) ? say("land_here_server") : say("land_here_team", Msg.team(here))));
			}

			for (Component line : lines) {
				src.sendSuccess(() -> line, false);
			}

			return Math.max(1, chunks);
		}
	}

	private static LiteralArgumentBuilder<CommandSourceStack> expiry() {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("expiry").requires(OPERATOR))
				.executes(c -> expiryList((CommandSourceStack)c.getSource())))
			.then(Commands.literal("run").executes(c -> expiryRun((CommandSourceStack)c.getSource())));
	}

	private static int expiryList(CommandSourceStack src) {
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else {
			int days = s.cfg().claimExpiryDays;
			if (days <= 0) {
				src.sendSuccess(() -> say("expiry_off"), false);
				return 0;
			} else {
				List<Expiry.Standing> standings = s.expiry.standings();
				if (standings.isEmpty()) {
					src.sendSuccess(() -> say("expiry_no_land", days), false);
					return 0;
				} else {
					long now = s.now();
					src.sendSuccess(() -> say("expiry_header", days), false);
					int shown = Math.min(standings.size(), 40);

					for (int i = 0; i < shown; i++) {
						Expiry.Standing st = standings.get(i);
						Component line = st.online()
							? say("expiry_online", Msg.team(st.team()), st.chunks())
							: (
								st.due() <= now
									? say("expiry_due", Msg.team(st.team()), st.chunks(), span(now - st.lastSeen(), false))
									: say("expiry_entry", Msg.team(st.team()), st.chunks(), span(now - st.lastSeen(), false), span(st.due() - now, true))
							);
						src.sendSuccess(() -> line, false);
					}

					if (shown < standings.size()) {
						int more = standings.size() - shown;
						src.sendSuccess(() -> say("list_more", more), false);
					}

					return standings.size();
				}
			}
		}
	}

	private static int expiryRun(CommandSourceStack src) {
		TeamService s = service();
		if (s == null) {
			src.sendFailure(Msg.tr("cfhgui_teams.command.not_running"));
			return 0;
		} else if (s.cfg().claimExpiryDays <= 0) {
			src.sendSuccess(() -> say("expiry_off"), false);
			return 0;
		} else {
			List<TeamData> released = s.expiry.run();
			s.sync.flush();
			if (released.isEmpty()) {
				src.sendSuccess(() -> say("expiry_none"), true);
				return 0;
			} else {
				MutableComponent names = Component.empty();

				for (int i = 0; i < released.size(); i++) {
					if (i > 0) {
						names.append(", ");
					}

					names.append(Msg.team(released.get(i)));
				}

				src.sendSuccess(() -> say("expiry_released", released.size(), names), true);
				return released.size();
			}
		}
	}

	private static Component span(long ms, boolean up) {
		long day = 1440L;
		long left = Math.max(0L, ms);
		long minutes = up ? (left + 59999L) / 60000L : left / 60000L;
		if (minutes < 1L) {
			return say("span_moment");
		} else if (minutes >= 2880L) {
			return say("span_days", up ? (minutes + 1440L - 1L) / 1440L : minutes / 1440L);
		} else {
			return minutes >= 120L ? say("span_hours", up ? (minutes + 59L) / 60L : minutes / 60L) : say("span_minutes", minutes);
		}
	}

	private static int admin(CommandContext<CommandSourceStack> c, TeamCommands.Op op, Function<OpResult, Component> said) throws CommandSyntaxException {
		return act(c, (s, a) -> op.run(s, a.asAdmin()), said);
	}

	private static SuggestionProvider<CommandSourceStack> names(Function<CommandContext<CommandSourceStack>, Iterable<String>> source) {
		return (c, b) -> {
			try {
				return SharedSuggestionProvider.suggest(source.apply(c), b);
			} catch (RuntimeException var4) {
				return b.buildFuture();
			}
		};
	}

	private static List<String> teams(CommandContext<CommandSourceStack> c, BiPredicate<TeamService, TeamData> which) {
		TeamService s = service();
		List<String> out = new ArrayList<>();
		if (s != null) {
			for (TeamData t : s.teams()) {
				if (which.test(s, t)) {
					out.add(t.name);
				}
			}
		}

		return out;
	}

	@Nullable
	private static TeamData own(CommandContext<CommandSourceStack> c) {
		TeamService s = service();
		return s == null ? null : s.teamOf(actor((CommandSourceStack)c.getSource()).id());
	}

	private static CommandNode<CommandSourceStack> operatorsOnly(CommandNode<CommandSourceStack> node) {
		ArgumentBuilder<CommandSourceStack, ?> copy = node.createBuilder();
		copy.requires(node.getRequirement().and(OPERATOR));

		for (CommandNode<CommandSourceStack> child : node.getChildren()) {
			copy.then(child);
		}

		return copy.build();
	}

	private static boolean detach(CommandNode<CommandSourceStack> root, String name) {
		try {
			List<Map<?, ?>> maps = new ArrayList<>(3);

			for (String map : new String[]{"children", "literals", "arguments"}) {
				Field f = CommandNode.class.getDeclaredField(map);
				f.setAccessible(true);
				maps.add((Map<?, ?>)f.get(root));
			}

			for (Map<?, ?> map : maps) {
				map.remove(name);
			}

			return root.getChild(name) == null;
		} catch (RuntimeException | ReflectiveOperationException var8) {
			return false;
		}
	}

	private TeamCommands() {
	}

	@FunctionalInterface
	private interface Op {
		OpResult run(TeamService var1, Actor var2) throws CommandSyntaxException;
	}
}
