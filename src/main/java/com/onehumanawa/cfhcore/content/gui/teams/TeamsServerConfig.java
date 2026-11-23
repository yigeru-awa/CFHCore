package com.onehumanawa.cfhcore.content.gui.teams;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;

public final class TeamsServerConfig {
	public boolean playersCreateTeams = true;
	public int maxMembers = 16;
	public int maxAllies = 8;
	public int maxWaypoints = 32;
	public int inviteMinutes = 30;
	public boolean claims = true;
	public boolean soloLand = true;
	public int baseClaims = 25;
	public int claimsPerMember = 10;
	public int claimsPerLevel = 5;
	public int maxClaims = 500;
	public int baseForceLoads = 2;
	public int forceLoadsPerLevel = 1;
	public int maxForceLoads = 25;
	public boolean forceLoadOnlyWhileOnline = true;
	public List<String> claimDimensions = new ArrayList<>();
	public boolean claimsMustTouch = false;
	public int claimReach = 12;
	public int claimExpiryDays = 0;
	public List<String> pinnedLandRules = new ArrayList<>();
	public boolean operatorsBypass = true;
	public boolean fakePlayersFollowOwner = true;
	public boolean homeTeleport = true;
	public int homeCooldownSeconds = 60;
	public int homeWarmupSeconds = 3;
	public boolean stash = true;
	public int stashRows = 3;
	public boolean pings = true;
	public boolean allyLocations = true;
	public boolean scoreboardTeams = true;
	private static JsonConfig<TeamsServerConfig> file;

	public static synchronized JsonConfig<TeamsServerConfig> file() {
		if (file == null) {
			file = JsonConfig.of("teams-server", TeamsServerConfig.class, TeamsServerConfig::new);
		}

		return file;
	}

	public static TeamsServerConfig get() {
		return (TeamsServerConfig)file().get();
	}

	public static void reload() {
		file().load();
	}
}
