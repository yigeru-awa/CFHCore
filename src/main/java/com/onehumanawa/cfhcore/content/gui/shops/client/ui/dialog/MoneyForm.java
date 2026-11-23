package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import java.util.List;

public final class MoneyForm extends ShopDialog {
	private final boolean admin;
	private final CFHGuiTextField player;
	private final CFHGuiTextField amount;
	private final CFHGuiSegmented<String> what;
	private final CFHGuiButton ok;

	public MoneyForm(boolean admin) {
		super(Txt.t(admin ? "money.admin.title" : "money.pay.title"), admin ? Icon.WRENCH : Icon.SEND, 280);
		this.admin = admin;
		this.player = this.row(Txt.t("money.player"), new CFHGuiTextField(0, 0, 100, Txt.t("money.player")).maxLength(16).placeholder(Txt.t("money.player.hint")));
		this.player.setFilter(s -> s.matches("[A-Za-z0-9_]{0,16}"));
		if (admin) {
			this.what = this.row(Txt.t("money.what"), new CFHGuiSegmented<>(0, 0, 100, List.of("give", "take", "set"), "give", w -> Txt.t("money.what." + w), w -> {
			}));
		} else {
			this.what = null;
		}

		this.amount = this.row(Txt.t("money.amount"), new CFHGuiTextField(0, 0, 100, Txt.t("money.amount")).maxLength(13));
		this.amount.setFilter(s -> s.matches("\\d{0,13}"));
		if (!admin) {
			this.hint(Txt.t("money.pay.hint", ShopsState.money(ShopsState.balance())), 0, this.cursor - 7 + 1);
			this.cursor += 11;
		}

		this.ok = this.buttons(Txt.t(admin ? "money.admin.ok" : "money.pay.ok"), Variant.PRIMARY, this::go);
		this.firstFocus(this.player);
	}

	private long value() {
		try {
			return this.amount.getValue().isEmpty() ? -1L : Long.parseLong(this.amount.getValue());
		} catch (NumberFormatException var2) {
			return -1L;
		}
	}

	@Override
	protected void update() {
		long v = this.value();
		boolean good = this.player.getValue().length() >= 2 && v >= (long)(this.admin ? 0 : 1) && v <= 1000000000000L;
		this.ok.active = good && !this.waiting;
	}

	private void go() {
		String name = this.player.getValue();
		long v = this.value();
		if (this.admin) {
			this.send(then -> ShopsActions.money(name, (String)this.what.value(), v, then));
		} else {
			this.send(then -> ShopsActions.pay(name, v, then));
		}
	}
}
