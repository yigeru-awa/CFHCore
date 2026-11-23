package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class EditorMenus {
	private static final float[] SIZES = new float[]{0.8F, 1.0F, 1.3F, 1.4F, 1.6F, 2.0F};

	static void open(EditSession s, EditorTool.Hit hit, double mouseX, double mouseY, float treeX, float treeY) {
		Chapter work = s.work();
		if (work != null) {
			int x = (int)mouseX;
			int y = (int)mouseY;

			List<Item> items = switch (hit.kind()) {
				case 1, 2 -> s.selection.size() > 1 ? several(s, x, y) : quest(s, hit.quest(), x, y);
				case 3 -> s.selection.size() > 1 ? several(s, x, y) : decoration(s);
				case 4 -> line(s, hit.from(), hit.to());
				default -> ground(s, treeX, treeY);
			};
			if (!items.isEmpty()) {
				Popups.open(new MenuPopup(x, y, items, 150));
			}
		}
	}

	private static Item item(String key, Icon icon, Runnable action) {
		return Item.of(Tr.t("menu." + key), icon, action);
	}

	private static Item ticked(String key, Icon icon, boolean on, Runnable action) {
		return new Item(Tr.t("menu." + key), icon, true, on, false, false, action);
	}

	private static List<Item> quest(EditSession s, @Nullable String id, int x, int y) {
		List<Item> items = new ArrayList<>();
		Quest q = s.quest(id);
		if (q != null && id != null) {
			String key = s.key(id);
			items.add(item("edit", Icon.EDIT, s.panel::focusFirst));
			items.add(item("duplicate", Icon.DUPLICATE, s::duplicateSelection));
			items.add(item("copy", Icon.COPY, s::copySelection));
			items.add(item("link", Icon.LINK, () -> s.tool.startLink(id, true)));
			items.add(Item.sep());
			if (q.isGateway()) {
				items.add(item("tree.open", Icon.EXTERNAL, () -> s.enter(key)));
				items.add(item("tree.remove", Icon.CLOSE, () -> s.edit(id, null, quest -> quest.subtree = "", true)));
			} else {
				items.add(item("tree.make", Icon.LAYERS, () -> s.makeGateway(id)));
			}

			items.add(Item.sep());
			items.add(item("shape", Icon.STOP, () -> Popups.open(new MenuPopup(x, y, shapes(s, q.shape), 120))));
			items.add(item("size", Icon.RESIZE, () -> Popups.open(new MenuPopup(x, y, sizes(s, q.size), 90))));
			items.add(item("frame", Icon.TROPHY, () -> Popups.open(new MenuPopup(x, y, frames(s, q.frame), 110))));
			items.add(ticked("optional", Icon.TOGGLE, q.optional, () -> s.editSelected(null, quest -> quest.optional = !q.optional)));
			items.add(Item.sep());
			if (!QuestsState.status(key).done()) {
				items.add(Item.of(Component.translatable("cfhgui_quests.menu.complete"), Icon.CHECK, () -> QuestActions.complete(key, null, null)));
			}

			items.add(
				Item.of(
					Component.translatable("cfhgui_quests.menu.reset"),
					Icon.UNDO,
					() -> CFHGuiModal.confirmDanger(
							Component.translatable("cfhgui_quests.menu.reset.title"),
							Component.translatable("cfhgui_quests.menu.reset.body", new Object[]{QuestText.title(q)}),
							Component.translatable("cfhgui_quests.menu.reset.ok"),
							() -> QuestActions.reset(key, null, null)
						)
				)
			);
			items.add(Item.sep());
			items.add(Item.danger(Tr.t("menu.delete"), Icon.TRASH, s::deleteSelection));
			return items;
		} else {
			return items;
		}
	}

	private static List<Item> several(EditSession s, int x, int y) {
		List<Item> items = new ArrayList<>();
		int n = s.selection.size();
		items.add(Item.of(Tr.t("menu.duplicate.n", n), Icon.DUPLICATE, s::duplicateSelection));
		items.add(Item.of(Tr.t("menu.copy.n", n), Icon.COPY, s::copySelection));
		if (s.selection.quests().size() > 1) {
			items.add(Item.sep());
			items.add(item("align", Icon.ALIGN_LEFT, () -> Popups.open(new MenuPopup(x, y, aligns(s), 130))));
			items.add(item("shape", Icon.STOP, () -> Popups.open(new MenuPopup(x, y, shapes(s, null), 120))));
			items.add(item("size", Icon.RESIZE, () -> Popups.open(new MenuPopup(x, y, sizes(s, -1.0F), 90))));
		}

		items.add(Item.sep());
		items.add(Item.danger(Tr.t("menu.delete.n", n), Icon.TRASH, s::deleteSelection));
		return items;
	}

	private static List<Item> shapes(EditSession s, @Nullable Quest.Shape current) {
		List<Item> items = new ArrayList<>();
		items.add(Item.checked(Tr.t("v.shape.chapter"), current == null && s.selection.size() == 1, () -> s.editSelected(null, q -> q.shape = null)));

		for (Quest.Shape shape : Quest.Shape.values()) {
			items.add(Item.checked(Tr.value("shape", shape), shape == current, () -> s.editSelected(null, q -> q.shape = shape)));
		}

		return items;
	}

	private static List<Item> sizes(EditSession s, float current) {
		List<Item> items = new ArrayList<>();

		for (float size : SIZES) {
			items.add(Item.checked(Component.literal("× " + EditorTool.fmt(size)), Math.abs(size - current) < 0.01F, () -> s.editSelected(null, q -> q.size = size)));
		}

		return items;
	}

	private static List<Item> frames(EditSession s, Quest.Frame current) {
		List<Item> items = new ArrayList<>();

		for (Quest.Frame frame : Quest.Frame.values()) {
			items.add(Item.checked(Tr.value("frame", frame), frame == current, () -> s.editSelected(null, q -> q.frame = frame)));
		}

		return items;
	}

	private static List<Item> aligns(EditSession s) {
		List<Item> items = new ArrayList<>();
		Icon[] icons = new Icon[]{Icon.ALIGN_LEFT, Icon.ALIGN_CENTER, Icon.ALIGN_RIGHT, Icon.ALIGN_TOP, Icon.ALIGN_MIDDLE, Icon.ALIGN_BOTTOM};

		for (EditorOps.Align how : EditorOps.Align.values()) {
			items.add(Item.of(Tr.value("align", how), icons[how.ordinal()], () -> align(s, how)));
		}

		items.add(Item.sep());
		if (s.selection.quests().size() > 2) {
			items.add(item("spread.across", Icon.DOTS, () -> spread(s, true)));
			items.add(item("spread.down", Icon.MENU, () -> spread(s, false)));
		} else {
			items.add(Item.disabled(Tr.t("menu.spread.across"), Icon.DOTS));
			items.add(Item.disabled(Tr.t("menu.spread.down"), Icon.MENU));
		}

		return items;
	}

	static void align(EditSession s, EditorOps.Align how) {
		List<String> ids = new ArrayList<>(s.selection.quests());
		s.change(null, () -> EditorOps.align(s.work(), ids, how));
	}

	static void spread(EditSession s, boolean across) {
		List<String> ids = new ArrayList<>(s.selection.quests());
		s.change(null, () -> EditorOps.distribute(s.work(), ids, across));
	}

	private static List<Item> decoration(EditSession s) {
		List<Item> items = new ArrayList<>();
		items.add(item("edit", Icon.EDIT, s.panel::focusFirst));
		items.add(item("duplicate", Icon.DUPLICATE, s::duplicateSelection));
		items.add(item("copy", Icon.COPY, s::copySelection));
		items.add(Item.sep());
		items.add(Item.danger(Tr.t("menu.delete"), Icon.TRASH, s::deleteSelection));
		return items;
	}

	private static List<Item> line(EditSession s, @Nullable String from, @Nullable String to) {
		List<Item> items = new ArrayList<>();
		Quest a = s.quest(from);
		Quest b = s.quest(to);
		if (a != null && b != null) {
			items.add(Item.of(Tr.t("menu.goto", QuestText.title(a)), Icon.ARROW_LEFT, () -> choose(s, from)));
			items.add(Item.of(Tr.t("menu.goto", QuestText.title(b)), Icon.ARROW_RIGHT, () -> choose(s, to)));
			items.add(ticked("hide_lines", Icon.EYE_OFF, b.hideLines, () -> s.edit(to, null, q -> q.hideLines = !b.hideLines, true)));
			items.add(Item.sep());
			items.add(Item.danger(Tr.t("menu.unlink"), Icon.TRASH, () -> s.unlink(from, to)));
			return items;
		} else {
			return items;
		}
	}

	static void choose(EditSession s, @Nullable String questId) {
		if (questId != null && s.quest(questId) != null) {
			s.selection.only(questId);
			QuestCanvas canvas = s.canvas();
			if (canvas != null) {
				canvas.reveal(s.key(questId));
			}
		}
	}

	private static List<Item> ground(EditSession s, float treeX, float treeY) {
		List<Item> items = new ArrayList<>();
		float[] at = s.tool.place(treeX, treeY);
		items.add(item("new_quest", Icon.PLUS, () -> s.newQuest(at[0], at[1], null)));
		if (s.canPaste()) {
			items.add(item("paste", Icon.COPY, () -> s.paste(treeX, treeY)));
		} else {
			items.add(Item.disabled(Tr.t("menu.paste"), Icon.COPY));
		}

		items.add(item("new_label", Icon.TEXT, () -> s.newDecoration(false, at[0], at[1])));
		items.add(item("new_image", Icon.IMAGE, () -> s.newDecoration(true, at[0], at[1])));
		items.add(Item.sep());
		items.add(item("select_all", Icon.SELECTION, s::selectAll));
		QuestCanvas canvas = s.canvas();
		if (canvas != null) {
			items.add(Item.of(Component.translatable("cfhgui_quests.menu.fit"), Icon.FULLSCREEN, canvas::fit));
		}

		items.add(Item.sep());
		items.add(item("chapter", Icon.SETTINGS, s.panel::showChapter));
		return items;
	}

	private EditorMenus() {
	}
}
