package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public final class Confirm extends ShopDialog {
	public Confirm(Component title, Component body, Component okLabel, Consumer<Consumer<ShopsNet.Result>> action) {
		super(title, Icon.WARNING, 280);
		this.words(body);
		this.buttons(okLabel, Variant.DANGER, () -> this.send(action));
	}
}
