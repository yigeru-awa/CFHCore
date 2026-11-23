package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList.RowRenderer;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class PickForm<T> implements FormPart {
	private static final int ROWS = 5;
	private final Component title;
	private final Icon icon;
	private final Component submit;
	private final List<T> all;
	private final Function<T, String> words;
	private final RowRenderer<T> row;
	private final int rowHeight;
	private final Consumer<T> onPick;
	private CFHGuiSearchField search;
	private CFHGuiList<T> list;
	private FormPart.Host host;

	public PickForm(Component title, Icon icon, Component submit, List<T> all, Function<T, String> words, RowRenderer<T> row, int rowHeight, Consumer<T> onPick) {
		this.title = title;
		this.icon = icon;
		this.submit = submit;
		this.all = new ArrayList<>(all);
		this.words = words;
		this.row = row;
		this.rowHeight = rowHeight;
		this.onPick = onPick;
	}

	public PickForm<T> host(FormPart.Host host) {
		this.host = host;
		return this;
	}

	@Override
	public Component title() {
		return this.title;
	}

	@Override
	public Icon icon() {
		return this.icon;
	}

	@Override
	public int preferredWidth() {
		return 216;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		this.search = new CFHGuiSearchField(0, 0, width, q -> this.filter());
		placer.place(this.search, 0, 0);
		int listH = 5 * (this.rowHeight + 2) - 2;
		this.list = new CFHGuiList(0, 0, width, listH, this.rowHeight, this.row).gap(2).emptyText(TeamsText.t("pick.none"));
		this.list.onActivate(item -> {
			if (this.host != null) {
				this.submit(this.host);
			}
		});
		placer.place(this.list, 0, 26);
		this.filter();
		return 26 + listH;
	}

	private void filter() {
		String q = this.search.getValue().trim().toLowerCase(Locale.ROOT);
		List<T> shown = new ArrayList<>();

		for (T item : this.all) {
			if (q.isEmpty() || this.words.apply(item).toLowerCase(Locale.ROOT).contains(q)) {
				shown.add(item);
			}
		}

		this.list.clearSelection();
		this.list.items(shown);
		if (shown.size() == 1) {
			this.list.select(0);
		}
	}

	@Override
	public Component submitLabel() {
		return this.submit;
	}

	@Override
	public boolean ready() {
		return this.list.selectedItem() != null;
	}

	@Override
	public boolean mayBeRefused() {
		return false;
	}

	@Override
	public void submit(FormPart.Host host) {
		T item = (T)this.list.selectedItem();
		if (item != null) {
			host.done();
			this.onPick.accept(item);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return (keyCode == 264 || keyCode == 265) && !this.list.items().isEmpty() ? this.list.keyPressed(keyCode, scanCode, modifiers) : false;
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.search;
	}
}
