package com.onehumanawa.cfhcore.content.gui.shops;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;

public final class ShopNodes {
	public static final Node USE = Perms.declare("cfhgui_shops.use", 0);
	public static final Node CREATE = Perms.declare("cfhgui_shops.create", 0);
	public static final Node PAY = Perms.declare("cfhgui_shops.pay", 0);
	public static final Node ADMIN = Perms.declare("cfhgui_shops.admin", 2);
	public static final Node MONEY = Perms.declare("cfhgui_shops.money", 2);
	public static final Node SELFTEST = Perms.declare("cfhgui_shops.selftest", 4);

	static void init() {
	}

	private ShopNodes() {
	}
}
