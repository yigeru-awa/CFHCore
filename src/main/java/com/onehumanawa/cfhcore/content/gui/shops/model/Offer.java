package com.onehumanawa.cfhcore.content.gui.shops.model;

public final class Offer {
	public int id;
	public String item = "";
	public long price;
	public long sellPrice;
	public long stock;
	public long sold;
	public long bought;

	public boolean sells() {
		return this.price > 0L;
	}

	public boolean buys() {
		return this.sellPrice > 0L;
	}

	public void sanitize() {
		if (this.item == null) {
			this.item = "";
		}

		this.price = Math.max(0L, Math.min(1000000000L, this.price));
		this.sellPrice = Math.max(0L, Math.min(1000000000L, this.sellPrice));
		this.stock = Math.max(0L, this.stock);
		this.sold = Math.max(0L, this.sold);
		this.bought = Math.max(0L, this.bought);
	}
}
