package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.compat.FtbPreflight;
import com.onehumanawa.cfhcore.content.gui.quests.compat.TeamsBridge;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbAuto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class QuestsServer {
	private static final boolean DEBUG = Boolean.getBoolean("cfh.quests.debug");
	private static boolean initialised;
	@Nullable
	private static MinecraftServer server;
	@Nullable
	private static QuestEngine engine;
	private static TeamLink teams = TeamLink.NONE;
	private static long tickNanos;
	private static long tickWorst;
	private static int tickCount;
	private static final Map<String, byte[]> WORDS = new HashMap<>();
	private static final Map<UUID, String> SPOKEN = new HashMap<>();

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			CFHGuiEvents.SERVER_STARTED.register(QuestsServer::started);
			CFHGuiEvents.SERVER_STOPPING.register(QuestsServer::stopping);
			CFHGuiEvents.SERVER_TICK_END.register(QuestsServer::tick);
			CFHGuiEvents.PLAYER_JOINED.register(QuestsServer::joined);
			CFHGuiEvents.PLAYER_LEFT.register(QuestsServer::left);
			if (CFHGuiPlatform.get().isModLoaded("ftbquests")) {
				FtbPreflight.run(CFHGuiPlatform.get().isClient());
			}

			if (CFHGuiPlatform.get().isModLoaded("cfhgui_teams")) {
				try {
					teams = QuestsServer.TeamsSide.link();
					CFHGuiQuests.LOGGER.info("[CFHGui Quests] CFHGui Teams is here: a team's quests are shared by its members");
				} catch (Throwable var1) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] CFHGui Teams is installed but could not be linked; every player quests alone ({})", var1.toString());
				}
			}
		}
	}

	public static boolean quietsFtb() {
		QuestEngine e = engine;
		return e != null && "ftbquests".equals(e.book().source) && QuestsServerConfig.get().ftbQuiet;
	}

	public static Path bookDir() {
		return JsonConfig.dir().resolve("quests");
	}

	@Nullable
	public static QuestBook book() {
		return engine == null ? null : engine.book();
	}

	@Nullable
	public static BookIndex index() {
		return engine == null ? null : engine.index();
	}

	@Nullable
	static QuestEngine engine() {
		return engine;
	}

	@Nullable
	static MinecraftServer server() {
		return server;
	}

	static UUID holderOf(UUID player) {
		UUID team = QuestsServerConfig.get().teamProgress ? teams.holderOf(player) : null;
		return team == null ? player : team;
	}

	@Nullable
	static Seat seat(ServerPlayer player) {
		QuestEngine e = engine;
		Seat s = e == null ? null : e.seat(player.getUUID());
		if (s != null && s.player != player) {
			MinecraftServer srv = server;
			if (srv != null && srv.getPlayerList().getPlayer(player.getUUID()) == player) {
				s.player = player;
				return s;
			} else {
				return null;
			}
		} else {
			return s;
		}
	}

	private static void started(MinecraftServer started) {
		QuestsServerConfig.reload();
		server = started;
		Path progress = started.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("quests").resolve("progress");
		engine = new QuestEngine(new LiveWorld(started, teams), new HolderStore(progress), started.registryAccess(), System.nanoTime());
		FtbAuto.atStart(started);
		load();
	}

	private static void stopping(MinecraftServer stopping) {
		QuestEngine e = engine;
		engine = null;
		server = null;
		SPOKEN.clear();
		if (e != null) {
			try {
				e.leaveAll();
			} finally {
				e.store().close();
			}
		}
	}

	static void load() {
		QuestEngine e = engine;
		if (e != null) {
			Path dir = bookDir();
			QuestBook loaded;
			if (!BookJson.exists(dir) && QuestsServerConfig.get().installExamples) {
				loaded = BookJson.loadBundled();

				try {
					BookJson.save(dir, loaded);
					CFHGuiQuests.LOGGER.info("[CFHGui Quests] no quest book in {}: the example quest lines were written there", dir);
				} catch (Exception var5) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the example quest lines could not be written to {}: {}", dir, var5.toString());
				}
			} else {
				loaded = BookJson.load(dir);
			}

			loaded.revision = System.currentTimeMillis();
			words(dir);
			long t0 = System.nanoTime();
			e.book(loaded);
			CFHGuiQuests.LOGGER
				.info(
					"[CFHGui Quests] the book \"{}\": {} chapter(s), {} quest(s), indexed in {} ms",
					new Object[]{loaded.title, loaded.chapters.size(), loaded.questCount(), (System.nanoTime() - t0) / 1000000L}
				);
		}
	}

	private static void joined(ServerPlayer player) {
		QuestEngine e = engine;
		if (e != null) {
			long t0 = System.nanoTime();
			e.join(player.getUUID(), player.getGameProfile().getName(), player);
			if (DEBUG) {
				CFHGuiQuests.LOGGER.info("[CFHGui Quests] debug: seating {} took {} us", player.getGameProfile().getName(), (System.nanoTime() - t0) / 1000L);
			}

			FtbAuto.joined(player);
		}
	}

	private static void words(Path dir) {
		WORDS.clear();
		Path lang = dir.resolve("lang");
		if (Files.isDirectory(lang)) {
			try (Stream<Path> s = Files.list(lang)) {
				for (Path p : s.toList()) {
					String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
					if (name.endsWith(".json")) {
						try {
							JsonObject words = (JsonObject)BookJson.GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
							if (words != null && words.size() > 0) {
								WORDS.put(name.substring(0, name.length() - 5), JsonWire.encode(BookJson.GSON, words));
							}
						} catch (Exception var8) {
							CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the translation {} cannot be read and is left out: {}", p, var8.toString());
						}
					}
				}
			} catch (IOException var10) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} cannot be listed: {}", lang, var10.toString());
			}
		}
	}

	static void sendWords(ServerPlayer player) {
		String locale = player.clientInformation().language().toLowerCase(Locale.ROOT);
		String before = SPOKEN.put(player.getUUID(), locale);
		byte[] words = WORDS.get(locale);
		if (words != null) {
			QuestsNet.send(player, new QuestsNet.Words(locale, words));
		} else if (before != null && !before.equals(locale) && WORDS.containsKey(before)) {
			QuestsNet.send(player, new QuestsNet.Words(locale, new byte[0]));
		}
	}

	private static void spoken(MinecraftServer running) {
		if (!SPOKEN.isEmpty()) {
			for (ServerPlayer player : running.getPlayerList().getPlayers()) {
				String before = SPOKEN.get(player.getUUID());
				if (before != null && !before.equals(player.clientInformation().language().toLowerCase(Locale.ROOT))) {
					sendWords(player);
				}
			}
		}
	}

	private static void left(ServerPlayer player) {
		SPOKEN.remove(player.getUUID());
		QuestEngine e = engine;
		if (e != null) {
			e.leave(player.getUUID());
		}
	}

	private static void tick(MinecraftServer ticking) {
		QuestEngine e = engine;
		if (e != null) {
			FtbAuto.firstTick(ticking);
			if (!WORDS.isEmpty() && ticking.getTickCount() % 20 == 0) {
				spoken(ticking);
			}

			if (!DEBUG) {
				e.tick();
			} else {
				long t0 = System.nanoTime();
				e.tick();
				long took = System.nanoTime() - t0;
				tickNanos += took;
				tickWorst = Math.max(tickWorst, took);
				if (++tickCount >= 200) {
					CFHGuiQuests.LOGGER
						.info(
							"[CFHGui Quests] debug: {} players, a tick costs {} us on average, {} us at worst",
							new Object[]{e.seats().size(), tickNanos / (long)tickCount / 1000L, tickWorst / 1000L}
						);
					tickWorst = 0L;
					tickNanos = 0L;
					tickCount = 0;
				}
			}
		}
	}

	static boolean mayEdit(ServerPlayer player) {
		QuestsServerConfig config = QuestsServerConfig.get();
		if (player.hasPermissions(config.editorPermissionLevel)) {
			return true;
		} else {
			MinecraftServer s = server;
			return config.ownerMayEdit && s != null && s.isSingleplayerOwner(player.getGameProfile());
		}
	}

	public static void onAction(ServerPlayer player, QuestsNet.Action action) {
		QuestEngine.Outcome outcome;
		try {
			outcome = act(player, action.op(), action.args());
		} catch (Exception var4) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] the action {} of {} failed", new Object[]{action.op(), player.getGameProfile().getName(), var4});
			outcome = QuestEngine.Outcome.fail("invalid");
		}

		QuestEngine e = engine;
		if (e != null) {
			e.flush();
		}

		QuestsNet.send(player, new QuestsNet.Result(action.request(), outcome.ok(), outcome.code(), outcome.args()));
	}

	private static QuestEngine.Outcome act(ServerPlayer player, String op, CompoundTag args) {
		QuestEngine e = engine;
		Seat s = seat(player);
		if (e == null || s == null) {
			return QuestEngine.Outcome.fail("disabled");
		} else if (e.tooFast(s)) {
			return QuestEngine.Outcome.fail("too_fast");
		} else {
			switch (op) {
				case "claim":
					return e.claim(
						s, args.getString("quest"), args.contains("reward", 99) ? args.getInt("reward") : -1, args.contains("choice", 99) ? args.getInt("choice") : -1
					);
				case "claim_all":
					return e.claimAll(s, args.getBoolean("pick"));
				case "submit":
					return e.submit(s, args.getString("quest"), args.getInt("task"));
				case "sync":
					e.sync(s);
					return QuestEngine.Outcome.OK;
				case "emergency":
					return e.emergency(s);
				case "edit_head":
				case "edit_chapter":
				case "delete_chapter":
				case "rename_chapter":
				case "rename_quest":
				case "reload":
				case "reset":
				case "complete":
					if (!mayEdit(player)) {
						return QuestEngine.Outcome.fail("no_permission");
					}

					return edit(e, s, op, args);
				default:
					return QuestEngine.Outcome.fail("unknown");
			}
		}
	}

	private static QuestEngine.Outcome edit(QuestEngine e, Seat s, String op, CompoundTag args) {
		switch (op) {
			case "edit_head":
				return BookEditor.editHead(e, bookDir(), args.getByteArray("book"));
			case "edit_chapter":
				return BookEditor.editChapter(e, bookDir(), args.getString("id"), args.getByteArray("chapter"));
			case "delete_chapter":
				return BookEditor.deleteChapter(e, bookDir(), args.getString("id"));
			case "rename_chapter":
				return BookEditor.renameChapter(e, bookDir(), args.getString("id"), args.getString("to"));
			case "rename_quest":
				return BookEditor.renameQuest(e, bookDir(), args.getString("quest"), args.getString("to"));
			case "reload":
				reload();
				return QuestEngine.Outcome.OK;
			case "reset":
			case "complete":
				Seat target = s;
				if (args.contains("player", 8) && !args.getString("player").isBlank()) {
					MinecraftServer srv = server;
					ServerPlayer named = srv == null ? null : srv.getPlayerList().getPlayerByName(args.getString("player"));
					target = named == null ? null : seat(named);
					if (target == null) {
						return QuestEngine.Outcome.fail("no_such_player", args.getString("player"));
					}
				}

				return op.equals("reset") ? e.reset(target, args.getString("target")) : e.complete(target, args.getString("quest"));
			default:
				return QuestEngine.Outcome.fail("unknown");
		}
	}

	static void reload() {
		boolean shared = QuestsServerConfig.get().teamProgress;
		QuestsServerConfig.reload();
		load();
		QuestEngine e = engine;
		if (e != null && shared != QuestsServerConfig.get().teamProgress) {
			for (Seat s : new ArrayList<>(e.seats())) {
				e.rebind(s);
			}
		}
	}

	public static int apiProgress(ServerPlayer player, String task, long amount) {
		QuestEngine e = engine;
		Seat s = seat(player);
		return e != null && s != null && task != null ? e.custom(s, task, Math.max(1L, amount)) : 0;
	}

	public static boolean apiProgress(ServerPlayer player, String quest, String task, long amount) {
		QuestEngine e = engine;
		Seat s = seat(player);
		return e != null && s != null && quest != null && task != null && e.task(s, quest, task, amount).ok();
	}

	public static long apiCount(ServerPlayer player, String name, long amount) {
		QuestEngine e = engine;
		Seat s = seat(player);
		return e != null && s != null && name != null ? e.counter(s, name, amount, false) : 0L;
	}

	public static Rules.Status apiStatus(ServerPlayer player, String quest) {
		QuestEngine e = engine;
		Seat s = seat(player);
		return e != null && s != null && quest != null ? e.status(s, quest) : Rules.Status.HIDDEN;
	}

	public static void teamJoined(UUID team, UUID player) {
		QuestEngine e = engine;
		if (e != null) {
			e.memberJoined(team, player);
		}
	}

	public static void teamLeft(UUID team, UUID player) {
		QuestEngine e = engine;
		if (e != null) {
			e.memberLeft(team, player);
		}
	}

	public static void teamDisbanded(UUID team) {
		QuestEngine e = engine;
		if (e != null) {
			e.disbanded(team);
		}
	}

	private QuestsServer() {
	}

	private static final class TeamsSide {
		static TeamLink link() {
			return TeamsBridge.install();
		}
	}
}
