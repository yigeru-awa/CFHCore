package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Travel;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class WarpsPage extends ServerPage {
	private final boolean places;
	private CFHGuiScrollPanel panel;
	private String query = "";
	private boolean waiting;

	public WarpsPage(ServerScreen screen, String id, Component title, boolean places) {
		super(screen, id, title, places ? Icon.MAP : Icon.PIN);
		this.places = places;
	}

	@Nullable
	@Override
	protected String feature() {
		return this.places ? null : "warps";
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		if (ServerState.may(Nodes.WARP_SET)) {
			CFHGuiButton create = new CFHGuiButton(0, 0, 110, Txt.t("warp.create_here"), () -> new Forms.WarpForm(null).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
			create.setWidth(create.preferredWidth() + 14);
			create.tip(Txt.t("warp.create_here.tip"));
			bar.right(create);
		}

		this.panel = null;
		CFHGuiSearchField search = bar.fill(new CFHGuiSearchField(0, 0, 160, q -> {
			String typed = q.trim().toLowerCase(Locale.ROOT);
			if (!typed.equals(this.query)) {
				this.query = typed;
				if (this.panel != null) {
					refill(this.panel, this::fill);
					this.panel.snapScroll(0.0);
				}
			}
		}), 220);
		search.setValue(this.query);
		search.placeholder(Txt.t("warp.search"));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
	}

	@Override
	public void stateChanged() {
		if (this.panel == null) {
			super.stateChanged();
		} else {
			refill(this.panel, this::fill);
		}
	}

	public void tick() {
		if (this.panel != null && this.waiting && Travel.cooldownLeft("tp") <= 0L) {
			this.stateChanged();
		}
	}

	private void fill() {
		int w = this.panel.innerWidth();
		this.waiting = Travel.cooldownLeft("tp") > 0L;
		int y = 0;
		Place spawn = ServerState.warps().spawn;
		if (this.places && (spawn != null || ServerState.may(Nodes.SPAWN_SET))) {
			y = section(this.panel, y, w, Txt.t("spawn.section"));
			this.panel.add(this.spawn(spawn, w), 0, y);
			y += 44;
		}

		List<Warp> warps = new ArrayList<>();

		for (Warp warp : ServerState.warps().warps) {
			if (this.query.isEmpty() || warp.name.toLowerCase(Locale.ROOT).contains(this.query) || warp.description.toLowerCase(Locale.ROOT).contains(this.query)) {
				warps.add(warp);
			}
		}

		warps.sort(Comparator.comparing(x -> x.name, String.CASE_INSENSITIVE_ORDER));
		if (this.places) {
			y = section(this.panel, y + 2, w, Txt.t("warp.section", ServerState.warps().warps.size()));
		}

		if (warps.isEmpty()) {
			boolean none = ServerState.warps().warps.isEmpty();
			int h = Math.max(96, this.panel.getHeight() - y - 4);
			this.panel
				.add(
					new PaintedCard(
							w,
							h,
							(g, f) -> Ui.empty(
									g,
									new Rect(f.x(), f.y(), f.w(), f.h()),
									none ? Icon.PIN : Icon.SEARCH,
									Txt.t(none ? "warp.none.title" : "warp.no_match.title"),
									none ? Txt.t(ServerState.may(Nodes.WARP_SET) ? "warp.none.may" : "warp.none") : Txt.t("warp.no_match", this.query),
									f.alpha()
								)
						)
						.bare(),
					0,
					y
				);
			this.panel.setContentHeight(y + h);
		} else {
			y = grid(this.panel, y, w, warps.size(), 210, 68, (i, cw) -> this.warp(warps.get(i), cw));
			this.panel.setContentHeight(y + 2);
		}
	}

	private PaintedCard spawn(@Nullable Place spawn, int w) {
		PaintedCard card = new PaintedCard(
				w,
				40,
				(g, f) -> {
					float a = f.alpha();
					Ui.glyphTile(g, Icon.WORLD, f.x() + 8, f.y() + 7, 26, 7323486, a);
					Ui.textFit(g, Fonts.heading(Txt.t("travel.spawn")), f.x() + 42, f.y() + 9, f.w() - 240, Ui.strong(), a);
					Ui.textFit(
						g,
						spawn == null ? Txt.t("spawn.world") : Component.literal("").append(Txt.dimension(spawn.dimension)).append("  ·  ").append(Travel.where(spawn)),
						f.x() + 42,
						f.y() + 21,
						f.w() - 240,
						Ui.soft(),
						a
					);
				}
			)
			.flat()
			.narrate(Txt.t("travel.spawn"));
		int right = w - 8;
		if (ServerState.may(Nodes.SPAWN_SET)) {
			CFHGuiButton set = new CFHGuiButton(
				0,
				0,
				100,
				18,
				Txt.t("spawn.set_here"),
				() -> Forms.confirm(Txt.t("spawn.set.title"), Txt.t("spawn.set.body"), Txt.t("spawn.set_here"), () -> ServerActions.spawnSet(null))
			);
			set.setWidth(set.preferredWidth() + 12);
			right -= set.getWidth();
			card.add(set, right, 11);
			right -= 6;
		}

		if (spawn != null && ServerState.may(Nodes.SPAWN_USE)) {
			CFHGuiButton go = new CFHGuiButton(0, 0, 76, 18, Txt.t("travel.go"), ServerActions::spawnGo);
			go.icon(Icon.ARROW_RIGHT).iconSize(8);
			card.add(go, right - 76, 11);
		}

		return card;
	}

	private PaintedCard warp(Warp warp, int w) {
		boolean maySet = ServerState.may(Nodes.WARP_SET);
		PaintedCard card = new PaintedCard(
				w,
				68,
				(g, f) -> {
					Theme t = Theme.current();
					Palette p = t.palette();
					float a = f.alpha();
					Ui.wash(g, f.x() + 1, f.y() + 1, Math.min(f.w() - 2, 120), f.h() - 2, warp.color, a * 0.8F);
					Ui.markTile(g, warp.icon, Icon.PIN, f.x() + 8, f.y() + 8, 26, warp.color, a);
					int right = Ui.chipRight(g, Txt.dimension(warp.place.dimension), f.x() + f.w() - 8, f.y() + 7, HomesPage.dimensionColor(warp.place.dimension, p), a);
					if (warp.restricted) {
						right -= 14;
						Icons.draw(g, Icon.LOCK, right, f.y() + 9, 8, Colors.scaleAlpha(p.warning(), a));
						f.tip(right - f.x(), 6, 12, 12, Txt.t("warp.restricted.mark"));
					}

					Ui.textFit(g, Fonts.heading(warp.name), f.x() + 42, f.y() + 9, right - f.x() - 48, Ui.ink(warp.color), a);
					Component about = warp.description.isEmpty() ? Txt.t("warp.no_description") : Component.literal(warp.description);
					Ui.textFit(g, about, f.x() + 42, f.y() + 21, f.w() - 50, warp.description.isEmpty() ? Ui.caption() : Ui.soft(), a);
					Ui.textFit(g, Travel.where(warp.place), f.x() + 42, f.y() + 32, f.w() - 50, Ui.caption(), a);
					Component foot = this.places && !warp.createdByName.isEmpty()
						? Txt.t("warp.visits.by", Txt.number(warp.visits), warp.createdByName)
						: Txt.t("warp.visits", Txt.number(warp.visits));
					Ui.textFit(g, foot, f.x() + 8, f.y() + 50, f.w() - (maySet ? 122 : 100), Ui.caption(), a);
				}
			)
			.flat()
			.narrate(Component.literal(warp.name));
		CFHGuiButton go = new CFHGuiButton(0, 0, 76, 16, Txt.t("travel.go"), () -> ServerActions.warpGo(warp.name));
		go.icon(Icon.ARROW_RIGHT).iconSize(8);
		boolean mayGo = ServerState.may(Nodes.WARP_USE);
		go.enabled(mayGo && !this.waiting);
		if (!mayGo) {
			go.tip(Txt.t("denied"));
		} else if (this.waiting) {
			go.tip(Txt.t("travel.wait.tip"));
		}

		card.add(go, w - 84, 46);
		if (maySet) {
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 16, Icon.DOTS, Txt.t("more"), null);
			more.onPress(() -> this.menu(warp, more.getX() - 96, more.getY() + 17));
			card.add(more, w - 104, 46);
			card.onRightClick(() -> this.menu(warp, Ui.mouseX(), Ui.mouseY()));
		}

		return card;
	}

	private void menu(Warp warp, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("warp.edit"), Icon.EDIT, () -> new Forms.WarpForm(warp).show()));
		items.add(
			Item.of(
				Txt.t("home.move"),
				Icon.PIN,
				() -> Forms.confirm(
						Txt.t("warp.move.title", warp.name),
						Txt.t("warp.move.body", warp.name),
						Txt.t("home.move"),
						() -> ServerActions.warpSet(warp.name, null, -1, null, warp.restricted, null)
					)
			)
		);
		items.add(Item.sep());
		items.add(
			Item.danger(
				Txt.t("form.delete"),
				Icon.TRASH,
				() -> Forms.confirmDelete(Txt.t("warp.delete.title", warp.name), Txt.t("warp.delete.body"), () -> ServerActions.warpDelete(warp.name, null))
			)
		);
		CFHGuiContextMenu.open((double)x, (double)y, items);
	}
}
