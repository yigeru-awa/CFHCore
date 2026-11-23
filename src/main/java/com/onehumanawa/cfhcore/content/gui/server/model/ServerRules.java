package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;

public final class ServerRules {
	public boolean homes = true;
	public int maxHomes = 3;
	public boolean warps = true;
	public boolean spawn = true;
	public boolean back = true;
	public boolean backToDeath = true;
	public boolean requests = true;
	public int requestSeconds = 60;
	public boolean randomTeleport = true;
	public int randomMinDistance = 500;
	public int randomMaxDistance = 5000;
	public List<String> randomDimensions = new ArrayList<>(List.of("minecraft:overworld"));
	public int randomCooldownSeconds = 300;
	public int warmupSeconds = 3;
	public int cooldownSeconds = 5;
	public boolean safeLanding = true;
	public List<String> noTeleportDimensions = new ArrayList<>();
	public List<String> noHomeDimensions = new ArrayList<>();
	public boolean crossDimension = true;
	public boolean afk = true;
	public int afkSeconds = 300;
	public boolean afkAnnounce = true;
	public int afkKickMinutes = 0;
	public boolean afkSkipsSleep = true;
	public boolean mail = true;
	public int mailBoxSize = 50;
	public boolean nicknames = true;
	public boolean kits = true;
	public boolean deathPlace = true;
	public List<String> joinMessage = new ArrayList<>();
	public List<String> rules = new ArrayList<>();
	public List<ServerRules.Link> links = new ArrayList<>();
	public boolean stats = true;
	public boolean rankings = true;
	public List<String> featuredRankings = new ArrayList<>(
		List.of(
			"minecraft:custom|minecraft:play_time",
			"minecraft:custom|minecraft:mob_kills",
			"minecraft:custom|minecraft:deaths",
			"minecraft:custom|minecraft:walk_one_cm",
			"minecraft:custom|minecraft:player_kills",
			"minecraft:custom|minecraft:jump"
		)
	);
	public int statsRefreshMinutes = 10;
	public boolean operatorsHaveEverything = true;
	public boolean prefixInChat = true;
	public boolean prefixInTabList = true;
	public boolean staffChat = true;
	public boolean maintenance = false;
	public String maintenanceMessage = "";
	public List<String> announcements = new ArrayList<>();
	public int announcementMinutes = 15;
	public int auditDays = 90;

	public List<String> features() {
		List<String> out = new ArrayList<>();
		if (this.homes) {
			out.add("homes");
		}

		if (this.warps) {
			out.add("warps");
		}

		if (this.spawn) {
			out.add("spawn");
		}

		if (this.back) {
			out.add("back");
		}

		if (this.requests) {
			out.add("requests");
		}

		if (this.randomTeleport) {
			out.add("random_teleport");
		}

		if (this.afk) {
			out.add("afk");
		}

		if (this.mail) {
			out.add("mail");
		}

		if (this.nicknames) {
			out.add("nicknames");
		}

		if (this.kits) {
			out.add("kits");
		}

		if (this.stats) {
			out.add("stats");
		}

		if (this.rankings) {
			out.add("rankings");
		}

		if (this.staffChat) {
			out.add("staff_chat");
		}

		return out;
	}

	public ServerRules sanitize() {
		if (this.randomDimensions == null) {
			this.randomDimensions = new ArrayList<>();
		}

		if (this.noTeleportDimensions == null) {
			this.noTeleportDimensions = new ArrayList<>();
		}

		if (this.noHomeDimensions == null) {
			this.noHomeDimensions = new ArrayList<>();
		}

		if (this.joinMessage == null) {
			this.joinMessage = new ArrayList<>();
		}

		if (this.rules == null) {
			this.rules = new ArrayList<>();
		}

		if (this.links == null) {
			this.links = new ArrayList<>();
		}

		this.links.removeIf(l -> l == null || l.name == null || l.url == null);
		if (this.featuredRankings == null) {
			this.featuredRankings = new ArrayList<>();
		}

		if (this.announcements == null) {
			this.announcements = new ArrayList<>();
		}

		if (this.maintenanceMessage == null) {
			this.maintenanceMessage = "";
		}

		this.maxHomes = Math.max(0, this.maxHomes);
		this.requestSeconds = Math.max(5, this.requestSeconds);
		this.randomMinDistance = Math.max(0, this.randomMinDistance);
		this.randomMaxDistance = Math.max(this.randomMinDistance + 16, this.randomMaxDistance);
		this.randomCooldownSeconds = Math.max(0, this.randomCooldownSeconds);
		this.warmupSeconds = Math.max(0, this.warmupSeconds);
		this.cooldownSeconds = Math.max(0, this.cooldownSeconds);
		this.afkSeconds = Math.max(30, this.afkSeconds);
		this.afkKickMinutes = Math.max(0, this.afkKickMinutes);
		this.mailBoxSize = Math.max(1, this.mailBoxSize);
		this.statsRefreshMinutes = Math.max(1, this.statsRefreshMinutes);
		this.announcementMinutes = Math.max(1, this.announcementMinutes);
		this.auditDays = Math.max(0, this.auditDays);
		return this;
	}

	public static final class Link {
		public String name = "";
		public String url = "";
	}
}
