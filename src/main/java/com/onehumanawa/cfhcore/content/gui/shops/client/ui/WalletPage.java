package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.MoneyForm;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog.Prompt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Hello;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.service.Stacks;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class WalletPage extends ShopPage {
	private static final int FIGURE_H = 52;
	private static final int LINE_H = 24;

	WalletPage(SidebarScreen host) {
		super(host, "wallet", Txt.t("page.wallet"), Icon.GPU);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		Hello hello = ShopsState.hello();
		ShopPage.Bar bar = new ShopPage.Bar(s, area);
		if (ShopsState.may("pay")) {
			CFHGuiButton pay = new CFHGuiButton(0, 0, 80, Txt.t("wallet.pay"), () -> new MoneyForm(false).show()).variant(Variant.PRIMARY).icon(Icon.SEND);
			pay.setWidth(pay.preferredWidth() + 14);
			bar.right(pay);
		}

		if (ShopsState.may("money")) {
			CFHGuiButton set = new CFHGuiButton(0, 0, 80, Txt.t("wallet.admin"), () -> new MoneyForm(true).show()).variant(Variant.SECONDARY).icon(Icon.WRENCH);
			set.setWidth(set.preferredWidth() + 14);
			set.tip(Txt.t("wallet.admin.tip"));
			bar.right(set);
		}

		ItemStack exchange = Stacks.plain(hello.exchangeItem);
		if (!exchange.isEmpty()) {
			Component item = exchange.getHoverName();
			CFHGuiButton deposit = new CFHGuiButton(0, 0, 80, Txt.t("wallet.deposit"), () -> ShopsActions.exchange("deposit", 0L, told("deposit")))
				.variant(Variant.SECONDARY)
				.icon(Icon.DOWNLOAD);
			deposit.setWidth(deposit.preferredWidth() + 14);
			deposit.tip(Txt.t("wallet.deposit.tip", item, ShopsState.money(hello.exchangeValue)));
			bar.left(deposit);
			CFHGuiButton withdraw = new CFHGuiButton(
					0,
					0,
					80,
					Txt.t("wallet.withdraw"),
					() -> new Prompt(
								Txt.t("wallet.withdraw.title", item),
								Icon.UPLOAD,
								Txt.t("wallet.withdraw.caption", ShopsState.money(hello.exchangeValue)),
								"1",
								6,
								Txt.t("wallet.withdraw"),
								v -> v.matches("\\d{1,6}") && Long.parseLong(v) > 0L,
								(v, then) -> ShopsActions.exchange("withdraw", Long.parseLong(v), then)
							)
							.done("withdraw")
							.show()
				)
				.variant(Variant.SECONDARY)
				.icon(Icon.UPLOAD);
			withdraw.setWidth(withdraw.preferredWidth() + 14);
			withdraw.tip(Txt.t("wallet.withdraw.tip", item));
			bar.left(withdraw);
		}

		Rect r = under(area);
		CFHGuiScrollPanel panel = this.list(s, r);
		int w = panel.innerWidth();
		long balance = ShopsState.balance();
		panel.add(new PaintedCard(w, 52, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			Ui.text(g, Txt.t("wallet.balance"), f.x() + 12, f.y() + 10, Ui.caption(), a);
			long shown = Math.round((double)balance * (double)f.grow());
			Ui.textFit(g, Fonts.heading(Component.literal(ShopsState.money(shown))), f.x() + 12, f.y() + 24, f.w() - 24, p.success(), a);
			String shop = ShopsState.self().shop;
			if (!shop.isEmpty() && ShopsState.mine() != null) {
				Ui.textRight(g, Txt.t("wallet.shop", ShopsState.mine().name), f.x() + f.w() - 12, f.y() + 10, Ui.caption(), a);
			}
		}).flat().narrate(Txt.t("wallet.balance")), 0, 0);
		int y = section(panel, 60, w, Txt.t("wallet.history"));
		List<Wallet.Line> history = ShopsState.self().history;
		if (history.isEmpty()) {
			int h = Math.max(70, panel.getHeight() - y - 4);
			panel.add(
				new PaintedCard(
						w,
						h,
						(g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.HISTORY, Txt.t("wallet.history.none.title"), Txt.t("wallet.history.none"), f.alpha())
					)
					.bare(),
				0,
				y
			);
			panel.setContentHeight(y + h);
		} else {
			int widest = 40;

			for (Wallet.Line line : history) {
				widest = Math.max(widest, CFHGuiDraw.width(Component.literal(amount(line))));
			}

			int aw = widest + 12;

			for (Wallet.Line line : history) {
				panel.add(new PaintedCard(w, 24, (g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Ui.textFit(g, Component.literal(amount(line)), f.x() + 8, f.y() + 8, aw, line.amount >= 0L ? p.success() : p.danger(), a);
					Component when = Txt.ago(line.at);
					Ui.textRight(g, when, f.x() + f.w() - 8, f.y() + 8, Ui.caption(), a);
					Component what = Component.translatable("cfhgui_shops.history." + line.kind, new Object[]{line.what, line.who});
					Ui.textFit(g, what, f.x() + 8 + aw, f.y() + 8, f.w() - aw - 24 - CFHGuiDraw.width(when), Ui.soft(), a);
				}).flat(), 0, y);
				y += 27;
			}

			panel.setContentHeight(y + 2);
			this.settle();
		}
	}

	private static String amount(Wallet.Line line) {
		return (line.amount > 0L ? "+" : "") + ShopsState.money(line.amount);
	}
}
