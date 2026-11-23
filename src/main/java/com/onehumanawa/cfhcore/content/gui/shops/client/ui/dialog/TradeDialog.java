package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import com.onehumanawa.cfhcore.content.gui.shops.service.Pocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class TradeDialog extends ShopDialog {
	private final Shop shop;
	private final Offer offer;
	private final boolean buy;
	private final ItemStack stack;
	private final CFHGuiTextField lots;
	private final CFHGuiButton ok;
	private final int totalY;

	public TradeDialog(Shop shop, Offer offer, boolean buy) {
		super(Txt.t(buy ? "trade.buy.title" : "trade.sell.title", shop.name), buy ? Icon.DOWNLOAD : Icon.UPLOAD, 270);
		this.shop = shop;
		this.offer = offer;
		this.buy = buy;
		this.stack = ShopsState.stack(offer);
		this.cursor = 30;
		this.caption(Txt.t("trade.lots"), 0, this.cursor);
		int y = this.cursor + 11;
		this.add(new CFHGuiIconButton(0, 0, 20, Icon.MINUS, Txt.t("trade.less"), () -> this.step(-1)), 0, y);
		this.lots = this.add(new CFHGuiTextField(0, 0, 56, Txt.t("trade.lots")).maxLength(2), 24, y);
		this.lots.setFilter(s -> s.matches("\\d{0,2}"));
		this.lots.setValue("1");
		this.add(new CFHGuiIconButton(0, 0, 20, Icon.PLUS, Txt.t("trade.more"), () -> this.step(1)), 84, y);
		CFHGuiButton most = this.add(new CFHGuiButton(0, 0, 60, Txt.t("trade.most"), () -> this.lots.setValue(Integer.toString(Math.max(1, this.most())))), 108, y);
		most.variant(Variant.GHOST);
		most.setWidth(most.preferredWidth() + 12);
		this.cursor = y + 20 + 7;
		this.totalY = this.cursor;
		this.cursor += 24;
		this.ok = this.buttons(Txt.t(buy ? "trade.buy" : "trade.sell"), Variant.PRIMARY, this::go);
		this.firstFocus(this.lots);
	}

	private int count() {
		try {
			return this.lots.getValue().isEmpty() ? 0 : Integer.parseInt(this.lots.getValue());
		} catch (NumberFormatException var2) {
			return 0;
		}
	}

	private void step(int by) {
		this.lots.setValue(Integer.toString(Math.max(1, Math.min(64, this.count() + by))));
	}

	private int most() {
		int lot = Math.max(1, this.stack.getCount());
		Minecraft mc = Minecraft.getInstance();
		long n = 64L;
		if (mc.player != null && !this.stack.isEmpty()) {
			Pocket pocket = new Pocket(mc.player.getInventory().items, () -> {
			});
			n = Math.min(n, this.buy ? pocket.room(this.stack) / (long)lot : pocket.count(this.stack) / (long)lot);
		}

		if (this.buy) {
			if (!this.shop.server) {
				n = Math.min(n, this.offer.stock / (long)lot);
			}

			if (this.offer.price > 0L) {
				n = Math.min(n, ShopsState.balance() / this.offer.price);
			}
		}

		return (int)Math.max(0L, n);
	}

	@Override
	protected void update() {
		int n = this.count();
		this.lots.setInvalid(n < 1 || n > 64);
		this.ok.active = n >= 1 && n <= 64 && !this.waiting && !this.stack.isEmpty();
	}

	@Override
	protected void answer(ShopsNet.Result result) {
		super.answer(result);
		if (result.ok()) {
			CFHGuiToasts.show(Txt.t("done"), Component.translatable("cfhgui_shops.done." + (this.buy ? "buy" : "sell"), result.args().toArray()), Icon.CHECK);
		}
	}

	private void go() {
		int n = this.count();
		this.send(then -> {
			if (this.buy) {
				ShopsActions.buy(this.shop.id, this.offer.id, n, then);
			} else {
				ShopsActions.sell(this.shop.id, this.offer.id, n, then);
			}
		});
	}

	@Override
	protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
		Palette p = Theme.current().palette();
		Ui.well(g, x, y + 1, 24, 24, alpha);
		if (!this.stack.isEmpty()) {
			Ui.item(g, this.stack, x + 4, y + 5, 16, alpha);
			if (this.stack.getCount() > 1) {
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 200.0F);
				String c = Integer.toString(this.stack.getCount());
				g.drawString(CFHGuiDraw.font(), c, x + 23 - CFHGuiDraw.width(c), y + 15, -1, true);
				g.pose().popPose();
			}
		}

		Ui.textFit(
			g, Fonts.heading((Component)(this.stack.isEmpty() ? Txt.t("offer.unknown") : this.stack.getHoverName())), x + 30, y + 3, w - 30, Ui.strong(), alpha
		);
		long each = this.buy ? this.offer.price : this.offer.sellPrice;
		Ui.textFit(g, Txt.t("trade.each", ShopsState.money(each), Math.max(1, this.stack.getCount())), x + 30, y + 15, w - 30, Ui.caption(), alpha);
		int n = Math.max(0, this.count());
		long total = each * (long)n;
		long after = ShopsState.balance() + (this.buy ? -total : total);
		int ty = y + this.totalY;
		Ui.text(
			g,
			Txt.t(this.buy ? "trade.total.buy" : "trade.total.sell", (long)n * (long)Math.max(1, this.stack.getCount()), ShopsState.money(total)),
			x,
			ty,
			Ui.strong(),
			alpha
		);
		Component left = Txt.t("trade.after", ShopsState.money(after));
		Ui.text(g, left, x, ty + 11, after < 0L ? p.danger() : Ui.caption(), alpha);
	}
}
