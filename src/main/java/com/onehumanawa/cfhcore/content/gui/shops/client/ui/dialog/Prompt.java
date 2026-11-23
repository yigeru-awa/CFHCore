package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Prompt extends ShopDialog {
	private final CFHGuiTextField field;
	private final CFHGuiButton ok;
	private final Predicate<String> valid;
	@Nullable
	private String done;

	public Prompt(
		Component title,
		Icon icon,
		Component caption,
		String initial,
		int maxLength,
		Component okLabel,
		Predicate<String> valid,
		BiConsumer<String, Consumer<ShopsNet.Result>> sender
	) {
		super(title, icon, 260);
		this.valid = valid;
		this.field = this.row(caption, new CFHGuiTextField(0, 0, 100, caption).maxLength(maxLength));
		this.field.setValue(initial == null ? "" : initial);
		this.ok = this.buttons(okLabel, Variant.PRIMARY, () -> this.send(then -> sender.accept(this.field.getValue().trim(), then)));
		this.firstFocus(this.field);
	}

	public Prompt done(String op) {
		this.done = op;
		return this;
	}

	@Override
	protected void answer(ShopsNet.Result result) {
		super.answer(result);
		if (result.ok() && this.done != null) {
			CFHGuiToasts.show(Txt.t("done"), Component.translatable("cfhgui_shops.done." + this.done, result.args().toArray()), Icon.CHECK);
		}
	}

	@Override
	protected void update() {
		String v = this.field.getValue().trim();
		boolean good = this.valid.test(v);
		this.field.setInvalid(!good && !v.isEmpty());
		this.ok.active = good && !this.waiting;
	}
}
