package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.ArrayList;
import java.util.List;

public final class ServerInfo {
	public int maxMembers = 16;
	public int maxAllies = 8;
	public int maxWaypoints = 32;
	public boolean claims = true;
	public int maxClaims = 64;
	public int maxForceLoads = 4;
	public List<String> claimDimensions = new ArrayList<>();
	public int claimReach = -1;
	public List<String> pinnedLandRules = new ArrayList<>();
	public boolean homeTeleport = true;
	public int homeCooldownSeconds = 60;
	public boolean stash = true;
	public boolean pings = true;
	public boolean soloLand = false;
	public boolean playersCreateTeams = true;
	public boolean admin = false;

	public ServerInfo sanitize() {
		if (this.claimDimensions == null) {
			this.claimDimensions = new ArrayList<>();
		}

		if (this.pinnedLandRules == null) {
			this.pinnedLandRules = new ArrayList<>();
		}

		return this;
	}
}
