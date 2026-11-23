package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.server.BookDoor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class AdvService {
	public static final String NO_TREE = "no_tree";
	public static final String NOTHING_TO_UNDO = "nothing_to_undo";
	public static final String EDITED_SINCE = "edited_since";
	public static final String BAD_ID = "bad_id";
	public static final String ID_TWICE = "id_twice";
	public static final String TOO_BIG = "too_big";
	private static final int UNDO_STEPS = 8;
	private static final Deque<AdvService.Step> UNDO = new ArrayDeque<>();
	@Nullable
	private static MinecraftServer undoOf;

	private static Deque<AdvService.Step> steps(MinecraftServer server) {
		if (undoOf != server) {
			undoOf = server;
			UNDO.clear();
		}

		return UNDO;
	}

	public static AdvService.Trees trees(MinecraftServer server) {
		AdvService.Trees out = new AdvService.Trees();
		QuestBook book = BookDoor.book();
		Map<String, String> made = new LinkedHashMap<>();
		Map<String, AdvStore.Kept> kept = AdvStore.all();

		for (Entry<String, AdvStore.Kept> e : kept.entrySet()) {
			if (book != null && book.chapter(e.getKey()) != null) {
				made.putIfAbsent(e.getValue().root, e.getKey());
			}
		}

		for (AdvancementNode root : AdvTrees.roots(server)) {
			AdvTrees.Tree t = AdvTrees.describe(server, root);
			t.exists = book != null && book.chapter(t.chapter) != null;
			String chapter = made.get(t.root);
			if (chapter != null) {
				t.converted = chapter;
				t.options = kept.get(chapter).options;
			}

			out.trees.add(t);
		}

		if (book != null) {
			for (QuestBook.Group g : book.groups) {
				out.groups.put(g.id, g.title);
			}
		}

		AdvService.Step last = steps(server).peek();
		if (last != null) {
			for (AdvService.Was was : last.chapters()) {
				out.undo.add(was.id());
			}
		}

		return out;
	}

	@Nullable
	private static AdvService.Plan plan(AdvConverter converter, AdvancementNode root, AdvOptions options, QuestBook book, Set<String> taken, String[] problem) {
		ResourceLocation rootId = root.holder().id();
		AdvOptions.Name name = options.names.get(rootId.toString());
		String id = name != null && !name.id.isEmpty() ? name.id : AdvConverter.chapterId(rootId);
		if (!Ids.valid(id)) {
			problem[0] = "bad_id";
			problem[1] = id;
			return null;
		} else {
			Chapter standing = book.chapter(id);
			boolean foreign = standing != null && "merge".equals(options.exists) && !madeFrom(standing, rootId);
			boolean copy = standing != null && ("copy".equals(options.exists) || foreign);
			if (copy) {
				String base = id.replaceFirst("_\\d+$", "");

				for (int n = 2; book.chapter(id) != null || taken.contains(id); n++) {
					String tail = "_" + n;
					id = (base.length() + tail.length() > 64 ? base.substring(0, 64 - tail.length()) : base) + tail;
				}
			}

			if (!taken.add(id)) {
				problem[0] = "id_twice";
				problem[1] = id;
				return null;
			} else {
				AdvConverter.Made made = converter.convert(root, options, id, name == null ? "" : name.title);
				Chapter next = made.chapter();
				AdvAccount account = made.account();
				next.group = options.group.isEmpty() && standing != null ? standing.group : options.group;
				if (standing != null) {
					next.order = standing.order;
				}

				next = BookDoor.cleaned(next);
				if (copy) {
					account.mode = "copy";
					return new AdvService.Plan(root, id, null, next, next, account);
				} else if (standing == null) {
					account.mode = "new";
					return new AdvService.Plan(root, id, null, next, next, account);
				} else if ("replace".equals(options.exists)) {
					account.mode = "replaced";
					return new AdvService.Plan(root, id, standing, next, next, account);
				} else {
					account.mode = "merged";
					AdvStore.Kept kept = AdvStore.read(id);
					JsonObject base = kept == null ? null : kept.chapter;
					Chapter merged = BookDoor.cleaned(AdvMerge.merge(base, standing, next, account));
					return new AdvService.Plan(root, id, standing, merged, next, account);
				}
			}
		}
	}

	private static boolean madeFrom(Chapter chapter, ResourceLocation root) {
		AdvStore.Kept kept = AdvStore.read(chapter.id);
		if (kept != null) {
			return root.toString().equals(kept.root);
		} else {
			for (Quest q : chapter.quests) {
				if (q.tags.contains(root.toString())) {
					return true;
				}
			}

			return false;
		}
	}

	public static AdvService.Result preview(MinecraftServer server, String root, AdvOptions options) {
		QuestBook book = BookDoor.book();
		if (book == null) {
			return AdvService.Result.fail("disabled");
		} else {
			options.sanitize();
			List<AdvancementNode> found = AdvTrees.find(server, root);
			if (found.size() != 1) {
				return AdvService.Result.fail("no_tree", root);
			} else {
				String[] problem = new String[2];
				AdvService.Plan plan = plan(new AdvConverter(server), found.get(0), options, book, new HashSet<>(), problem);
				if (plan == null) {
					return AdvService.Result.fail(problem[0], problem[1]);
				} else {
					AdvService.Preview out = new AdvService.Preview();
					out.chapter = plan.chapter();
					out.account = plan.account();
					out.exists = plan.standing() != null || "copy".equals(plan.account().mode);
					return AdvService.Result.ok(out);
				}
			}
		}
	}

	public static AdvService.Result convert(MinecraftServer server, List<String> roots, AdvOptions options) {
		QuestBook book = BookDoor.book();
		if (book == null) {
			return AdvService.Result.fail("disabled");
		} else {
			options.sanitize();
			List<AdvancementNode> nodes = new ArrayList<>();

			for (String root : roots) {
				List<AdvancementNode> found = AdvTrees.find(server, root);
				if (found.isEmpty()) {
					return AdvService.Result.fail("no_tree", root);
				}

				for (AdvancementNode n : found) {
					if (!nodes.contains(n)) {
						nodes.add(n);
					}
				}
			}

			if (nodes.isEmpty()) {
				return AdvService.Result.fail("no_tree", "");
			} else if (!options.group.isEmpty() && !Ids.valid(options.group)) {
				return AdvService.Result.fail("bad_id", options.group);
			} else {
				AdvConverter converter = new AdvConverter(server);
				List<AdvService.Plan> plans = new ArrayList<>();
				Set<String> taken = new HashSet<>();
				String[] problem = new String[2];

				for (AdvancementNode root : nodes) {
					AdvService.Plan plan = plan(converter, root, options, book, taken, problem);
					if (plan == null) {
						return AdvService.Result.fail(problem[0], problem[1]);
					}

					plans.add(plan);
				}

				List<QuestBook.Group> groupsBefore = null;
				String groupsAfter = null;
				if (!options.group.isEmpty() && book.group(options.group) == null) {
					groupsBefore = new ArrayList<>(book.groups);
					QuestBook.Group group = new QuestBook.Group();
					group.id = options.group;
					group.title = options.groupTitle.isEmpty() ? AdvTrees.modName(nodes.get(0).holder().id().getNamespace()) : options.groupTitle;
					group.icon = plans.get(0).chapter().icon;
					List<QuestBook.Group> groups = new ArrayList<>(book.groups);
					groups.add(group);
					String code = BookDoor.groups(groups);
					if (!"ok".equals(code)) {
						return AdvService.Result.fail(code);
					}

					groupsAfter = BookJson.GSON.toJson(book.groups);
				}

				int order = -1;

				for (Chapter c : book.chapters.values()) {
					if (!c.isSubtree() && c.group.equals(options.group)) {
						order = Math.max(order, c.order);
					}
				}

				List<AdvService.Was> done = new ArrayList<>();
				AdvService.Done out = new AdvService.Done();
				String failed = null;

				for (AdvService.Plan plan : plans) {
					Chapter chapter = plan.chapter();
					if (plan.standing() == null) {
						chapter.order = ++order;
					}

					String before = plan.standing() == null ? null : BookJson.GSON.toJson(plan.standing());
					String keptBefore = AdvStore.text(plan.id());
					String code = BookDoor.put(chapter);
					if (!"ok".equals(code)) {
						failed = code;
						break;
					}

					Chapter written = book.chapter(plan.id());
					done.add(new AdvService.Was(plan.id(), before, keptBefore, BookJson.GSON.toJson(written == null ? chapter : written)));
					AdvStore.Kept kept = new AdvStore.Kept();
					kept.root = plan.root().holder().id().toString();
					kept.at = System.currentTimeMillis();
					kept.options = options;
					kept.chapter = AdvMerge.tree(plan.converted());
					AdvStore.write(plan.id(), kept);
					out.accounts.add(plan.account());
					CFHGuiQuests.LOGGER
						.info(
							"[CFHGui Quests] the advancement tree {} is the chapter {} ({}): {} quests, {} with tasks of their own, {} partly, {} kept the advancement{}",
							new Object[]{
								kept.root,
								plan.id(),
								plan.account().mode,
								plan.account().quests,
								plan.account().translated,
								plan.account().partly,
								plan.account().kept,
								(plan.account().triggers.isEmpty() ? "" : " (no task for: " + plan.account().triggersInWords(8) + ")")
									+ (plan.account().changes.isEmpty() ? "" : "; the merge changed " + String.join(", ", plan.account().changes))
							}
						);
				}

				if (!done.isEmpty() || groupsBefore != null) {
					Deque<AdvService.Step> steps = steps(server);
					steps.push(new AdvService.Step(done, groupsBefore, groupsAfter));

					while (steps.size() > 8) {
						steps.removeLast();
					}
				}

				if (failed != null) {
					return AdvService.Result.fail(failed);
				} else {
					int quests = 0;
					int kept = 0;

					for (AdvAccount a : out.accounts) {
						quests += a.quests;
						kept += a.kept;
					}

					return AdvService.Result.ok(out, Integer.toString(out.accounts.size()), Integer.toString(quests), Integer.toString(kept), out.accounts.get(0).chapter);
				}
			}
		}
	}

	public static AdvService.Result undo(MinecraftServer server) {
		QuestBook book = BookDoor.book();
		if (book == null) {
			return AdvService.Result.fail("disabled");
		} else {
			Deque<AdvService.Step> steps = steps(server);
			AdvService.Step step = steps.peek();
			if (step == null) {
				return AdvService.Result.fail("nothing_to_undo");
			} else {
				for (AdvService.Was was : step.chapters()) {
					Chapter now = book.chapter(was.id());
					if (now != null && !BookJson.GSON.toJson(now).equals(was.written())) {
						return AdvService.Result.fail("edited_since", was.id());
					}
				}

				steps.pop();
				int n = 0;

				for (int i = step.chapters().size() - 1; i >= 0; i--) {
					AdvService.Was wasx = step.chapters().get(i);
					String code = "ok";
					if (wasx.before() == null) {
						if (book.chapter(wasx.id()) != null) {
							code = BookDoor.remove(wasx.id());
						}
					} else {
						Chapter before = (Chapter)BookJson.GSON.fromJson(wasx.before(), Chapter.class);
						before.id = wasx.id();
						code = BookDoor.put(before.sanitize());
					}

					if (!"ok".equals(code)) {
						return AdvService.Result.fail(code);
					}

					AdvStore.restore(wasx.id(), wasx.keptBefore());
					n++;
				}

				if (step.groupsBefore() != null && BookJson.GSON.toJson(book.groups).equals(step.groupsAfter())) {
					BookDoor.groups(step.groupsBefore());
				}

				CFHGuiQuests.LOGGER.info("[CFHGui Quests] the last conversion of advancement trees was taken back ({} chapter(s))", n);
				return AdvService.Result.ok(null, Integer.toString(n));
			}
		}
	}

	static void forget(MinecraftServer server, String prefix) {
		steps(server).removeIf(step -> {
			if (step.chapters().isEmpty()) {
				return false;
			} else {
				for (AdvService.Was was : step.chapters()) {
					if (!was.id().startsWith(prefix)) {
						return false;
					}
				}

				return true;
			}
		});
	}

	private AdvService() {
	}

	public static final class Done {
		public List<AdvAccount> accounts = new ArrayList<>();
	}

	private static record Plan(AdvancementNode root, String id, @Nullable Chapter standing, Chapter chapter, Chapter converted, AdvAccount account) {
	}

	public static final class Preview {
		public Chapter chapter = new Chapter();
		public AdvAccount account = new AdvAccount();
		public boolean exists;
	}

	public static record Result(boolean ok, String code, List<String> args, @Nullable Object data) {
		static AdvService.Result ok(Object data, String... args) {
			return new AdvService.Result(true, "ok", List.of(args), data);
		}

		static AdvService.Result fail(String code, String... args) {
			return new AdvService.Result(false, code, List.of(args), null);
		}
	}

	private static record Step(List<AdvService.Was> chapters, @Nullable List<QuestBook.Group> groupsBefore, @Nullable String groupsAfter) {
	}

	public static final class Trees {
		public List<AdvTrees.Tree> trees = new ArrayList<>();
		public Map<String, String> groups = new LinkedHashMap<>();
		public List<String> undo = new ArrayList<>();
	}

	private static record Was(String id, @Nullable String before, @Nullable String keptBefore, String written) {
	}
}
