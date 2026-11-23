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

final class MyShopPage extends ShopPage {
	MyShopPage(SidebarScreen host) {
		super(host, "mine", Txt.t("page.mine"), Icon.HOME);
	}

	@Override
	protected String needs() {
		return "create";
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		Shop shop = ShopsState.mine();
		if (shop == null) {
			if (!ShopsState.hello().playerShops) {
				s.addPageWidget(empty(area, Icon.LOCK, Txt.t("mine.off.title"), Txt.t("mine.off")));
			} else {
				CFHGuiButton make = new CFHGuiButton(0, 0, 120, Txt.t("mine.open"), () -> new ShopForm(null, false).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
				make.setWidth(make.preferredWidth() + 14);
				s.addPageWidget(empty(area, Icon.HOME, Txt.t("mine.none.title"), Txt.t("mine.none"), make));
			}
		} else {
			ShopPage.Bar bar = new ShopPage.Bar(s, area);
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 20, Icon.DOTS, Txt.t("more"), null);
			more.onPress(() -> menu(shop, more.getX() - 130, more.getY() + 21));
			bar.right(more);
			CFHGuiButton add = new CFHGuiButton(0, 0, 100, Txt.t("offer.new"), () -> new OfferDialog(shop, null).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
			add.setWidth(add.preferredWidth() + 14);
			add.active = shop.offers.size() < ShopsState.hello().maxOffers;
			add.tip(add.active ? Txt.t("offer.new.tip") : Txt.t("offer.new.full", ShopsState.hello().maxOffers));
			bar.right(add);
			CFHGuiButton stockAll = new CFHGuiButton(0, 0, 100, Txt.t("stock.add_all"), () -> ShopsActions.stockAdd(shop.id, 0, told("stock_add")))
				.variant(Variant.SECONDARY)
				.icon(Icon.PACK);
			stockAll.setWidth(stockAll.preferredWidth() + 14);
			stockAll.active = !shop.offers.isEmpty();
			stockAll.tip(Txt.t("stock.add_all.tip"));
			bar.right(stockAll);
			CFHGuiButton openClose = new CFHGuiButton(
					0, 0, 80, Txt.t(shop.open ? "mine.close" : "mine.reopen"), () -> ShopsActions.edit(shop.id, null, null, !shop.open, after(() -> {
						}))
				)
				.variant(Variant.GHOST)
				.icon(shop.open ? Icon.LOCK : Icon.UNLOCK);
			openClose.setWidth(openClose.preferredWidth() + 14);
			openClose.tip(Txt.t(shop.open ? "mine.close.tip" : "mine.reopen.tip"));
			bar.left(openClose);
			Rect r = under(area);
			CFHGuiScrollPanel panel = this.list(s, r);
			int w = panel.innerWidth();
			int y = ShopCards.head(panel, shop, w);
			if (shop.offers.isEmpty()) {
				int h = Math.max(80, panel.getHeight() - y - 4);
				panel.add(
					new PaintedCard(
							w, h, (g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.TAG, Txt.t("mine.no_offers.title"), Txt.t("mine.no_offers"), f.alpha())
						)
						.bare(),
					0,
					y
				);
				panel.setContentHeight(y + h);
			} else {
				for (Offer o : shop.offers) {
					panel.add(OfferRows.row(shop, o, w, OfferRows.Mode.OWNER), 0, y);
					y += 38;
				}

				panel.setContentHeight(y + 2);
				this.settle();
			}
		}
	}

	static void menu(Shop shop, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("shop.edit"), Icon.EDIT, () -> new ShopForm(shop, false).show()));
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
