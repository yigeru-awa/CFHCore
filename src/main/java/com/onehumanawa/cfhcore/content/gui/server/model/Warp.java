package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.Locale;
import java.util.UUID;

public final class Warp {
	public static final int NAME_MIN = 1;
	public static final int NAME_MAX = 24;
	public static final int DESCRIPTION_MAX = 120;
	public String name = "";
	public Place place = new Place();
	public String icon = "";
	public int color = 7317724;
	public String description = "";
	public boolean restricted = false;
	public UUID createdBy = new UUID(0L, 0L);
	public String createdByName = "";
	public long createdAt;
	public long visits;

	public String node() {
		return "cfhgui_server.warp.to." + this.name.toLowerCase(Locale.ROOT);
	}

	public Warp sanitize() {
		if (this.name == null) {
			this.name = "";
		}

		if (this.place == null) {
			this.place = new Place();
		}

		this.place.sanitize();
		if (this.icon == null) {
			this.icon = "";
		}

		if (this.description == null) {
			this.description = "";
		}

		if (this.createdBy == null) {
			this.createdBy = new UUID(0L, 0L);
		}

		if (this.createdByName == null) {
			this.createdByName = "";
		}

		this.color &= 16777215;
		return this;
	}
}
