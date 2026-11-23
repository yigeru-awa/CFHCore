package com.onehumanawa.cfhcore.content.gui.shops.model;

import java.util.Locale;

public final class ShopRules {
	public static final long MAX_BALANCE = 1000000000000L;
	public static final long MAX_PRICE = 1000000000L;
	public String currencyName = "Coin";
	public String currencyPlural = "Coins";
	public String currencySymbol = "";
	public long startingBalance = 100L;
	public boolean playerShops = true;
	public int maxOffers = 27;
	public String exchangeItem = "";
	public long exchangeValue = 1L;

	public void sanitize() {
		if (this.currencyName == null || this.currencyName.isBlank()) {
			this.currencyName = "Coin";
		}

		if (this.currencyPlural == null || this.currencyPlural.isBlank()) {
			this.currencyPlural = this.currencyName;
		}

		if (this.currencySymbol == null) {
			this.currencySymbol = "";
		}

		this.currencyName = cut(this.currencyName.strip(), 24);
		this.currencyPlural = cut(this.currencyPlural.strip(), 24);
		this.currencySymbol = cut(this.currencySymbol.strip(), 4);
		this.startingBalance = Math.max(0L, Math.min(1000000000000L, this.startingBalance));
		this.maxOffers = Math.max(1, Math.min(54, this.maxOffers));
		if (this.exchangeItem == null) {
			this.exchangeItem = "";
		}

		this.exchangeItem = this.exchangeItem.strip();
		this.exchangeValue = Math.max(1L, Math.min(1000000000L, this.exchangeValue));
	}

	public String format(long amount) {
		return format(amount, this.currencySymbol, this.currencyName, this.currencyPlural);
	}

	public static String format(long amount, String symbol, String name, String plural) {
		String n = String.format(Locale.ROOT, "%,d", Math.abs(amount));
		String sign = amount < 0L ? "-" : "";
		return symbol != null && !symbol.isEmpty() ? sign + symbol + n : sign + n + " " + (Math.abs(amount) == 1L ? name : plural);
	}

	private static String cut(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}
}
