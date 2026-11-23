package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class ShopCards {
	static final int CARD_H = 50;
	static final int CARD_MIN_W = 190;
	static final Comparator<Shop> ORDER = Comparator.<Shop>comparingInt(s -> s.server ? 0 : 1).thenComparing(s -> s.name.toLowerCase(Locale.ROOT));

	static PaintedCard card(Shop shop, int w, Consumer<Shop> open) {
		ItemStack mark = mark(shop);
		return new PaintedCard(w, 50, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			tile(g, shop, mark, f.x() + 8, f.y() + 8, a);
			Ui.textFit(g, Fonts.heading(Component.literal(shop.name)), f.x() + 42, f.y() + 9, f.w() - 50, Ui.strong(), a);
			int cx = f.x() + 42;
			if (shop.server) {
				cx += Ui.chip(g, Txt.t("shop.server"), cx, f.y() + 21, p.accentInk(), a) + 4;
			} else {
				cx += Ui.chip(g, Txt.t("shop.by", shop.ownerName), cx, f.y() + 21, p.textMuted(), a) + 4;
			}

			if (!shop.open) {
				cx += Ui.chip(g, Txt.t("shop.closed"), cx, f.y() + 21, p.danger(), a) + 4;
			}

			Ui.textFit(g, Txt.t(shop.offers.size() == 1 ? "shop.offers.one" : "shop.offers", shop.offers.size()), f.x() + 42, f.y() + 36, f.w() - 50, Ui.caption(), a);
		}).onClick(() -> open.accept(shop)).narrate(Component.literal(shop.name));
	}

	static int head(CFHGuiScrollPanel panel, Shop shop, int w) {
		ItemStack mark = mark(shop);
		boolean described = !shop.description.isEmpty();
		int h = described ? 50 + Ui.wrappedHeight(Component.literal(shop.description), w - 50, 3) : 42;
		panel.add(new PaintedCard(w, h, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			tile(g, shop, mark, f.x() + 8, f.y() + 8, a);
			Ui.textFit(g, Fonts.heading(Component.literal(shop.name)), f.x() + 42, f.y() + 9, f.w() - 50, Ui.strong(), a);
			int cx = f.x() + 42;
			if (shop.server) {
				cx += Ui.chip(g, Txt.t("shop.server"), cx, f.y() + 21, p.accentInk(), a) + 4;
			} else {
				cx += Ui.chip(g, Txt.t("shop.by", shop.ownerName), cx, f.y() + 21, p.textMuted(), a) + 4;
			}

			cx += Ui.chip(g, shop.open ? Txt.t("shop.open") : Txt.t("shop.closed"), cx, f.y() + 21, shop.open ? p.success() : p.danger(), a) + 4;
			if (!shop.server) {
				Ui.chip(g, Txt.t("shop.holds", shop.stockTotal()), cx, f.y() + 21, p.textMuted(), a);
			}

			if (described) {
				Ui.wrapped(g, Component.literal(shop.description), f.x() + 42, f.y() + 38, f.w() - 50, 3, p.textMuted(), a);
			}
		}).flat().narrate(Component.literal(shop.name)), 0, 0);
		return h + 6;
	}

	static List<Shop> sorted(Predicate<Shop> keep) {
		List<Shop> out = new ArrayList<>();

		for (Shop s : ShopsState.shops()) {
			if (keep.test(s)) {
				out.add(s);
			}
		}

		out.sort(ORDER);
		return out;
	}

	private static ItemStack mark(Shop shop) {
		for (Offer o : shop.offers) {
			ItemStack s = ShopsState.stack(o);
			if (!s.isEmpty()) {
				return s;
			}
		}

		return ItemStack.EMPTY;
	}

	private static void tile(GuiGraphics g, Shop shop, ItemStack mark, int x, int y, float a) {
		Theme t = Theme.current();
		int c = shop.server ? t.palette().accentInk() : t.palette().textMuted();
		if (t.isVanilla()) {
			CFHGuiDraw.rect(g, x, y, 26, 26, Colors.scaleAlpha(-1610612736, a));
			CFHGuiDraw.outline(g, x, y, 26, 26, Colors.scaleAlpha(Colors.withAlpha(c, 192), a), 0);
		} else {
			CFHGuiDraw.pixelRound(g, x, y, 26, 26, Colors.scaleAlpha(Colors.withAlpha(c, 48), a), 2);
			CFHGuiDraw.outline(g, x, y, 26, 26, Colors.scaleAlpha(Colors.withAlpha(c, 144), a), 2);
		}

		if (!mark.isEmpty()) {
			Ui.item(g, mark, x + 5, y + 5, 16, a);
		} else {
			Icons.draw(g, shop.server ? Icon.CROWN : Icon.TAG, x + 7, y + 7, 12, Colors.scaleAlpha(Ui.ink(c & 16777215), a));
		}
	}

	private ShopCards() {
	}
}
