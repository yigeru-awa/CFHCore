package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.Confirm;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.OfferDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.Prompt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.TradeDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class OfferRows {
	static final int ROW_H = 34;
	private static final int COMPACT_BELOW = 380;

	static PaintedCard row(Shop shop, Offer offer, int w, OfferRows.Mode mode) {
		ItemStack stack = ShopsState.stack(offer);
		List<AbstractWidget> buttons = new ArrayList<>();
		List<Integer> at = new ArrayList<>();
		int right = w - 7;
		if (mode == OfferRows.Mode.CUSTOMER) {
			if (offer.sells()) {
				boolean can = shop.open && (shop.server || offer.stock >= (long)Math.max(1, stack.getCount()));
				CFHGuiButton buy = new CFHGuiButton(0, 0, 60, Txt.t("offer.buy", ShopsState.money(offer.price)), () -> trade(shop, offer, true)).variant(Variant.PRIMARY);
				buy.setWidth(buy.preferredWidth() + 12);
				buy.active = can && !stack.isEmpty();
				buy.tip(Txt.t(can ? "offer.buy.tip" : "offer.out.tip"));
				right -= buy.getWidth();
				buttons.add(buy);
				at.add(right);
				right -= 4;
			}

			if (offer.buys()) {
				CFHGuiButton sell = new CFHGuiButton(0, 0, 60, Txt.t("offer.sell", ShopsState.money(offer.sellPrice)), () -> trade(shop, offer, false))
					.variant(Variant.SECONDARY);
				sell.setWidth(sell.preferredWidth() + 12);
				sell.active = shop.open && !stack.isEmpty();
				sell.tip(Txt.t("offer.sell.tip"));
				right -= sell.getWidth();
				buttons.add(sell);
				at.add(right);
			}
		} else {
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 18, Icon.DOTS, Txt.t("more"), null);
			more.onPress(() -> menu(shop, offer, more.getX() - 120, more.getY() + 19));
			right -= 18;
			buttons.add(more);
			at.add(right);
			if (mode == OfferRows.Mode.OWNER) {
				boolean compact = w < 380;
				Runnable takeOut = () -> take(shop, offer);
				Runnable putIn = () -> ShopsActions.stockAdd(shop.id, offer.id, ShopPage.told("stock_add"));
				CFHGuiButton take = (CFHGuiButton)(compact
					? new CFHGuiIconButton(0, 0, 18, Icon.MINUS, Txt.t("stock.take"), takeOut)
					: new CFHGuiButton(0, 0, 50, Txt.t("stock.take"), takeOut).variant(Variant.SECONDARY));
				CFHGuiButton add = (CFHGuiButton)(compact
					? new CFHGuiIconButton(0, 0, 18, Icon.PLUS, Txt.t("stock.add"), putIn)
					: new CFHGuiButton(0, 0, 50, Txt.t("stock.add"), putIn).variant(Variant.SECONDARY).icon(Icon.PLUS));
				if (!compact) {
					take.setWidth(take.preferredWidth() + 12);
					add.setWidth(add.preferredWidth() + 12);
				}

				take.active = offer.stock > 0L;
				take.tip(List.of(Txt.t("stock.take"), Txt.t("stock.take.tip")));
				add.tip(List.of(Txt.t("stock.add"), Txt.t("stock.add.tip")));
				int var15 = right - (4 + take.getWidth());
				buttons.add(take);
				at.add(var15);
				right = var15 - (4 + add.getWidth());
				buttons.add(add);
				at.add(right);
			}
		}

		int room = Math.max(40, right - 6);
		PaintedCard card = new PaintedCard(w, 34, (g, f) -> paint(g, f, shop, offer, stack, mode, room))
			.flat()
			.narrate((Component)(stack.isEmpty() ? Txt.t("offer.unknown") : stack.getHoverName()));

		for (int i = 0; i < buttons.size(); i++) {
			AbstractWidget b = buttons.get(i);
			card.add(b, at.get(i), (34 - b.getHeight()) / 2);
		}

		if (mode != OfferRows.Mode.CUSTOMER) {
			card.onRightClick(() -> menu(shop, offer, Ui.mouseX(), Ui.mouseY()));
		}

		return card;
	}

	private static void paint(GuiGraphics g, PaintedCard.Frame f, Shop shop, Offer offer, ItemStack stack, OfferRows.Mode mode, int room) {
		Palette p = Theme.current().palette();
		float a = f.alpha();
		int ix = f.x() + 6;
		int iy = f.y() + (f.h() - 22) / 2;
		Ui.well(g, ix, iy, 22, 22, a);
		if (!stack.isEmpty()) {
			Ui.item(g, stack, ix + 3, iy + 3, 16, a);
			if (stack.getCount() > 1 && a > 0.3F) {
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 200.0F);
				String n = Integer.toString(stack.getCount());
				g.drawString(CFHGuiDraw.font(), n, ix + 21 - CFHGuiDraw.width(n), iy + 13, -1, true);
				g.pose().popPose();
			}

			Minecraft mc = Minecraft.getInstance();
			f.tip(ix - f.x(), iy - f.y(), 22, 22, Screen.getTooltipFromItem(mc, stack));
		}

		int tx = f.x() + 36;
		int end = f.x() + room;
		Component name = (Component)(stack.isEmpty() ? Txt.t("offer.unknown") : stack.getHoverName());
		Ui.textFit(g, name, tx, f.y() + 7, end - tx, Ui.strong(), a);
		int cx = tx;
		if (!shop.server) {
			boolean out = offer.stock < (long)Math.max(1, stack.getCount());
			Component held = offer.stock == 0L ? Txt.t("offer.none_held") : Txt.t("offer.held", offer.stock);
			cx = chip(g, held, tx, f.y() + 18, out && offer.sells() ? p.danger() : p.textMuted(), a, end);
		}

		if (mode != OfferRows.Mode.CUSTOMER) {
			if (offer.sells()) {
				cx = chip(g, Txt.t("offer.price", ShopsState.money(offer.price)), cx, f.y() + 18, p.success(), a, end);
			}

			if (offer.buys()) {
				cx = chip(g, Txt.t("offer.pays", ShopsState.money(offer.sellPrice)), cx, f.y() + 18, p.warning(), a, end);
			}

			if (end - cx > 50) {
				Ui.textFit(g, Txt.t("offer.traded", offer.sold, offer.bought), cx, f.y() + 20, end - cx, Ui.caption(), a);
			}
		} else if (stack.getCount() > 1 && end - cx > 40) {
			Ui.textFit(g, Txt.t("offer.lot", stack.getCount()), cx, f.y() + 20, end - cx, Ui.caption(), a);
		}
	}

	private static int chip(GuiGraphics g, Component label, int x, int y, int color, float alpha, int end) {
		return x + Ui.chipWidth(label) > end ? x : x + Ui.chip(g, label, x, y, color, alpha) + 4;
	}

	private static void trade(Shop shop, Offer offer, boolean buy) {
		if (Screen.hasShiftDown()) {
			if (buy) {
				ShopsActions.buy(shop.id, offer.id, 1, ShopPage.told("buy"));
			} else {
				ShopsActions.sell(shop.id, offer.id, 1, ShopPage.told("sell"));
			}
		} else {
			new TradeDialog(shop, offer, buy).show();
		}
	}

	private static void take(Shop shop, Offer offer) {
		new Prompt(
				Txt.t("stock.take.title"),
				Icon.PACK,
				Txt.t("stock.take.caption", offer.stock),
				Long.toString(offer.stock),
				10,
				Txt.t("stock.take"),
				v -> v.matches("\\d{1,10}") && Long.parseLong(v) > 0L,
				(v, then) -> ShopsActions.stockTake(shop.id, offer.id, Long.parseLong(v), then)
			)
			.done("stock_take")
			.show();
	}

	private static void menu(Shop shop, Offer offer, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("offer.edit"), Icon.EDIT, () -> new OfferDialog(shop, offer).show()));
		items.add(Item.sep());
		if (offer.stock > 0L) {
			items.add(Item.disabled(Txt.t("offer.remove.held"), Icon.TRASH));
		} else {
			items.add(
				Item.danger(
					Txt.t("offer.remove"),
					Icon.TRASH,
					() -> new Confirm(
								Txt.t("offer.remove.title"), Txt.t("offer.remove.body"), Txt.t("offer.remove"), then -> ShopsActions.offerRemove(shop.id, offer.id, then)
							)
							.show()
				)
			);
		}

		CFHGuiContextMenu.open((double)x, (double)y, items);
	}

	private OfferRows() {
	}

	static enum Mode {
		CUSTOMER,
		OWNER,
		ADMIN;
	}
}
