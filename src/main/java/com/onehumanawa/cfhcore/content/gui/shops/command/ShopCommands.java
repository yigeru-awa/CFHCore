package com.onehumanawa.cfhcore.content.gui.shops.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.shops.ShopNodes;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import com.onehumanawa.cfhcore.content.gui.shops.service.Actor;
import com.onehumanawa.cfhcore.content.gui.shops.service.Msg;
import com.onehumanawa.cfhcore.content.gui.shops.service.OpResult;
import com.onehumanawa.cfhcore.content.gui.shops.service.SelfTest;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopService;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopsRuntime;
import com.onehumanawa.cfhcore.content.gui.shops.service.Stacks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ShopCommands {
	private static final int ACCENT = 14722136;
	private static final SuggestionProvider<CommandSourceStack> SHOPS = (c, b) -> {
		ShopService s = ShopsRuntime.service();
		if (s == null) {
			return b.buildFuture();
		} else {
			List<String> ids = new ArrayList<>();

			for (Shop shop : s.store().shops()) {
				ids.add(shop.id);
			}

			return SharedSuggestionProvider.suggest(ids, b);
		}
	};
	private static final SuggestionProvider<CommandSourceStack> OFFERS = (c, b) -> {
		ShopService s = ShopsRuntime.service();
		Shop shop = s == null ? null : s.store().shop(StringArgumentType.getString(c, "shop"));
		if (shop == null) {
			return b.buildFuture();
		} else {
			List<String> ids = new ArrayList<>();

			for (Offer o : shop.offers) {
				ids.add(Integer.toString(o.id));
			}

			return SharedSuggestionProvider.suggest(ids, b);
		}
	};
	private static final SuggestionProvider<CommandSourceStack> OWN_OFFERS = (c, b) -> {
		ShopService s = ShopsRuntime.service();
		ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayer();
		Shop shop = s != null && p != null ? s.store().shopOf(p.getUUID()) : null;
		if (shop == null) {
			return b.buildFuture();
		} else {
			List<String> ids = new ArrayList<>();

			for (Offer o : shop.offers) {
				ids.add(Integer.toString(o.id));
			}

			return SharedSuggestionProvider.suggest(ids, b);
		}
	};
	private static final SuggestionProvider<CommandSourceStack> PLAYERS = (c, b) -> {
		List<String> names = new ArrayList<>(((CommandSourceStack)c.getSource()).getOnlinePlayerNames());
		ShopService s = ShopsRuntime.service();
		if (s != null) {
			for (Wallet w : s.store().wallets().values()) {
				if (names.size() > 200) {
					break;
				}

				if (!w.name.isEmpty() && names.stream().noneMatch(n -> n.equalsIgnoreCase(w.name))) {
					names.add(w.name);
				}
			}
		}

		names.sort(String.CASE_INSENSITIVE_ORDER);
		return SharedSuggestionProvider.suggest(names, b);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> money = moneyTree("money");
		LiteralArgumentBuilder<CommandSourceStack> pay = payTree("pay");
		LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
																				"shops"
																			)
																			.requires(may(ShopNodes.USE)))
																		.executes(c -> browse((CommandSourceStack)c.getSource())))
																	.then(
																		Commands.literal("view")
																			.then(
																				Commands.argument("shop", StringArgumentType.word())
																					.suggests(SHOPS)
																					.executes(c -> view((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "shop")))
																			)
																	))
																.then(Commands.literal("buy").then(trade(true))))
															.then(Commands.literal("sell").then(trade(false))))
														.then(
															((LiteralArgumentBuilder)Commands.literal("create").requires(may(ShopNodes.CREATE)))
																.then(
																	Commands.argument("name", StringArgumentType.greedyString())
																		.executes(c -> act(c, "create", (s, a) -> s.create(a, StringArgumentType.getString(c, "name"))))
																)
														))
													.then(
														((LiteralArgumentBuilder)Commands.literal("rename").requires(may(ShopNodes.CREATE)))
															.then(
																Commands.argument("name", StringArgumentType.greedyString())
																	.executes(c -> own(c, "edit", (s, a, shop) -> s.edit(a, shop.id, StringArgumentType.getString(c, "name"), null, null)))
															)
													))
												.then(
													((LiteralArgumentBuilder)Commands.literal("describe").requires(may(ShopNodes.CREATE)))
														.then(
															Commands.argument("text", StringArgumentType.greedyString())
																.executes(c -> own(c, "edit", (s, a, shop) -> s.edit(a, shop.id, null, StringArgumentType.getString(c, "text"), null)))
														)
												))
											.then(
												((LiteralArgumentBuilder)Commands.literal("open").requires(may(ShopNodes.CREATE)))
													.executes(c -> own(c, "open", (s, a, shop) -> s.edit(a, shop.id, null, null, true)))
											))
										.then(
											((LiteralArgumentBuilder)Commands.literal("close").requires(may(ShopNodes.CREATE)))
												.executes(c -> own(c, "close", (s, a, shop) -> s.edit(a, shop.id, null, null, false)))
										))
									.then(
										((LiteralArgumentBuilder)Commands.literal("delete").requires(may(ShopNodes.CREATE)))
											.executes(c -> own(c, "delete", (s, a, shop) -> s.delete(a, shop.id)))
									))
								.then(
									((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("offer").requires(may(ShopNodes.CREATE)))
												.then(
													Commands.literal("add")
														.then(
															((RequiredArgumentBuilder)Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
																	.executes(c -> own(c, "offer_add", (s, a, shop) -> s.offerAdd(a, shop.id, held(c), LongArgumentType.getLong(c, "price"), 0L))))
																.then(
																	Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
																		.executes(
																			c -> own(
																					c,
																					"offer_add",
																					(s, a, shop) -> s.offerAdd(a, shop.id, held(c), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice"))
																				)
																		)
																)
														)
												))
											.then(
												Commands.literal("price")
													.then(
														Commands.argument("offer", IntegerArgumentType.integer(1))
															.suggests(OWN_OFFERS)
															.then(
																((RequiredArgumentBuilder)Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
																		.executes(
																			c -> own(
																					c,
																					"offer_edit",
																					(s, a, shop) -> s.offerEdit(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "price"), 0L)
																				)
																		))
																	.then(
																		Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
																			.executes(
																				c -> own(
																						c,
																						"offer_edit",
																						(s, a, shop) -> s.offerEdit(
																								a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice")
																							)
																					)
																			)
																	)
															)
													)
											))
										.then(
											Commands.literal("remove")
												.then(
													Commands.argument("offer", IntegerArgumentType.integer(1))
														.suggests(OWN_OFFERS)
														.executes(c -> own(c, "offer_remove", (s, a, shop) -> s.offerRemove(a, shop.id, IntegerArgumentType.getInteger(c, "offer"))))
												)
										)
								))
							.then(
								((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stock").requires(may(ShopNodes.CREATE)))
											.executes(c -> stock((CommandSourceStack)c.getSource())))
										.then(
											((LiteralArgumentBuilder)Commands.literal("add").executes(c -> own(c, "stock_add", (s, a, shop) -> s.stockAdd(a, shop.id, 0))))
												.then(
													Commands.argument("offer", IntegerArgumentType.integer(1))
														.suggests(OWN_OFFERS)
														.executes(c -> own(c, "stock_add", (s, a, shop) -> s.stockAdd(a, shop.id, IntegerArgumentType.getInteger(c, "offer"))))
												)
										))
									.then(
										Commands.literal("take")
											.then(
												((RequiredArgumentBuilder)Commands.argument("offer", IntegerArgumentType.integer(1))
														.suggests(OWN_OFFERS)
														.executes(c -> own(c, "stock_take", (s, a, shop) -> s.stockTake(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), 0L))))
													.then(
														Commands.argument("count", LongArgumentType.longArg(1L))
															.executes(
																c -> own(
																		c, "stock_take", (s, a, shop) -> s.stockTake(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "count"))
																	)
															)
													)
											)
									)
							))
						.then(
							((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
															"admin"
														)
														.requires(may(ShopNodes.ADMIN)))
													.executes(c -> admin((CommandSourceStack)c.getSource())))
												.then(
													Commands.literal("create")
														.then(
															Commands.argument("name", StringArgumentType.greedyString())
																.executes(c -> act(c, "create", (s, a) -> s.adminCreate(a, StringArgumentType.getString(c, "name"))))
														)
												))
											.then(
												Commands.literal("delete")
													.then(
														Commands.argument("shop", StringArgumentType.word())
															.suggests(SHOPS)
															.executes(c -> act(c, "delete", (s, a) -> s.delete(a, StringArgumentType.getString(c, "shop"))))
													)
											))
										.then(
											Commands.literal("open")
												.then(
													Commands.argument("shop", StringArgumentType.word())
														.suggests(SHOPS)
														.executes(c -> act(c, "open", (s, a) -> s.edit(a, StringArgumentType.getString(c, "shop"), null, null, true)))
												)
										))
									.then(
										Commands.literal("close")
											.then(
												Commands.argument("shop", StringArgumentType.word())
													.suggests(SHOPS)
													.executes(c -> act(c, "close", (s, a) -> s.edit(a, StringArgumentType.getString(c, "shop"), null, null, false)))
											)
									))
								.then(
									Commands.literal("offer")
										.then(
											((RequiredArgumentBuilder)Commands.argument("shop", StringArgumentType.word())
													.suggests(SHOPS)
													.then(
														Commands.literal("add")
															.then(
																((RequiredArgumentBuilder)Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
																		.executes(
																			c -> act(c, "offer_add", (s, a) -> s.offerAdd(a, StringArgumentType.getString(c, "shop"), held(c), LongArgumentType.getLong(c, "price"), 0L))
																		))
																	.then(
																		Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
																			.executes(
																				c -> act(
																						c,
																						"offer_add",
																						(s, a) -> s.offerAdd(
																								a, StringArgumentType.getString(c, "shop"), held(c), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice")
																							)
																					)
																			)
																	)
															)
													))
												.then(
													Commands.literal("remove")
														.then(
															Commands.argument("offer", IntegerArgumentType.integer(1))
																.executes(
																	c -> act(c, "offer_remove", (s, a) -> s.offerRemove(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer")))
																)
														)
												)
										)
								)
						))
					.then(((LiteralArgumentBuilder)Commands.literal("reload").requires(may(ShopNodes.ADMIN))).executes(c -> {
						OpResult r = ShopsRuntime.reload();
						return report((CommandSourceStack)c.getSource(), "reload", r);
					})))
				.then(((LiteralArgumentBuilder)Commands.literal("selftest").requires(may(ShopNodes.SELFTEST))).executes(c -> selfTest((CommandSourceStack)c.getSource()))))
			.then(Commands.literal("version").executes(c -> {
				String version = CFHGuiPlatform.get().modInfo("cfhcore").map(info -> info.version()).orElse("?");
				((CommandSourceStack)c.getSource()).sendSuccess(() -> Msg.tr("cfhgui_shops.command.version", version), false);
				return 1;
			}));
		List<String> taken = new ArrayList<>();
		LiteralCommandNode<CommandSourceStack> moneyNode = money.build();
		LiteralCommandNode<CommandSourceStack> payNode = pay.build();
		((LiteralArgumentBuilder)root.then(moneyNode)).then(payNode);
		dispatcher.register(root);

		for (LiteralCommandNode<CommandSourceStack> node : List.of(moneyNode, payNode)) {
			if (dispatcher.getRoot().getChild(node.getLiteral()) == null) {
				dispatcher.getRoot().addChild(node);
			} else {
				taken.add(node.getLiteral());
			}
		}

		if (dispatcher.getRoot().getChild("balance") == null) {
			dispatcher.register(
				(LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("balance").requires(may(ShopNodes.USE)))
					.executes(c -> balance((CommandSourceStack)c.getSource()))
			);
		} else {
			taken.add("balance");
		}

		if (!taken.isEmpty()) {
			CFHGuiShops.LOGGER.info("[CFHGui Shops] these words were taken by the game or another mod and are reached as /shops <word>: {}", String.join(", ", taken));
		}
	}

	private static LiteralArgumentBuilder<CommandSourceStack> moneyTree(String word) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
										word
									)
									.requires(may(ShopNodes.USE)))
								.executes(c -> balance((CommandSourceStack)c.getSource())))
							.then(
								((LiteralArgumentBuilder)Commands.literal("deposit").executes(c -> act(c, "deposit", (s, a) -> s.exchange(a, "deposit", 0L))))
									.then(
										Commands.argument("count", LongArgumentType.longArg(1L))
											.executes(c -> act(c, "deposit", (s, a) -> s.exchange(a, "deposit", LongArgumentType.getLong(c, "count"))))
									)
							))
						.then(
							Commands.literal("withdraw")
								.then(
									Commands.argument("count", LongArgumentType.longArg(1L))
										.executes(c -> act(c, "withdraw", (s, a) -> s.exchange(a, "withdraw", LongArgumentType.getLong(c, "count"))))
								)
						))
					.then(adminMoney("give")))
				.then(adminMoney("take")))
			.then(adminMoney("set"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> adminMoney(String what) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(what).requires(may(ShopNodes.MONEY)))
			.then(
				Commands.argument("player", StringArgumentType.word())
					.suggests(PLAYERS)
					.then(
						Commands.argument("amount", LongArgumentType.longArg(0L, 1000000000000L))
							.executes(c -> act(c, "money", (s, a) -> s.money(a, StringArgumentType.getString(c, "player"), what, LongArgumentType.getLong(c, "amount"))))
					)
			);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> payTree(String word) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(word).requires(may(ShopNodes.PAY)))
			.then(
				Commands.argument("player", StringArgumentType.word())
					.suggests(PLAYERS)
					.then(
						Commands.argument("amount", LongArgumentType.longArg(1L, 1000000000000L))
							.executes(c -> act(c, "pay", (s, a) -> s.pay(a, StringArgumentType.getString(c, "player"), LongArgumentType.getLong(c, "amount"))))
					)
			);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> trade(boolean buy) {
		String op = buy ? "buy" : "sell";
		return (RequiredArgumentBuilder<CommandSourceStack, String>)Commands.argument("shop", StringArgumentType.word())
			.suggests(SHOPS)
			.then(
				((RequiredArgumentBuilder)Commands.argument("offer", IntegerArgumentType.integer(1))
						.suggests(OFFERS)
						.executes(
							c -> act(
									c,
									op,
									(s, a) -> buy
											? s.buy(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer"), 1)
											: s.sell(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer"), 1)
								)
						))
					.then(
						Commands.argument("lots", IntegerArgumentType.integer(1, 64))
							.executes(
								c -> act(
										c,
										op,
										(s, a) -> buy
												? s.buy(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer"), IntegerArgumentType.getInteger(c, "lots"))
												: s.sell(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer"), IntegerArgumentType.getInteger(c, "lots"))
									)
							)
					)
			);
	}

	private static Predicate<CommandSourceStack> may(Node node) {
		return source -> Perms.has(source, node);
	}

	private static int held(CommandContext<CommandSourceStack> c) {
		ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayer();
		return p == null ? -1 : p.getInventory().selected;
	}

	private static int act(CommandContext<CommandSourceStack> c, String op, BiFunction<ShopService, Actor, OpResult> action) {
		ShopService s = ShopsRuntime.service();
		return s == null
			? report((CommandSourceStack)c.getSource(), op, OpResult.fail("unavailable"))
			: report((CommandSourceStack)c.getSource(), op, action.apply(s, Actor.of((CommandSourceStack)c.getSource())));
	}

	private static int own(CommandContext<CommandSourceStack> c, String op, ShopCommands.OnOwn action) {
		ShopService s = ShopsRuntime.service();
		if (s == null) {
			return report((CommandSourceStack)c.getSource(), op, OpResult.fail("unavailable"));
		} else {
			Actor a = Actor.of((CommandSourceStack)c.getSource());
			if (a.id() == null) {
				return report((CommandSourceStack)c.getSource(), op, OpResult.fail("player_only"));
			} else {
				Shop own = s.store().shopOf(a.id());
				return own == null
					? report((CommandSourceStack)c.getSource(), op, OpResult.fail("no_shop"))
					: report((CommandSourceStack)c.getSource(), op, action.run(s, a, own));
			}
		}
	}

	private static int report(CommandSourceStack source, String op, OpResult r) {
		if (r.ok()) {
			source.sendSuccess(() -> r.message(op), false);
		} else {
			source.sendFailure(r.message(op));
		}

		return r.ok() ? 1 : 0;
	}

	private static int selfTest(CommandSourceStack source) {
		SelfTest.Outcome outcome = SelfTest.run(source.getServer());
		Component line = Msg.tr("cfhgui_shops.command.selftest", outcome.passed(), outcome.failed());
		if (outcome.failed() == 0) {
			source.sendSuccess(() -> line, true);
		} else {
			source.sendFailure(line);
		}

		return outcome.failed() == 0 ? 1 : 0;
	}

	private static boolean screen(CommandSourceStack source) {
		ServerPlayer p = source.getPlayer();
		return p != null && ShopsNet.has(p);
	}

	private static int browse(CommandSourceStack source) {
		if (screen(source)) {
			ShopsRuntime.open(source.getPlayer(), "browse", "");
			return 1;
		} else {
			ShopService s = ShopsRuntime.service();
			if (s == null) {
				return report(source, "list", OpResult.fail("unavailable"));
			} else {
				List<Shop> shops = new ArrayList<>(s.store().shops());
				shops.sort((x, y) -> x.server != y.server ? (x.server ? -1 : 1) : x.name.compareToIgnoreCase(y.name));
				source.sendSuccess(() -> head(Msg.tr("cfhgui_shops.command.list_header", shops.size())), false);
				if (shops.isEmpty()) {
					source.sendSuccess(() -> Msg.tr("cfhgui_shops.command.list_empty").withStyle(ChatFormatting.GRAY), false);
				}

				for (Shop shop : shops) {
					MutableComponent line = Component.literal(" ").append(link(Component.literal(shop.name), "/shops view " + shop.id));
					line.append(
						Component.literal(" ")
							.append(shop.server ? Msg.tr("cfhgui_shops.command.server_shop") : Msg.tr("cfhgui_shops.command.by", shop.ownerName))
							.withStyle(ChatFormatting.GRAY)
					);
					line.append(
						Component.literal(" · ")
							.append(Msg.tr(shop.offers.size() == 1 ? "cfhgui_shops.command.offers.one" : "cfhgui_shops.command.offers", shop.offers.size()))
							.withStyle(ChatFormatting.DARK_GRAY)
					);
					if (!shop.open) {
						line.append(Component.literal(" ").append(Msg.tr("cfhgui_shops.command.closed")).withStyle(ChatFormatting.RED));
					}

					source.sendSuccess(() -> line, false);
				}

				return shops.size();
			}
		}
	}

	private static int view(CommandSourceStack source, String id) {
		ShopService s = ShopsRuntime.service();
		if (s == null) {
			return report(source, "view", OpResult.fail("unavailable"));
		} else {
			Shop shop = s.store().shop(id);
			if (shop == null) {
				return report(source, "view", OpResult.fail("no_such_shop", id));
			} else if (screen(source)) {
				ShopsRuntime.open(source.getPlayer(), "shop", shop.id);
				return 1;
			} else {
				MutableComponent title = Component.literal(shop.name)
					.append(" ")
					.append(shop.server ? Msg.tr("cfhgui_shops.command.server_shop") : Msg.tr("cfhgui_shops.command.by", shop.ownerName));
				if (!shop.open) {
					title.append(" ").append(Msg.tr("cfhgui_shops.command.closed").withStyle(ChatFormatting.RED));
				}

				source.sendSuccess(() -> head(title), false);
				if (!shop.description.isEmpty()) {
					source.sendSuccess(() -> Component.literal(shop.description).withStyle(ChatFormatting.GRAY), false);
				}

				if (shop.offers.isEmpty()) {
					source.sendSuccess(() -> Msg.tr("cfhgui_shops.command.no_offers").withStyle(ChatFormatting.GRAY), false);
				}

				for (Offer o : shop.offers) {
					ItemStack like = s.template(o);
					MutableComponent line = Component.literal(" #" + o.id + " ")
						.withStyle(ChatFormatting.DARK_GRAY)
						.append(like.isEmpty() ? Component.literal("?") : Stacks.named(like, (long)like.getCount()).withStyle(ChatFormatting.WHITE));
					if (o.sells()) {
						line.append(" ").append(link(Msg.tr("cfhgui_shops.command.buy_for", s.rules().format(o.price)), "/shops buy " + shop.id + " " + o.id));
					}

					if (o.buys()) {
						line.append(" ").append(link(Msg.tr("cfhgui_shops.command.sell_for", s.rules().format(o.sellPrice)), "/shops sell " + shop.id + " " + o.id));
					}

					if (!shop.server) {
						line.append(Component.literal(" ").append(Msg.tr("cfhgui_shops.command.in_stock", o.stock)).withStyle(ChatFormatting.GRAY));
					}

					source.sendSuccess(() -> line, false);
				}

				return 1;
			}
		}
	}

	private static int stock(CommandSourceStack source) {
		ShopService s = ShopsRuntime.service();
		if (s == null) {
			return report(source, "stock", OpResult.fail("unavailable"));
		} else {
			ServerPlayer p = source.getPlayer();
			if (p == null) {
				return report(source, "stock", OpResult.fail("player_only"));
			} else {
				Shop own = s.store().shopOf(p.getUUID());
				if (own == null) {
					return report(source, "stock", OpResult.fail("no_shop"));
				} else if (screen(source)) {
					ShopsRuntime.open(p, "mine", "");
					return 1;
				} else {
					source.sendSuccess(() -> head(Msg.tr("cfhgui_shops.command.stock_header", own.name, own.stockTotal())), false);

					for (Offer o : own.offers) {
						ItemStack like = s.template(o);
						MutableComponent line = Component.literal(" #" + o.id + " ")
							.withStyle(ChatFormatting.DARK_GRAY)
							.append(like.isEmpty() ? Component.literal("?") : like.getHoverName().copy().withStyle(ChatFormatting.WHITE))
							.append(Component.literal(" ").append(Msg.tr("cfhgui_shops.command.in_stock", o.stock)).withStyle(ChatFormatting.GRAY))
							.append(" ")
							.append(link(Msg.tr("cfhgui_shops.command.add"), "/shops stock add " + o.id));
						if (o.stock > 0L) {
							line.append(" ").append(link(Msg.tr("cfhgui_shops.command.take"), "/shops stock take " + o.id));
						}

						source.sendSuccess(() -> line, false);
					}

					return 1;
				}
			}
		}
	}

	private static int admin(CommandSourceStack source) {
		if (screen(source)) {
			ShopsRuntime.open(source.getPlayer(), "admin", "");
			return 1;
		} else {
			return browse(source);
		}
	}

	private static int balance(CommandSourceStack source) {
		ShopService s = ShopsRuntime.service();
		if (s == null) {
			return report(source, "balance", OpResult.fail("unavailable"));
		} else {
			ServerPlayer p = source.getPlayer();
			if (p == null) {
				return report(source, "balance", OpResult.fail("player_only"));
			} else if (screen(source)) {
				ShopsRuntime.open(p, "wallet", "");
				return 1;
			} else {
				Wallet w = s.store().wallet(p.getUUID());
				long balance = w == null ? 0L : w.balance;
				source.sendSuccess(() -> Msg.tr("cfhgui_shops.command.balance", s.rules().format(balance)), false);
				if (w != null) {
					for (int i = 0; i < Math.min(5, w.history.size()); i++) {
						Wallet.Line l = w.history.get(i);
						MutableComponent line = Component.literal(" " + (l.amount >= 0L ? "+" : "") + s.rules().format(l.amount) + " ")
							.withStyle(l.amount >= 0L ? ChatFormatting.GREEN : ChatFormatting.RED)
							.append(Msg.tr("cfhgui_shops.history." + l.kind, l.what, l.who).withStyle(ChatFormatting.GRAY));
						source.sendSuccess(() -> line, false);
					}
				}

				return 1;
			}
		}
	}

	private static MutableComponent head(Component text) {
		return Component.literal("— ").append(text).append(" —").withStyle(style -> style.withColor(14722136));
	}

	private static MutableComponent link(Component text, String command) {
		return Component.literal("[")
			.append(text)
			.append("]")
			.withStyle(
				style -> style.withColor(14722136)
						.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
						.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
			);
	}

	public static Optional<LiteralArgumentBuilder<CommandSourceStack>> buildSubcommand(CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> money = moneyTree("money");
		LiteralArgumentBuilder<CommandSourceStack> pay = payTree("pay");
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("shops")
			.requires(may(ShopNodes.USE))
			.executes(c -> browse((CommandSourceStack)c.getSource()))
			.then(
				Commands.literal("view")
					.then(
						Commands.argument("shop", StringArgumentType.word())
							.suggests(SHOPS)
							.executes(c -> view((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "shop")))
					)
			)
			.then(Commands.literal("buy").then(trade(true)))
			.then(Commands.literal("sell").then(trade(false)))
			.then(
				Commands.literal("create").requires(may(ShopNodes.CREATE))
					.then(
						Commands.argument("name", StringArgumentType.greedyString())
							.executes(c -> act(c, "create", (s, a) -> s.create(a, StringArgumentType.getString(c, "name"))))
					)
			)
			.then(
				Commands.literal("rename").requires(may(ShopNodes.CREATE))
					.then(
						Commands.argument("name", StringArgumentType.greedyString())
							.executes(c -> own(c, "edit", (s, a, shop) -> s.edit(a, shop.id, StringArgumentType.getString(c, "name"), null, null)))
					)
			)
			.then(
				Commands.literal("describe").requires(may(ShopNodes.CREATE))
					.then(
						Commands.argument("text", StringArgumentType.greedyString())
							.executes(c -> own(c, "edit", (s, a, shop) -> s.edit(a, shop.id, null, StringArgumentType.getString(c, "text"), null)))
					)
			)
			.then(
				Commands.literal("open").requires(may(ShopNodes.CREATE))
					.executes(c -> own(c, "open", (s, a, shop) -> s.edit(a, shop.id, null, null, true)))
			)
			.then(
				Commands.literal("close").requires(may(ShopNodes.CREATE))
					.executes(c -> own(c, "close", (s, a, shop) -> s.edit(a, shop.id, null, null, false)))
			)
			.then(
				Commands.literal("delete").requires(may(ShopNodes.CREATE))
					.executes(c -> own(c, "delete", (s, a, shop) -> s.delete(a, shop.id)))
			)
			.then(
				Commands.literal("offer").requires(may(ShopNodes.CREATE))
					.then(
						Commands.literal("add")
							.then(
								Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
									.executes(c -> own(c, "offer_add", (s, a, shop) -> s.offerAdd(a, shop.id, held(c), LongArgumentType.getLong(c, "price"), 0L)))
							)
							.then(
								Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
									.executes(c -> own(c, "offer_add", (s, a, shop) -> s.offerAdd(a, shop.id, held(c), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice"))))
							)
					)
					.then(
						Commands.literal("price")
							.then(
								Commands.argument("offer", IntegerArgumentType.integer(1))
									.suggests(OWN_OFFERS)
									.then(
										Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
											.executes(c -> own(c, "offer_edit", (s, a, shop) -> s.offerEdit(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "price"), 0L)))
									)
									.then(
										Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
											.executes(c -> own(c, "offer_edit", (s, a, shop) -> s.offerEdit(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice"))))
									)
							)
					)
					.then(
						Commands.literal("remove")
							.then(
								Commands.argument("offer", IntegerArgumentType.integer(1))
									.suggests(OWN_OFFERS)
									.executes(c -> own(c, "offer_remove", (s, a, shop) -> s.offerRemove(a, shop.id, IntegerArgumentType.getInteger(c, "offer"))))
							)
					)
			)
			.then(
				Commands.literal("stock").requires(may(ShopNodes.CREATE))
					.executes(c -> stock((CommandSourceStack)c.getSource()))
					.then(
						Commands.literal("add")
							.executes(c -> own(c, "stock_add", (s, a, shop) -> s.stockAdd(a, shop.id, 0)))
							.then(
								Commands.argument("offer", IntegerArgumentType.integer(1))
									.suggests(OWN_OFFERS)
									.executes(c -> own(c, "stock_add", (s, a, shop) -> s.stockAdd(a, shop.id, IntegerArgumentType.getInteger(c, "offer"))))
							)
					)
					.then(
						Commands.literal("take")
							.then(
								Commands.argument("offer", IntegerArgumentType.integer(1))
									.suggests(OWN_OFFERS)
									.executes(c -> own(c, "stock_take", (s, a, shop) -> s.stockTake(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), 0L)))
							)
							.then(
								Commands.argument("count", LongArgumentType.longArg(1L))
									.executes(c -> own(c, "stock_take", (s, a, shop) -> s.stockTake(a, shop.id, IntegerArgumentType.getInteger(c, "offer"), LongArgumentType.getLong(c, "count"))))
							)
					)
			)
			.then(
				Commands.literal("admin").requires(may(ShopNodes.ADMIN))
					.executes(c -> admin((CommandSourceStack)c.getSource()))
					.then(
						Commands.literal("create")
							.then(
								Commands.argument("name", StringArgumentType.greedyString())
									.executes(c -> act(c, "create", (s, a) -> s.adminCreate(a, StringArgumentType.getString(c, "name"))))
							)
					)
					.then(
						Commands.literal("delete")
							.then(
								Commands.argument("shop", StringArgumentType.word())
									.suggests(SHOPS)
									.executes(c -> act(c, "delete", (s, a) -> s.delete(a, StringArgumentType.getString(c, "shop"))))
							)
					)
					.then(
						Commands.literal("open")
							.then(
								Commands.argument("shop", StringArgumentType.word())
									.suggests(SHOPS)
									.executes(c -> act(c, "open", (s, a) -> s.edit(a, StringArgumentType.getString(c, "shop"), null, null, true)))
							)
					)
					.then(
						Commands.literal("close")
							.then(
								Commands.argument("shop", StringArgumentType.word())
									.suggests(SHOPS)
									.executes(c -> act(c, "close", (s, a) -> s.edit(a, StringArgumentType.getString(c, "shop"), null, null, false)))
							)
					)
					.then(
						Commands.literal("offer")
							.then(
								Commands.argument("shop", StringArgumentType.word())
									.suggests(SHOPS)
									.then(
										Commands.literal("add")
											.then(
												Commands.argument("price", LongArgumentType.longArg(0L, 1000000000L))
													.executes(c -> act(c, "offer_add", (s, a) -> s.offerAdd(a, StringArgumentType.getString(c, "shop"), held(c), LongArgumentType.getLong(c, "price"), 0L)))
											)
											.then(
												Commands.argument("sellPrice", LongArgumentType.longArg(0L, 1000000000L))
													.executes(c -> act(c, "offer_add", (s, a) -> s.offerAdd(a, StringArgumentType.getString(c, "shop"), held(c), LongArgumentType.getLong(c, "price"), LongArgumentType.getLong(c, "sellPrice"))))
											)
									)
									.then(
										Commands.literal("remove")
											.then(
												Commands.argument("offer", IntegerArgumentType.integer(1))
													.executes(c -> act(c, "offer_remove", (s, a) -> s.offerRemove(a, StringArgumentType.getString(c, "shop"), IntegerArgumentType.getInteger(c, "offer"))))
											)
									)
							)
					)
			)
			.then(Commands.literal("reload").requires(may(ShopNodes.ADMIN)).executes(c -> {
				OpResult r = ShopsRuntime.reload();
				return report((CommandSourceStack)c.getSource(), "reload", r);
			}))
			.then(Commands.literal("selftest").requires(may(ShopNodes.SELFTEST)).executes(c -> selfTest((CommandSourceStack)c.getSource())))
			.then(Commands.literal("version").executes(c -> {
				String version = CFHGuiPlatform.get().modInfo("cfhcore").map(info -> info.version()).orElse("?");
				((CommandSourceStack)c.getSource()).sendSuccess(() -> Msg.tr("cfhgui_shops.command.version", version), false);
				return 1;
			}));

		root.then(money);
		root.then(pay);

		return Optional.of(root);
	}

	private ShopCommands() {
	}

	@FunctionalInterface
	private interface OnOwn {
		OpResult run(ShopService var1, Actor var2, Shop var3);
	}
}
