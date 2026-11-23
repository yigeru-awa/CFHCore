package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamSummary {
	public static final UUID SERVER_LAND = UUID.fromString("5e7ae7ea-51a7-4000-8000-00005e7a1a2d");
	public UUID id = new UUID(0L, 0L);
	public String name = "";
	public String tag = "";
	public int color = 14254174;
	public String motto = "";
	public BannerData banner = new BannerData();
	public Privacy privacy = Privacy.INVITE;
	public int members;
	public int online;
	public int level = 1;
	public int claims;
	public UUID owner = new UUID(0L, 0L);
	public String ownerName = "";
	public long createdAt;
	public List<UUID> allies = new ArrayList<>();
	public long quests;
	public boolean server;
	public boolean solo;

	public static TeamSummary of(TeamData t, int online, int claims) {
		TeamSummary s = new TeamSummary();
		s.id = t.id;
		s.name = t.name;
		s.tag = t.tag;
		s.color = t.color;
		s.motto = t.motto;
		s.banner = t.banner;
		s.privacy = t.privacy;
		s.members = t.members.size();
		s.online = online;
		s.level = t.level;
		s.claims = claims;
		TeamData.Member o = t.owner();
		if (o != null) {
			s.owner = o.id;
			s.ownerName = o.name;
		}

		s.createdAt = t.createdAt;
		s.allies = new ArrayList<>(t.allies);
		s.quests = t.counter("quests");
		s.solo = t.solo;
		return s;
	}

	public TeamSummary sanitize() {
		if (this.id == null) {
			this.id = new UUID(0L, 0L);
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.tag == null) {
			this.tag = "";
		}

		if (this.motto == null) {
			this.motto = "";
		}

		if (this.banner == null) {
			this.banner = new BannerData();
		}

		this.banner.sanitize();
		if (this.privacy == null) {
			this.privacy = Privacy.INVITE;
		}

		if (this.owner == null) {
			this.owner = new UUID(0L, 0L);
		}

		if (this.ownerName == null) {
			this.ownerName = "";
		}

		if (this.allies == null) {
			this.allies = new ArrayList<>();
		}

		return this;
	}
}
