package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.server.Foreign;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class FtbAuto {
	public static final String STATE = "ftb-source.json";
	private static final String BASE = "ftb-base";
	private static final String PROGRESS = "ftb-progress.json";
	private static final String BACKUPS = "quests-backups";
	private static final int BACKUPS_KEPT = 10;
	private static boolean progressDue;

	public static Path ftbDir() {
		return CFHGuiPlatform.get().configDir().resolve("ftbquests");
	}

	public static Path besideDir() {
		return Foreign.bookDir().resolveSibling("quests-ftb");
	}

	public static void atStart(MinecraftServer server) {
		progressDue = true;
		LegacyItems.install();
		if (QuestsServerConfig.get().ftbConvert) {
			try {
				Path from = ftbDir();
				if (FtbBook.locate(from) == null) {
					return;
				}

				FtbAuto.Done done = convert(from, false);
				if (done != null) {
					CFHGuiQuests.LOGGER.info("[CFHGui Quests] {}", done.message().getString());
				}
			} catch (Throwable var3) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the FTB Quests book could not be converted; the server keeps the book it has", var3);
			}
		}
	}

	@Nullable
	public static FtbAuto.Done convert(Path from, boolean force) throws IOException {
		FtbAuto.Prepared made = prepare(from, force);
		return made == null ? null : install(made);
	}

	@Nullable
	public static synchronized FtbAuto.Prepared prepare(Path from, boolean force) throws IOException {
		Path located = FtbBook.locate(from);
		if (located == null) {
			return new FtbAuto.Prepared(
				FtbAuto.Done.fail("cfhgui_quests.command.import.none", "No FTB Quests book was found in %s (its chapters folder is missing).", shown(from))
			);
		} else {
			Path bookDir = Foreign.bookDir();
			FtbAuto.State state = state(bookDir);
			boolean ours = state != null || !standing(bookDir) || example(bookDir);
			Path into = !ours && !force ? besideDir() : bookDir;
			FtbAuto.State known = into.equals(bookDir) ? state : state(into);
			String quick = FtbBook.quick(located);
			if (!force && known != null && known.quick.equals(quick)) {
				return null;
			} else {
				FtbBook book = FtbBook.read(from);
				if (book == null) {
					return null;
				} else {
					int questsThere = 0;

					for (Map<String, Object> chapter : book.chapters) {
						questsThere += Snbt.list(chapter.get("quests")).size();
					}

					if (questsThere == 0) {
						return force
							? new FtbAuto.Prepared(FtbAuto.Done.fail("cfhgui_quests.command.import.empty", "The FTB Quests book in %s has no quests.", shown(located)))
							: null;
					} else if (!force && known != null && known.hash.equals(book.hash)) {
						known.quick = quick;
						write(into, known);
						return null;
					} else {
						FtbImport.IdMap before = FtbImport.ids(into);
						QuestBook current = standing(into) ? BookJson.load(into) : null;
						FtbImport.Result result = FtbImport.convert(book, before, (Collection<String>)(before != null && current != null ? keys(current) : List.of()));
						QuestBook made = result.book();
						List<String> extra = new ArrayList<>();
						boolean again = known != null && current != null && BookJson.exists(into);
						if (again) {
							Path base = into.resolve("ftb-base");
							if (BookJson.exists(base)) {
								FtbMerge.Tally tally = new FtbMerge.Tally();
								made = FtbMerge.merge(BookJson.load(base), current, result.book(), tally);
								extra.add("");
								extra.add("CONVERTED AGAIN: the FTB book had changed. What was edited in CFHGui's book since the last conversion was kept.");
								extra.add(
									"  chapters added "
										+ tally.chaptersAdded
										+ ", removed "
										+ tally.chaptersRemoved
										+ "; quests added "
										+ tally.questsAdded
										+ ", removed "
										+ tally.questsRemoved
								);
								extra.add("  fields brought up to the FTB book: " + tally.fieldsUpdated + "; fields kept as edited in CFHGui: " + tally.fieldsKept);

								for (String note : tally.notes) {
									extra.add("  " + note);
								}
							} else {
								extra.add("");
								extra.add("CONVERTED AGAIN: the FTB book had changed. No record of the last conversion was found beside the book, so edits made");
								extra.add("in CFHGui's book could not be told apart and the new conversion stands as it is.");
							}
						}

						return new FtbAuto.Prepared(null, into, located, into.equals(bookDir), again, known, book, quick, result, made, extra);
					}
				}
			}
		}
	}

	public static synchronized FtbAuto.Done install(FtbAuto.Prepared p) throws IOException {
		if (p.said != null) {
			return p.said;
		} else {
			Path into = p.into;
			List<String> extra = new ArrayList<>(p.extra);
			String told = "converted";
			if (standing(into)) {
				String backup = backup(into);
				if (p.again) {
					extra.add("  the book as it was before: " + backup);
					told = "updated";
				} else {
					extra.add("");
					extra.add("The book that stood in " + into + " before was kept as " + backup + ".");
				}
			}

			Map<String, String> follow = new HashMap<>();
			if (p.server) {
				for (Entry<String, String> e : p.result.moved().entrySet()) {
					if (p.made.quest(e.getKey()) == null && p.made.quest(e.getValue()) != null) {
						follow.put(e.getKey(), e.getValue());
					}
				}
			}

			if (!follow.isEmpty()) {
				extra.add("  " + follow.size() + " quests stand in another chapter of the FTB book than before: what was done in them goes with them");
			}

			install(into, p.result, p.made, extra);
			Foreign.renamed(follow);
			if (!p.server) {
				told = "beside";
			}

			FtbAuto.State next = new FtbAuto.State();
			next.hash = p.book.hash;
			next.quick = p.quick;
			next.folder = p.located.toString();
			next.format = p.book.format.words;
			next.at = System.currentTimeMillis();
			next.chapters = p.made.chapters.size();
			next.quests = p.made.questCount();
			next.tasks = p.result.counts().writtenTasks;
			next.rewards = p.result.counts().writtenRewards;
			next.approximated = p.result.counts().approximated;
			next.needsMod = p.result.counts().needsMod;
			if (p.known != null) {
				next.progressHolders = p.known.progressHolders;
				next.progressQuests = p.known.progressQuests;
				next.progressTeams = p.known.progressTeams;

				for (String s : p.known.untold) {
					if (s.equals("progress")) {
						next.untold.add(s);
					}
				}
			}

			next.untold.add(told);
			write(into, next);
			thin(into.resolveSibling("quests-backups"));

			for (String line : p.result.summary()) {
				CFHGuiQuests.LOGGER.info("[CFHGui Quests] FTB Quests: {}", line);
			}

			return notice(told, next, into);
		}
	}

	private static Set<String> keys(QuestBook book) {
		Set<String> out = new HashSet<>();

		for (Chapter c : book.chapters.values()) {
			out.add(c.id);

			for (Quest q : c.quests) {
				out.add(c.id + "/" + q.id);
			}
		}

		return out;
	}

	private static void install(Path into, FtbImport.Result result, QuestBook made, List<String> extra) throws IOException {
		Path chapters = into.resolve("chapters");
		if (Files.isDirectory(chapters)) {
			try (Stream<Path> s = Files.list(chapters)) {
				for (Path p : s.toList()) {
					String name = p.getFileName().toString();
					if (name.endsWith(".json") && !made.chapters.containsKey(name.substring(0, name.length() - 5))) {
						Files.deleteIfExists(p);
					}
				}
			}
		}

		clear(into.resolve("lang"));
		List<String> report = new ArrayList<>(result.report());
		report.addAll(extra);
		FtbImport.write(
			into,
			new FtbImport.Result(made, report, result.summary(), result.counts(), result.ids(), result.translations(), result.hash(), result.format(), result.moved())
		);
		Path base = into.resolve("ftb-base");
		clear(base.resolve("chapters"));
		BookJson.saveHead(base, result.book());

		for (Chapter c : result.book().chapters.values()) {
			FtbImport.put(base.resolve("chapters").resolve(c.id + ".json"), FtbImport.compact(c) + "\n");
		}
	}

	private static void clear(Path dir) throws IOException {
		if (Files.isDirectory(dir)) {
			try (Stream<Path> s = Files.list(dir)) {
				for (Path p : s.toList()) {
					if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")) {
						Files.deleteIfExists(p);
					}
				}
			}
		}
	}

	private static String backup(Path dir) throws IOException {
		Path root = dir.resolveSibling("quests-backups");
		Path to = root.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

		for (int n = 2; Files.exists(to); n++) {
			to = root.resolve(to.getFileName() + "-" + n);
		}

		Files.createDirectories(to);

		try (Stream<Path> s = Files.walk(dir)) {
			for (Path p : s.toList()) {
				Path rel = dir.relativize(p);
				if (!rel.toString().isEmpty() && !rel.startsWith("ftb-base")) {
					if (Files.isDirectory(p)) {
						Files.createDirectories(to.resolve(rel.toString()));
					} else if (Files.isRegularFile(p)) {
						Files.copy(p, to.resolve(rel.toString()), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}

		return to.toString();
	}

	private static void thin(Path root) {
		if (Files.isDirectory(root)) {
			try {
				List<Path> ours;
				try (Stream<Path> s = Files.list(root)) {
					ours = s.filter(px -> Files.isDirectory(px, LinkOption.NOFOLLOW_LINKS) && px.getFileName().toString().matches("\\d{8}-\\d{6}(-\\d+)*"))
						.sorted(Comparator.comparing(px -> px.getFileName().toString()))
						.toList();
				}

				for (Path old : ours.subList(0, Math.max(0, ours.size() - 10))) {
					try (Stream<Path> s = Files.walk(old)) {
						for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
							Files.deleteIfExists(p);
						}
					}
				}
			} catch (IOException var11) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] older backups of the book in {} could not be removed: {}", root, var11.toString());
			}
		}
	}

	private static boolean standing(Path dir) {
		if (BookJson.exists(dir)) {
			return true;
		} else {
			for (String part : List.of("chapters", "lang")) {
				Path folder = dir.resolve(part);
				if (Files.isDirectory(folder)) {
					try (Stream<Path> s = Files.list(folder)) {
						if (s.anyMatch(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))) {
							return true;
						}
					} catch (IOException var9) {
						return true;
					}
				}
			}

			return false;
		}
	}

	private static boolean example(Path dir) {
		try {
			QuestBook bundled = BookJson.loadBundled();
			QuestBook here = BookJson.load(dir);
			if (!bundled.chapters.isEmpty() && bundled.chapters.keySet().equals(here.chapters.keySet()) && bundled.title.equals(here.title)) {
				for (Chapter c : bundled.chapters.values()) {
					if (!BookJson.GSON.toJson(c).equals(BookJson.GSON.toJson(here.chapter(c.id)))) {
						return false;
					}
				}

				return true;
			} else {
				return false;
			}
		} catch (RuntimeException var5) {
			return false;
		}
	}

	public static void renamedChapter(Path dir, String id, String to, Function<QuestBook, Set<String>> inBase) {
		FtbImport.IdMap ids = FtbImport.ids(dir);
		if (ids != null && ids.chapters.containsValue(id)) {
			ids.chapters.replaceAll((ftb, chapter) -> chapter.equals(id) ? to : chapter);
			String from = id + "/";
			String into = to + "/";
			follow(dir, ids, id, key -> key.startsWith(from) ? into + key.substring(from.length()) : key, inBase);
		}
	}

	public static void renamedQuest(Path dir, String key, String now, Function<QuestBook, Set<String>> inBase) {
		FtbImport.IdMap ids = FtbImport.ids(dir);
		if (ids != null && ids.quests.containsValue(key)) {
			follow(dir, ids, null, k -> k.equals(key) ? now : k, inBase);
		}
	}

	private static void follow(Path dir, FtbImport.IdMap ids, @Nullable String gone, UnaryOperator<String> keys, Function<QuestBook, Set<String>> inBase) {
		try {
			ids.quests.replaceAll((ftb, key) -> keys.apply(key));

			for (Map<String, String> places : List.of(ids.tasks, ids.rewards)) {
				places.replaceAll((ftb, at) -> {
					int hash = at.lastIndexOf(35);
					return (String)(hash < 0 ? at : keys.apply(at.substring(0, hash)) + at.substring(hash));
				});
			}

			FtbImport.put(dir.resolve("ftb-ids.json"), BookJson.PRETTY.toJson(ids) + "\n");
			Path base = dir.resolve("ftb-base");
			if (!BookJson.exists(base)) {
				return;
			}

			QuestBook kept = BookJson.load(base);

			for (String id : inBase.apply(kept)) {
				Chapter c = kept.chapter(id);
				if (c != null) {
					FtbImport.put(base.resolve("chapters").resolve(c.id + ".json"), FtbImport.compact(c) + "\n");
				}
			}

			if (gone != null) {
				Files.deleteIfExists(base.resolve("chapters").resolve(gone + ".json"));
			}
		} catch (Exception var10) {
			CFHGuiQuests.LOGGER
				.warn(
					"[CFHGui Quests] a rename could not be carried into what the conversion left beside the book in {} ({}): the next conversion of the FTB book will not know what was renamed",
					dir,
					var10.toString()
				);
		}
	}

	@Nullable
	public static FtbAuto.State state(Path dir) {
		Path file = dir.resolve("ftb-source.json");
		if (!Files.isRegularFile(file)) {
			return null;
		} else {
			try {
				FtbAuto.State s = (FtbAuto.State)BookJson.GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), FtbAuto.State.class);
				return s == null ? null : s.sanitize();
			} catch (Exception var3) {
				return null;
			}
		}
	}

	private static void write(Path dir, FtbAuto.State state) {
		try {
			FtbImport.put(dir.resolve("ftb-source.json"), BookJson.PRETTY.toJson(state) + "\n");
		} catch (IOException var3) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} could not be written: {}", dir.resolve("ftb-source.json"), var3.toString());
		}
	}

	public static void firstTick(MinecraftServer server) {
		if (progressDue) {
			progressDue = false;
			if (QuestsServerConfig.get().ftbProgress) {
				try {
					FtbAuto.Done done = progress(server, false);
					if (done != null) {
						CFHGuiQuests.LOGGER.info("[CFHGui Quests] {}", done.message().getString());
					}
				} catch (Throwable var2) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] progress made in FTB Quests could not be carried over", var2);
				}
			}
		}
	}

	@Nullable
	public static FtbAuto.Done progress(MinecraftServer server, boolean force) throws IOException {
		Path bookDir = Foreign.bookDir();
		QuestBook book = Foreign.book();
		BookIndex index = Foreign.index();
		FtbAuto.State state = state(bookDir);
		FtbImport.IdMap ids = FtbImport.ids(bookDir);
		if (book != null && index != null && state != null && ids != null) {
			Path world = server.getWorldPath(LevelResource.ROOT);
			Map<UUID, Path> files = FtbProgress.files(world);
			if (files.isEmpty()) {
				return force
					? FtbAuto.Done.fail("cfhgui_quests.command.import.progress.none", "This world has no progress of FTB Quests (no ftbquests folder in it).")
					: null;
			} else {
				Path markerFile = world.resolve("slate").resolve("quests").resolve("ftb-progress.json");
				Map<String, String> taken = marker(markerFile);
				Map<UUID, Path> todo = new LinkedHashMap<>();

				for (Entry<UUID, Path> e : files.entrySet()) {
					if (force || !taken.containsKey(e.getKey().toString())) {
						todo.put(e.getKey(), e.getValue());
					}
				}

				if (todo.isEmpty()) {
					return null;
				} else {
					Map<UUID, String> names = new HashMap<>();
					Map<UUID, Foreign.Party> parties = new HashMap<>();

					for (Foreign.Party p : FtbProgress.parties(world, names)) {
						parties.put(p.id(), p);
					}

					List<Foreign.Party> founding = new ArrayList<>();

					for (UUID id : todo.keySet()) {
						if (parties.containsKey(id)) {
							founding.add(parties.get(id));
						}
					}

					Map<UUID, UUID> teams = QuestsServerConfig.get().teamProgress ? Foreign.foundTeams(server, founding) : Map.of();
					int holders = 0;
					int quests = 0;
					int done = 0;
					int unknown = 0;
					int unread = 0;
					int became = 0;
					int alone = 0;
					int apart = 0;

					for (Entry<UUID, Path> ex : todo.entrySet()) {
						Foreign.Party party = parties.get(ex.getKey());
						FtbProgress.Team team = FtbProgress.read(
							ex.getKey(), ex.getValue(), ids, index, (Collection<UUID>)(party == null ? List.of(ex.getKey()) : party.members().keySet())
						);
						if (team == null) {
							unread++;
						} else {
							unknown += team.unknown();
							if (!team.quests().isEmpty()) {
								if (party == null) {
									quests += Foreign.absorb(ex.getKey(), team.quests());
									holders++;
								} else {
									UUID cfhId = teams.get(ex.getKey());
									int in = 0;
									int out = 0;

									for (UUID member : party.members().keySet()) {
										if (cfhId != null && cfhId.equals(Foreign.holderOf(member))) {
											in++;
										} else {
											quests += Foreign.absorb(member, team.quests());
											holders++;
											out++;
										}
									}

									if (in > 0) {
										quests += Foreign.absorb(cfhId, team.quests());
										holders++;
										became++;
										apart += out;
									} else {
										alone++;
									}
								}

								for (Foreign.Quest q : team.quests()) {
									if (q.done()) {
										done++;
									}
								}
							}

							taken.put(ex.getKey().toString(), team.fingerprint());
						}
					}

					FtbImport.put(markerFile, BookJson.PRETTY.toJson(new TreeMap<>(taken)) + "\n");
					List<String> lines = new ArrayList<>();
					lines.add("");
					lines.add("PROGRESS carried over from FTB Quests (" + world + ", " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "):");
					lines.add(
						"  "
							+ todo.size()
							+ " team files read, "
							+ holders
							+ " holders of progress here took them in: "
							+ quests
							+ " quests with progress, "
							+ done
							+ " of them done"
					);
					lines.add(
						"  "
							+ became
							+ " parties of FTB Teams became teams of CFHGui Teams"
							+ (
								alone > 0
									? "; "
										+ alone
										+ " parties did not (no CFHGui Teams, teams that do not share their quests on this server, a leader who is in a team with others, or the team could not be founded): each member keeps the party's progress as their own"
									: ""
							)
					);
					if (apart > 0) {
						lines.add(
							"  "
								+ apart
								+ " members of those parties are not in their party's team (it had no room for them, or they have another team): each keeps the party's progress as their own"
						);
					}

					if (unknown > 0) {
						lines.add("  " + unknown + " entries named a quest, task or reward that is not in the book any more and were left out");
					}

					if (unread > 0) {
						lines.add("  " + unread + " team files could not be read and were left as they are (they are tried again at the next start)");
					}

					try {
						Files.writeString(
							bookDir.resolve("ftb-report.txt"), String.join("\n", lines) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND
						);
					} catch (IOException var33) {
						CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the report could not be added to: {}", var33.toString());
					}

					state.progressHolders = holders;
					state.progressQuests = done;
					state.progressTeams = became;
					if (!state.untold.contains("progress")) {
						state.untold.add("progress");
					}

					write(bookDir, state);
					tell(server);
					return notice("progress", state, bookDir);
				}
			}
		} else {
			return force
				? FtbAuto.Done.fail(
					"cfhgui_quests.command.import.progress.no_book", "The server's quest book is not a converted FTB Quests book, so there is nothing to carry progress into."
				)
				: null;
		}
	}

	private static Map<String, String> marker(Path file) {
		Map<String, String> out = new HashMap<>();
		if (!Files.isRegularFile(file)) {
			return out;
		} else {
			try {
				Map<?, ?> read = (Map<?, ?>)BookJson.GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Map.class);
				if (read != null) {
					for (Entry<?, ?> e : read.entrySet()) {
						out.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
					}
				}
			} catch (Exception var5) {
			}

			return out;
		}
	}

	public static String shown(Path path) {
		try {
			Path game = CFHGuiPlatform.get().gameDir().toAbsolutePath().normalize();
			Path full = path.toAbsolutePath().normalize();
			if (full.startsWith(game) && !full.equals(game)) {
				return game.relativize(full).toString().replace('\\', '/');
			}
		} catch (RuntimeException var3) {
		}

		return path.toString();
	}

	private static FtbAuto.Done notice(String what, FtbAuto.State s, Path dir) {
		String report = shown(dir.resolve("ftb-report.txt"));

		return switch (what) {
			case "updated" -> FtbAuto.Done.ok(
			"cfhgui_quests.ftb.updated",
			"The FTB Quests book had changed and was converted again: %s chapters, %s quests. What was edited in CFHGui's book was kept, and a backup of the book as it was. Report: %s",
			s.chapters,
			s.quests,
			report
		);
			case "beside" -> FtbAuto.Done.ok(
			"cfhgui_quests.ftb.beside",
			"An FTB Quests book was found and converted (%s chapters, %s quests), but this server has a quest book of its own, which was not touched. The conversion is in %s; \"/cfhquests import ftb install\" makes it the server's book.",
			s.chapters,
			s.quests,
			shown(dir)
		);
			case "progress" -> FtbAuto.Done.ok(
			"cfhgui_quests.ftb.progress",
			"Progress made in FTB Quests was carried over: %s teams and players, %s quests done; %s parties of FTB Teams became teams.",
			s.progressHolders,
			s.progressQuests,
			s.progressTeams
		);
			default -> FtbAuto.Done.ok(
			"cfhgui_quests.ftb.converted",
			"The FTB Quests book was converted into CFHGui's quest book: %s chapters, %s quests, %s tasks, %s rewards; %s things were approximated, %s need another mod. Report: %s",
			s.chapters,
			s.quests,
			s.tasks,
			s.rewards,
			s.approximated,
			s.needsMod,
			report
		);
		};
	}

	public static void tell(MinecraftServer server) {
		for (Path dir : List.of(Foreign.bookDir(), besideDir())) {
			FtbAuto.State state = state(dir);
			if (state != null && !state.untold.isEmpty()) {
				boolean anyone = false;

				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (Foreign.mayEdit(player)) {
						anyone = true;
					}
				}

				if (anyone) {
					for (String what : state.untold) {
						Component line = Component.literal("[CFHGui Quests] ")
							.withStyle(ChatFormatting.GOLD)
							.append(notice(what, state, dir).message().withStyle(ChatFormatting.WHITE));
						Foreign.tellEditors(server, line);
					}

					state.untold.clear();
					write(dir, state);
				}
			}
		}
	}

	public static void joined(ServerPlayer player) {
		try {
			if (player.getServer() != null && Foreign.mayEdit(player)) {
				tell(player.getServer());
			}
		} catch (Throwable var2) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the notice about the converted book could not be given: {}", var2.toString());
		}
	}

	public static List<FtbAuto.Done> status() {
		List<FtbAuto.Done> out = new ArrayList<>();
		Path from = ftbDir();
		Path located = FtbBook.locate(from);
		out.add(
			located == null
				? FtbAuto.Done.ok("cfhgui_quests.ftb.status.none", "No FTB Quests book in %s.", shown(from))
				: FtbAuto.Done.ok("cfhgui_quests.ftb.status.found", "FTB Quests book: %s", shown(located))
		);
		FtbAuto.State state = state(Foreign.bookDir());
		if (state != null) {
			out.add(
				FtbAuto.Done.ok(
					"cfhgui_quests.ftb.status.converted",
					"The server's book is a conversion of it: %s chapters, %s quests, %s approximated, %s need another mod. Up to date: %s",
					state.chapters,
					state.quests,
					state.approximated,
					state.needsMod,
					word(located != null && state.quick.equals(FtbBook.quick(located)), "yes", "no")
				)
			);
		} else if (state(besideDir()) != null) {
			out.add(FtbAuto.Done.ok("cfhgui_quests.ftb.status.beside", "The server has a book of its own; the conversion is kept beside it in %s.", shown(besideDir())));
		} else {
			out.add(FtbAuto.Done.ok("cfhgui_quests.ftb.status.not", "The server's book is not a conversion of an FTB Quests book."));
		}

		out.add(
			FtbAuto.Done.ok(
				"cfhgui_quests.ftb.status.mod",
				"FTB Quests is %s. Converting by itself: %s. Carrying progress over: %s.",
				word(CFHGuiPlatform.get().isModLoaded("ftbquests"), "installed", "missing"),
				word(QuestsServerConfig.get().ftbConvert, "on", "off"),
				word(QuestsServerConfig.get().ftbProgress, "on", "off")
			)
		);
		return out;
	}

	public static List<String> selfCheck(Consumer<Boolean> each) {
		List<String> failures = new ArrayList<>();
		BiConsumer<String, Boolean> is = (what, ok) -> {
			each.accept(ok);
			if (!ok) {
				failures.add(what);
			}
		};
		Path tmp = null;

		Object r1;
		try {
			tmp = Files.createTempDirectory("cfhgui-quests-ftb");
			Path old = tmp.resolve("old").resolve("quests");
			FtbImport.put(
				old.resolve("data.snbt"), "{\n\tversion: 13\n\temergency_items: [{ id: \"minecraft:bread\", Count: 4b }]\n\temergency_items_cooldown: 120\n}\n"
			);
			String one = "\t\t{\n\t\t\tid: \"01\"\n\t\t\ttitle: \"%s\"\n\t\t\tx: 0.0d\n\t\t\ty: 0.0d\n\t\t\tdescription: [\"{image:mod:textures/x.png width:100 height:50}\", \"{@pagebreak}\"]\n\t\t\ttasks: [{ id: \"11\", type: \"item\", item: \"minecraft:stone\", count: %dL }]\n\t\t\trewards: [{ id: \"21\", type: \"item\", item: \"minecraft:apple\", count: 2 }, { id: \"22\", type: \"xp\", xp: 10, team_reward: true }]\n\t\t}\n";
			String two = "\t\t{\n\t\t\tid: \"02\"\n\t\t\ttitle: \"Two\"\n\t\t\tx: 2.0d\n\t\t\ty: 0.0d\n\t\t\tdependencies: [\"01\"]\n\t\t\ttasks: [{ id: \"12\", type: \"checkmark\" }]\n\t\t}\n";
			String three = "\t\t{\n\t\t\tid: \"03\"\n\t\t\ttitle: \"Three\"\n\t\t\tx: 4.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"13\", type: \"checkmark\" }]\n\t\t}\n";
			String head = "{\n\tid: \"0A\"\n\tfilename: \"a\"\n\ttitle: \"Alpha\"\n\tquests: [\n";
			String tail = "\t]\n}\n";
			FtbImport.put(
				old.resolve("chapters").resolve("a.snbt"),
				"{\n\tid: \"0A\"\n\tfilename: \"a\"\n\ttitle: \"Alpha\"\n\tquests: [\n"
					+ String.format(
						"\t\t{\n\t\t\tid: \"01\"\n\t\t\ttitle: \"%s\"\n\t\t\tx: 0.0d\n\t\t\ty: 0.0d\n\t\t\tdescription: [\"{image:mod:textures/x.png width:100 height:50}\", \"{@pagebreak}\"]\n\t\t\ttasks: [{ id: \"11\", type: \"item\", item: \"minecraft:stone\", count: %dL }]\n\t\t\trewards: [{ id: \"21\", type: \"item\", item: \"minecraft:apple\", count: 2 }, { id: \"22\", type: \"xp\", xp: 10, team_reward: true }]\n\t\t}\n",
						"One",
						4
					)
					+ "\t\t{\n\t\t\tid: \"02\"\n\t\t\ttitle: \"Two\"\n\t\t\tx: 2.0d\n\t\t\ty: 0.0d\n\t\t\tdependencies: [\"01\"]\n\t\t\ttasks: [{ id: \"12\", type: \"checkmark\" }]\n\t\t}\n\t]\n}\n"
			);
			FtbBook first = FtbBook.read(tmp.resolve("old"));
			is.accept("a book with its words in the chapter files is read as the format of 1.16 to 1.20", first != null && first.format == FtbBook.Format.INLINE);
			if (first != null) {
				FtbImport.Result r1x = FtbImport.convert(first, null);
				QuestBook base = r1x.book();
				is.accept("its quests have ids from their titles", base.quest("a/one") != null && base.quest("a/two") != null);
				is.accept(
					"a picture and a page break in a description are written in CFHGui's marks",
					base.quest("a/one") != null && base.quest("a/one").description.equals(List.of("![](mod:textures/x.png){w=100 h=50}", "---"))
				);
				is.accept(
					"the emergency items and their rest carry over",
					base.emergency.size() == 1 && base.emergency.get(0).item.contains("bread") && base.emergency.get(0).count == 4 && base.emergencyCooldownSeconds == 120
				);
				QuestBook current = copy(base);
				current.quest("a/one").icon = "minecraft:diamond";
				Quest mine = new Quest();
				mine.id = "mine";
				mine.title = "Mine";
				current.chapter("a").quests.add(mine);
				current.chapter("a").quests.removeIf(q -> q.id.equals("two"));
				FtbImport.put(
					old.resolve("chapters").resolve("a.snbt"),
					"{\n\tid: \"0A\"\n\tfilename: \"a\"\n\ttitle: \"Alpha\"\n\tquests: [\n"
						+ String.format(
							"\t\t{\n\t\t\tid: \"01\"\n\t\t\ttitle: \"%s\"\n\t\t\tx: 0.0d\n\t\t\ty: 0.0d\n\t\t\tdescription: [\"{image:mod:textures/x.png width:100 height:50}\", \"{@pagebreak}\"]\n\t\t\ttasks: [{ id: \"11\", type: \"item\", item: \"minecraft:stone\", count: %dL }]\n\t\t\trewards: [{ id: \"21\", type: \"item\", item: \"minecraft:apple\", count: 2 }, { id: \"22\", type: \"xp\", xp: 10, team_reward: true }]\n\t\t}\n",
							"Uno",
							8
						)
						+ "\t\t{\n\t\t\tid: \"02\"\n\t\t\ttitle: \"Two\"\n\t\t\tx: 2.0d\n\t\t\ty: 0.0d\n\t\t\tdependencies: [\"01\"]\n\t\t\ttasks: [{ id: \"12\", type: \"checkmark\" }]\n\t\t}\n\t\t{\n\t\t\tid: \"03\"\n\t\t\ttitle: \"Three\"\n\t\t\tx: 4.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"13\", type: \"checkmark\" }]\n\t\t}\n\t]\n}\n"
				);
				FtbBook second = FtbBook.read(tmp.resolve("old"));
				FtbImport.Result r2 = FtbImport.convert(second, r1x.ids());
				is.accept("a book that changed has another fingerprint", !second.hash.equals(first.hash));
				is.accept(
					"converted again, a quest keeps its id whatever its title has become", r2.book().quest("a/one") != null && r2.book().quest("a/one").title.equals("Uno")
				);
				FtbMerge.Tally tally = new FtbMerge.Tally();
				QuestBook merged = FtbMerge.merge(base, current, r2.book(), tally);
				Quest m = merged.quest("a/one");
				is.accept("brought together: what was edited in CFHGui stays", m != null && m.icon.equals("minecraft:diamond"));
				is.accept("brought together: what changed in FTB's book arrives", m != null && m.title.equals("Uno") && m.tasks.size() == 1 && m.tasks.get(0).count == 8L);
				is.accept(
					"brought together: a quest written in CFHGui stays, a new one of FTB's is added", merged.quest("a/mine") != null && merged.quest("a/three") != null
				);
				is.accept(
					"brought together: a quest deleted in CFHGui stays deleted, and the report says so",
					merged.quest("a/two") == null && tally.notes.stream().anyMatch(n -> n.contains("a/two"))
				);
				is.accept("brought together: what was done is counted", tally.questsAdded == 1 && tally.fieldsKept >= 1 && tally.fieldsUpdated >= 2);
				FtbMerge.Tally none = new FtbMerge.Tally();
				QuestBook same = FtbMerge.merge(base, copy(base), r2.book(), none);
				is.accept(
					"a book nobody edited becomes the new conversion",
					BookJson.GSON.toJson(same.chapter("a")).equals(BookJson.GSON.toJson(r2.book().chapter("a"))) && none.fieldsKept == 0
				);
				Path kept = tmp.resolve("slate");
				install(kept, r1x, r1x.book(), List.of());
				renamedQuest(kept, "a/one", "a/first", b -> {
					b.quest("a/one").id = "first";
					return Set.of("a");
				});
				FtbImport.IdMap renamed = FtbImport.ids(kept);
				is.accept(
					"a quest renamed in CFHGui's editor goes by its new id in what the conversion left beside the book",
					renamed != null
						&& "a/first".equals(renamed.quests.get("01"))
						&& "a/first#0".equals(renamed.tasks.get("11"))
						&& "a/first#1".equals(renamed.rewards.get("22"))
						&& "a/two".equals(renamed.quests.get("02"))
						&& BookJson.load(kept.resolve("ftb-base")).quest("a/first") != null
						&& BookJson.load(kept.resolve("ftb-base")).quest("a/one") == null
				);
				is.accept(
					"and the next conversion finds it there", renamed != null && FtbImport.convert(FtbBook.read(tmp.resolve("old")), renamed).book().quest("a/first") != null
				);
				String mineToo = "\t\t{\n\t\t\tid: \"04\"\n\t\t\ttitle: \"Mine\"\n\t\t\tx: 6.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"14\", type: \"checkmark\" }]\n\t\t}\n";
				String threeAgain = "\t\t{\n\t\t\tid: \"05\"\n\t\t\ttitle: \"Three\"\n\t\t\tx: 8.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"15\", type: \"checkmark\" }]\n\t\t}\n";
				String alpha = "{\n\tid: \"0A\"\n\tfilename: \"alpha\"\n\ttitle: \"Alpha\"\n\tquests: [\n";
				String beta = "{\n\tid: \"0B\"\n\tfilename: \"a\"\n\ttitle: \"Beta\"\n\tquests: [\n";
				FtbImport.put(
					old.resolve("chapters").resolve("alpha.snbt"),
					"{\n\tid: \"0A\"\n\tfilename: \"alpha\"\n\ttitle: \"Alpha\"\n\tquests: [\n"
						+ String.format(
							"\t\t{\n\t\t\tid: \"01\"\n\t\t\ttitle: \"%s\"\n\t\t\tx: 0.0d\n\t\t\ty: 0.0d\n\t\t\tdescription: [\"{image:mod:textures/x.png width:100 height:50}\", \"{@pagebreak}\"]\n\t\t\ttasks: [{ id: \"11\", type: \"item\", item: \"minecraft:stone\", count: %dL }]\n\t\t\trewards: [{ id: \"21\", type: \"item\", item: \"minecraft:apple\", count: 2 }, { id: \"22\", type: \"xp\", xp: 10, team_reward: true }]\n\t\t}\n",
							"Uno",
							8
						)
						+ "\t\t{\n\t\t\tid: \"04\"\n\t\t\ttitle: \"Mine\"\n\t\t\tx: 6.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"14\", type: \"checkmark\" }]\n\t\t}\n\t]\n}\n"
				);
				FtbImport.put(
					old.resolve("chapters").resolve("a.snbt"),
					"{\n\tid: \"0B\"\n\tfilename: \"a\"\n\ttitle: \"Beta\"\n\tquests: [\n\t\t{\n\t\t\tid: \"02\"\n\t\t\ttitle: \"Two\"\n\t\t\tx: 2.0d\n\t\t\ty: 0.0d\n\t\t\tdependencies: [\"01\"]\n\t\t\ttasks: [{ id: \"12\", type: \"checkmark\" }]\n\t\t}\n\t]\n}\n"
				);
				FtbImport.Result later = FtbImport.convert(FtbBook.read(tmp.resolve("old")), r2.ids(), keys(merged));
				is.accept(
					"a chapter keeps its id whatever a new chapter is called", "a".equals(later.ids().chapters.get("0A")) && "a_2".equals(later.ids().chapters.get("0B"))
				);
				is.accept(
					"a new quest of FTB's does not take the id of a quest written in CFHGui's book",
					"a/mine_2".equals(later.ids().quests.get("04")) && later.book().quest("a/mine") == null
				);
				is.accept("a quest that stands in another chapter now is known to have moved", "a_2/two".equals(later.moved().get("a/two")) && later.moved().size() == 1);
				is.accept(
					"what a quest that is gone went by is remembered",
					later.ids().retired.contains("a/three") && later.ids().retired.contains("a/two") && !later.ids().retired.contains("a/one")
				);
				FtbImport.put(
					old.resolve("chapters").resolve("alpha.snbt"),
					"{\n\tid: \"0A\"\n\tfilename: \"alpha\"\n\ttitle: \"Alpha\"\n\tquests: [\n"
						+ String.format(
							"\t\t{\n\t\t\tid: \"01\"\n\t\t\ttitle: \"%s\"\n\t\t\tx: 0.0d\n\t\t\ty: 0.0d\n\t\t\tdescription: [\"{image:mod:textures/x.png width:100 height:50}\", \"{@pagebreak}\"]\n\t\t\ttasks: [{ id: \"11\", type: \"item\", item: \"minecraft:stone\", count: %dL }]\n\t\t\trewards: [{ id: \"21\", type: \"item\", item: \"minecraft:apple\", count: 2 }, { id: \"22\", type: \"xp\", xp: 10, team_reward: true }]\n\t\t}\n",
							"Uno",
							8
						)
						+ "\t\t{\n\t\t\tid: \"04\"\n\t\t\ttitle: \"Mine\"\n\t\t\tx: 6.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"14\", type: \"checkmark\" }]\n\t\t}\n\t\t{\n\t\t\tid: \"05\"\n\t\t\ttitle: \"Three\"\n\t\t\tx: 8.0d\n\t\t\ty: 0.0d\n\t\t\ttasks: [{ id: \"15\", type: \"checkmark\" }]\n\t\t}\n\t]\n}\n"
				);
				FtbImport.Result latest = FtbImport.convert(FtbBook.read(tmp.resolve("old")), later.ids());
				is.accept(
					"and is not given to a new quest that happens to be called the same",
					"a/three_2".equals(latest.ids().quests.get("05")) && latest.ids().retired.contains("a/three")
				);
				UUID p1 = new UUID(1L, 1L);
				UUID p2 = new UUID(1L, 2L);
				UUID p3 = new UUID(1L, 3L);
				UUID party = new UUID(9L, 9L);
				Path world = tmp.resolve("world");
				String undashed1 = p1.toString().replace("-", "");
				String undashed3 = p3.toString().replace("-", "");
				String nobody = "0".repeat(32);
				FtbImport.put(
					world.resolve("ftbquests").resolve(party + ".snbt"),
					"{\n\tversion: 1\n\tname: \"The Crew\"\n\ttask_progress: {\n\t\t11: 3L\n\t\tFFFF: 2L\n\t}\n\tstarted: { 01: 5L }\n\tcompleted: { 02: 9L, 12: 9L, 0A: 9L }\n\tclaimed_rewards: {\n\t\t\""
						+ undashed1
						+ ":21\": 7L\n\t\t\""
						+ nobody
						+ ":22\": 8L\n\t}\n}\n"
				);
				FtbImport.put(world.resolve("ftbteams").resolve("player").resolve(p1 + ".snbt"), "{\n\tid: \"" + p1 + "\"\n\ttype: \"player\"\n\tplayer_name: \"Ada\"\n}\n");
				FtbImport.put(
					world.resolve("ftbteams").resolve("party").resolve(party + ".snbt"),
					"{\n\tid: \""
						+ party
						+ "\"\n\ttype: \"party\"\n\tranks: {\n\t\t"
						+ p1
						+ ": \"owner\"\n\t\t"
						+ p2
						+ ": \"officer\"\n\t\t"
						+ p3
						+ ": \"invited\"\n\t}\n\tproperties: {\n\t\t\"ftbteams:display_name\": \"The Crew\"\n\t\t\"ftbteams:color\": \"#5968FF\"\n\t}\n}\n"
				);
				Map<UUID, Path> files = FtbProgress.files(world);
				is.accept("a world's FTB Quests files are found by team", files.size() == 1 && files.containsKey(party));
				FtbProgress.Team team = FtbProgress.read(party, files.get(party), r2.ids(), new BookIndex(r2.book()), List.of(p1, p2));
				is.accept("a team's file is read", team != null && team.quests().size() == 2);
				if (team != null && team.quests().size() == 2) {
					Foreign.Quest q1 = team.quests().get(0);
					Foreign.Quest q2 = team.quests().get(1);
					is.accept(
						"a task's count comes over by the task it became",
						q1.key().equals("a/one") && q1.tasks().length == 1 && q1.tasks()[0] == 3L && !q1.done() && q1.startedAt() == 5L
					);
					is.accept("a done quest is done", q2.key().equals("a/two") && q2.done() && q2.doneAt() == 9L);
					is.accept(
						"a reward claimed by a player is that player's claim, one claimed for the team the team's",
						Long.valueOf(1L).equals(q1.claims().get(p1.toString())) && q1.teamClaims() == 2L
					);
					is.accept("what names nothing in the book any more is counted and left out", team.unknown() == 1);
				}

				Map<UUID, String> names = new HashMap<>();
				List<Foreign.Party> parties = FtbProgress.parties(world, names);
				is.accept(
					"FTB Teams' parties are read: name, colour, leader, officers; the invited are no members",
					parties.size() == 1
						&& parties.get(0).name().equals("The Crew")
						&& parties.get(0).color() == 5859583
						&& parties.get(0).owner().equals(p1)
						&& parties.get(0).members().size() == 2
						&& parties.get(0).members().get(p2).officer()
						&& "Ada".equals(names.get(p1))
						&& parties.get(0).members().get(p1).name().equals("Ada")
				);
				Path own = world.resolve("ftbquests").resolve(p3 + ".snbt");
				FtbImport.put(
					own, "{\n\tversion: 1\n\tcompleted: { 01: 9L }\n\tclaimed_rewards: {\n\t\t\"" + undashed3 + ":21\": 7L\n\t\t\"" + nobody + ":22\": 8L\n\t}\n}\n"
				);
				FtbProgress.Team alone = FtbProgress.read(p3, own, r2.ids(), new BookIndex(r2.book()), List.of(p3));
				is.accept(
					"in a player's own file, what they claimed is their own claim and what is a team's the team's",
					alone != null
						&& alone.quests().size() == 1
						&& alone.quests().get(0).done()
						&& Long.valueOf(1L).equals(alone.quests().get(0).claims().get(p3.toString()))
						&& alone.quests().get(0).teamClaims() == 2L
				);
				Path legacy = tmp.resolve("legacy").resolve("normal");
				FtbImport.put(legacy.resolve("file.snbt"), "{\n\tversion: 9,\n\temergency_items_cooldown: \"5m\"\n}\n");
				FtbImport.put(legacy.resolve("chapters").resolve("index.snbt"), "{\n\tindex: [\n\t\t\"aaaa0001\"\n\t]\n}\n");
				FtbImport.put(legacy.resolve("chapters").resolve("aaaa0001").resolve("chapter.snbt"), "{\n\ttitle: \"Old Days\",\n\ticon: \"minecraft:stone 1 2\"\n}\n");
				FtbImport.put(
					legacy.resolve("chapters").resolve("aaaa0001").resolve("00000001.snbt"),
					"{\n\tx: 1.0d,\n\ty: 2.0d,\n\ttitle: \"Planks\",\n\tdescription: \"A line under it\",\n\ttext: [\n\t\t\"Long ago\"\n\t],\n\ttasks: [{\n\t\tuid: \"0000aaaa\",\n\t\ttype: \"item\",\n\t\titems: [{\n\t\t\titem: \"minecraft:planks 3 1\"\n\t\t}, {\n\t\t\tid: \"minecraft:bone\",\n\t\t\tCount: 1b\n\t\t}]\n\t}, {\n\t\tuid: \"0000aaab\",\n\t\ttype: \"dimension\",\n\t\tdim: -1\n\t}]\n}\n"
				);
				FtbBook third = FtbBook.read(tmp.resolve("legacy"));
				is.accept("a book with a folder a chapter is read as the format of 1.12", third != null && third.format == FtbBook.Format.LEGACY);
				if (third != null) {
					FtbImport.Result r3 = FtbImport.convert(third, null);
					Quest planks = r3.book().quest("aaaa0001/planks");
					is.accept(
						"its quests are converted: place, title, the line under it, its text",
						planks != null && planks.x == 1.0F && planks.y == 2.0F && planks.subtitle.equals("A line under it") && planks.description.equals(List.of("Long ago"))
					);
					is.accept(
						"an item written as \"id count meta\" is an item to look for, several of them any of them",
						planks != null
							&& planks.tasks.size() == 2
							&& planks.tasks.get(0).is("item")
							&& planks.tasks.get(0).item.contains("planks")
							&& planks.tasks.get(0).item.contains(" | minecraft:bone")
							&& planks.tasks.get(0).count == 3L
					);
					is.accept(
						"a dimension by its old number has its name", planks != null && planks.tasks.size() == 2 && planks.tasks.get(1).dimension.equals("minecraft:the_nether")
					);
					is.accept("a rest written as \"5m\" is five minutes", r3.book().emergencyCooldownSeconds == 300);
				}

				FtbItems items = new FtbItems(3465, true, new FtbItems.Notes() {
					@Override
					public void approx(String kind, String place) {
					}

					@Override
					public void turn(String kind, String place) {
					}

					@Override
					public void needs(String kind, String place) {
					}
				});
				is.accept(
					"a filter of items and tags is written out, an old common tag by its new name",
					items.filter("or(item(minecraft:coal)item_tag(forge:ingots/iron))", "x").equals("minecraft:coal | #c:ingots/iron")
				);
				is.accept(
					"a filter that asks more is kept in its own words for FTB Filter System",
					items.filter("and(item_tag(c:ores)not(item(minecraft:coal_ore)))", "x").startsWith("ftbfilter:and(")
				);
				return failures;
			}

			r1 = failures;
		} catch (Throwable var69) {
			each.accept(false);
			failures.add("the FTB converter's own checks: stopped by " + var69);
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] selftest: the FTB converter's checks threw", var69);
			return failures;
		} finally {
			if (tmp != null) {
				try (Stream<Path> s = Files.walk(tmp)) {
					for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
						Files.deleteIfExists(p);
					}
				} catch (IOException var68) {
				}
			}
		}

		return (List<String>)r1;
	}

	private static QuestBook copy(QuestBook book) {
		QuestBook c = (QuestBook)BookJson.GSON.fromJson(BookJson.GSON.toJson(book), QuestBook.class);

		for (Chapter chapter : book.chapters.values()) {
			c.chapters.put(chapter.id, BookJson.copy(chapter));
		}

		return c.sanitize();
	}

	private static Component word(boolean which, String yes, String no) {
		String w = which ? yes : no;
		return Foreign.msg("cfhgui_quests.word." + w, w.equals("missing") ? "not installed" : w);
	}

	private FtbAuto() {
	}

	public static record Done(boolean ok, String key, String english, Object[] args) {
		static FtbAuto.Done ok(String key, String english, Object... args) {
			return new FtbAuto.Done(true, key, english, args);
		}

		static FtbAuto.Done fail(String key, String english, Object... args) {
			return new FtbAuto.Done(false, key, english, args);
		}

		public MutableComponent message() {
			return Foreign.msg(this.key, this.english, this.args);
		}
	}

	public static final class Prepared {
		@Nullable
		private final FtbAuto.Done said;
		private final Path into;
		private final Path located;
		private final boolean server;
		private final boolean again;
		@Nullable
		private final FtbAuto.State known;
		private final FtbBook book;
		private final String quick;
		private final FtbImport.Result result;
		private final QuestBook made;
		private final List<String> extra;

		private Prepared(FtbAuto.Done said) {
			this(said, null, null, false, false, null, null, "", null, null, List.of());
		}

		private Prepared(
			@Nullable FtbAuto.Done said,
			Path into,
			Path located,
			boolean server,
			boolean again,
			@Nullable FtbAuto.State known,
			FtbBook book,
			String quick,
			FtbImport.Result result,
			QuestBook made,
			List<String> extra
		) {
			this.said = said;
			this.into = into;
			this.located = located;
			this.server = server;
			this.again = again;
			this.known = known;
			this.book = book;
			this.quick = quick;
			this.result = result;
			this.made = made;
			this.extra = extra;
		}
	}

	public static final class State {
		public String hash = "";
		public String quick = "";
		public String folder = "";
		public String format = "";
		public long at;
		public int chapters;
		public int quests;
		public int tasks;
		public int rewards;
		public int approximated;
		public int needsMod;
		public List<String> untold = new ArrayList<>();
		public int progressHolders;
		public int progressQuests;
		public int progressTeams;

		FtbAuto.State sanitize() {
			if (this.hash == null) {
				this.hash = "";
			}

			if (this.quick == null) {
				this.quick = "";
			}

			if (this.folder == null) {
				this.folder = "";
			}

			if (this.format == null) {
				this.format = "";
			}

			if (this.untold == null) {
				this.untold = new ArrayList<>();
			}

			return this;
		}
	}
}
