package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import org.jetbrains.annotations.Nullable;

public final class ShopForm extends ShopDialog {
	@Nullable
	private final Shop edit;
	private final boolean server;
	private final CFHGuiTextField name;
	private final CFHGuiTextField description;
	private final CFHGuiButton ok;

	public ShopForm(@Nullable Shop edit, boolean server) {
		super(Txt.t(edit != null ? "shop.edit.title" : (server ? "shop.new_server.title" : "shop.new.title")), edit == null && server ? Icon.CROWN : Icon.TAG, 300);
		this.edit = edit;
		this.server = server;
		this.name = this.row(
			Txt.t("shop.field.name"), new CFHGuiTextField(0, 0, 100, Txt.t("shop.field.name")).maxLength(32).placeholder(Txt.t("shop.field.name.hint"))
		);
		this.name.setValue(edit == null ? "" : edit.name);
		this.description = this.row(
			Txt.t("shop.field.description"),
			new CFHGuiTextField(0, 0, 100, Txt.t("shop.field.description")).maxLength(120).placeholder(Txt.t("shop.field.description.hint"))
		);
		this.description.setValue(edit == null ? "" : edit.description);
		this.ok = this.buttons(Txt.t(edit == null ? "shop.new.ok" : "form.save"), Variant.PRIMARY, this::go);
		this.firstFocus(this.name);
	}

	@Override
	protected void update() {
		String n = this.name.getValue().trim();
		this.ok.active = !n.isEmpty() && !this.waiting;
	}

	private void go() {
		String n = this.name.getValue().trim();
		String d = this.description.getValue().trim();
		if (this.edit != null) {
			this.send(then -> ShopsActions.edit(this.edit.id, n, d, null, then));
		} else {
			this.send(then -> ShopsActions.create(n, this.server, r -> {
					if (r.ok() && !d.isEmpty() && r.args().size() > 1) {
						ShopsActions.edit(r.args().get(1), null, d, null, ignored -> {
						});
					}

					then.accept(r);
				}));
		}
	}
}
