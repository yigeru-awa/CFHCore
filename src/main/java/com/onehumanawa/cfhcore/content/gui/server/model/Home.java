package com.onehumanawa.cfhcore.content.gui.server.model;

public final class Home {
	public static final int NAME_MIN = 1;
	public static final int NAME_MAX = 24;
	public static final String DEFAULT_NAME = "home";
	public String name = "home";
	public Place place = new Place();
	public String icon = "";
	public int color = 14254174;
	public long createdAt;

	public Home sanitize() {
		if (this.name == null || this.name.isBlank()) {
			this.name = "home";
		}

		if (this.place == null) {
			this.place = new Place();
		}

		this.place.sanitize();
		if (this.icon == null) {
			this.icon = "";
		}

		this.color &= 16777215;
		return this;
	}
}
