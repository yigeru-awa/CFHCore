package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;

final class OtherForms {
	static void several(Form f, EditSession s) {
		Chapter work = s.work();
		if (work != null) {
			List<Quest> quests = new ArrayList<>();

			for (String id : s.selection.quests()) {
				Quest q = work.quest(id);
				if (q != null) {
					quests.add(q);
				}
			}

			int labels = s.selection.decorations().size();
			if (quests.size() > 1 && f.section("several.place", Tr.t("section.several.place"), 0)) {
				Icon[] icons = new Icon[]{Icon.ALIGN_LEFT, Icon.ALIGN_CENTER, Icon.ALIGN_RIGHT, Icon.ALIGN_TOP, Icon.ALIGN_MIDDLE, Icon.ALIGN_BOTTOM};
				int x = 0;

				for (EditorOps.Align how : EditorOps.Align.values()) {
					CFHGuiIconButton b = new CFHGuiIconButton(0, 0, 18, icons[how.ordinal()], Tr.value("align", how), () -> EditorMenus.align(s, how));
					f.stop("align." + how.name(), b);
					f.at(b, x, 0);
					if (f.first() == null) {
						f.first(b);
					}

					x += 20;
				}

				f.advance(18);
				f.label(Tr.t("several.spread"));
				CFHGuiButton[] spread = f.buttons(
					"spread.across",
					Tr.t("several.across"),
					null,
					() -> EditorMenus.spread(s, true),
					"spread.down",
					Tr.t("several.down"),
					null,
					() -> EditorMenus.spread(s, false),
					false
				);
				spread[0].tip(Tr.t("several.across.tip"));
				spread[1].tip(Tr.t("several.down.tip"));
				spread[0].active = spread[1].active = quests.size() > 2;
			}

			if (!quests.isEmpty() && f.section("several.fields", Tr.t("section.several.fields", quests.size()), 0)) {
				Quest first = quests.get(0);
				f.shape("shape", same(quests, q -> q.shape == first.shape) ? first.shape : null, true, work.shape, v -> s.editSelected(null, q -> q.shape = v));
				f.slider("size", 0.5, 4.0, 0.1, (double)first.size, v -> "× " + EditorTool.fmt((float)v), null, v -> s.editSelected(null, q -> q.size = (float)v));
				f.choice("frame", Arrays.asList(Quest.Frame.values()), first.frame, v -> Tr.value("frame", v), v -> s.editSelected(null, q -> q.frame = v));
				f.dropdown(
					"visibility", Arrays.asList(Quest.Visibility.values()), first.visibility, v -> Tr.value("visibility", v), v -> s.editSelected(null, q -> q.visibility = v)
				);
				flag(f, s, quests, "optional", q -> q.optional, (q, v) -> q.optional = v);
				flag(f, s, quests, "autoClaim", q -> q.autoClaim, (q, v) -> q.autoClaim = v);
				flag(f, s, quests, "silent", q -> q.silent, (q, v) -> q.silent = v);
				flag(f, s, quests, "hideLines", q -> q.hideLines, (q, v) -> q.hideLines = v);
				flag(f, s, quests, "looseDeps", q -> q.looseDeps, (q, v) -> q.looseDeps = v);
				f.note(Tr.t("several.note"));
			}

			if (quests.isEmpty() && labels > 0) {
				f.note(Tr.t("several.labels", labels));
			}

			f.gap(2);
			f.buttons(
				"several.duplicate",
				Tr.t("menu.duplicate"),
				Icon.DUPLICATE,
				s::duplicateSelection,
				"several.delete",
				Tr.t("menu.delete"),
				Icon.TRASH,
				s::deleteSelection,
				true
			);
		}
	}

	private static boolean same(List<Quest> quests, Predicate<Quest> as) {
		for (Quest q : quests) {
			if (!as.test(q)) {
				return false;
			}
		}

		return true;
	}

	private static void flag(Form f, EditSession s, List<Quest> quests, String name, Predicate<Quest> get, BiConsumer<Quest, Boolean> set) {
		boolean value = get.test(quests.get(0));
		boolean differ = !same(quests, q -> get.test(q) == value);
		CFHGuiToggle t = f.toggle(name, value, v -> s.editSelected(null, q -> set.accept(q, v)));
		if (differ) {
			t.setMessage(Component.empty().append(Tr.field(name)).append(" *"));
		}
	}

	static void decoration(Form f, EditSession s, int index) {
		Chapter work = s.work();
		if (work != null && index >= 0 && index < work.decorations.size()) {
			Chapter.Decoration d = work.decorations.get(index);
			boolean image = "image".equals(d.type);
			f.choice("deco.type", List.of("label", "image"), image ? "image" : "label", v -> Tr.value("deco", v), v -> change(s, index, x -> {
					x.type = v;
					if (v.equals("image") && x.image.isBlank()) {
						x.image = "minecraft:textures/item/compass_00.png";
						x.w = x.h = 1.07F;
					}
				}, true));
			if (image) {
				f.first(f.pick("deco.image", d.image, Pickers.What.IMAGE, v -> change(s, index, x -> x.image = v.trim(), false)));
				f.pair("deco.w", d.w, v -> change(s, index, x -> x.w = Math.max(0.1F, v), false), "deco.h", d.h, v -> change(s, index, x -> x.h = Math.max(0.1F, v), false));
			} else {
				f.first(f.text("deco.text", d.text, 512, v -> change(s, index, x -> x.text = v, false)));
				f.toggle("deco.heading", d.heading, v -> change(s, index, x -> x.heading = v, false));
				f.slider("deco.scale", 0.5, 4.0, 0.1, (double)d.scale, v -> "× " + EditorTool.fmt((float)v), null, v -> change(s, index, x -> x.scale = (float)v, false));
				f.color("deco.color", d.color, Theme.current().palette().textMuted(), hex -> s.soon("deco.color", () -> change(s, index, x -> x.color = hex, false)));
			}

			f.slider("deco.alpha", 0.1, 1.0, 0.05, (double)d.alpha, v -> Math.round(v * 100.0) + "%", null, v -> change(s, index, x -> x.alpha = (float)v, false));
			f.pair("x", d.x, v -> change(s, index, x -> x.x = EditorOps.tidy(v), false), "y", d.y, v -> change(s, index, x -> x.y = EditorOps.tidy(v), false));
			f.slider("deco.rotation", -180.0, 180.0, 5.0, (double)d.rotation, v -> Math.round(v) + "°", null, v -> change(s, index, x -> x.rotation = (float)v, false));
			f.number("deco.order", (long)d.order, -1000L, 1000L, v -> change(s, index, x -> x.order = v.intValue(), false));
			f.text("deco.hover", String.join(" | ", d.hover), 2048, v -> change(s, index, x -> {
					x.hover = new ArrayList<>();

					for (String line : v.split("\\s\\|\\s")) {
						if (!line.isBlank()) {
							x.hover.add(line.trim());
						}
					}
				}, false));
			f.text("deco.link", d.link, 1024, v -> change(s, index, x -> x.link = v.trim(), false)).hint(Tr.t("deco.link.hint"));
			f.pick("deco.requires", d.requires, Pickers.What.QUEST, v -> change(s, index, x -> x.requires = v.trim(), false));
			f.toggle("deco.editorOnly", d.editorOnly, v -> change(s, index, x -> x.editorOnly = v, false));
			f.gap(2);
			f.buttons(
				"deco.duplicate", Tr.t("menu.duplicate"), Icon.DUPLICATE, s::duplicateSelection, "deco.delete", Tr.t("menu.delete"), Icon.TRASH, s::deleteSelection, true
			);
		}
	}

	private static void change(EditSession s, int index, Consumer<Chapter.Decoration> edit, boolean structural) {
		s.change(null, () -> {
			Chapter work = s.work();
			if (index < work.decorations.size()) {
				edit.accept(work.decorations.get(index));
			}
		}, structural);
	}

	static void line(Form f, EditSession s, String from, String to) {
		Quest a = s.quest(from);
		Quest b = s.quest(to);
		if (a != null && b != null) {
			f.label(Tr.t("line.from"));
			Bits.ListRow first = new Bits.ListRow(f.width, icon(s, from), QuestText.title(a), null, () -> EditorMenus.choose(s, from));
			f.stop("line.from", first);
			f.first((Bits.ListRow)f.row(first));
			f.label(Tr.t("line.to"));
			Bits.ListRow second = new Bits.ListRow(f.width, icon(s, to), QuestText.title(b), null, () -> EditorMenus.choose(s, to));
			f.stop("line.to", second);
			f.row(second);
			f.toggle("hideLines", b.hideLines, v -> s.edit(to, null, x -> x.hideLines = v, false));
			f.toggle("looseDeps", b.looseDeps, v -> s.edit(to, null, x -> x.looseDeps = v, false));
			f.gap(2);
			f.button("line.remove", Tr.t("menu.unlink"), Icon.TRASH, () -> s.unlink(from, to)).variant(Variant.DANGER);
		}
	}

	private static QuestIcons.Drawable icon(EditSession s, String questId) {
		BookIndex.Entry entry = QuestsState.entry(s.key(questId));
		return entry == null ? QuestIcons.glyph(Icon.QUEST) : QuestIcons.of(entry);
	}

	private OtherForms() {
	}
}
