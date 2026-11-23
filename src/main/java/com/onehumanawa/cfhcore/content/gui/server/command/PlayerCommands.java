package com.onehumanawa.cfhcore.content.gui.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Row;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Snapshot;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerCard;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.service.Actor;
import com.onehumanawa.cfhcore.content.gui.server.service.Msg;
import com.onehumanawa.cfhcore.content.gui.server.service.OpResult;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerService;
import com.onehumanawa.cfhcore.content.gui.server.service.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class PlayerCommands {
	static final Map<String, String> BOARDS = new LinkedHashMap<>();

	static void build(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		travel(out);
		requests(out);
		people(out);
		things(out);
	}

	private static void travel(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("home", Nodes.HOME_USE, false)
					.executes(
						c -> {
							ServerService service = Cmd.service();
							SelfState self = service == null ? null : service.self(Cmd.actor((CommandSourceStack)c.getSource()));
							return self != null && self.homes.size() > 1 && self.homes.stream().noneMatch(h -> h.name.equalsIgnoreCase("home"))
								? homes(c, null)
								: Cmd.act(c, (s, a) -> s.travel().homeGo(a, null, null), Cmd.journey(r -> Cmd.say("home_going", r.arg(0))));
						}
					))
				.then(
					((RequiredArgumentBuilder)Cmd.word("name")
							.suggests(Cmd.HOMES)
							.executes(c -> Cmd.act(c, (s, a) -> s.travel().homeGo(a, Cmd.text(c, "name"), null), Cmd.journey(r -> Cmd.say("home_going", r.arg(0))))))
						.then(((RequiredArgumentBuilder)Cmd.word("player").requires(Cmd.may(Nodes.HOME_OTHERS))).suggests(Cmd.KNOWN).executes(c -> Cmd.act(c, (s, a) -> {
								ServerService.Target owner = s.target(null, Cmd.text(c, "player"));
								return owner == null ? OpResult.fail("no_such_player", Cmd.text(c, "player")) : s.travel().homeGo(a, Cmd.text(c, "name"), owner);
							}, Cmd.journey(r -> Cmd.say("home_going", r.arg(0))))))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("sethome", Nodes.HOME_SET, false)
					.executes(c -> Cmd.act(c, (s, a) -> s.travel().homeSet(a, null, null, null), r -> Cmd.say("home_set", r.arg(0)))))
				.then(
					Cmd.word("name")
						.suggests(Cmd.HOMES)
						.executes(c -> Cmd.act(c, (s, a) -> s.travel().homeSet(a, Cmd.text(c, "name"), null, null), r -> Cmd.say("home_set", r.arg(0))))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("delhome", Nodes.HOME_SET, false).executes(c -> Cmd.usage(c, "delhome")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("name")
							.suggests(Cmd.HOMES)
							.executes(c -> Cmd.act(c, (s, a) -> s.travel().homeDelete(a, Cmd.text(c, "name"), null), r -> Cmd.say("home_deleted", r.arg(0)))))
						.then(((RequiredArgumentBuilder)Cmd.word("player").requires(Cmd.may(Nodes.HOME_OTHERS))).suggests(Cmd.KNOWN).executes(c -> Cmd.act(c, (s, a) -> {
								ServerService.Target owner = s.target(null, Cmd.text(c, "player"));
								return owner == null ? OpResult.fail("no_such_player", Cmd.text(c, "player")) : s.travel().homeDelete(a, Cmd.text(c, "name"), owner);
							}, r -> Cmd.say("home_deleted", r.arg(0)))))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("homes", Nodes.HOME_USE, false).executes(c -> homes(c, null)))
				.then(((RequiredArgumentBuilder)Cmd.word("player").requires(Cmd.may(Nodes.HOME_OTHERS))).suggests(Cmd.KNOWN).executes(c -> homes(c, Cmd.text(c, "player"))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("back", Nodes.BACK_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.travel().back(a), Cmd.journey(r -> Cmd.say("back_going"))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("spawn", Nodes.SPAWN_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.travel().spawnGo(a), Cmd.journey(r -> Cmd.say("spawn_going"))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("setspawn", Nodes.SPAWN_SET, true)
				.executes(c -> Cmd.act(c, (s, a) -> s.travel().spawnSet(a), r -> Cmd.say("spawn_set", r.arg(0))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("warp", Nodes.WARP_USE, false).executes(PlayerCommands::warps))
				.then(
					Cmd.word("name")
						.suggests(Cmd.WARPS)
						.executes(c -> Cmd.act(c, (s, a) -> s.travel().warpGo(a, Cmd.text(c, "name")), Cmd.journey(r -> Cmd.say("warp_going", r.arg(0)))))
				)
		);
		out.add((LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("warps", Nodes.WARP_USE, false).executes(PlayerCommands::warps));
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("setwarp", Nodes.WARP_SET, true).executes(c -> Cmd.usage(c, "setwarp")))
				.then(
					((RequiredArgumentBuilder)Cmd.word("name")
							.suggests(Cmd.WARPS)
							.executes(c -> Cmd.act(c, (s, a) -> s.travel().warpSet(a, Cmd.text(c, "name"), null, null, null, null), r -> Cmd.say("warp_set", r.arg(0)))))
						.then(
							Cmd.greedy("description")
								.executes(
									c -> Cmd.act(c, (s, a) -> s.travel().warpSet(a, Cmd.text(c, "name"), null, null, Cmd.text(c, "description"), null), r -> Cmd.say("warp_set", r.arg(0)))
								)
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("delwarp", Nodes.WARP_SET, true).executes(c -> Cmd.usage(c, "delwarp")))
				.then(
					Cmd.word("name")
						.suggests(Cmd.WARPS)
						.executes(c -> Cmd.act(c, (s, a) -> s.travel().warpDelete(a, Cmd.text(c, "name")), r -> Cmd.say("warp_deleted", r.arg(0))))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("rtp", Nodes.RTP_USE, false)
					.executes(c -> Cmd.act(c, (s, a) -> s.travel().random(a, null), Cmd.journey(r -> Cmd.say("rtp_searching")))))
				.then(Cmd.greedy("dimension").suggests((c, b) -> {
					ServerService s = Cmd.service();
					return SharedSuggestionProvider.suggest(s == null ? List.of() : s.rules().randomDimensions, b);
				}).executes(c -> Cmd.act(c, (s, a) -> s.travel().random(a, Cmd.text(c, "dimension")), Cmd.journey(r -> Cmd.say("rtp_searching")))))
		);
	}

	private static int homes(CommandContext<CommandSourceStack> c, @Nullable String player) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService service = Cmd.service();
		return player == null && service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "homes")
			? 1
			: Cmd.act(
				c,
				(s, a) -> {
					ServerService.Target owner = player == null ? null : s.target(null, player);
					if (player != null && owner == null) {
						return OpResult.fail("no_such_player", player);
					} else {
						OpResult r = s.travel().homes(a, owner);
						Home[] homes = r.data(Home[].class);
						if (homes == null) {
							return r;
						} else {
							boolean others = owner != null && !owner.id().equals(a.id());
							List<Component> lines = new ArrayList<>();
							if (homes.length == 0) {
								lines.add(others ? Cmd.say("homes_none_other", owner.name()) : Cmd.say("homes_none"));
							} else {
								lines.add(
									Cmd.head(
										others
											? Cmd.say("homes_header_other", owner.name(), homes.length)
											: Cmd.say("homes_header", homes.length, s.hello(a).number(Nodes.HOMES_MAX.id(), s.rules().maxHomes))
									)
								);

								for (Home h : homes) {
									lines.add(
										Component.literal(" ")
											.append(Cmd.link(h.name, others ? "/home " + h.name + " " + owner.name() : "/home " + h.name))
											.append("  ")
											.append(Cmd.place(h.place))
									);
								}
							}

							for (Component line : lines) {
								source.sendSuccess(() -> line, false);
							}

							return OpResult.OK;
						}
					}
				},
				null
			);
	}

	private static int warps(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService service = Cmd.service();
		return service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "warps") ? 1 : Cmd.act(c, (s, a) -> {
			if (!s.rules().warps) {
				return OpResult.fail("disabled");
			} else {
				WarpList list = s.warps(a);
				List<Component> lines = new ArrayList<>();
				if (list.warps.isEmpty()) {
					lines.add(Cmd.say("warps_none"));
				} else {
					lines.add(Cmd.head(Cmd.say("warps_header", list.warps.size())));

					for (Warp w : list.warps) {
						MutableComponent line = Component.literal(" ").append(Cmd.link(w.name, "/warp " + w.name));
						if (!w.description.isEmpty()) {
							line.append(Cmd.dim(Component.literal("  " + w.description)));
						}

						if (w.restricted) {
							line.append(" ").append(Cmd.dim(Cmd.say("warp_restricted")));
						}

						lines.add(line);
					}
				}

				for (Component linex : lines) {
					source.sendSuccess(() -> linex, false);
				}

				return OpResult.OK;
			}
		}, null);
	}

	private static void requests(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tpa", Nodes.TPA_USE, false).executes(c -> Cmd.usage(c, "tpa")))
				.then(
					Cmd.word("player")
						.suggests(Cmd.ONLINE)
						.executes(
							c -> Cmd.act(
									c,
									(s, a) -> s.requests().ask(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), false),
									r -> Cmd.say("tpa_sent", r.arg(0), r.arg(1)).append(" ").append(Cmd.link("[/tpcancel]", "/tpcancel"))
								)
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tpahere", Nodes.TPA_HERE, false).executes(c -> Cmd.usage(c, "tpahere")))
				.then(
					Cmd.word("player")
						.suggests(Cmd.ONLINE)
						.executes(
							c -> Cmd.act(
									c,
									(s, a) -> s.requests().ask(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), true),
									r -> Cmd.say("tpahere_sent", r.arg(0), r.arg(1)).append(" ").append(Cmd.link("[/tpcancel]", "/tpcancel"))
								)
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tpaccept", Nodes.TPA_USE, false)
					.executes(c -> Cmd.act(c, (s, a) -> s.requests().accept(a, null), Cmd.journey(r -> Cmd.say("tpa_accepted", r.arg(0))))))
				.then(Cmd.word("player").suggests(Cmd.ASKERS).executes(c -> Cmd.act(c, (s, a) -> {
						ServerService.Target from = s.target(null, Cmd.text(c, "player"));
						return from == null ? OpResult.fail("no_request") : s.requests().accept(a, from.id());
					}, Cmd.journey(r -> Cmd.say("tpa_accepted", r.arg(0))))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("tpdeny", Nodes.TPA_USE, false)
					.executes(c -> Cmd.act(c, (s, a) -> s.requests().deny(a, null), r -> Cmd.say("tpa_denied", r.arg(0)))))
				.then(Cmd.word("player").suggests(Cmd.ASKERS).executes(c -> Cmd.act(c, (s, a) -> {
						ServerService.Target from = s.target(null, Cmd.text(c, "player"));
						return from == null ? OpResult.fail("no_request") : s.requests().deny(a, from.id());
					}, r -> Cmd.say("tpa_denied", r.arg(0)))))
		);
		out.add((LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("tpcancel", null, false).executes(c -> Cmd.act(c, (s, a) -> {
				OpResult warmup = s.travel().cancel(a);
				OpResult asked = s.requests().cancel(a, null);
				return !warmup.ok() && !asked.ok() ? (asked.code().equals("no_request") ? warmup : asked) : OpResult.OK;
			}, r -> Cmd.say("tp_cancelled"))));
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("tptoggle", Nodes.TPA_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.requests().toggle(a, null), r -> Cmd.say(Boolean.parseBoolean(r.arg(0)) ? "tptoggle_on" : "tptoggle_off")))
		);
	}

	private static void people(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("afk", Nodes.AFK_USE, false)
				.executes(
					c -> Cmd.act(
							c, (s, a) -> s.people().afk(a, null), r -> Boolean.parseBoolean(r.arg(1)) ? null : Cmd.say(Boolean.parseBoolean(r.arg(0)) ? "afk_on" : "afk_off")
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("seen", Nodes.SEEN_USE, false).executes(c -> Cmd.usage(c, "seen")))
				.then(
					Cmd.word("player")
						.suggests(Cmd.KNOWN)
						.executes(
							c -> Cmd.act(c, (s, a) -> s.people().card(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player")), r -> seen(s(c), r.data(PlayerCard.class)))
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("playtime", Nodes.PLAYTIME_USE, false)
					.executes(c -> Cmd.act(c, (s, a) -> s.people().card(a, s.target(a.id(), null), a.name()), r -> playtime(r.data(PlayerCard.class)))))
				.then(
					Cmd.word("player")
						.suggests(Cmd.KNOWN)
						.executes(
							c -> Cmd.act(c, (s, a) -> s.people().card(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player")), r -> playtime(r.data(PlayerCard.class)))
						)
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("stats", null, false).executes(c -> stats(c, null)))
				.then(
					((RequiredArgumentBuilder)Cmd.word("player").requires(Cmd.may(Nodes.STATS_OTHERS))).suggests(Cmd.KNOWN).executes(c -> stats(c, Cmd.text(c, "player")))
				)
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("top", Nodes.RANKINGS_USE, false).executes(c -> top(c, null)))
				.then(Cmd.greedy("statistic").suggests((c, b) -> SharedSuggestionProvider.suggest(BOARDS.keySet(), b)).executes(c -> top(c, Cmd.text(c, "statistic"))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Cmd.command(
								"mail", Nodes.MAIL_USE, false
							)
							.executes(c -> mail(c, false)))
						.then(Cmd.alias("read", null).executes(c -> mail(c, true))))
					.then(
						((LiteralArgumentBuilder)Cmd.alias("send", null).executes(c -> Cmd.usage(c, "mail")))
							.then(
								((RequiredArgumentBuilder)Cmd.word("player").suggests(Cmd.KNOWN).executes(c -> Cmd.usage(c, "mail")))
									.then(
										Cmd.greedy("text")
											.executes(
												c -> Cmd.act(
														c,
														(s, a) -> s.people().mailSend(a, s.target(null, Cmd.text(c, "player")), Cmd.text(c, "player"), Cmd.text(c, "text")),
														r -> Cmd.say("mail_sent", r.arg(0))
													)
											)
									)
							)
					))
				.then(Cmd.alias("clear", null).executes(c -> Cmd.act(c, (s, a) -> s.people().mailDelete(a, null), r -> Cmd.say("mail_cleared", r.arg(0)))))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Cmd.command("nick", Nodes.NICK_USE, false)
						.executes(c -> Cmd.act(c, (s, a) -> {
								SelfState self = s.self(a);
								if (self == null) {
									return OpResult.fail("player_only");
								} else {
									((CommandSourceStack)c.getSource()).sendSuccess(() -> self.nick.isEmpty() ? Cmd.say("nick_none") : Cmd.say("nick_is", Text.styled(self.nick)), false);
									return OpResult.OK;
								}
							}, null)))
					.then(Cmd.alias("off", null).executes(c -> Cmd.act(c, (s, a) -> s.people().nick(a, ""), r -> Cmd.say("nick_off")))))
				.then(Cmd.greedy("name").executes(c -> Cmd.act(c, (s, a) -> s.people().nick(a, Cmd.text(c, "name")), r -> Cmd.say("nick_set", r.arg(0)))))
		);
		LiteralArgumentBuilder<CommandSourceStack> kit = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Cmd.command("kit", Nodes.KIT_USE, false)
				.executes(PlayerCommands::kits))
			.then(
				Cmd.word("kit").suggests(Cmd.KITS).executes(c -> Cmd.act(c, (s, a) -> s.people().kitClaim(a, Cmd.text(c, "kit")), r -> Cmd.say("kit_claimed", r.arg(0))))
			);
		StaffCommands.kitAdmin(kit);
		out.add(kit);
		out.add((LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("kits", Nodes.KIT_USE, false).executes(PlayerCommands::kits));
	}

	private static ServerService s(CommandContext<CommandSourceStack> c) {
		return Cmd.service();
	}

	private static Component seen(ServerService s, @Nullable PlayerCard card) {
		if (card == null) {
			return Cmd.say("nothing");
		} else {
			MutableComponent name = Text.styled(card.nick.isEmpty() ? card.name : card.nick);
			if (!card.nick.isEmpty()) {
				name.append(Cmd.dim(Component.literal(" (" + card.name + ")")));
			}

			long now = s.now();
			MutableComponent line = card.online
				? (
					card.afk
						? Cmd.say("seen_online_afk", name, Text.span((now - card.lastSeen) / 1000L))
						: Cmd.say("seen_online", name, Text.span((now - card.lastSeen) / 1000L))
				)
				: Cmd.say("seen_offline", name, Text.span(Math.max(0L, now - card.lastSeen) / 1000L), Text.date(card.lastSeen));
			if (!card.roles.isEmpty()) {
				line.append(Cmd.dim(Component.literal("  [")));

				for (int i = 0; i < card.roles.size(); i++) {
					line.append(i == 0 ? "" : ", ").append(Cmd.role(card.roles.get(i)));
				}

				line.append(Cmd.dim(Component.literal("]")));
			}

			return line;
		}
	}

	private static Component playtime(@Nullable PlayerCard card) {
		if (card == null) {
			return Cmd.say("nothing");
		} else {
			String name = Text.plainNick(card.nick.isEmpty() ? card.name : card.nick);
			long seconds = card.playTicks / 20L;
			long idle = card.afkTicks / 20L;
			return idle > 0L
				? Cmd.say("playtime_idle", name, Text.span(seconds), Text.span(idle), card.sessions)
				: Cmd.say("playtime", name, Text.span(seconds), card.sessions);
		}
	}

	private static int stats(CommandContext<CommandSourceStack> c, @Nullable String player) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		return Cmd.act(
			c,
			(s, a) -> {
				ServerService.Target who = player == null ? s.target(a.id(), null) : s.target(null, player);
				OpResult r = s.statistics().stats(a, who, player == null ? a.name() : player);
				Snapshot snap = r.data(Snapshot.class);
				if (snap == null) {
					return r;
				} else {
					if (source.getEntity() instanceof ServerPlayer p && s.open(p, "stats", player == null ? "" : snap.player().toString())) {
						return OpResult.OK;
					}

					List<Component> lines = new ArrayList<>();
					lines.add(Cmd.head(Cmd.say("stats_header", snap.name())));

					for (Entry<String, String> e : BOARDS.entrySet()) {
						String key = e.getValue();
						long value = snap.stats().containsKey(key) ? snap.stat(key) : snap.extra(key);
						if (value > 0L) {
							lines.add(
								Component.literal(" ")
									.append(Msg.tr("cfhgui_server.board." + e.getKey()))
									.append(Cmd.dim(Component.literal(": ")))
									.append(Component.literal(amount(key, value)))
							);
						}
					}

					if (lines.size() == 1) {
						lines.add(Cmd.dim(Cmd.say("stats_none")));
					}

					for (Component line : lines) {
						source.sendSuccess(() -> line, false);
					}

					return OpResult.OK;
				}
			},
			null
		);
	}

	private static int top(CommandContext<CommandSourceStack> c, @Nullable String wanted) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService service = Cmd.service();
		return wanted == null && service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "rankings") ? 1 : Cmd.act(c, (s, a) -> {
			String word = wanted == null ? "playtime" : wanted.trim().toLowerCase(Locale.ROOT);
			String key = BOARDS.getOrDefault(word, wanted == null ? word : wanted.trim());
			OpResult r = s.statistics().board(a, key, 0, 10);
			Board board = r.data(Board.class);
			if (board == null) {
				return r;
			} else {
				List<Component> lines = new ArrayList<>();
				Component title = BOARDS.containsKey(word) ? Msg.tr("cfhgui_server.board." + word) : Component.literal(key);
				lines.add(Cmd.head(Cmd.say("top_header", title, board.total())));
				if (board.rows().isEmpty()) {
					lines.add(Cmd.dim(Cmd.say("top_none")));
				}

				for (Row row : board.rows()) {
					lines.add(rank(key, row, false));
				}

				Row self = board.self();
				if (self != null && self.rank() > board.rows().size()) {
					lines.add(rank(key, self, true));
				}

				if (wanted == null) {
					lines.add(Cmd.dim(Cmd.say("top_hint", String.join(", ", BOARDS.keySet()))));
				}

				for (Component line : lines) {
					source.sendSuccess(() -> line, false);
				}

				return OpResult.OK;
			}
		}, null);
	}

	private static Component rank(String key, Row row, boolean own) {
		MutableComponent line = Component.literal(" " + row.rank() + ". ")
			.withStyle(style -> style.withColor(11184810))
			.append(Component.literal(row.name()).withStyle(style -> style.withColor(own ? 5621992 : 16777215)))
			.append(Component.literal("  " + amount(key, row.value())).withStyle(style -> style.withColor(14540253)));
		return own ? line.append(" ").append(Cmd.dim(Cmd.say("top_you"))) : line;
	}

	static String amount(String key, long value) {
		if (key.equals("first_seen_ms") || key.equals("last_seen_ms")) {
			return Text.date(value);
		} else if (key.equals("afk_ticks") || key.endsWith("_time") || key.contains("time_since") || key.endsWith(":play_time")) {
			return Text.span(value / 20L);
		} else if (key.endsWith("_one_cm")) {
			return value >= 100000L ? String.format(Locale.ROOT, "%.1f km", (double)value / 100000.0) : value / 100L + " m";
		} else {
			return key.contains("damage_") ? String.format(Locale.ROOT, "%.1f", (double)value / 10.0) : String.format(Locale.ROOT, "%,d", value);
		}
	}

	private static int mail(CommandContext<CommandSourceStack> c, boolean all) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService service = Cmd.service();
		return !all && service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "mail")
			? 1
			: Cmd.act(
				c,
				(s, a) -> {
					if (!s.rules().mail) {
						return OpResult.fail("disabled");
					} else if (!s.present(a)) {
						return OpResult.fail("player_only");
					} else {
						PlayerRecord.Mail[] letters = s.mail(a);
						List<Component> lines = new ArrayList<>();
						if (letters.length == 0) {
							lines.add(Cmd.say("mail_none"));
						} else {
							int unread = 0;

							for (PlayerRecord.Mail m : letters) {
								if (!m.read) {
									unread++;
								}
							}

							lines.add(Cmd.head(Cmd.say("mail_header", letters.length, unread)));
							int shown = all ? letters.length : Math.min(letters.length, 8);

							for (int i = 0; i < shown; i++) {
								PlayerRecord.Mail mx = letters[i];
								lines.add(
									Component.literal(mx.read ? " " : " * ")
										.withStyle(style -> style.withColor(5621992))
										.append(Component.literal(mx.fromName).withStyle(style -> style.withColor(16777215)))
										.append(Cmd.dim(Component.literal(" " + Text.date(mx.at) + ": ")))
										.append(Component.literal(mx.text).withStyle(style -> style.withColor(14540253)))
								);
							}

							if (shown < letters.length) {
								lines.add(Cmd.dim(Cmd.say("mail_more", letters.length - shown)).append(" ").append(Cmd.link("[/mail read]", "/mail read")));
							}

							lines.add(Cmd.dim(Cmd.say("mail_hint")));
						}

						for (Component line : lines) {
							source.sendSuccess(() -> line, false);
						}

						return letters.length == 0 ? OpResult.OK : s.people().mailRead(a, null);
					}
				},
				null
			);
	}

	private static int kits(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService service = Cmd.service();
		return service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "kits")
			? 1
			: Cmd.act(
				c,
				(s, a) -> {
					if (!s.rules().kits) {
						return OpResult.fail("disabled");
					} else {
						SelfState self = s.self(a);
						if (self == null) {
							return OpResult.fail("player_only");
						} else {
							List<Component> lines = new ArrayList<>();
							if (self.kits.isEmpty()) {
								lines.add(Cmd.say("kits_none"));
							} else {
								long now = s.now();
								lines.add(Cmd.head(Cmd.say("kits_header", self.kits.size())));

								for (Kit.View k : self.kits) {
									MutableComponent line = Component.literal(" ").append(Cmd.link(k.id, "/kit " + k.id));
									if (!k.name.equals(k.id)) {
										line.append(Cmd.dim(Component.literal("  " + k.name)));
									}

									line.append("  ")
										.append(
											k.readyAt < 0L
												? Cmd.dim(Cmd.say("kit_taken"))
												: (
													k.readyAt > now ? Cmd.dim(Cmd.say("kit_ready_in", Text.until(k.readyAt, now))) : Cmd.say("kit_ready").withStyle(style -> style.withColor(6074742))
												)
										);
									lines.add(line);
								}
							}

							for (Component line : lines) {
								source.sendSuccess(() -> line, false);
							}

							return OpResult.OK;
						}
					}
				},
				null
			);
	}

	private static void things(List<LiteralArgumentBuilder<CommandSourceStack>> out) {
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("trash", Nodes.TRASH_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.people().open(a, "trash"), null))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("enderchest", Nodes.ENDERCHEST_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.people().open(a, "enderchest"), null))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.alias("ec", Nodes.ENDERCHEST_USE)
				.executes(c -> Cmd.act(c, (s, a) -> s.people().open(a, "enderchest"), null))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("workbench", Nodes.WORKBENCH_USE, false)
				.executes(c -> Cmd.act(c, (s, a) -> s.people().open(a, "workbench"), null))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.alias("craft", Nodes.WORKBENCH_USE)
				.executes(c -> Cmd.act(c, (s, a) -> s.people().open(a, "workbench"), null))
		);
		out.add(
			(LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("rules", null, false)
				.executes(
					c -> {
						CommandSourceStack source = (CommandSourceStack)c.getSource();
						ServerService service = Cmd.service();
						return service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "rules", "")
							? 1
							: Cmd.act(
								c,
								(s, a) -> {
									ServerRules rules = s.rules();
									List<Component> lines = new ArrayList<>();
									if (rules.rules.isEmpty()) {
										lines.add(Cmd.say("rules_none"));
									} else {
										lines.add(Cmd.head(Cmd.say("rules_header")));
										int n = 0;

										for (String rule : rules.rules) {
											if (rule != null && !rule.isBlank()) {
												lines.add(
													Component.literal(" " + ++n + ". ")
														.withStyle(style -> style.withColor(11184810))
														.append(Text.styled(rule).withStyle(style -> style.withColor(16777215)))
												);
											}
										}

										for (ServerRules.Link link : rules.links) {
											lines.add(
												Component.literal(" ")
													.append(
														Component.literal(link.name)
															.withStyle(
																style -> style.withColor(5621992)
																		.withUnderlined(true)
																		.withClickEvent(new ClickEvent(Action.OPEN_URL, link.url))
																		.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(link.url)))
															)
													)
											);
										}
									}

									for (Component line : lines) {
										source.sendSuccess(() -> line, false);
									}

									return OpResult.OK;
								},
								null
							);
					}
				)
		);
		out.add((LiteralArgumentBuilder<CommandSourceStack>)Cmd.command("menu", Nodes.HUB_USE, false).executes(c -> {
			CommandSourceStack source = (CommandSourceStack)c.getSource();
			ServerService service = Cmd.service();
			return service != null && source.getEntity() instanceof ServerPlayer p && service.open(p, "hub", "") ? 1 : ServerCommands.help(source, false, 1);
		}));
	}

	static Component roles(Actor viewer, PlayerCard card) {
		MutableComponent line = Cmd.say("role_player", Text.plainNick(card.nick.isEmpty() ? card.name : card.nick));
		if (card.roles.isEmpty()) {
			return line.append(" ").append(Cmd.dim(Cmd.say("role_player_none")));
		} else {
			for (int i = 0; i < card.roles.size(); i++) {
				Role.Tag tag = card.roles.get(i);
				line.append(i == 0 ? " " : ", ").append(Cmd.role(tag));
			}

			return line;
		}
	}

	private PlayerCommands() {
	}

	static {
		BOARDS.put("playtime", StatsLink.key("minecraft:custom", "minecraft:play_time"));
		BOARDS.put("deaths", StatsLink.key("minecraft:custom", "minecraft:deaths"));
		BOARDS.put("mob_kills", StatsLink.key("minecraft:custom", "minecraft:mob_kills"));
		BOARDS.put("player_kills", StatsLink.key("minecraft:custom", "minecraft:player_kills"));
		BOARDS.put("walked", StatsLink.key("minecraft:custom", "minecraft:walk_one_cm"));
		BOARDS.put("flown", StatsLink.key("minecraft:custom", "minecraft:aviate_one_cm"));
		BOARDS.put("jumps", StatsLink.key("minecraft:custom", "minecraft:jump"));
		BOARDS.put("damage_dealt", StatsLink.key("minecraft:custom", "minecraft:damage_dealt"));
		BOARDS.put("fish", StatsLink.key("minecraft:custom", "minecraft:fish_caught"));
		BOARDS.put("trades", StatsLink.key("minecraft:custom", "minecraft:traded_with_villager"));
		BOARDS.put("idle", "afk_ticks");
		BOARDS.put("visits", "sessions");
	}
}
