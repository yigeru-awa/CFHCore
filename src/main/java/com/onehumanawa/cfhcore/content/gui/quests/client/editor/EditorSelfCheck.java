package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

final class EditorSelfCheck {
	private static int passed;
	private static final List<String> FAILED = new ArrayList<>();

	private static void check(String what, boolean ok) {
		if (ok) {
			passed++;
		} else {
			FAILED.add(what);
		}
	}

	private static void check(String what, BooleanSupplier ok) {
		try {
			check(what, ok.getAsBoolean());
		} catch (RuntimeException var3) {
			FAILED.add(what + " (" + var3 + ")");
		}
	}

	static void run(EditSession s) {
		passed = 0;
		FAILED.clear();
		boolean snapping = EditSession.snapping;
		EditSession.snapping = true;
		Form.unfoldAll();
		EditorSelfCheck.Made made = null;

		try {
			made = edits(s);
		} catch (RuntimeException var7) {
			FAILED.add("the check itself threw: " + var7);
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] editor self-check threw", var7);
		} finally {
			EditSession.snapping = snapping;
			Popups.closeAll();
		}

		report("editor self-check");
		EditorSelfCheck.Made left = made;
		EditorShots.later(2500, () -> {
			CFHGuiQuests.LOGGER.info("[CFHGui Quests] editor self-check: afterwards the editor stands at {}, {}", s.save(), s.debug());
			if (left != null && !QuestsState.preview()) {
				withServer(s, left);
			}
		});
	}

	private static void report(String what) {
		CFHGuiQuests.LOGGER.info("[CFHGui Quests] {}: {} passed, {} failed", new Object[]{what, passed, FAILED.size()});

		for (String f : FAILED) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} FAILED: {}", what, f);
		}
	}

	private static void withServer(EditSession s, EditorSelfCheck.Made m) {
		passed = 0;
		FAILED.clear();
		QuestBook book = QuestsState.book();
		check("every change was answered and none was refused", () -> s.save() == EditSession.Save.SAVED);
		check(
			"the chapter the server sent back is the one that was worked on",
			() -> {
				Chapter theirs = book == null ? null : book.chapter(m.chapter());
				return theirs != null
					&& s.work() != null
					&& theirs.quests.size() == s.work().quests.size()
					&& theirs.decorations.size() == s.work().decorations.size()
					&& theirs.accent.equals("#5BA8E0")
					&& theirs.backdrop.equals("workshop");
			}
		);
		check("the tree made for a quest is the server's too", () -> {
			Quest q = s.quest(m.d());
			return q != null && q.isGateway() && book != null && book.chapter(q.subtree) != null;
		});
		s.renameQuest(m.a(), "first");
		EditorShots.later(
			1500,
			() -> {
				check(
					"the server gave a quest another id, and what waited for it still does",
					() -> s.quest("first") != null && s.quest(m.a()) == null && EditorOps.dependsOn(s.work(), s.quest(m.c()), "first")
				);
				s.renameChapter(m.chapter() + "_renamed");
				EditorShots.later(
					1500,
					() -> {
						check(
							"the server gave the chapter another id and it is still the one shown",
							() -> s.work() != null
									&& s.work().id.equals(m.chapter() + "_renamed")
									&& QuestsState.book().chapter(m.chapter()) == null
									&& QuestsState.book().chapter(m.chapter() + "_renamed") != null
						);
						check("its tree still hangs under it", () -> {
							Quest q = s.quest(m.d());
							Chapter tree = q == null ? null : QuestsState.book().chapter(q.subtree);
							return tree != null && tree.parent.equals(m.chapter() + "_renamed");
						});
						check("and the editor stands saved", () -> s.save() == EditSession.Save.SAVED);
						report("editor self-check, with the server");
					}
				);
			}
		);
	}

	@Nullable
	private static EditorSelfCheck.Made edits(EditSession s) {
		QuestBook book = QuestsState.book();
		QuestCanvas canvas = s.canvas();
		check("there is a book and a canvas", book != null && canvas != null);
		if (book != null && canvas != null) {
			int before = book.chapters.size();
			s.newChapter("Editor Self Check");
			Chapter made = s.work();
			check("a new chapter is made and shown", made != null && made.id.startsWith("editor_self_check") && book.chapters.size() == before + 1);
			if (made == null) {
				return null;
			} else {
				String chapter = made.id;
				check("its id is valid", Ids.valid(chapter));
				check("it is in the client's book", book.chapter(chapter) != null);
				String a = s.newQuest(-4.0F, 0.0F, null);
				String b0 = s.newQuest(-2.0F, 0.0F, null);
				String c = s.newQuest(0.0F, 0.0F, null);
				check("three quests are made", () -> s.work().quests.size() == 3 && a != null && b0 != null && c != null);
				check("their ids are valid, free and of the editor's making", () -> Ids.valid(a) && EditorOps.generated(a) && !a.equals(b0) && !b0.equals(c));
				check("a new quest has a checkmark to do (it is not done by being there)", () -> s.quest(a).tasks.size() == 1 && s.quest(a).tasks.get(0).is("checkmark"));
				check("the last one made is chosen", () -> s.selection.has(c) && s.selection.size() == 1);
				check("making a quest is a step", s.steps() == 3);
				s.selection.only(b0);
				QuestForm.title(s, b0, "Stone Age");
				String b = "stone_age";
				check(
					"a quest made here takes its id from its first name",
					() -> s.quest("stone_age") != null && s.quest(b0) == null && s.quest("stone_age").title.equals("Stone Age")
				);
				check("what was chosen follows the new id", s.selection.has("stone_age"));
				QuestForm.title(s, "stone_age", "The Stone Age");
				check("a second name leaves the id alone", () -> s.quest("stone_age") != null && s.quest("stone_age").title.equals("The Stone Age"));
				drag(s, canvas, "stone_age", -2.0F, 2.0F, false);
				check("a drag moves a quest, on the grid", () -> at(s, "stone_age", -2.0F, 2.0F));
				s.undo();
				check("undo puts it back", () -> at(s, "stone_age", -2.0F, 0.0F));
				s.redo();
				check("redo moves it again", () -> at(s, "stone_age", -2.0F, 2.0F));
				drag(s, canvas, "stone_age", -1.7F, 2.0F, true);
				check("with Alt it is placed freely", () -> at(s, "stone_age", -1.7F, 2.0F));
				drag(s, canvas, "stone_age", -2.1F, 2.1F, false);
				check("without it snaps back to the grid", () -> at(s, "stone_age", -2.0F, 2.0F));
				s.selection.set(List.of(a, "stone_age"), List.of());
				drag(s, canvas, a, -4.0F, 1.0F, false);
				check("several chosen quests move together", () -> at(s, a, -4.0F, 1.0F) && at(s, "stone_age", -2.0F, 3.0F));
				s.undo();
				check("... and back together", () -> at(s, a, -4.0F, 0.0F) && at(s, "stone_age", -2.0F, 2.0F));
				s.selection.only("stone_age");
				s.nudge(0.5F, 0.0F);
				s.nudge(0.5F, 0.0F);
				check("the arrows nudge", () -> at(s, "stone_age", -1.0F, 2.0F));
				s.undo();
				check("a run of nudges is one step", () -> at(s, "stone_age", -2.0F, 2.0F));
				pull(s, canvas, a, c);
				check("a line pulled from a quest's rim to another makes that one wait", () -> EditorOps.dependsOn(s.work(), s.quest(c), a));
				check("the new line is chosen", s.selection.hasLink(a, c));
				s.undo();
				check("undo takes the line away", () -> !EditorOps.dependsOn(s.work(), s.quest(c), a));
				s.redo();
				check("redo draws it again", () -> EditorOps.dependsOn(s.work(), s.quest(c), a));
				int steps = s.steps();
				pull(s, canvas, c, a);
				check("a line that would close a circle is refused", () -> !EditorOps.dependsOn(s.work(), s.quest(a), c) && s.steps() == steps);
				pull(s, canvas, a, c);
				check("a line that is there already is not made twice", () -> s.quest(c).deps.size() == 1 && s.steps() == steps);
				pull(s, canvas, a, "stone_age");
				check("a second line", () -> EditorOps.dependsOn(s.work(), s.quest("stone_age"), a));
				s.selection.link(a, "stone_age");
				s.deleteSelection();
				check("Delete on a chosen line removes the dependency", () -> !EditorOps.dependsOn(s.work(), s.quest("stone_age"), a));
				s.undo();
				NodeView nb = canvas.graph().node(Ids.key(chapter, "stone_age"));
				NodeView nc = canvas.graph().node(Ids.key(chapter, c));
				if (nb != null && nc != null) {
					s.tool.gesture(canvas, (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), (double)canvas.sx(nc.x), (double)canvas.sy(nc.y), false, true, false);
				}

				check("with Ctrl a line is pulled from a quest's body", () -> EditorOps.dependsOn(s.work(), s.quest(c), "stone_age") && at(s, "stone_age", -2.0F, 2.0F));
				s.undo();
				NodeView na = canvas.graph().node(Ids.key(chapter, a));
				if (na != null && nb != null) {
					s.selection.only(a);
					s.tool.gesture(canvas, (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), true, false, false);
					check("a click with Shift adds a quest to what is chosen", () -> s.selection.has(a) && s.selection.has("stone_age") && s.selection.size() == 2);
					s.tool.gesture(canvas, (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), true, false, false);
					check("... and takes it out again", () -> s.selection.has(a) && s.selection.size() == 1);
					s.tool.gesture(canvas, (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), (double)canvas.sx(nb.x), (double)canvas.sy(nb.y), false, false, false);
					check("a plain click chooses that quest alone", () -> s.selection.has("stone_age") && s.selection.size() == 1);
					s.tool.gesture(canvas, (double)canvas.sx(-5.0F), (double)canvas.sy(-1.0F), (double)canvas.sx(-1.0F), (double)canvas.sy(3.0F), false, false, false);
					check("a band on the ground chooses the quests it touches", () -> s.selection.has(a) && s.selection.has("stone_age") && s.selection.size() == 2);
					s.tool.gesture(canvas, (double)canvas.sx(-1.0F), (double)canvas.sy(-1.0F), (double)canvas.sx(1.0F), (double)canvas.sy(1.0F), true, false, false);
					check("with Shift a band adds to them", () -> s.selection.has(a) && s.selection.has("stone_age") && s.selection.has(c) && s.selection.size() == 3);
					check("choosing changes nothing in the chapter", () -> at(s, a, -4.0F, 0.0F) && at(s, "stone_age", -2.0F, 2.0F) && at(s, c, 0.0F, 0.0F));
					s.tool.press(canvas, (double)canvas.sx(na.x), (double)canvas.sy(na.y), 1, false, false);
					s.tool.release(canvas, (double)canvas.sx(na.x), (double)canvas.sy(na.y), false);
					check("the right button on one of them opens the menu, and they stay chosen", () -> Popups.any() && s.selection.size() == 3);
					Popups.closeAll();
					s.tool.press(canvas, (double)canvas.sx(-5.0F), (double)canvas.sy(-1.0F), 0, false, false);
					s.tool.release(canvas, (double)canvas.sx(-5.0F), (double)canvas.sy(-1.0F), false);
					check("a click on the ground lets go of them", () -> s.selection.size() == 0);
				}

				NodeView from = canvas.graph().node(Ids.key(chapter, c));
				int count = s.work().quests.size();
				if (from != null) {
					float r = canvas.radiusOf(from);
					s.tool
						.gesture(
							canvas, (double)(canvas.sx(from.x) + r + 3.0F), (double)canvas.sy(from.y), (double)canvas.sx(0.0F), (double)canvas.sy(-2.0F), false, false, false
						);
				}

				String d = s.selection.quest();
				check(
					"a line pulled onto the ground makes a quest that waits for the first",
					() -> s.work().quests.size() == count + 1 && d != null && !d.equals(c) && EditorOps.dependsOn(s.work(), s.quest(d), c) && at(s, d, 0.0F, -2.0F)
				);
				s.undo();
				check("undo takes the quest away again", () -> s.work().quests.size() == count);
				s.redo();
				check("redo brings it back", () -> s.work().quests.size() == count + 1 && s.quest(d) != null);
				s.selection.only("stone_age");
				s.duplicateSelection();
				String b2 = s.selection.quest();
				check(
					"a copy has an id of its own and stands beside the original",
					() -> b2 != null && !b2.equals("stone_age") && Ids.valid(b2) && at(s, b2, -1.0F, 3.0F) && s.quest(b2).title.equals(s.quest("stone_age").title)
				);
				s.selection.set(List.of(a, c), List.of());
				check("what is chosen is copied", s.copySelection());
				s.paste(-2.0F, 6.0F);
				List<String> pasted = new ArrayList<>(s.selection.quests());
				check("a paste makes as many quests as were copied, about the place it was asked for", () -> pasted.size() == 2 && s.work().quests.size() == count + 4);
				check("the copies keep what they depend on among themselves", () -> {
					Quest pa = s.quest(pasted.get(0));
					Quest pc = s.quest(pasted.get(1));
					return pc.deps.contains(pa.id) && !pc.deps.contains(a);
				});
				check(
					"their middle is where the paste was asked for",
					() -> Math.abs((s.quest(pasted.get(0)).x + s.quest(pasted.get(1)).x) / 2.0F + 2.0F) < 0.01F && Math.abs(s.quest(pasted.get(0)).y - 6.0F) < 0.01F
				);
				s.undo();
				check("undo takes the paste away", () -> s.work().quests.size() == count + 2);
				s.selection.only(b2);
				s.deleteSelection();
				check("Delete removes a quest nothing waits for at once", () -> s.quest(b2) == null);
				s.undo();
				check("... and undo brings it back", () -> s.quest(b2) != null);
				s.redo();
				s.selection.only(c);
				s.deleteSelection();
				check("a quest others wait for is asked about before it goes", () -> Popups.any() && s.quest(c) != null);
				Popups.closeAll();
				s.selection.set(List.of(a, "stone_age", d), List.of());
				EditorMenus.align(s, EditorOps.Align.TOP);
				check("lining up by the top", () -> s.quest(a).y == s.quest(d).y && s.quest("stone_age").y == s.quest(d).y);
				s.undo();
				tasks(s, a);
				rewards(s, a);
				fields(s, "stone_age");
				s.newDecoration(false, -4.0F, -2.0F);
				check("a label is made and chosen", () -> s.work().decorations.size() == 1 && s.selection.kind() == Selection.Kind.DECORATION);
				Form deco = form();
				OtherForms.decoration(deco, s, 0);
				type(deco, "deco.text", "Start here");
				press(deco, "deco.heading");
				check("its words and its heading are set from its form", () -> s.work().decorations.get(0).text.equals("Start here") && s.work().decorations.get(0).heading);
				s.selection.onlyDecoration(0);
				s.nudge(0.0F, -0.5F);
				check("a label is nudged like a quest", () -> s.work().decorations.get(0).y == -2.5F);
				s.selection.clear();
				if (!canvas.graph().chapter.decorations.isEmpty()) {
					float[] box = s.tool.box(canvas, canvas.graph().chapter.decorations.get(0));
					float lx = (box[0] + box[2]) / 2.0F;
					float ly = (box[1] + box[3]) / 2.0F;
					s.tool.gesture(canvas, (double)lx, (double)ly, (double)lx, (double)(ly + canvas.unit()), false, false, false);
				}

				check(
					"... and dragged like one: a press on it chooses it, the drag moves it a unit",
					() -> s.selection.kind() == Selection.Kind.DECORATION
							&& Math.abs(s.work().decorations.get(0).y + 1.5F) < 0.01F
							&& Math.abs(s.work().decorations.get(0).x + 4.0F) < 0.01F
				);
				s.undo();
				check("undo puts the label back", () -> s.work().decorations.get(0).y == -2.5F);
				pickers();
				s.selection.clear();
				Form ch = form();
				ChapterForm.chapter(ch, s, s.work());
				type(ch, "chapter.subtitle", "Made by the editor's self-check");
				press(ch, "chapter.hidden");
				check("the chapter's line and its hiding are set from its form", () -> s.work().subtitle.equals("Made by the editor's self-check") && s.work().hidden);
				press(ch, "chapter.hidden");
				s.change(null, () -> s.work().accent = "#5BA8E0");
				s.change(null, () -> s.work().backdrop = "workshop");
				check("its colour and its ground", () -> s.work().accent.equals("#5BA8E0") && s.work().backdrop.equals("workshop"));
				int chapters = book.chapters.size();
				s.makeGateway(d);
				check("a quest is given a tree of its own: a chapter that hangs under this one", () -> {
					Quest q = s.quest(d);
					Chapter tree = book.chapter(q.subtree);
					return q.isGateway() && tree != null && chapter.equals(tree.parent) && book.chapters.size() == chapters + 1 && Ids.valid(tree.id);
				});
				String title = book.title;
				s.changeHead(null, h -> h.title = "Checked", false);
				check("the book's name is changed", book.title.equals("Checked"));
				EditSession.bookTab = true;
				s.selection.clear();
				s.undo();
				check("... and put back by undo", book.title.equals(title));
				EditSession.bookTab = false;
				int groups = book.groups.size();
				s.changeHead(null, h -> {
					QuestBook.Group g = new QuestBook.Group();
					g.id = "editor_check_group";
					g.title = "Checked";
					h.groups.add(g);
				}, true);
				check("a group is made", () -> book.groups.size() == groups + 1 && book.group("editor_check_group") != null);
				String listed = s.work().group;
				s.change(null, () -> s.work().group = "editor_check_group");
				check("the chapter is listed under it", () -> book.chapter(s.work().id).group.equals("editor_check_group"));
				s.deleteGroup("editor_check_group");
				check(
					"when the group goes its chapter stays, listed under none",
					() -> book.groups.size() == groups && book.group("editor_check_group") == null && s.work().group.isEmpty() && book.chapter(s.work().id) != null
				);
				s.change(null, () -> s.work().group = listed);
				int had = book.chapters.size();
				s.duplicateChapter();
				Chapter copy = s.work();
				check(
					"a copy of the chapter is made under an id of its own and shown",
					() -> copy != null
							&& !copy.id.equals(chapter)
							&& Ids.valid(copy.id)
							&& book.chapters.size() == had + 1
							&& copy.quests.size() == book.chapter(chapter).quests.size()
				);
				check("the copy's quests are no second ways into the original's trees", () -> {
					for (Quest q : copy.quests) {
						if (q.isGateway()) {
							return false;
						}
					}

					return true;
				});
				if (copy != null && !copy.id.equals(chapter)) {
					String copyId = copy.id;
					check("the copy is listed right after the original", () -> place(book, copyId) == place(book, chapter) + 1);
					s.moveChapter(copyId, -1);
					check("a chapter is moved up in its list", () -> place(book, copyId) == place(book, chapter) - 1);
					s.deleteChapter();
					check("deleting a chapter is asked back", () -> Popups.any() && book.chapter(copyId) != null);
					Popups.closeAll();
					s.deleteChapterNow();
					check("and then it is gone", () -> book.chapter(copyId) == null && book.chapters.size() == had);
				}

				s.goTo(chapter);
				check(
					"back on the chapter that was worked on, as it was left",
					() -> s.work() != null && s.work().id.equals(chapter) && s.work().quests.size() == book.chapter(chapter).quests.size() && s.quest(a) != null
				);
				if (QuestsState.preview()) {
					s.renameQuest(a, "first");
					check(
						"a quest is given another id, and what waited for it still does",
						() -> s.quest("first") != null && s.quest(a) == null && EditorOps.dependsOn(s.work(), s.quest(c), "first")
					);
					s.renameChapter(chapter + "_renamed");
					check(
						"the chapter is given another id and is still the one shown",
						() -> s.work() != null && s.work().id.equals(chapter + "_renamed") && book.chapter(chapter) == null && book.chapter(chapter + "_renamed") != null
					);
					check("its tree still hangs under it", () -> {
						Quest q = s.quest(d);
						Chapter tree = q == null ? null : book.chapter(q.subtree);
						return tree != null && tree.parent.equals(chapter + "_renamed");
					});
				}

				check("every change was answered", () -> QuestsState.preview() ? s.save() == EditSession.Save.SAVED : s.save() != EditSession.Save.REFUSED);
				check("the book and the working copy agree", () -> {
					Chapter shown = book.chapter(s.work().id);
					return shown != null && shown.quests.size() == s.work().quests.size();
				});
				return new EditorSelfCheck.Made(chapter, a, c, d);
			}
		} else {
			return null;
		}
	}

	private static void pickers() {
		String[] got = new String[]{null};
		PickerPopup p = new PickerPopup(Pickers.What.ICON, "", v -> got[0] = v);
		p.show();
		List<Catalog.Entry> all = p.shownForTest();
		check("the picker lists the game's items", all.size() > 500);
		String third = all.size() > 2 ? all.get(2).value : "";
		int[] at = p.placeForTest(2);
		p.mouseClicked((double)at[0], (double)(at[1] + p.rise()), 0);
		check("a click on a picture picks that item", third.equals(got[0]));
		p = new PickerPopup(Pickers.What.ICON, "", v -> got[0] = v);
		p.show();
		p.searchForTest("minecraft:oak_log");
		check("an id typed into the search finds the one item", p.shownForTest().size() == 1);
		p.keyPressed(257, 0, 0);
		check("Enter takes what was found", "minecraft:oak_log".equals(got[0]));
		p = new PickerPopup(Pickers.What.ITEM, "", v -> got[0] = v);
		p.show();
		p.searchForTest("@minecraft pickaxe");
		List<Catalog.Entry> found = p.shownForTest();
		check("a mod and a word narrow the list to that mod's things of that name", () -> {
			if (found.size() >= 5 && found.size() <= 20) {
				for (Catalog.Entry e : found) {
					if (!e.id.startsWith("minecraft:") || !e.id.contains("pickaxe")) {
						return false;
					}
				}

				return true;
			} else {
				return false;
			}
		});
		p.keyPressed(264, 0, 0);
		p.keyPressed(262, 0, 0);
		String second = found.size() > 1 ? found.get(1).value : "";
		p.keyPressed(257, 0, 0);
		check("the arrows move through what was found and Enter takes it", second.equals(got[0]));
		p = new PickerPopup(Pickers.What.ICON, "icon:trophy", v -> got[0] = v);
		p.show();
		check("a glyph in the field opens the picker on CFHGui's glyphs", p.shownForTest().size() == Icon.values().length);
		p.close();
		p = new PickerPopup(Pickers.What.MOB, "", v -> got[0] = v);
		p.show();
		p.searchForTest("minecraft:zombie");
		int[] row = p.placeForTest(0);
		p.mouseClicked((double)row[0], (double)(row[1] + p.rise()), 0);
		check("a click on a line of a list picks it", "minecraft:zombie".equals(got[0]));
		got[0] = null;
		p = new PickerPopup(Pickers.What.ITEM, "", v -> got[0] = v);
		p.show();
		p.manualForTest("somemod:thing[custom_data={a:1}]");
		check("what is written by hand is taken as it stands", "somemod:thing[custom_data={a:1}]".equals(got[0]));
		check(
			"what a field can hold is told from what it cannot",
			() -> Pickers.readable(Pickers.What.ITEM, "minecraft:diamond")
					&& Pickers.readable(Pickers.What.ITEM, "#minecraft:logs")
					&& !Pickers.readable(Pickers.What.ITEM, "minecraft:no_such_item")
					&& Pickers.readable(Pickers.What.ICON, "icon:trophy")
					&& !Pickers.readable(Pickers.What.ICON, "icon:no_such_glyph")
					&& Pickers.readable(Pickers.What.MOB, "minecraft:zombie")
					&& !Pickers.readable(Pickers.What.MOB, "minecraft:no_such_mob")
					&& Pickers.readable(Pickers.What.STAT, "minecraft:custom/minecraft:jump")
					&& Pickers.readable(Pickers.What.ITEM, "")
		);
		Popups.closeAll();
	}

	private static int place(QuestBook book, String chapterId) {
		List<Chapter> top = book.topChapters();

		for (int i = 0; i < top.size(); i++) {
			if (top.get(i).id.equals(chapterId)) {
				return i;
			}
		}

		return -1;
	}

	private static boolean at(EditSession s, String id, float x, float y) {
		Quest q = s.quest(id);
		return q != null && Math.abs(q.x - x) < 0.005F && Math.abs(q.y - y) < 0.005F;
	}

	private static void drag(EditSession s, QuestCanvas canvas, String id, float toX, float toY, boolean alt) {
		NodeView n = canvas.graph().node(s.key(id));
		if (n != null) {
			if (!s.selection.has(id)) {
				s.selection.only(id);
			}

			s.tool.gesture(canvas, (double)canvas.sx(n.x), (double)canvas.sy(n.y), (double)canvas.sx(toX), (double)canvas.sy(toY), false, false, alt);
		}
	}

	private static void pull(EditSession s, QuestCanvas canvas, String from, String to) {
		NodeView a = canvas.graph().node(s.key(from));
		NodeView b = canvas.graph().node(s.key(to));
		if (a != null && b != null) {
			float side = b.x >= a.x ? -1.0F : 1.0F;
			s.tool
				.gesture(
					canvas,
					(double)(canvas.sx(a.x) + side * (canvas.radiusOf(a) + 3.0F)),
					(double)canvas.sy(a.y),
					(double)canvas.sx(b.x),
					(double)canvas.sy(b.y),
					false,
					false,
					false
				);
		}
	}

	private static Form form() {
		return new Form(180, (widget, x, y) -> {
		}, () -> {
		});
	}

	private static void type(Form f, String name, String text) {
		if (f.named(name) instanceof EditField field) {
			field.setValue(text);
			field.commit();
		} else {
			FAILED.add("the form has no field " + name);
		}
	}

	private static void press(Form f, String name) {
		AbstractWidget w = f.named(name);
		if (!(w instanceof CFHGuiToggle)
			&& !(w instanceof CFHGuiDropdown)
			&& !(w instanceof CFHGuiSegmented)
			&& !(w instanceof CFHGuiSlider)
			&& !(w instanceof Bits.ShapeRow)) {
			FAILED.add("the form has no control " + name);
		} else {
			w.keyPressed(262, 0, 0);
		}
	}

	private static void tasks(EditSession s, String id) {
		int had = s.quest(id).tasks.size();

		for (String type : Kinds.TASKS) {
			TaskDialog dialog = new TaskDialog(Kinds.task("item"), true, made -> s.edit(id, null, q -> q.tasks.add(made), true));
			dialog.layout();
			dialog.draftForTest().type = type;
			dialog.rebuild();
			dialog.layout();
			Form f = dialog.form;
			switch (type) {
				case "item":
					set(f, "task.item", "minecraft:diamond");
					set(f, "task.count", "3");
					press(f, "task.consume");
					break;
				case "craft":
					set(f, "task.item", "#minecraft:planks");
					set(f, "task.count", "8");
					break;
				case "kill":
					set(f, "task.entity", "minecraft:zombie");
					set(f, "task.count", "10");
					set(f, "task.mobname", "Bob");
					break;
				case "break":
				case "place":
					set(f, "task.block", "minecraft:stone");
					set(f, "task.count", "16");
					break;
				case "advancement":
					set(f, "task.advancement", "minecraft:story/mine_stone");
					set(f, "task.criterion", "get_stone");
					break;
				case "fluid":
					set(f, "task.fluid", "minecraft:lava");
					set(f, "task.millibuckets", "3000");
					press(f, "task.consume");
					break;
				case "energy":
					set(f, "task.count", "5000");
					break;
				case "stage":
					set(f, "task.stage", "iron_age");
					break;
				case "counter":
					set(f, "task.counter", "pack:kills");
					set(f, "task.count", "7");
					break;
				case "stat":
					set(f, "task.stat", "minecraft:custom/minecraft:jump");
					set(f, "task.count", "100");
					break;
				case "xp":
					set(f, "task.count", "30");
					press(f, "task.points");
					break;
				case "location":
					set(f, "task.dimension", "minecraft:the_nether");
					set(f, "task.biome", "minecraft:crimson_forest");
					set(f, "task.x", "12");
					set(f, "task.y", "64");
					set(f, "task.z", "-7");
					set(f, "task.radius", "20");
					set(f, "task.sizeX", "4");
					set(f, "task.sizeY", "5");
					set(f, "task.sizeZ", "6");
					break;
				case "observe":
					set(f, "task.block", "minecraft:beacon");
					set(f, "task.seconds", "5");
					break;
				case "quest":
					set(f, "task.quest", "first_steps/logs");
					break;
				case "chapter":
					set(f, "task.chapter", "first_steps");
					break;
				case "team":
					press(f, "task.value");
					set(f, "task.count", "4");
					break;
				case "custom":
					set(f, "task.name", "found_the_cave");
					set(f, "task.count", "2");
			}

			set(f, "task.title", "Check " + type);
			if (type.equals("checkmark")) {
				press(f, "task.optional");
			}

			boolean var10000;
			label317: {
				label316: {
					dialog.submit();
					List<Task> tasks = s.quest(id).tasks;
					Task t = tasks.isEmpty() ? new Task() : tasks.get(tasks.size() - 1);
					if (t.is(type) && t.title.equals("Check " + type)) {
						switch (type) {
							case "item":
								if (t.item.equals("minecraft:diamond") && t.count == 3L && t.consume) {
									break label316;
								}
								break;
							case "craft":
								if (t.item.equals("#minecraft:planks") && t.count == 8L) {
									break label316;
								}
								break;
							case "kill":
								if (t.entity.equals("minecraft:zombie") && t.count == 10L && t.item.isEmpty() && t.name.equals("Bob")) {
									break label316;
								}
								break;
							case "break":
							case "place":
								if (t.block.equals("minecraft:stone") && t.count == 16L && t.name.isEmpty()) {
									break label316;
								}
								break;
							case "advancement":
								if (t.advancement.equals("minecraft:story/mine_stone") && t.count == 1L && t.criterion.equals("get_stone")) {
									break label316;
								}
								break;
							case "fluid":
								if (t.fluid.equals("minecraft:lava") && t.count == 3000L && t.consume && t.manual() && t.item.isEmpty()) {
									break label316;
								}
								break;
							case "energy":
								if (t.count == 5000L && t.manual()) {
									break label316;
								}
								break;
							case "stage":
								if (t.value.equals("iron_age") && t.count == 1L) {
									break label316;
								}
								break;
							case "counter":
								if (t.value.equals("pack:kills") && t.count == 7L) {
									break label316;
								}
								break;
							case "checkmark":
								if (t.optional) {
									break label316;
								}
								break;
							case "stat":
								if (t.stat.equals("minecraft:custom/minecraft:jump") && t.count == 100L) {
									break label316;
								}
								break;
							case "xp":
								if (t.count == 30L && t.points) {
									break label316;
								}
								break;
							case "location":
								if (t.dimension.equals("minecraft:the_nether")
									&& t.biome.equals("minecraft:crimson_forest")
									&& t.x == 12
									&& t.y == 64
									&& t.z == -7
									&& t.radius == 20
									&& t.sizeX == 4
									&& t.sizeY == 5
									&& t.sizeZ == 6
									&& t.box()) {
									break label316;
								}
								break;
							case "observe":
								if (t.block.equals("minecraft:beacon") && t.seconds == 5 && t.count == 100L) {
									break label316;
								}
								break;
							case "quest":
								if (t.quest.equals("first_steps/logs")) {
									break label316;
								}
								break;
							case "chapter":
								if (t.chapter.equals("first_steps")) {
									break label316;
								}
								break;
							case "team":
								if (t.value.equals("claims") && t.count == 4L) {
									break label316;
								}
								break;
							case "custom":
								if (t.value.equals("found_the_cave") && t.count == 2L) {
									break label316;
								}
								break;
							default:
								break label316;
						}
					}

					var10000 = false;
					break label317;
				}

				var10000 = true;
			}

			boolean ok = var10000;
			check("a task of the kind \"" + type + "\" is made from its form", ok);
		}

		check("every kind of task was added", s.quest(id).tasks.size() == had + Kinds.TASKS.size());
		s.undo();
		check("undo takes the last task away", s.quest(id).tasks.size() == had + Kinds.TASKS.size() - 1);
		s.redo();
		check("redo gives it back", s.quest(id).tasks.size() == had + Kinds.TASKS.size());
		Task first = s.quest(id).tasks.get(1);
		TaskDialog edit = new TaskDialog(first, false, changed -> s.edit(id, null, q -> q.tasks.set(1, changed), true));
		edit.layout();
		set(edit.form, "task.count", "5");
		edit.submit();
		check("a task is changed from its form", s.quest(id).tasks.get(1).count == 5L);
		s.edit(id, null, q -> Collections.swap(q.tasks, 1, 2), true);
		check("a task is moved in the list", s.quest(id).tasks.get(2).count == 5L);
		s.edit(id, null, q -> q.tasks.remove(0), true);
		check("a task is taken out", s.quest(id).tasks.size() == had + Kinds.TASKS.size() - 1);
	}

	private static void rewards(EditSession s, String id) {
		for (String type : Kinds.REWARDS) {
			RewardDialog dialog = new RewardDialog(Kinds.reward("item"), true, false, 0, made -> s.edit(id, null, q -> q.rewards.add(made), true));
			dialog.layout();
			dialog.draftForTest().type = type;
			if (type.equals("choice") || type.equals("random") || type.equals("all")) {
				Reward one = Kinds.reward("item");
				Reward two = Kinds.reward("xp");
				one.item = "minecraft:bread";
				one.count = 4;
				two.weight = 3;
				dialog.draftForTest().choices.add(one);
				dialog.draftForTest().choices.add(two);
			}

			dialog.rebuild();
			dialog.layout();
			Form f = dialog.form;
			switch (type) {
				case "item":
					set(f, "reward.item", "minecraft:emerald");
					set(f, "reward.count", "5");
					set(f, "reward.bonus", "2");
					break;
				case "xp":
					set(f, "reward.xp", "7");
					press(f, "reward.levels");
					break;
				case "command":
					set(f, "reward.command", "say {player} did it");
					set(f, "reward.level", "3");
					set(f, "reward.said", "Done");
					break;
				case "random":
					set(f, "reward.rolls", "2");
					set(f, "reward.empty", "4");
					break;
				case "toast":
					set(f, "reward.text", "Well done");
					break;
				case "stage":
					set(f, "reward.stage", "iron_age");
					press(f, "reward.remove");
					break;
				case "counter":
					set(f, "reward.counter", "pack:kills");
					set(f, "reward.amount", "3");
					break;
				case "currency":
					set(f, "reward.amount", "25");
					press(f, "reward.auto");
					press(f, "reward.excluded");
					break;
				case "custom":
					set(f, "reward.name", "pack:blessing");
					press(f, "reward.hidden");
					break;
				case "loot":
					set(f, "reward.loot", "minecraft:chests/simple_dungeon");
					break;
				case "team_xp":
					set(f, "reward.xp", "40");
					break;
				case "advancement":
					set(f, "reward.advancement", "minecraft:story/root");
			}

			boolean var10000;
			label229: {
				label228: {
					set(f, "reward.title", "Check " + type);
					press(f, "reward.team");
					dialog.submit();
					List<Reward> rewards = s.quest(id).rewards;
					Reward r = rewards.isEmpty() ? new Reward() : rewards.get(rewards.size() - 1);
					if (r.is(type) && r.title.equals("Check " + type) && r.team) {
						switch (type) {
							case "item":
								if (r.item.equals("minecraft:emerald") && r.count == 5 && r.bonus == 2) {
									break label228;
								}
								break;
							case "xp":
								if (r.xp == 7 && r.levels) {
									break label228;
								}
								break;
							case "command":
								if (r.command.equals("say {player} did it") && r.item.isEmpty() && r.level == 3 && r.text.equals("Done")) {
									break label228;
								}
								break;
							case "loot":
								if (r.loot.equals("minecraft:chests/simple_dungeon")) {
									break label228;
								}
								break;
							case "choice":
							case "all":
								if (r.choices.size() == 2 && r.choices.get(0).item.equals("minecraft:bread") && r.choices.get(1).weight == 3) {
									break label228;
								}
								break;
							case "random":
								if (r.choices.size() == 2 && r.choices.get(0).item.equals("minecraft:bread") && r.choices.get(1).weight == 3 && r.rolls == 2 && r.empty == 4) {
									break label228;
								}
								break;
							case "toast":
								if (r.text.equals("Well done")) {
									break label228;
								}
								break;
							case "stage":
								if (r.value.equals("iron_age") && r.remove) {
									break label228;
								}
								break;
							case "counter":
								if (r.value.equals("pack:kills") && r.count == 3) {
									break label228;
								}
								break;
							case "currency":
								if (r.count == 25 && r.auto && r.excluded && r.atOnce()) {
									break label228;
								}
								break;
							case "custom":
								if (r.value.equals("pack:blessing") && r.hidden && r.atOnce()) {
									break label228;
								}
								break;
							case "team_xp":
								if (r.xp == 40 && !r.levels) {
									break label228;
								}
								break;
							case "advancement":
								if (r.advancement.equals("minecraft:story/root")) {
									break label228;
								}
								break;
							default:
								break label228;
						}
					}

					var10000 = false;
					break label229;
				}

				var10000 = true;
			}

			boolean ok = var10000;
			check("a reward of the kind \"" + type + "\" is made from its form", ok);
		}

		check("every kind of reward was added", s.quest(id).rewards.size() == Kinds.REWARDS.size());
		s.undo();
		check("undo takes the last reward away", s.quest(id).rewards.size() == Kinds.REWARDS.size() - 1);
		s.redo();
		check("redo gives it back", s.quest(id).rewards.size() == Kinds.REWARDS.size());
	}

	private static void set(@Nullable Form f, String name, String text) {
		if ((f == null ? null : f.named(name)) instanceof EditField field) {
			field.setValue(text);
		} else {
			FAILED.add("the dialog has no field " + name);
		}
	}

	private static void fields(EditSession s, String id) {
		Quest q = s.quest(id);
		if (q == null) {
			FAILED.add("the quest to set fields on is gone");
		} else {
			Form f = form();
			QuestForm.build(f, s, q);
			type(f, "subtitle", "A line under it");
			check("subtitle", () -> s.quest(id).subtitle.equals("A line under it"));
			type(f, "icon", "minecraft:stone_pickaxe");
			check("icon", () -> s.quest(id).icon.equals("minecraft:stone_pickaxe"));
			press(f, "shape");
			check("shape", () -> s.quest(id).shape == Quest.Shape.CIRCLE);
			press(f, "size");
			check("size", () -> Math.abs(s.quest(id).size - 1.1F) < 0.001F);
			press(f, "frame");
			check("frame", () -> s.quest(id).frame == Quest.Frame.GOAL);
			press(f, "kind");
			check("kind", () -> s.quest(id).kind == Quest.Kind.ADVANCEMENT);
			s.undo();
			check("undo of a choice", () -> s.quest(id).kind == Quest.Kind.QUEST);
			press(f, "sync");
			check("sync", () -> s.quest(id).sync == Quest.Sync.TEAM);
			press(f, "visibility");
			check("visibility", () -> s.quest(id).visibility == Quest.Visibility.HIDDEN);
			press(f, "optional");
			check("optional", () -> s.quest(id).optional);
			press(f, "silent");
			check("silent", () -> s.quest(id).silent);
			type(f, "teamXp", "25");
			check("teamXp", () -> s.quest(id).teamXp == 25);
			type(f, "tags", "stone, mining , ,tools");
			check("tags", () -> s.quest(id).tags.equals(List.of("stone", "mining", "tools")));
			press(f, "repeatable");
			check("repeatable", () -> s.quest(id).repeatable);
			Form again = form();
			QuestForm.build(again, s, s.quest(id));
			type(again, "cooldownSeconds", "600");
			check("cooldownSeconds", () -> s.quest(id).cooldownSeconds == 600);
			type(again, "x", "-2.5");
			check("x, typed", () -> s.quest(id).x == -2.5F);
			s.undo();
			check("undo of a typed field", () -> s.quest(id).x == -2.0F);
			if (again.named("description") instanceof TextArea area) {
				area.setValue("First line with **bold**.\nSecond line.\n\n");
				area.commit();
			}

			check(
				"description: a paragraph a line, nothing for the empty lines at the end",
				() -> s.quest(id).description.equals(List.of("First line with **bold**.", "Second line."))
			);
			s.edit(id, null, x -> {
				x.deps.clear();
				x.deps.add(s.work().quests.get(0).id);
				x.deps.add(s.work().quests.get(2).id);
			}, true);
			Form deps = form();
			QuestForm.build(deps, s, s.quest(id));
			press(deps, "depMode");
			check("depMode", () -> s.quest(id).depMode == Quest.DepMode.ANY);
			press(deps, "hideLines");
			press(deps, "looseDeps");
			check("hideLines and looseDeps", () -> s.quest(id).hideLines && s.quest(id).looseDeps);
			press(deps, "hideLines");
			press(deps, "looseDeps");
		}
	}

	private EditorSelfCheck() {
	}

	private static record Made(String chapter, String a, String c, String d) {
	}
}
