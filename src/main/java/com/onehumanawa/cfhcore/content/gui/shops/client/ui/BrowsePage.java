package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class BrowsePage extends ShopPage {
	@Nullable
	private static String pending;
	private String query = "";
	private BrowsePage.Filter filter = BrowsePage.Filter.ALL;
	@Nullable
	private String open;
	@Nullable
	private CFHGuiScrollPanel panel;

	BrowsePage(SidebarScreen host) {
		super(host, "browse", Txt.t("page.browse"), Icon.TAG);
	}

	static void openOn(String shop) {
		pending = shop;
	}

	@Override
	protected String needs() {
		return "use";
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		if (pending != null) {
			this.open = pending;
			pending = null;
		}

		Shop shop = ShopsState.shop(this.open);
		if (this.open != null && shop == null) {
			this.open = null;
		}

		ShopPage.Bar bar = new ShopPage.Bar(s, area);
		if (shop != null) {
			CFHGuiButton back = new CFHGuiButton(0, 0, 80, Txt.t("browse.back"), () -> {
				this.open = null;
				this.host.refreshPage();
			}).variant(Variant.GHOST).icon(Icon.ARROW_LEFT);
			back.setWidth(back.preferredWidth() + 14);
			bar.left(back);
		} else {
			CFHGuiSegmented<BrowsePage.Filter> kinds = new CFHGuiSegmented<>(
				0, 0, 180, List.of(BrowsePage.Filter.values()), this.filter, f -> Txt.t("browse.filter." + f.name().toLowerCase(Locale.ROOT)), f -> {
					this.filter = f;
					this.refillPanel();
				}
			);
			bar.right(kinds);
			CFHGuiSearchField search = new CFHGuiSearchField(0, 0, 200, v -> {
				this.query = v.trim().toLowerCase(Locale.ROOT);
				this.refillPanel();
			});
			search.setValue(this.query);
			bar.fill(search, 260);
		}

		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
	}

	@Override
	protected void stateChanged() {
		if (this.open != null && ShopsState.shop(this.open) == null) {
			this.open = null;
			this.host.refreshPage();
		} else {
			this.refillPanel();
		}
	}

	private void refillPanel() {
		if (this.panel != null) {
			refill(this.panel, this::fill);
		}
	}

	private void fill() {
		CFHGuiScrollPanel p = this.panel;
		if (p != null) {
			int w = p.innerWidth();
			Shop shop = ShopsState.shop(this.open);
			if (shop != null) {
				this.fillShop(p, shop, w);
			} else {
				List<Shop> shops = ShopCards.sorted(this::shown);
				if (shops.isEmpty()) {
					int h = Math.max(96, p.getHeight() - 4);
					boolean none = ShopsState.shops().isEmpty();
					p.add(
						new PaintedCard(
								w,
								h,
								(g, f) -> Ui.empty(
										g,
										new Rect(f.x(), f.y(), f.w(), f.h()),
										Icon.TAG,
										Txt.t(none ? "browse.none.title" : "browse.no_match.title"),
										Txt.t(none ? "browse.none" : "browse.no_match"),
										f.alpha()
									)
							)
							.bare(),
						0,
						0
					);
					p.setContentHeight(h);
				} else {
					int y = grid(p, 0, w, shops.size(), 190, 50, (i, cw) -> ShopCards.card(shops.get(i), cw, s -> {
							this.open = s.id;
							this.host.refreshPage();
						}));
					p.setContentHeight(y + 2);
				}
			}
		}
	}

	private void fillShop(CFHGuiScrollPanel p, Shop shop, int w) {
		int y = ShopCards.head(p, shop, w);
		boolean mine = shop.ownedBy(ShopsState.me());
		if (shop.offers.isEmpty()) {
			int h = Math.max(80, p.getHeight() - y - 4);
			p.add(
				new PaintedCard(
						w,
						h,
						(g, f) -> Ui.empty(
								g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.TAG, Txt.t("shop.empty.title"), Txt.t(mine ? "shop.empty.mine" : "shop.empty"), f.alpha()
							)
					)
					.bare(),
				0,
				y
			);
			p.setContentHeight(y + h);
		} else {
			OfferRows.Mode mode = mine ? OfferRows.Mode.OWNER : OfferRows.Mode.CUSTOMER;

			for (Offer o : shop.offers) {
				p.add(OfferRows.row(shop, o, w, mode), 0, y);
				y += 38;
			}

			p.setContentHeight(y + 2);
		}
	}

	private boolean shown(Shop s) {
		if ((this.filter != BrowsePage.Filter.SERVER || s.server) && (this.filter != BrowsePage.Filter.PLAYERS || !s.server)) {
			if (this.query.isEmpty()) {
				return true;
			} else if (!s.name.toLowerCase(Locale.ROOT).contains(this.query) && !s.ownerName.toLowerCase(Locale.ROOT).contains(this.query)) {
				for (Offer o : s.offers) {
					ItemStack stack = ShopsState.stack(o);
					if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(this.query)) {
						return true;
					}
				}

				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private static enum Filter {
		ALL,
		SERVER,
		PLAYERS;
	}
}
