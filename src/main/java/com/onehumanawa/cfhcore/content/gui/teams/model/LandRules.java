package com.onehumanawa.cfhcore.content.gui.teams.model;

public final class LandRules {
	public Access build = Access.MEMBERS;
	public Access interact = Access.ALLIES;
	public Access containers = Access.MEMBERS;
	public Access entities = Access.MEMBERS;
	public boolean explosions = false;
	public boolean mobGriefing = false;
	public boolean fireSpread = false;
	public boolean pvp = true;
	public Rank claimRank = Rank.MODERATOR;

	public void sanitize() {
		if (this.build == null) {
			this.build = Access.MEMBERS;
		}

		if (this.interact == null) {
			this.interact = Access.ALLIES;
		}

		if (this.containers == null) {
			this.containers = Access.MEMBERS;
		}

		if (this.entities == null) {
			this.entities = Access.MEMBERS;
		}

		if (this.claimRank == null) {
			this.claimRank = Rank.MODERATOR;
		}
	}
}
