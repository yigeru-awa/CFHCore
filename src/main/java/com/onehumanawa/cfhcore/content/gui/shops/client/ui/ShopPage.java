package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class ShopPage extends SidebarPage {
	protected static final int BAR_H = 20;
	protected static final int BAR_GAP = 6;
	protected final SidebarScreen host;
	private final Runnable changed = () -> this.dirty = true;
	private boolean listening;
	private boolean dirty;
	@Nullable
	private CFHGuiScrollPanel list;
	private double listAt;

	protected ShopPage(SidebarScreen host, String id, Component title, Icon icon) {
		super(id, title, icon);
		this.host = host;
	}

	@Nullable
	protected String needs() {
		return null;
	}

	public final void build(SidebarScreen s, Rect area) {
		if (!this.listening) {
			this.listening = true;
			ShopsState.CHANGED.register(this.changed);
		}

		this.dirty = false;
		if (!ShopsState.available()) {
			boolean inWorld = Minecraft.getInstance().level != null;
			s.addPageWidget(
				empty(area, Icon.TAG, Txt.t(inWorld ? "state.no_server.title" : "state.no_world.title"), Txt.t(inWorld ? "state.no_server" : "state.no_world"))
			);
		} else if (ShopsState.newerServer()) {
			s.addPageWidget(empty(area, Icon.DOWNLOAD, Txt.t("state.newer.title"), Txt.t("state.newer")));
		} else {
			String needs = this.needs();
			if (needs != null && !ShopsState.may(needs)) {
				s.addPageWidget(empty(area, Icon.LOCK, Txt.t("state.denied.title"), Txt.t("state.denied")));
			} else {
				this.buildPage(s, area);
			}
		}
	}

	protected abstract void buildPage(SidebarScreen var1, Rect var2);

	protected CFHGuiScrollPanel list(SidebarScreen s, Rect r) {
		this.listAt = this.list == null ? 0.0 : this.list.scrollAmount();
		this.list = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		return this.list;
	}

	protected void settle() {
		if (this.list != null) {
			this.list.snapScroll(this.listAt);
		}
	}

	protected void resetList() {
		this.list = null;
	}

	public void tick() {
		if (this.dirty) {
			this.dirty = false;
			this.stateChanged();
		}
	}

	protected void stateChanged() {
		this.host.refreshPage();
	}

	public void onHide() {
		if (this.listening) {
			this.listening = false;
			ShopsState.CHANGED.unregister(this.changed);
		}
	}

	public static Consumer<ShopsNet.Result> after(Runnable then) {
		return r -> {
			if (r.ok()) {
				then.run();
			} else {
				CFHGuiToasts.show(Txt.t("failed"), ShopsActions.describe(r), Icon.WARNING);
			}
		};
	}

	public static Consumer<ShopsNet.Result> told(String op) {
		return r -> {
			if (r.ok()) {
				CFHGuiToasts.show(Txt.t("done"), Component.translatable("cfhgui_shops.done." + op, r.args().toArray()), Icon.CHECK);
			} else {
				CFHGuiToasts.show(Txt.t("failed"), ShopsActions.describe(r), Icon.WARNING);
			}
		};
	}

	protected static PaintedCard empty(Rect area, Icon icon, Component title, @Nullable Component body) {
		PaintedCard card = new PaintedCard(area.w(), area.h(), (g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), icon, title, body, f.alpha())).bare();
		card.setX(area.x());
		card.setY(area.y());
		card.narrate(title);
		return card;
	}

	protected static PaintedCard empty(Rect area, Icon icon, Component title, @Nullable Component body, AbstractWidget button) {
		PaintedCard card = empty(area, icon, title, body);
		card.add(button, (area.w() - button.getWidth()) / 2, Ui.emptyButtonsY(area.h(), body, area.w()));
		return card;
	}

	protected static int section(CFHGuiScrollPanel panel, int y, int w, Component caption) {
		panel.add(new CFHGuiSeparator(0, 0, w, caption), 0, y);
		return y + 14;
	}

	protected static void refill(CFHGuiScrollPanel panel, Runnable fill) {
		double at = panel.scrollAmount();
		panel.clear();
		fill.run();
		panel.snapScroll(at);
	}

	protected static int grid(CFHGuiScrollPanel panel, int y, int w, int count, int minWidth, int cardHeight, ShopPage.GridCard make) {
		int gap = 6;
		int cols = Math.max(1, Math.min(Math.max(1, count), (w + 6) / (minWidth + 6)));
		int cw = (w - 6 * (cols - 1)) / cols;

		for (int i = 0; i < count; i++) {
			panel.add(make.make(i, cw), i % cols * (cw + 6), y + i / cols * (cardHeight + 6));
		}

		return y + (count + cols - 1) / cols * (cardHeight + 6);
	}

	protected static Rect under(Rect area) {
		return new Rect(area.x(), area.y() + 20 + 6, area.w(), area.h() - 20 - 6);
	}

	protected static final class Bar {
		private final SidebarScreen s;
		private final int y;
		private int left;
		private int right;

		public Bar(SidebarScreen s, Rect area) {
			this.s = s;
			this.y = area.y();
			this.left = area.x();
			this.right = area.right();
		}

		public <T extends AbstractWidget> T right(T w) {
			w.setX(this.right - w.getWidth());
			w.setY(this.y + (20 - w.getHeight()) / 2);
			this.right = this.right - (w.getWidth() + 4);
			return (T)this.s.addPageWidget(w);
		}

		public <T extends AbstractWidget> T left(T w) {
			w.setX(this.left);
			w.setY(this.y + (20 - w.getHeight()) / 2);
			this.left = this.left + w.getWidth() + 4;
			return (T)this.s.addPageWidget(w);
		}

		public int room() {
			return Math.max(0, this.right - this.left);
		}

		public <T extends AbstractWidget> T fill(T w, int maxWidth) {
			w.setWidth(Math.max(40, Math.min(maxWidth, this.room())));
			return this.left(w);
		}
	}

	@FunctionalInterface
	protected interface GridCard {
		AbstractWidget make(int var1, int var2);
	}
}
