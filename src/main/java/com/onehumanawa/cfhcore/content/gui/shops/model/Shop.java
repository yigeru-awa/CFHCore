package com.onehumanawa.cfhcore.content.gui.shops.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class Shop {
	public String id = "";
	public String name = "";
	public String description = "";
	public boolean server;
	@Nullable
	public UUID owner;
	public String ownerName = "";
	public boolean open = true;
	public long created;
	public int nextOffer = 1;
	public List<Offer> offers = new ArrayList<>();

	@Nullable
	public Offer offer(int id) {
		for (Offer o : this.offers) {
			if (o.id == id) {
				return o;
			}
		}

		return null;
	}

	public long stockTotal() {
		long n = 0L;

		for (Offer o : this.offers) {
			n += o.stock;
		}

		return n;
	}

	public boolean ownedBy(@Nullable UUID player) {
		return !this.server && this.owner != null && this.owner.equals(player);
	}

	public void sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.description == null) {
			this.description = "";
		}

		if (this.ownerName == null) {
			this.ownerName = "";
		}

		if (this.offers == null) {
			this.offers = new ArrayList<>();
		}

		this.offers.removeIf(ox -> ox == null);
		int next = Math.max(1, this.nextOffer);

		for (Offer o : this.offers) {
			o.sanitize();
			next = Math.max(next, o.id + 1);
		}

		this.nextOffer = next;
		if (this.server) {
			this.owner = null;
		}
	}
}
