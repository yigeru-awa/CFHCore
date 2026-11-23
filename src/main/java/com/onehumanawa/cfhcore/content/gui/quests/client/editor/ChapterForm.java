package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.Trees;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.Backdrops;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class ChapterForm {
	private static final List<String> BACKDROPS = List.of("stars", "grid", "meadow", "cave", "nether", "end", "ocean", "parchment", "workshop", "texture");

	static void chapter(Form f, EditSession s, Chapter c) {
		QuestBook book = s.book();
		if (book != null) {
			if (f.section("chapter", Tr.t("section.chapter"), 0)) {
				f.first(f.text("chapter.title", c.title, 256, v -> s.change(null, () -> s.work().title = v.trim(), false)));
				f.text("chapter.subtitle", c.subtitle, 256, v -> s.change(null, () -> s.work().subtitle = v.trim(), false));
				f.icon(
					"chapter.icon",
					c.icon,
					Pickers.What.ICON,
					() -> QuestIcons.of(s.work() == null ? null : s.work().icon),
					v -> s.change(null, () -> s.work().icon = v.trim(), false)
				);
				if (c.isSubtree()) {
					Chapter parent = book.chapter(c.parent);
					f.label(Tr.field("chapter.parent"));
					Bits.ListRow row = new Bits.ListRow(
						f.width,
						parent == null ? QuestIcons.glyph(Icon.WARNING) : QuestIcons.of(parent.icon),
						(Component)(parent == null ? Component.literal(c.parent) : QuestText.title(parent)),
						null,
						parent == null ? null : () -> s.goTo(parent.id)
					);
					row.tip(Tr.tip("chapter.parent"));
					f.stop("chapter.parent", row);
					f.row(row);
				} else {
					List<String> groups = new ArrayList<>();
					groups.add("");

					for (QuestBook.Group g : book.groups) {
						groups.add(g.id);
					}

					if (!groups.contains(c.group)) {
						groups.add(c.group);
					}

					f.dropdown("chapter.group", groups, c.group, id -> groupName(book, id), v -> s.change(null, () -> s.work().group = v, false));
				}
			}

			if (f.section("chapter.text", Tr.t("section.chapter.text"), 0)) {
				QuestForm.text(
					f,
					s,
					QuestText.title(c),
					c.description,
					lines -> s.change(null, () -> s.work().description = new ArrayList<>(lines), false),
					lines -> s.change(null, () -> s.work().description = new ArrayList<>(lines), true)
				);
			}

			if (f.section("chapter.look", Tr.t("section.chapter.look"), 0)) {
				f.color("chapter.accent", c.accent, Theme.current().accent(), hex -> s.soon("accent", () -> s.change("accent", () -> s.work().accent = hex, false)));
				boolean texture = Backdrops.kind(c.backdrop) == Backdrops.Kind.TEXTURE;
				String named = texture ? "texture" : (!c.backdrop.isBlank() && BACKDROPS.contains(c.backdrop) ? c.backdrop : "stars");
				f.dropdown("chapter.backdrop", BACKDROPS, named, v -> Tr.value("backdrop", v), v -> {
					if (!v.equals("texture")) {
						s.change(null, () -> s.work().backdrop = v, true);
					} else {
						Pickers.open(Pickers.What.BACKDROP, texture ? c.backdrop : "", picked -> {
							if (!picked.isBlank()) {
								s.change(null, () -> s.work().backdrop = picked.trim(), true);
							}
						});
					}
				});
				if (texture) {
					f.pick("chapter.texture", c.backdrop, Pickers.What.BACKDROP, v -> {
						if (!v.isBlank()) {
							s.change(null, () -> s.work().backdrop = v.trim(), false);
						}
					});
				}

				f.shape("chapter.shape", c.shape, false, c.shape, v -> s.change(null, () -> s.work().shape = v, false));
			}

			if (f.section("chapter.rules", Tr.t("section.chapter.rules"), 0)) {
				f.choice("chapter.sync", Arrays.asList(Quest.Sync.values()), c.sync, v -> Tr.value("sync", v), v -> s.change(null, () -> s.work().sync = v, false));
				if (!c.isSubtree()) {
					f.toggle("chapter.hidden", c.hidden, v -> s.change(null, () -> s.work().hidden = v, false));
				}

				if (!c.isSubtree()) {
					f.toggle("chapter.unlisted", c.unlisted, v -> s.change(null, () -> s.work().unlisted = v, false));
				}

				f.pick("chapter.focus", c.focus.isEmpty() ? "" : Ids.key(c.id, c.focus), Pickers.What.QUEST, v -> {
					String picked = v.trim();
					boolean here = !picked.isEmpty() && Ids.chapterOf(picked).equals(c.id) && c.quest(Ids.questOf(picked)) != null;
					s.change(null, () -> s.work().focus = here ? Ids.questOf(picked) : "", false);
				});
				f.label(Tr.field("chapter.requires"));
				if (c.requires.isEmpty()) {
					f.note(Tr.t(c.isSubtree() ? "requires.none.tree" : "requires.none"));
				}

				for (int i = 0; i < c.requires.size(); i++) {
					String need = c.requires.get(i);
					BookIndex.Entry entry = QuestsState.entry(Ids.resolve(c.id, need));
					Bits.ListRow row = new Bits.ListRow(
						f.width,
						entry == null ? QuestIcons.glyph(Icon.WARNING) : QuestIcons.of(entry),
						(Component)(entry == null ? Component.literal(need) : QuestText.title(entry)),
						(Component)(entry == null ? Tr.t("deps.gone") : QuestText.title(entry.chapter())),
						entry == null ? null : () -> s.goTo(entry.chapter().id)
					);
					row.actions(null, null, () -> s.change(null, () -> s.work().requires.remove(need)));
					f.stop("requires." + i, row);
					f.row(row);
				}

				f.button("requires.add", Tr.t("requires.add"), Icon.PLUS, () -> Pickers.open(Pickers.What.QUEST, "", picked -> {
						if (!picked.isBlank() && QuestsState.entry(picked) != null) {
							s.change(null, () -> {
								if (!s.work().requires.contains(picked)) {
									s.work().requires.add(picked);
								}
							});
						}
					})).tip(Tr.tip("chapter.requires"));
			}

			if (f.section("chapter.file", Tr.t("section.chapter.file"), 0)) {
				Bits.ListRow file = new Bits.ListRow(f.width, QuestIcons.glyph(Icon.FOLDER), Component.literal(c.id), Tr.t("chapter.id.rename"), () -> renameChapter(s, c));
				file.tip(Tr.tip("chapter.id"));
				f.stop("chapter.id", file);
				f.row(file);
				if (!c.isSubtree()) {
					f.buttons(
						"chapter.up",
						Tr.t("chapter.up"),
						Icon.ARROW_UP,
						() -> s.moveChapter(c.id, -1),
						"chapter.down",
						Tr.t("chapter.down"),
						Icon.ARROW_DOWN,
						() -> s.moveChapter(c.id, 1),
						false
					);
				}

				f.buttons(
					"chapter.duplicate",
					Tr.t("chapter.duplicate"),
					Icon.DUPLICATE,
					s::duplicateChapter,
					"chapter.delete",
					Tr.t("chapter.delete"),
					Icon.TRASH,
					s::deleteChapter,
					true
				);
			}
		}
	}

	private static Component groupName(QuestBook book, String id) {
		if (id.isEmpty()) {
			return Tr.t("v.group.none");
		} else {
			QuestBook.Group g = book.group(id);
			return g == null ? Component.literal(id) : QuestText.text(g.title);
		}
	}

	private static void renameChapter(EditSession s, Chapter c) {
		new Prompt(Tr.t("chapter.rename.title"), Icon.EDIT, Tr.t("chapter.rename.body"), c.id, Tr.t("rename.ok"), id -> wrongId(id, c.id, other -> {
				QuestBook book = s.book();
				return book != null && book.chapters.containsKey(other);
			}), s::renameChapter).show();
	}

	@Nullable
	static Component wrongId(String id, String current, Predicate<String> taken) {
		if (id.isEmpty()) {
			return Tr.t("id.empty");
		} else if (!Ids.valid(id)) {
			return Tr.t("id.invalid");
		} else {
			return !id.equals(current) && taken.test(id) ? Tr.t("id.taken") : null;
		}
	}

	static void book(Form f, EditSession s) {
		QuestBook book = s.book();
		if (book != null) {
			if (f.section("book", Tr.t("section.book"), 0)) {
				f.first(f.text("book.title", book.title, 256, v -> s.changeHead(null, h -> h.title = v.trim(), false)));
				f.text("book.subtitle", book.subtitle, 256, v -> s.changeHead(null, h -> h.subtitle = v.trim(), false));
				f.toggle("book.closed", book.closed, v -> s.changeHead(null, h -> h.closed = v, false));
				if ("ftbquests".equals(book.source)) {
					f.note(Tr.t("book.source.ftb"));
				}
			}

			if (f.section("emergency", Tr.t("section.emergency"), book.emergency.size())) {
				if (book.emergency.isEmpty()) {
					f.note(Tr.t("emergency.none"));
				}

				for (int i = 0; i < book.emergency.size(); i++) {
					final int ix = i;
					Reward r = book.emergency.get(i);
					Bits.ListRow row = new Bits.ListRow(
						f.width, QuestIcons.of(r), QuestText.label(r), null, () -> new RewardDialog(r, false, false, 1, changed -> s.changeHead(null, h -> {
									if (ix < h.emergency.size()) {
										h.emergency.set(ix, changed);
									}
								}, true)).only("item", "xp").show()
					);
					row.actions(null, null, () -> s.changeHead(null, h -> {
							if (ix < h.emergency.size()) {
								h.emergency.remove(ix);
							}
						}, true));
					f.stop("emergency." + i, row);
					f.row(row);
				}

				CFHGuiButton more = f.button("emergency.add", Tr.t("emergency.add"), Icon.PLUS, () -> Pickers.open(Pickers.What.GIFT, "", picked -> {
						if (!picked.isBlank()) {
							s.changeHead(null, h -> {
								Reward made = Kinds.reward("item");
								made.item = picked.trim();
								h.emergency.add(made);
							}, true);
						}
					}));
				more.active = book.emergency.size() < 27;
				if (!book.emergency.isEmpty()) {
					f.number(
						"book.emergencyCooldown",
						(long)book.emergencyCooldownSeconds,
						0L,
						31536000L,
						v -> s.changeHead(null, h -> h.emergencyCooldownSeconds = v.intValue(), false)
					);
				}
			}

			if (f.section("groups", Tr.t("section.groups"), book.groups.size())) {
				if (book.groups.isEmpty()) {
					f.note(Tr.t("groups.none"));
				}

				for (int i = 0; i < book.groups.size(); i++) {
					final int ix = i;
					QuestBook.Group g = book.groups.get(i);
					int members = 0;

					for (Chapter c : book.chapters.values()) {
						if (!c.readOnly && !c.isSubtree() && g.id.equals(c.group)) {
							members++;
						}
					}

					Bits.ListRow row = new Bits.ListRow(
						f.width,
						g.icon.isBlank() ? QuestIcons.glyph(Icon.FOLDER) : QuestIcons.of(g.icon),
						QuestText.text(g.title),
						Component.literal(Integer.toString(members)),
						() -> new ChapterForm.GroupDialog(g, changed -> s.changeHead(null, h -> {
									if (ix < h.groups.size()) {
										h.groups.get(ix).title = changed.title;
										h.groups.get(ix).icon = changed.icon;
									}
								}, true)).show()
					);
					row.actions(
						ix > 0 ? () -> s.changeHead(null, h -> Collections.swap(h.groups, ix, ix - 1), true) : null,
						ix < book.groups.size() - 1 ? () -> s.changeHead(null, h -> Collections.swap(h.groups, ix, ix + 1), true) : null,
						() -> s.askDeleteGroup(g)
					);
					row.tip(Tr.t("groups.row.tip"));
					f.stop("group." + i, row);
					f.row(row);
				}

				f.button(
					"group.add",
					Tr.t("groups.add"),
					Icon.PLUS,
					() -> new Prompt(
								Tr.t("groups.new.title"),
								Icon.FOLDER,
								Tr.t("groups.new.body"),
								"",
								Tr.t("create.ok"),
								name -> name.isEmpty() ? Tr.t("id.empty") : null,
								name -> s.changeHead(null, h -> {
										QuestBook.Group g = new QuestBook.Group();
										g.id = EditorOps.free(EditorOps.validId(name, "group"), id -> {
											for (QuestBook.Group x : h.groups) {
												if (x.id.equals(id)) {
													return true;
												}
											}

											return false;
										});
										g.title = name;
										h.groups.add(g);
									}, true)
							)
							.show()
				);
			}

			int own = 0;

			for (Chapter cx : book.chapters.values()) {
				if (!cx.readOnly) {
					own++;
				}
			}

			if (f.section("outline", Tr.t("section.outline"), own)) {
				if (own == 0) {
					f.note(Tr.t("outline.none"));
				}

				Chapter work = s.work();
				String group = null;

				for (Chapter top : book.topChapters()) {
					if (!top.readOnly) {
						if (!top.group.equals(group)) {
							group = top.group;
							f.label(groupName(book, group));
						}

						List<Chapter> row = EditorOps.siblings(book, top);
						int at = row.indexOf(top);

						for (Trees.Branch branch : Trees.branches(book, top)) {
							Chapter cxx = branch.chapter();
							if (!cxx.readOnly) {
								Bits.ListRow line = new Bits.ListRow(
									f.width, QuestIcons.of(cxx.icon), QuestText.title(cxx), Component.literal(Integer.toString(cxx.quests.size())), () -> s.goTo(cxx.id)
								);
								line.marked(work != null && cxx.id.equals(work.id)).indent(Math.min(4, branch.depth()) * 8);
								if (branch.depth() == 0) {
									line.actions(at > 0 ? () -> s.moveChapter(cxx.id, -1) : null, at >= 0 && at < row.size() - 1 ? () -> s.moveChapter(cxx.id, 1) : null, null);
								}

								line.tip(Tr.t("outline.row.tip"));
								f.stop("outline." + cxx.id, line);
								f.row(line);
							}
						}
					}
				}

				CFHGuiButton add = f.button("chapter.new", Tr.t("chapter.new"), Icon.PLUS, () -> newChapter(s));
				add.tip(Tr.t("chapter.new.tip"));
				f.toggle("book.advancements", book.advancements, v -> s.changeHead(null, h -> h.advancements = v, false));
			}
		}
	}

	static void newChapter(EditSession s) {
		new Prompt(
				Tr.t("chapter.new.title"), Icon.QUEST, Tr.t("chapter.new.body"), "", Tr.t("create.ok"), name -> name.isEmpty() ? Tr.t("id.empty") : null, s::newChapter
			)
			.show();
	}

	private ChapterForm() {
	}

	private static final class GroupDialog extends EditorDialog {
		private final QuestBook.Group draft = new QuestBook.Group();
		private final Consumer<QuestBook.Group> onDone;

		GroupDialog(QuestBook.Group group, Consumer<QuestBook.Group> onDone) {
			super(Tr.t("groups.edit"), Icon.FOLDER, 240);
			this.draft.id = group.id;
			this.draft.title = group.title;
			this.draft.icon = group.icon;
			this.onDone = onDone;
			this.button(Component.translatable("gui.cancel"), Variant.SECONDARY, this::close);
			this.button(Tr.t("dialog.done"), Variant.PRIMARY, this::submit);
		}

		@Override
		protected void fill(Form f) {
			f.first(f.text("group.title", this.draft.title, 256, v -> this.draft.title = v));
			f.icon("group.icon", this.draft.icon, Pickers.What.ICON, () -> QuestIcons.of(this.draft.icon), v -> this.draft.icon = v.trim());
		}

		@Override
		protected void submit() {
			if (this.draft.title.isBlank()) {
				this.fail(Tr.t("id.empty"));
			} else {
				this.close();
				this.draft.title = this.draft.title.trim();
				this.onDone.accept(this.draft);
			}
		}
	}
}
