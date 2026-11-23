package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

final class QuestForm {
	private static final List<String> SYNC = List.of("chapter", "team", "player");

	static void build(Form f, EditSession s, Quest q) {
		Chapter work = s.work();
		QuestBook book = s.book();
		if (work != null && book != null) {
			String id = q.id;
			String key = s.key(id);
			if (f.section("quest", Tr.t("section.quest"), 0)) {
				EditField title = f.text("title", q.title, 256, v -> title(s, id, v));
				if (q.title.isBlank()) {
					title.hint(QuestText.title(q));
				}

				f.first(title);
				f.text("subtitle", q.subtitle, 256, v -> s.edit(id, null, x -> x.subtitle = v.trim(), false));
				f.icon("icon", q.icon, Pickers.What.ICON, () -> shownIcon(key), v -> s.edit(id, null, x -> x.icon = v.trim(), false));
			}

			if (f.section("tasks", Tr.t("section.tasks"), q.tasks.size())) {
				tasks(f, s, q);
			}

			if (f.section("rewards", Tr.t("section.rewards"), q.rewards.size())) {
				rewards(f, s, q);
			}

			if (f.section("text", Tr.t("section.text"), 0)) {
				text(
					f,
					s,
					QuestText.title(q),
					q.description,
					lines -> s.edit(id, null, x -> x.description = new ArrayList<>(lines), false),
					lines -> s.edit(id, null, x -> x.description = new ArrayList<>(lines), true)
				);
			}

			if (f.section("look", Tr.t("section.look"), 0)) {
				f.shape("shape", q.shape, true, work.shape, v -> s.edit(id, null, x -> x.shape = v, false));
				f.slider(
					"size",
					0.5,
					4.0,
					0.1,
					(double)q.size,
					v -> "× " + EditorTool.fmt((float)v),
					v -> preview(s, key, (float)v),
					v -> s.edit(id, null, x -> x.size = (float)v, false)
				);
				f.choice("frame", Arrays.asList(Quest.Frame.values()), q.frame, v -> Tr.value("frame", v), v -> s.edit(id, null, x -> x.frame = v, false));
				f.choice("kind", Arrays.asList(Quest.Kind.values()), q.kind, v -> Tr.value("kind", v), v -> s.edit(id, null, x -> x.kind = v, true));
				f.slider(
					"iconScale", 0.25, 4.0, 0.05, (double)q.iconScale, v -> "× " + EditorTool.fmt((float)v), null, v -> s.edit(id, null, x -> x.iconScale = (float)v, false)
				);
			}

			if (f.section("deps", Tr.t("section.deps"), q.deps.size())) {
				deps(f, s, q);
			}

			if (f.section("tree", Tr.t("section.tree"), 0)) {
				tree(f, s, q);
			}

			if (f.section("behaviour", Tr.t("section.behaviour"), 0)) {
				String sync = q.sync == null ? "chapter" : q.sync.name().toLowerCase(Locale.ROOT);
				f.dropdown(
					"sync",
					SYNC,
					sync,
					v -> v.equals("chapter") ? Tr.t("v.sync.chapter", Tr.value("sync", work.sync)) : Tr.value("sync", v),
					v -> s.edit(id, null, x -> x.sync = v.equals("chapter") ? null : Quest.Sync.valueOf(v.toUpperCase(Locale.ROOT)), false)
				);
				f.dropdown(
					"visibility", Arrays.asList(Quest.Visibility.values()), q.visibility, v -> Tr.value("visibility", v), v -> s.edit(id, null, x -> x.visibility = v, true)
				);
				if (q.visibility == Quest.Visibility.INVISIBLE) {
					f.number("revealTasks", (long)q.revealTasks, 0L, 64L, v -> s.edit(id, null, x -> x.revealTasks = v.intValue(), false));
				}

				f.toggle("hideDetails", q.hideDetails, v -> s.edit(id, null, x -> x.hideDetails = v, false));
				f.toggle("hideText", q.hideText, v -> s.edit(id, null, x -> x.hideText = v, false));
				f.text("guide", q.guide, 256, v -> s.edit(id, null, x -> x.guide = v.trim(), false)).hint(Tr.t("guide.hint"));
				f.toggle("optional", q.optional, v -> s.edit(id, null, x -> x.optional = v, false));
				f.toggle("repeatable", q.repeatable, v -> s.edit(id, null, x -> x.repeatable = v, true));
				if (q.repeatable) {
					f.number("cooldownSeconds", (long)q.cooldownSeconds, 0L, 31536000L, v -> s.edit(id, null, x -> x.cooldownSeconds = v.intValue(), false));
				}

				f.toggle("silent", q.silent, v -> s.edit(id, null, x -> x.silent = v, false));
				f.number("teamXp", (long)q.teamXp, -1L, 1000000L, v -> s.edit(id, null, x -> x.teamXp = v.intValue(), false));
				f.text("tags", String.join(", ", q.tags), 512, v -> s.edit(id, null, x -> x.tags = tags(v), false));
			}

			if (f.section("place", Tr.t("section.place"), 0)) {
				f.pair("x", q.x, v -> s.edit(id, null, x -> x.x = EditorOps.tidy(v), false), "y", q.y, v -> s.edit(id, null, x -> x.y = EditorOps.tidy(v), false));
			}
		}
	}

	private static List<String> tags(String typed) {
		List<String> out = new ArrayList<>();

		for (String word : typed.split(",")) {
			if (!word.isBlank() && !out.contains(word.trim())) {
				out.add(word.trim());
			}
		}

		return out;
	}

	private static QuestIcons.Drawable shownIcon(String key) {
		BookIndex.Entry e = QuestsState.entry(key);
		return e == null ? QuestIcons.of((String)null) : QuestIcons.of(e);
	}

	private static void preview(EditSession s, String key, float size) {
		QuestCanvas canvas = s.canvas();
		NodeView n = canvas == null ? null : canvas.graph().node(key);
		if (n != null) {
			n.size = size;
		}
	}

	static void title(EditSession s, String id, String value) {
		String title = value.trim();
		String[] renamed = new String[]{null};
		boolean chosen = s.selection.has(id);
		s.change(null, () -> {
			Quest x = s.quest(id);
			if (x != null) {
				x.title = title;
				if (s.isFresh(id) && !title.isEmpty()) {
					String to = EditorOps.freeQuestId(s.work(), Ids.slug(title, id), Set.of());
					if (!to.equals(id) && !EditorOps.generated(to)) {
						EditorOps.reId(s.work(), id, to);
						renamed[0] = to;
					}
				}
			}
		}, false);
		if (renamed[0] != null) {
			s.renamed(id, renamed[0], chosen);
		}
	}

	static void text(Form f, EditSession s, Component of, List<String> lines, Consumer<List<String>> typed, Consumer<List<String>> edited) {
		Bits.RichBox box = new Bits.RichBox(f.width, 56, lines, Tr.t("text.preview.empty"));
		TextArea area = new TextArea(f.width - TextArea.barRoom(), TextArea.heightFor(6), Tr.t("text.hint"), Tr.field("description"));
		area.set(TextArea.join(lines));
		area.typing(text -> box.set(TextArea.split(text)));
		area.onCommit(text -> typed.accept(TextArea.split(text)));
		f.stop("description", area);
		f.row(area);
		f.row(box);
		f.button("description.open", Tr.t("text.open"), Icon.FULLSCREEN, () -> new TextDialog(Tr.t("text.of", of), TextArea.split(area.getValue()), edited).show())
			.tip(Tr.t("text.open.tip"));
	}

	private static void tasks(Form f, EditSession s, Quest q) {
		String id = q.id;
		if (q.tasks.isEmpty()) {
			f.note(Tr.t(q.isGateway() ? "tasks.none.gateway" : "tasks.none"));
		}

		for (int i = 0; i < q.tasks.size(); i++) {
			int at = i;
			Task task = q.tasks.get(i);
			Bits.ListRow row = new Bits.ListRow(
				f.width, QuestIcons.of(task), QuestText.label(task), null, () -> new TaskDialog(task, false, changed -> s.edit(id, null, x -> {
							if (at < x.tasks.size()) {
								x.tasks.set(at, changed);
							}
						}, true)).show()
			);
			row.actions(
				at > 0 ? () -> s.edit(id, null, x -> Collections.swap(x.tasks, at, at - 1), true) : null,
				at < q.tasks.size() - 1 ? () -> s.edit(id, null, x -> Collections.swap(x.tasks, at, at + 1), true) : null,
				() -> s.edit(id, null, x -> {
						if (at < x.tasks.size()) {
							x.tasks.remove(at);
						}
					}, true)
			);
			row.tip(Tr.t("row.task.tip", Tr.value("task", task.type)));
			f.stop("task." + i, row);
			f.row(row);
		}

		CFHGuiButton[] add = new CFHGuiButton[1];
		add[0] = f.button(
			"task.add",
			Tr.t("task.add"),
			Icon.PLUS,
			() -> {
				List<Item> items = new ArrayList<>();

				for (String type : Kinds.TASKS) {
					items.add(
						Item.of(
							Tr.value("task", type),
							Kinds.taskIcon(type),
							() -> new TaskDialog(Kinds.task(type), true, made -> s.edit(id, null, x -> x.tasks.add(made), true)).show()
						)
					);
				}

				Popups.open(new MenuPopup(add[0].getX(), add[0].getY() + add[0].getHeight() + 1, items, add[0].getWidth()));
			}
		);
		add[0].active = q.tasks.size() < 64;
		if (q.tasks.size() > 1) {
			f.toggle("anyTask", q.anyTask, v -> s.edit(id, null, x -> x.anyTask = v, false));
		}

		if (q.tasks.size() > 1) {
			f.toggle("sequential", q.sequential, v -> s.edit(id, null, x -> x.sequential = v, false));
		}
	}

	private static void rewards(Form f, EditSession s, Quest q) {
		String id = q.id;
		if (q.rewards.isEmpty()) {
			f.note(Tr.t("rewards.none"));
		}

		for (int i = 0; i < q.rewards.size(); i++) {
			int at = i;
			Reward reward = q.rewards.get(i);
			Bits.ListRow row = new Bits.ListRow(
				f.width,
				QuestIcons.of(reward),
				QuestText.label(reward),
				reward.team ? Tr.t("reward.team.short") : null,
				() -> new RewardDialog(reward, false, false, 0, changed -> s.edit(id, null, x -> {
							if (at < x.rewards.size()) {
								x.rewards.set(at, changed);
							}
						}, true)).show()
			);
			row.actions(
				at > 0 ? () -> s.edit(id, null, x -> Collections.swap(x.rewards, at, at - 1), true) : null,
				at < q.rewards.size() - 1 ? () -> s.edit(id, null, x -> Collections.swap(x.rewards, at, at + 1), true) : null,
				() -> s.edit(id, null, x -> {
						if (at < x.rewards.size()) {
							x.rewards.remove(at);
						}
					}, true)
			);
			row.tip(Tr.t("row.reward.tip", Tr.value("reward", reward.type)));
			f.stop("reward." + i, row);
			f.row(row);
		}

		CFHGuiButton[] add = new CFHGuiButton[1];
		add[0] = f.button(
			"reward.add",
			Tr.t("reward.add"),
			Icon.PLUS,
			() -> {
				List<Item> items = new ArrayList<>();

				for (String type : Kinds.REWARDS) {
					items.add(
						Item.of(
							Tr.value("reward", type),
							Kinds.rewardIcon(type),
							() -> new RewardDialog(Kinds.reward(type), true, false, 0, made -> s.edit(id, null, x -> x.rewards.add(made), true)).show()
						)
					);
				}

				Popups.open(new MenuPopup(add[0].getX(), add[0].getY() + add[0].getHeight() + 1, items, add[0].getWidth()));
			}
		);
		add[0].active = q.rewards.size() < 64;
		if (!q.rewards.isEmpty() && !q.isAdvancement()) {
			f.toggle("autoClaim", q.autoClaim, v -> s.edit(id, null, x -> x.autoClaim = v, false));
		}
	}

	private static void deps(Form f, EditSession s, Quest q) {
		Chapter work = s.work();
		String id = q.id;
		if (q.deps.isEmpty()) {
			f.note(Tr.t("deps.none"));
		}

		for (int i = 0; i < q.deps.size(); i++) {
			String dep = q.deps.get(i);
			String depKey = Ids.resolve(work.id, dep);
			boolean here = Ids.chapterOf(depKey).equals(work.id);
			Quest other = here ? work.quest(Ids.questOf(depKey)) : null;
			BookIndex.Entry entry = QuestsState.entry(depKey);
			Component label = (Component)(other != null ? QuestText.title(other) : (entry != null ? QuestText.title(entry) : Component.literal(dep)));
			Component note = (Component)(other != null ? null : (entry != null ? QuestText.title(entry.chapter()) : Tr.t("deps.gone")));
			QuestIcons.Drawable icon = entry != null ? QuestIcons.of(entry) : QuestIcons.glyph(Icon.WARNING);
			Bits.ListRow row = new Bits.ListRow(f.width, icon, label, note, () -> {
				if (other != null) {
					EditorMenus.choose(s, other.id);
				} else if (entry != null) {
					s.goTo(entry.chapter().id);
				}
			});
			row.actions(null, null, () -> s.edit(id, null, x -> x.deps.remove(dep), true));
			row.removeTip(Tr.t("deps.remove"));
			f.stop("dep." + i, row);
			f.row(row);
		}

		f.button("dep.add", Tr.t("deps.add"), Icon.PLUS, () -> Pickers.open(Pickers.What.QUEST, "", picked -> wait(s, id, picked))).tip(Tr.t("deps.add.tip"));
		if (q.deps.size() > 1) {
			f.choice("depMode", Arrays.asList(Quest.DepMode.values()), q.depMode, v -> Tr.value("depmode", v), v -> s.edit(id, null, x -> x.depMode = v, true));
			if (q.depMode == Quest.DepMode.ANY) {
				f.number("minDeps", (long)Math.min(q.minDeps, q.deps.size()), 1L, (long)q.deps.size(), v -> s.edit(id, null, x -> x.minDeps = v.intValue(), false));
			}
		}

		if (!q.deps.isEmpty()) {
			f.toggle("hideLines", q.hideLines, v -> s.edit(id, null, x -> x.hideLines = v, false));
			f.toggle("looseDeps", q.looseDeps, v -> s.edit(id, null, x -> x.looseDeps = v, false));
			f.toggle("depsStarted", q.depsStarted, v -> s.edit(id, null, x -> x.depsStarted = v, false));
		}

		f.toggle("hideOutLines", q.hideOutLines, v -> s.edit(id, null, x -> x.hideOutLines = v, false));
		f.number("maxDependents", (long)q.maxDependents, 0L, 256L, v -> s.edit(id, null, x -> x.maxDependents = v.intValue(), false));
	}

	private static void wait(EditSession s, String id, String picked) {
		Chapter work = s.work();
		if (work != null && !picked.isBlank() && QuestsState.entry(picked) != null) {
			boolean here = Ids.chapterOf(picked).equals(work.id);
			String ref = here ? Ids.questOf(picked) : picked;
			if (here) {
				EditorOps.Link can = EditorOps.canLink(work, ref, id);
				if (can != EditorOps.Link.OK) {
					s.notice(Tr.t(can == EditorOps.Link.CIRCLE ? "link.circle" : (can == EditorOps.Link.SELF ? "link.self" : "link.exists")));
					return;
				}
			}

			s.edit(id, null, x -> {
				if (!x.deps.contains(ref)) {
					x.deps.add(ref);
				}
			}, true);
		}
	}

	private static void tree(Form f, EditSession s, Quest q) {
		QuestBook book = s.book();
		String id = q.id;
		if (!q.isGateway()) {
			f.note(Tr.t("tree.none"));
			f.buttons(
				"tree.make",
				Tr.t("tree.make"),
				Icon.PLUS,
				() -> s.makeGateway(id),
				"tree.pick",
				Tr.t("tree.pick"),
				Icon.SEARCH,
				() -> Pickers.open(Pickers.What.CHAPTER, "", picked -> s.adoptTree(id, picked)),
				false
			);
		} else {
			Chapter tree = book.chapter(q.subtree);
			Component label = (Component)(tree == null ? Component.literal(q.subtree) : QuestText.title(tree));
			Component note = tree == null ? Tr.t("deps.gone") : Tr.t("tree.count", tree.quests.size());
			Bits.ListRow row = new Bits.ListRow(f.width, tree == null ? QuestIcons.glyph(Icon.WARNING) : QuestIcons.of(tree.icon), label, note, () -> s.enter(s.key(id)));
			row.actions(null, null, () -> s.edit(id, null, x -> x.subtree = "", true));
			row.removeTip(Tr.t("tree.remove"));
			row.tip(Tr.t("tree.open.tip"));
			f.stop("tree", row);
			f.row(row);
			f.choice(
				"inline", List.of(Boolean.FALSE, Boolean.TRUE), q.inline, v -> Tr.value("inline", v ? "inside" : "tab"), v -> s.edit(id, null, x -> x.inline = v, false)
			);
			f.note(Tr.t(q.tasks.isEmpty() ? "tree.note.done" : "tree.note"));
		}
	}

	private QuestForm() {
	}
}
