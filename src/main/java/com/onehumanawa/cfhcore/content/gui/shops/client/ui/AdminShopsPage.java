package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.Confirm;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.OfferDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.ShopForm;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class AdminShopsPage extends ShopPage {
	@Nullable
	private String open;

	public AdminShopsPage(String id, SidebarScreen host) {
		super(host, id, Txt.t("page.admin"), Icon.CROWN);
	}

	@Override
	protected String needs() {
		return "admin";
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		Shop shop = ShopsState.shop(this.open);
		if (this.open != null && shop == null) {
			this.open = null;
		}

		ShopPage.Bar bar = new ShopPage.Bar(s, area);
		if (shop != null) {
			CFHGuiButton back = new CFHGuiButton(0, 0, 80, Txt.t("admin.back"), () -> {
				this.open = null;
				this.resetList();
				this.host.refreshPage();
			}).variant(Variant.GHOST).icon(Icon.ARROW_LEFT);
			back.setWidth(back.preferredWidth() + 14);
			bar.left(back);
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 20, Icon.DOTS, Txt.t("more"), null);
			more.onPress(() -> menu(shop, more.getX() - 130, more.getY() + 21));
			bar.right(more);
			if (shop.server) {
				CFHGuiButton add = new CFHGuiButton(0, 0, 100, Txt.t("offer.new"), () -> new OfferDialog(shop, null).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
				add.setWidth(add.preferredWidth() + 14);
				add.tip(Txt.t("offer.new.tip"));
				bar.right(add);
			}
		} else {
			CFHGuiButton make = new CFHGuiButton(0, 0, 120, Txt.t("admin.new"), () -> new ShopForm(null, true).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
			make.setWidth(make.preferredWidth() + 14);
			make.tip(Txt.t("admin.new.tip"));
			bar.right(make);
		}

		Rect r = under(area);
		CFHGuiScrollPanel panel = this.list(s, r);
		int w = panel.innerWidth();
		if (shop == null) {
			List<Shop> server = ShopCards.sorted(x -> x.server);
			List<Shop> players = ShopCards.sorted(x -> !x.server);
			int y = section(panel, 0, w, Txt.t("admin.server_shops", server.size()));
			if (server.isEmpty()) {
				panel.add(
					new PaintedCard(w, 90, (g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.CROWN, Txt.t("admin.none.title"), Txt.t("admin.none"), f.alpha()))
						.bare(),
					0,
					y
				);
				y += 96;
			} else {
				y = grid(panel, y, w, server.size(), 190, 50, (i, cw) -> ShopCards.card(server.get(i), cw, this::show));
			}

			if (!players.isEmpty()) {
				y = section(panel, y + 4, w, Txt.t("admin.player_shops", players.size()));
				y = grid(panel, y, w, players.size(), 190, 50, (i, cw) -> ShopCards.card(players.get(i), cw, this::show));
			}

			panel.setContentHeight(y + 2);
			this.settle();
		} else {
			int yx = ShopCards.head(panel, shop, w);
			if (shop.offers.isEmpty()) {
				int h = Math.max(80, panel.getHeight() - yx - 4);
				panel.add(
					new PaintedCard(
							w,
							h,
							(g, f) -> Ui.empty(
									g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.TAG, Txt.t("shop.empty.title"), Txt.t(shop.server ? "admin.no_offers" : "shop.empty"), f.alpha()
								)
						)
						.bare(),
					0,
					yx
				);
				panel.setContentHeight(yx + h);
			} else {
				for (Offer o : shop.offers) {
					panel.add(OfferRows.row(shop, o, w, OfferRows.Mode.ADMIN), 0, yx);
					yx += 38;
				}

				panel.setContentHeight(yx + 2);
				this.settle();
			}
		}
	}

	private void show(Shop shop) {
		this.open = shop.id;
		this.resetList();
		this.host.refreshPage();
	}

	private static void menu(Shop shop, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("shop.edit"), Icon.EDIT, () -> new ShopForm(shop, shop.server).show()));
		items.add(
			Item.of(
				Txt.t(shop.open ? "mine.close" : "mine.reopen"),
				shop.open ? Icon.LOCK : Icon.UNLOCK,
				() -> ShopsActions.edit(shop.id, null, null, !shop.open, after(() -> {
					}))
			)
		);
		items.add(Item.sep());
		if (shop.stockTotal() > 0L) {
			items.add(Item.disabled(Txt.t("shop.delete.held"), Icon.TRASH));
		} else {
			items.add(
				Item.danger(
					Txt.t("shop.delete"),
					Icon.TRASH,
					() -> new Confirm(Txt.t("shop.delete.title", shop.name), Txt.t("shop.delete.body"), Txt.t("shop.delete"), then -> ShopsActions.delete(shop.id, then))
							.show()
				)
			);
		}

		CFHGuiContextMenu.open((double)x, (double)y, items);
	}
}
