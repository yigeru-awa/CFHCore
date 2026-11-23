package com.onehumanawa.cfhcore.content.gui.shops.model;

import java.util.ArrayList;
import java.util.List;

public final class Wallet {
	public static final int HISTORY = 30;
	public static final String KIND_BOUGHT = "bought";
	public static final String KIND_SOLD = "sold";
	public static final String KIND_SHOP_SOLD = "shop_sold";
	public static final String KIND_SHOP_BOUGHT = "shop_bought";
	public static final String KIND_PAID = "paid";
	public static final String KIND_RECEIVED = "received";
	public static final String KIND_ADMIN = "admin";
	public static final String KIND_EXCHANGE = "exchange";
	public static final String KIND_OTHER = "other";
	public String name = "";
	public long balance;
	public List<Wallet.Line> history = new ArrayList<>();

	public void note(String kind, long amount, String what, String who, long now) {
		Wallet.Line l = new Wallet.Line();
		l.at = now;
		l.kind = kind;
		l.amount = amount;
		l.what = what == null ? "" : what;
		l.who = who == null ? "" : who;
		this.history.add(0, l);

		while (this.history.size() > 30) {
			this.history.remove(this.history.size() - 1);
		}
	}

	public void sanitize() {
		if (this.name == null) {
			this.name = "";
		}

		this.balance = Math.max(0L, Math.min(1000000000000L, this.balance));
		if (this.history == null) {
			this.history = new ArrayList<>();
		}

		this.history.removeIf(l -> l == null);

		while (this.history.size() > 30) {
			this.history.remove(this.history.size() - 1);
		}
	}

	public static final class Line {
		public long at;
		public String kind = "";
		public long amount;
		public String what = "";
		public String who = "";
	}
}
