package com.onehumanawa.cfhcore.content.gui.shops.model;

import java.util.ArrayList;
import java.util.List;

public final class Hello {
	public static final int PROTOCOL = 1;
	public static final String USE = "use";
	public static final String CREATE = "create";
	public static final String PAY = "pay";
	public static final String ADMIN = "admin";
	public static final String MONEY = "money";
	public int protocol = 1;
	public String currencyName = "Coin";
	public String currencyPlural = "Coins";
	public String currencySymbol = "";
	public boolean playerShops = true;
	public int maxOffers = 27;
	public String exchangeItem = "";
	public long exchangeValue = 1L;
	public List<String> may = new ArrayList<>();

	public String format(long amount) {
		return ShopRules.format(amount, this.currencySymbol, this.currencyName, this.currencyPlural);
	}
}
