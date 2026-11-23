package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.UUID;

public final class InviteView {
	public UUID team = new UUID(0L, 0L);
	public String teamName = "";
	public String tag = "";
	public int color = 14254174;
	public BannerData banner = new BannerData();
	public int members;
	public String byName = "";
	public long expiresAt;

	public InviteView sanitize() {
		if (this.team == null) {
			this.team = new UUID(0L, 0L);
		}

		if (this.teamName == null) {
			this.teamName = "";
		}

		if (this.tag == null) {
			this.tag = "";
		}

		if (this.banner == null) {
			this.banner = new BannerData();
		}

		this.banner.sanitize();
		if (this.byName == null) {
			this.byName = "";
		}

		return this;
	}
}
