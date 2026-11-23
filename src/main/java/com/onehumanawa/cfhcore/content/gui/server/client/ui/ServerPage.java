package com.onehumanawa.cfhcore.content.gui.server.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class ServerPage extends SidebarPage {
	protected static final int BAR_H = 20;
	protected static final int BAR_GAP = 6;
	protected final ServerScreen screen;

	protected ServerPage(ServerScreen screen, String id, Component title, Icon icon) {
		super(id, title, icon);
		this.screen = screen;
	}

	@Nullable
	protected String feature() {
		return null;
	}

	@Nullable
	protected Node node() {
		return null;
	}

	public final void build(SidebarScreen s, Rect area) {
		boolean inWorld = Minecraft.getInstance().level != null;
		if (!ServerState.available()) {
			s.addPageWidget(
				empty(
					area,
					this.icon(),
					Txt.t(inWorld ? "state.no_server.title" : "state.no_world.title"),
					Component.translatable(inWorld ? "cfhgui_server.state.no_server" : "cfhgui_server.state.no_world")
				)
			);
		} else if (ServerState.newerServer()) {
			s.addPageWidget(empty(area, Icon.DOWNLOAD, Txt.t("state.newer.title"), Component.translatable("cfhgui_server.state.newer_server")));
		} else {
			String feature = this.feature();
			if (feature != null && !ServerState.feature(feature)) {
				s.addPageWidget(empty(area, this.icon(), Txt.t("state.off.title"), Txt.t("state.off")));
			} else {
				Node node = this.node();
				if (ServerState.may(this.screen.menuNode()) && (node == null || ServerState.may(node))) {
					this.buildPage(s, area);
				} else {
					s.addPageWidget(empty(area, Icon.LOCK, Txt.t("state.denied.title"), Txt.t("state.denied")));
				}
			}
		}
	}

	protected abstract void buildPage(SidebarScreen var1, Rect var2);

	public void stateChanged() {
		this.screen.refreshPage();
	}

	public void dashboardChanged() {
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

	protected static int grid(CFHGuiScrollPanel panel, int y, int w, int count, int minWidth, int cardHeight, ServerPage.GridCard make) {
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

	protected static <T> List<T> each(int n, IntFunction<T> make) {
		List<T> out = new ArrayList<>(n);

		for (int i = 0; i < n; i++) {
			out.add(make.apply(i));
		}

		return out;
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

		public int leftX() {
			return this.left;
		}

		public int y() {
			return this.y;
		}
	}

	@FunctionalInterface
	protected interface GridCard {
		AbstractWidget make(int var1, int var2);
	}
}
