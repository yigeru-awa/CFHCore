package com.onehumanawa.cfhcore.content.gui.teams.model;

public enum Permission {
	INVITE(Rank.MODERATOR),
	KICK(Rank.MODERATOR),
	WAYPOINTS(Rank.MODERATOR),
	RANKS(Rank.ADMIN),
	SETTINGS(Rank.ADMIN),
	ALLIES(Rank.ADMIN),
	HOME(Rank.ADMIN),
	OWN(Rank.OWNER);

	private final Rank min;

	private Permission(final Rank min) {
		this.min = min;
	}

	public Rank min() {
		return this.min;
	}

	public boolean allows(Rank rank) {
		return rank != null && rank.atLeast(this.min);
	}
}
