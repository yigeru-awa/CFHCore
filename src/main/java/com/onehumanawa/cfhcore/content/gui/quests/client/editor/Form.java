package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiColorField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class Form {
	static final int GAP = 4;
	static final int ROW = 18;
	private static final Set<String> FOLDED = new HashSet<>(Set.of("place", "chapter.rules", "chapter.file"));
	final int width;
	private final Form.Placer placer;
	private final Runnable rebuild;
	private int y;
	private boolean live;
	private final List<AbstractWidget> stops = new ArrayList<>();
	private final Map<AbstractWidget, String> names = new IdentityHashMap<>();
	@Nullable
	private AbstractWidget first;

	static void unfoldAll() {
		FOLDED.clear();
	}

	static void foldOnly(String... ids) {
		FOLDED.clear();
		FOLDED.addAll(ids.length == 0 ? List.of("place", "chapter.rules", "chapter.file") : List.of(ids));
	}

	Form(int width, Form.Placer placer, Runnable rebuild) {
		this.width = width;
		this.placer = placer;
		this.rebuild = rebuild;
	}

	Form live() {
		this.live = true;
		return this;
	}

	int height() {
		return this.y;
	}

	void gap(int px) {
		this.y += px;
	}

	List<AbstractWidget> stops() {
		return this.stops;
	}

	@Nullable
	String nameOf(@Nullable AbstractWidget widget) {
		return widget == null ? null : this.names.get(widget);
	}

	@Nullable
	AbstractWidget named(@Nullable String name) {
		if (name == null) {
			return null;
		} else {
			for (Entry<AbstractWidget, String> e : this.names.entrySet()) {
				if (e.getValue().equals(name)) {
					return e.getKey();
				}
			}

			return null;
		}
	}

	@Nullable
	AbstractWidget first() {
		return this.first;
	}

	<T extends AbstractWidget> T first(T widget) {
		this.first = widget;
		return widget;
	}

	void flush() {
		for (AbstractWidget w : new ArrayList<>(this.stops)) {
			if (w instanceof EditField f) {
				f.commit();
			} else if (w instanceof TextArea a) {
				a.commit();
			}
		}
	}

	<T extends AbstractWidget> T row(T widget) {
		this.placer.place(widget, 0, this.y);
		this.y = this.y + widget.getHeight() + 4;
		return widget;
	}

	<T extends AbstractWidget> T at(T widget, int x, int dy) {
		this.placer.place(widget, x, this.y + dy);
		return widget;
	}

	void advance(int height) {
		this.y += height + 4;
	}

	<T extends AbstractWidget> T stop(String name, T widget) {
		this.stops.add(widget);
		this.names.put(widget, name);
		return widget;
	}

	void label(Component text) {
		this.placer.place(new CFHGuiLabel(0, 0, this.width, text).style(Style.MUTED), 1, this.y);
		this.y += 11;
	}

	void note(Component text) {
		CFHGuiLabel label = new CFHGuiLabel(0, 0, this.width - 2, text).style(Style.CAPTION).wrap(true);
		this.placer.place(label, 1, this.y);
		this.y = this.y + label.getHeight() + 4;
	}

	boolean section(String id, Component title, int count) {
		boolean folded = FOLDED.contains(id);
		if (this.y > 0) {
			this.y += 2;
		}

		CFHGuiSectionHeader head = new CFHGuiSectionHeader(0, 0, this.width, title, count, folded, () -> {
			if (!FOLDED.remove(id)) {
				FOLDED.add(id);
			}

			this.rebuild.run();
		});
		this.stop("section." + id, head);
		this.placer.place(head, 0, this.y);
		this.y += 22;
		return !folded;
	}

	EditField text(String name, @Nullable String value, int max, Consumer<String> change) {
		this.label(Tr.field(name));
		return this.field(name, this.width, value, max, change);
	}

	EditField fieldAt(String name, int x, int w, @Nullable String value, int max, Consumer<String> change) {
		EditField f = new EditField(w, Tr.field(name), value).max(max).tooltip(Tr.tip(name));
		if (this.live) {
			f.typing(change);
		} else {
			f.onCommit(change);
		}

		this.stop(name, f);
		this.placer.place(f, x, this.y);
		return f;
	}

	private EditField field(String name, int w, @Nullable String value, int max, Consumer<String> change) {
		EditField f = this.fieldAt(name, 0, w, value, max, change);
		this.y = this.y + f.getHeight() + 4;
		return f;
	}

	<T extends AbstractWidget> T inline(String name, T control) {
		int h = Math.max(18, control.getHeight());
		this.placer.place(new CFHGuiLabel(0, 0, Math.max(10, this.width - control.getWidth() - 6), Tr.field(name)), 1, this.y + (h - 10) / 2 + 1);
		this.placer.place(control, this.width - control.getWidth(), this.y + (h - control.getHeight()) / 2);
		this.y += h + 4;
		return control;
	}

	EditField number(String name, long value, long min, long max, Consumer<Long> change) {
		EditField f = new EditField(Math.min(64, this.width / 2), Tr.field(name), Long.toString(value)).whole(min, max).tooltip(Tr.tip(name));
		Consumer<String> to = text -> change.accept(Math.round(f.number((double)value)));
		if (this.live) {
			f.typing(to);
		} else {
			f.onCommit(to);
		}

		this.stop(name, f);
		return this.inline(name, f);
	}

	EditField decimal(String name, float value, float min, float max, Consumer<Float> change) {
		EditField f = new EditField(Math.min(64, this.width / 2), Tr.field(name), EditorTool.fmt(value)).decimal((double)min, (double)max).tooltip(Tr.tip(name));
		Consumer<String> to = text -> change.accept((float)f.number((double)value));
		if (this.live) {
			f.typing(to);
		} else {
			f.onCommit(to);
		}

		this.stop(name, f);
		return this.inline(name, f);
	}

	void pair(String nameA, float a, Consumer<Float> changeA, String nameB, float b, Consumer<Float> changeB) {
		int half = (this.width - 6) / 2;
		this.half(nameA, 0, half, a, changeA);
		this.half(nameB, half + 6, half, b, changeB);
		this.y += 22;
	}

	void trio(String[] names, int[] values, List<Consumer<Integer>> changes) {
		int third = (this.width - 8) / 3;

		for (int i = 0; i < 3; i++) {
			int x = i * (third + 4);
			int value = values[i];
			Consumer<Integer> change = changes.get(i);
			Component letter = Tr.field(names[i]);
			int lw = CFHGuiDraw.width(letter) + 4;
			this.placer.place(new CFHGuiLabel(0, 0, lw, letter).style(Style.MUTED), x + 1, this.y + 5);
			EditField f = new EditField(third - lw, letter, Integer.toString(value)).whole(-30000000L, 30000000L).tooltip(Tr.tip(names[i]));
			Consumer<String> to = text -> change.accept((int)Math.round(f.number((double)value)));
			if (this.live) {
				f.typing(to);
			} else {
				f.onCommit(to);
			}

			this.stop(names[i], f);
			this.placer.place(f, x + lw, this.y);
		}

		this.y += 22;
	}

	private void half(String name, int x, int w, float value, Consumer<Float> change) {
		Component letter = Tr.field(name);
		int lw = CFHGuiDraw.width(letter) + 4;
		this.placer.place(new CFHGuiLabel(0, 0, lw, letter).style(Style.MUTED), x + 1, this.y + 5);
		EditField f = new EditField(w - lw, letter, EditorTool.fmt(value)).decimal(-100000.0, 100000.0).tooltip(Tr.tip(name));
		Consumer<String> to = text -> change.accept((float)f.number((double)value));
		if (this.live) {
			f.typing(to);
		} else {
			f.onCommit(to);
		}

		this.stop(name, f);
		this.placer.place(f, x + lw, this.y);
	}

	CFHGuiToggle toggle(String name, boolean value, Consumer<Boolean> change) {
		CFHGuiToggle t = new CFHGuiToggle(0, 0, this.width, Tr.field(name), value, change);
		t.setHeight(18);
		t.tip(Tr.tip(name));
		this.stop(name, t);
		return this.row(t);
	}

	<T> CFHGuiSegmented<T> choice(String name, List<T> options, T value, Function<T, Component> words, Consumer<T> change) {
		this.label(Tr.field(name));
		CFHGuiSegmented<T> seg = new CFHGuiSegmented<>(0, 0, this.width, options, value, words, change);
		seg.setHeight(18);
		seg.tip(Tr.tip(name));
		this.stop(name, seg);
		return this.row(seg);
	}

	<T> CFHGuiDropdown<T> dropdown(String name, List<T> options, T value, Function<T, Component> words, Consumer<T> change) {
		CFHGuiDropdown<T> d = new CFHGuiDropdown<>(0, 0, this.width, options, value, words, change).label(Tr.field(name));
		d.setHeight(18);
		d.tip(Tr.tip(name));
		this.stop(name, d);
		return this.row(d);
	}

	CFHGuiSlider slider(
		String name, double min, double max, double step, double value, DoubleFunction<String> words, @Nullable DoubleConsumer moving, DoubleConsumer change
	) {
		CFHGuiSlider slider = new CFHGuiSlider(0, 0, this.width, Tr.field(name), min, max, step, value, words, this.live ? change : moving).compact(true);
		if (!this.live) {
			slider.onCommit(change);
		}

		slider.tip(Tr.tip(name));
		this.stop(name, slider);
		return this.row(slider);
	}

	Bits.ShapeRow shape(String name, @Nullable Quest.Shape value, boolean inherits, Quest.Shape inherited, Consumer<Quest.Shape> change) {
		this.label(Tr.field(name));
		Bits.ShapeRow shapes = new Bits.ShapeRow(this.width, value, inherits, inherited, change);
		this.stop(name, shapes);
		return this.row(shapes);
	}

	EditField icon(String name, String value, Pickers.What what, Supplier<QuestIcons.Drawable> shown, Consumer<String> change) {
		this.label(Tr.field(name));
		EditField[] field = new EditField[1];
		Runnable pick = () -> Pickers.open(what, field[0].getValue(), picked -> {
				field[0].set(picked);
				change.accept(picked);
			});
		Bits.IconWell well = new Bits.IconWell(18, Tr.field(name), shown, pick);
		well.tip(Tr.t("pick.tip"));
		this.placer.place(well, 0, this.y);
		this.stop(name + ".well", well);
		field[0] = this.specField(name, 21, this.width - 18 - 3, value, what, pick, change);
		this.y += 22;
		return field[0];
	}

	EditField pick(String name, String value, Pickers.What what, Consumer<String> change) {
		this.label(Tr.field(name));
		EditField[] field = new EditField[1];
		Runnable pick = () -> Pickers.open(what, field[0].getValue(), picked -> {
				field[0].set(picked);
				change.accept(picked);
			});
		field[0] = this.specField(name, 0, this.width, value, what, pick, change);
		this.y += 22;
		return field[0];
	}

	private EditField specField(String name, int x, int w, String value, Pickers.What what, Runnable pick, Consumer<String> change) {
		EditField f = this.fieldAt(name, x, w - 18 - 2, value, 2048, change);
		f.valid(text -> Pickers.readable(what, text));
		CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 18, Icon.SEARCH, Tr.t("pick.tip"), pick);
		this.stop(name + ".pick", more);
		this.placer.place(more, x + w - 18, this.y);
		return f;
	}

	void color(String name, String hex, int fallback, Consumer<String> change) {
		int[] rowY = new int[]{this.y};
		CFHGuiIconButton[] none = new CFHGuiIconButton[1];
		CFHGuiColorField color = new CFHGuiColorField(0, 0, 88, Colors.fromHex(hex, fallback), c -> {
			change.accept(Colors.toHex(c));
			none[0].active = true;
		});
		color.setHeight(18);
		color.tip(Tr.tip(name));
		this.stop(name, color);
		none[0] = new CFHGuiIconButton(0, 0, 18, Icon.CLOSE, Tr.t("color.none"), () -> {
			color.setColor(fallback);
			change.accept("");
			none[0].active = false;
		});
		none[0].active = !hex.isEmpty();
		this.stop(name + ".none", none[0]);
		this.placer.place(new CFHGuiLabel(0, 0, Math.max(10, this.width - 88 - 18 - 8), Tr.field(name)), 1, rowY[0] + 5);
		this.placer.place(color, this.width - 88 - 18 - 2, rowY[0]);
		this.placer.place(none[0], this.width - 18, rowY[0]);
		this.y += 22;
	}

	CFHGuiButton button(String name, Component label, @Nullable Icon icon, Runnable press) {
		CFHGuiButton b = new CFHGuiButton(0, 0, this.width, 18, label, press).icon(icon);
		b.iconSize(8);
		this.stop(name, b);
		return this.row(b);
	}

	CFHGuiButton[] buttons(
		String nameA, Component labelA, @Nullable Icon iconA, Runnable pressA, String nameB, Component labelB, @Nullable Icon iconB, Runnable pressB, boolean dangerB
	) {
		int half = (this.width - 4) / 2;
		CFHGuiButton a = new CFHGuiButton(0, 0, half, 18, labelA, pressA).icon(iconA).iconSize(8);
		CFHGuiButton b = new CFHGuiButton(0, 0, this.width - half - 4, 18, labelB, pressB).icon(iconB).iconSize(8);
		if (dangerB) {
			b.variant(Variant.DANGER);
		}

		this.stop(nameA, a);
		this.stop(nameB, b);
		this.placer.place(a, 0, this.y);
		this.placer.place(b, half + 4, this.y);
		this.y += 22;
		return new CFHGuiButton[]{a, b};
	}

	@FunctionalInterface
	interface Placer {
		void place(AbstractWidget var1, int var2, int var3);
	}
}
