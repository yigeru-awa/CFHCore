package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvCommands;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbAuto;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbImport;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.LegacyItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class QuestCommands {
	private static final Map<String, String> ENGLISH = Map.ofEntries(
		Map.entry("ok", "Done"),
		Map.entry("disabled", "That is not available here."),
		Map.entry("no_permission", "Only operators may change the quest book."),
		Map.entry("no_such_quest", "That quest is gone."),
		Map.entry("no_such_chapter", "That chapter is gone."),
		Map.entry("not_done", "The quest is not done yet."),
		Map.entry("not_open", "The quest is not open yet."),
		Map.entry("nothing_to_claim", "There is nothing to claim."),
		Map.entry("invalid", "The server could not read that."),
		Map.entry("no_such_task", "That quest has no such task."),
		Map.entry("no_such_player", "No player named %s is on.")
	);
	private static final SuggestionProvider<CommandSourceStack> QUESTS = (c, b) -> {
		BookIndex index = QuestsServer.index();
		List<String> keys = new ArrayList<>();
		if (index != null) {
			for (BookIndex.Entry e : index.all()) {
				keys.add(e.key());
			}
		}

		return SharedSuggestionProvider.suggest(keys, b);
	};
	private static final SuggestionProvider<CommandSourceStack> CHAPTERS = (c, b) -> {
		QuestBook book = QuestsServer.book();
		return SharedSuggestionProvider.suggest((Iterable)(book == null ? List.of() : book.chapters.keySet()), b);
	};
	private static final SuggestionProvider<CommandSourceStack> TARGETS = (c, b) -> {
		QuestBook book = QuestsServer.book();
		BookIndex index = QuestsServer.index();
		List<String> all = new ArrayList<>();
		if (book != null) {
			all.addAll(book.chapters.keySet());
		}

		if (index != null) {
			for (BookIndex.Entry e : index.all()) {
				all.add(e.key());
			}
		}

		return SharedSuggestionProvider.suggest(all, b);
	};
	private static final SuggestionProvider<CommandSourceStack> TASKS = (c, b) -> {
		BookIndex index = QuestsServer.index();
		List<String> names = new ArrayList<>();

		try {
			BookIndex.Entry e = index == null ? null : index.get(key(c, "quest"));
			if (e != null) {
				for (int i = 0; i < e.quest().tasks.size(); i++) {
					Task t = e.quest().tasks.get(i);
					names.add(t.is("custom") && !t.value.isBlank() ? t.value : Integer.toString(i + 1));
				}
			}
		} catch (IllegalArgumentException var7) {
		}

		return SharedSuggestionProvider.suggest(names, b);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			(LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
													"cfhquests"
												)
												.requires(QuestCommands::allowed))
											.then(Commands.literal("reload").executes(c -> reload((CommandSourceStack)c.getSource()))))
										.then(
											Commands.literal("reset")
												.then(
													((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("players", EntityArgument.players())
																	.executes(c -> reset(c, "all")))
																.then(Commands.literal("all").executes(c -> reset(c, "all"))))
															.then(
																Commands.literal("chapter")
																	.then(
																		Commands.argument("chapter", StringArgumentType.word())
																			.suggests(CHAPTERS)
																			.executes(c -> reset(c, "chapter:" + StringArgumentType.getString(c, "chapter")))
																	)
															))
														.then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS).executes(c -> reset(c, key(c, "quest"))))
												)
										))
									.then(
										Commands.literal("complete")
											.then(
												Commands.argument("players", EntityArgument.players())
													.then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS).executes(QuestCommands::complete))
											)
									))
								.then(
									Commands.literal("task")
										.then(
											Commands.argument("players", EntityArgument.players())
												.then(
													Commands.argument("quest", ResourceLocationArgument.id())
														.suggests(QUESTS)
														.then(Commands.argument("task", StringArgumentType.greedyString()).suggests(TASKS).executes(QuestCommands::task))
												)
										)
								))
							.then(
								Commands.literal("open")
									.then(
										((RequiredArgumentBuilder)Commands.argument("players", EntityArgument.players()).executes(c -> open(c, "")))
											.then(Commands.argument("target", ResourceLocationArgument.id()).suggests(TARGETS).executes(c -> open(c, key(c, "target"))))
									)
							))
						.then(
							Commands.literal("import")
								.then(
									((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ftb")
														.executes(c -> importFtb((CommandSourceStack)c.getSource(), "")))
													.then(Commands.literal("install").executes(c -> installFtb((CommandSourceStack)c.getSource()))))
												.then(Commands.literal("progress").executes(c -> progressFtb((CommandSourceStack)c.getSource()))))
											.then(Commands.literal("status").executes(c -> statusFtb((CommandSourceStack)c.getSource()))))
										.then(
											((RequiredArgumentBuilder)Commands.argument("folder", StringArgumentType.greedyString()).requires(s -> s.hasPermission(4)))
												.executes(c -> importFtb((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "folder")))
										)
								)
						))
					.then(
						Commands.literal("counter")
							.then(
								Commands.argument("players", EntityArgument.players())
									.then(
										((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", StringArgumentType.string()).executes(c -> counter(c, 0L, false, true)))
												.then(
													Commands.literal("add")
														.then(Commands.argument("amount", LongArgumentType.longArg()).executes(c -> counter(c, LongArgumentType.getLong(c, "amount"), false, false)))
												))
											.then(
												Commands.literal("set")
													.then(Commands.argument("amount", LongArgumentType.longArg(0L)).executes(c -> counter(c, LongArgumentType.getLong(c, "amount"), true, false)))
											)
									)
							)
					))
				.then(Commands.literal("selftest").executes(c -> selftest((CommandSourceStack)c.getSource())))
		);
		AdvCommands.register(dispatcher);
	}

	private static boolean allowed(CommandSourceStack source) {
		if (source.hasPermission(QuestsServerConfig.get().editorPermissionLevel)) {
			return true;
		} else {
			if (source.getEntity() instanceof ServerPlayer player && QuestsServer.mayEdit(player)) {
				return true;
			}

			return false;
		}
	}

	private static String key(CommandContext<CommandSourceStack> c, String name) {
		ResourceLocation id = (ResourceLocation)c.getArgument(name, ResourceLocation.class);
		return id.getPath();
	}

	private static MutableComponent result(QuestEngine.Outcome o) {
		return ServerText.msg("cfhgui_quests.result." + o.code(), ENGLISH.getOrDefault(o.code(), o.code()), o.args().toArray());
	}

	private static int reload(CommandSourceStack source) {
		if (QuestsServer.engine() == null) {
			return fail(source, QuestEngine.Outcome.fail("disabled"));
		} else {
			QuestsServer.reload();
			QuestBook book = QuestsServer.book();
			source.sendSuccess(
				() -> ServerText.msg(
						"cfhgui_quests.command.reloaded",
						"The quest book was read again: %s chapters, %s quests.",
						book == null ? 0 : book.chapters.size(),
						book == null ? 0 : book.questCount()
					),
				true
			);
			return 1;
		}
	}

	private static int reset(CommandContext<CommandSourceStack> c, String target) throws CommandSyntaxException {
		int n = each(c, (e, s) -> e.reset(s, target));
		if (n > 0) {
			((CommandSourceStack)c.getSource())
				.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.reset", "Progress forgotten (players: %s): %s.", n, target), true);
		}

		return n;
	}

	private static int complete(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		String key = key(c, "quest");
		int n = each(c, (e, s) -> e.complete(s, key));
		if (n > 0) {
			((CommandSourceStack)c.getSource()).sendSuccess(() -> ServerText.msg("cfhgui_quests.command.completed", "%s is done (players: %s).", key, n), true);
		}

		return n;
	}

	private static int task(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		String key = key(c, "quest");
		String name = StringArgumentType.getString(c, "task").trim();
		long amount = 1L;
		int space = name.lastIndexOf(32);
		if (space > 0) {
			try {
				amount = Math.max(1L, Long.parseLong(name.substring(space + 1)));
				name = name.substring(0, space).trim();
			} catch (NumberFormatException var10) {
			}
		}

		String task = name;
		long by = amount;
		int n = each(c, (e, s) -> e.task(s, key, task, by));
		if (n > 0) {
			((CommandSourceStack)c.getSource())
				.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.task", "Task %s of %s moved on by %s (players: %s).", task, key, by, n), true);
		}

		return n;
	}

	private static int open(CommandContext<CommandSourceStack> c, String target) throws CommandSyntaxException {
		boolean quest = target.indexOf(47) >= 0;
		int n = 0;

		for (ServerPlayer player : EntityArgument.getPlayers(c, "players")) {
			if (QuestsNet.has(player)) {
				QuestsNet.send(player, new QuestsNet.OpenBook(quest ? "" : target, quest ? target : ""));
				n++;
			}
		}

		int opened = n;
		if (opened == 0) {
			((CommandSourceStack)c.getSource()).sendFailure(ServerText.msg("cfhgui_quests.command.open.none", "None of them has CFHGui Quests on their client."));
			return 0;
		} else {
			((CommandSourceStack)c.getSource())
				.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.opened", "The quest book was opened (players: %s).", opened), false);
			return opened;
		}
	}

	private static int each(CommandContext<CommandSourceStack> c, QuestCommands.PerSeat what) throws CommandSyntaxException {
		QuestEngine e = QuestsServer.engine();
		if (e == null) {
			return fail((CommandSourceStack)c.getSource(), QuestEngine.Outcome.fail("disabled"));
		} else {
			Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "players");
			int n = 0;

			for (ServerPlayer player : players) {
				Seat s = QuestsServer.seat(player);
				QuestEngine.Outcome o = s == null ? QuestEngine.Outcome.fail("no_such_player", player.getGameProfile().getName()) : what.run(e, s);
				if (o.ok()) {
					n++;
				} else {
					((CommandSourceStack)c.getSource()).sendFailure(Component.empty().append(player.getDisplayName()).append(": ").append(result(o)));
				}
			}

			return n;
		}
	}

	private static int fail(CommandSourceStack source, QuestEngine.Outcome o) {
		source.sendFailure(result(o));
		return 0;
	}

	private static int importFtb(CommandSourceStack source, String folder) {
		MinecraftServer server = source.getServer();
		Path gameDir = CFHGuiPlatform.get().gameDir();
		Path from = folder.isBlank() ? FtbAuto.ftbDir() : gameDir.resolve(folder.trim());
		Path into = CFHGuiPlatform.get().configDir().resolve("slate").resolve("quests-imported");
		LegacyItems.install();
		source.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.import.started", "Reading the FTB Quests book in %s ...", from.toString()), false);
		CompletableFuture.runAsync(
			() -> {
				boolean ok = false;

				MutableComponent said;
				try {
					FtbImport.Result r = FtbImport.read(from);
					if (r == null) {
						said = ServerText.msg("cfhgui_quests.command.import.none", "No FTB Quests book was found in %s (its chapters folder is missing).", from.toString());
					} else {
						Files.createDirectories(into);
						FtbImport.write(into, r);

						for (String line : r.summary()) {
							CFHGuiQuests.LOGGER.info("[CFHGui Quests] import: {}", line);
						}

						said = ServerText.msg(
							"cfhgui_quests.command.import.done",
							"Converted %s chapters with %s quests into %s. %s things were approximated: see ftb-report.txt there. Look it over; \"/cfhquests import ftb install\" makes the FTB Quests book the server's book.",
							r.book().chapters.size(),
							r.book().questCount(),
							into.toString(),
							r.unconverted()
						);
						ok = true;
					}
				} catch (Exception var9) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] importing the FTB Quests book in {} failed", from, var9);
					said = ServerText.msg("cfhgui_quests.command.import.failed", "The import failed: %s", String.valueOf(var9.getMessage()));
				}

				MutableComponent message = said;
				boolean went = ok;
				server.execute(() -> {
					if (went) {
						source.sendSuccess(() -> message, true);
					} else {
						source.sendFailure(message);
					}
				});
			},
			r -> {
				Thread t = new Thread(r, "CFHGui Quests import");
				t.setDaemon(true);
				t.start();
			}
		);
		return 1;
	}

	private static int installFtb(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		Path from = FtbAuto.ftbDir();
		LegacyItems.install();
		source.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.import.started", "Reading the FTB Quests book in %s ...", FtbAuto.shown(from)), false);
		CompletableFuture.runAsync(() -> {
			FtbAuto.Prepared made = null;
			Exception failed = null;

			try {
				made = FtbAuto.prepare(from, true);
			} catch (Throwable var7) {
				failed = (Exception)(var7 instanceof Exception ex ? ex : new RuntimeException(var7));
			}

			FtbAuto.Prepared prepared = made;
			Exception thrown = failed;
			server.execute(() -> installFtb(source, server, from, prepared, thrown));
		}, r -> {
			Thread t = new Thread(r, "CFHGui Quests conversion");
			t.setDaemon(true);
			t.start();
		});
		return 1;
	}

	private static void installFtb(CommandSourceStack source, MinecraftServer server, Path from, @Nullable FtbAuto.Prepared made, @Nullable Exception thrown) {
		if (QuestsServer.engine() != null) {
			try {
				if (thrown != null) {
					throw thrown;
				}

				FtbAuto.Done done = made == null ? null : FtbAuto.install(made);
				if (done == null || !done.ok()) {
					source.sendFailure(
						done == null
							? ServerText.msg("cfhgui_quests.command.import.none", "No FTB Quests book was found in %s (its chapters folder is missing).", FtbAuto.shown(from))
							: done.message()
					);
					return;
				}

				QuestsServer.reload();
				source.sendSuccess(done::message, true);
				FtbAuto.tell(server);
				FtbAuto.Done progress = FtbAuto.progress(server, false);
				if (progress != null && progress.ok()) {
					source.sendSuccess(progress::message, true);
				}
			} catch (Exception var7) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] converting the FTB Quests book failed", var7);
				source.sendFailure(ServerText.msg("cfhgui_quests.command.import.failed", "The import failed: %s", String.valueOf(var7.getMessage())));
			}
		}
	}

	private static int progressFtb(CommandSourceStack source) {
		try {
			FtbAuto.Done done = FtbAuto.progress(source.getServer(), true);
			if (done == null) {
				return 0;
			} else {
				if (done.ok()) {
					source.sendSuccess(done::message, true);
				} else {
					source.sendFailure(done.message());
				}

				return done.ok() ? 1 : 0;
			}
		} catch (Exception var2) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] carrying over the progress of FTB Quests failed", var2);
			source.sendFailure(ServerText.msg("cfhgui_quests.command.import.failed", "The import failed: %s", String.valueOf(var2.getMessage())));
			return 0;
		}
	}

	private static int statusFtb(CommandSourceStack source) {
		for (FtbAuto.Done line : FtbAuto.status()) {
			source.sendSuccess(line::message, false);
		}

		return 1;
	}

	private static int counter(CommandContext<CommandSourceStack> c, long amount, boolean set, boolean show) throws CommandSyntaxException {
		String name = StringArgumentType.getString(c, "name");
		QuestEngine e = QuestsServer.engine();
		if (e == null) {
			return fail((CommandSourceStack)c.getSource(), QuestEngine.Outcome.fail("disabled"));
		} else {
			int n = 0;

			for (ServerPlayer player : EntityArgument.getPlayers(c, "players")) {
				Seat s = QuestsServer.seat(player);
				if (s != null) {
					long now = show ? s.team.counter(name.trim()) : e.counter(s, name, amount, set);
					((CommandSourceStack)c.getSource())
						.sendSuccess(() -> ServerText.msg("cfhgui_quests.command.counter", "%s: the counter %s stands at %s.", player.getDisplayName(), name, now), !show);
					n++;
				}
			}

			if (!show) {
				e.lookFor();
			}

			return n;
		}
	}

	private static int selftest(CommandSourceStack source) {
		SelfTest.Report report = SelfTest.run(source.getServer(), source.getPlayer());

		for (String failure : report.failures()) {
			source.sendFailure(Component.literal(failure));
		}

		MutableComponent line = ServerText.msg("cfhgui_quests.command.selftest", "Self-test: %s passed, %s failed.", report.passed(), report.failures().size());
		source.sendSuccess(() -> line.withStyle(report.failures().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
		return report.failures().isEmpty() ? 1 : 0;
	}

	public static Optional<LiteralArgumentBuilder<CommandSourceStack>> buildSubcommand(CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("quests")
			.requires(QuestCommands::allowed)
			.then(Commands.literal("reload").executes(c -> reload((CommandSourceStack)c.getSource())))
			.then(
				Commands.literal("reset")
					.then(
						Commands.argument("players", EntityArgument.players())
							.then(Commands.literal("all").executes(c -> reset(c, "all")))
							.then(Commands.literal("chapter")
								.then(Commands.argument("chapter", StringArgumentType.word()).suggests(CHAPTERS).executes(c -> reset(c, "chapter:" + StringArgumentType.getString(c, "chapter")))))
							.then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS).executes(c -> reset(c, key(c, "quest"))))
					)
			)
			.then(
				Commands.literal("complete")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS).executes(QuestCommands::complete)))
			)
			.then(
				Commands.literal("task")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS)
							.then(Commands.argument("task", StringArgumentType.greedyString()).suggests(TASKS).executes(QuestCommands::task))))
			)
			.then(
				Commands.literal("open")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("target", ResourceLocationArgument.id()).suggests(TARGETS).executes(c -> open(c, key(c, "target")))))
			)
			.then(
				Commands.literal("import")
					.then(Commands.literal("ftb")
						.then(Commands.literal("install").executes(c -> installFtb((CommandSourceStack)c.getSource())))
						.then(Commands.literal("progress").executes(c -> progressFtb((CommandSourceStack)c.getSource())))
						.then(Commands.literal("status").executes(c -> statusFtb((CommandSourceStack)c.getSource())))
					)
					.then(Commands.argument("folder", StringArgumentType.greedyString()).requires(s -> s.hasPermission(4)).executes(c -> importFtb((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "folder"))))
			)
			.then(
				Commands.literal("counter")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("name", StringArgumentType.string())
							.then(Commands.literal("add").then(Commands.argument("amount", LongArgumentType.longArg()).executes(c -> counter(c, LongArgumentType.getLong(c, "amount"), false, false))))
						.then(Commands.literal("set").then(Commands.argument("amount", LongArgumentType.longArg(0L)).executes(c -> counter(c, LongArgumentType.getLong(c, "amount"), true, false))))
						.executes(c -> counter(c, 0L, false, true)))
			)
			.then(Commands.literal("selftest").executes(c -> selftest((CommandSourceStack)c.getSource())))
		);

		return Optional.of(root);
	}

	private QuestCommands() {
	}

	@FunctionalInterface
	private interface PerSeat {
		QuestEngine.Outcome run(QuestEngine var1, Seat var2);
	}
}
