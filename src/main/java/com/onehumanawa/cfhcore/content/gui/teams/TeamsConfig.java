package com.onehumanawa.cfhcore.content.gui.teams;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;

public final class TeamsConfig {
	public boolean partyFrames = true;
	public String partyAnchor = "TOP_LEFT";
	public int partyX = 6;
	public int partyY = 30;
	public double partyScale = 1.0;
	public int partyMax = 6;
	public boolean partyShowHealth = true;
	public boolean partyShowDistance = true;
	public boolean worldMarkers = true;
	public int markerMinDistance = 12;
	public boolean worldWaypoints = true;
	public boolean claimBorders = true;
	public boolean landNotice = true;
	public boolean pingSound = true;
	public boolean inventoryButton = true;
	public boolean notices = true;
	public boolean mapTeammates = true;
	public boolean mapClaims = true;
	public boolean mapWaypoints = true;
	public double mapClaimOpacity = 0.35;
	public List<String> hiddenWaypoints = new ArrayList<>();
	private static JsonConfig<TeamsConfig> file;

	public boolean showsWaypoint(String waypointId) {
		return this.hiddenWaypoints == null || !this.hiddenWaypoints.contains(waypointId);
	}

	public static synchronized JsonConfig<TeamsConfig> file() {
		if (file == null) {
			file = JsonConfig.of("teams", TeamsConfig.class, TeamsConfig::new);
		}

		return file;
	}

	public static TeamsConfig get() {
		return (TeamsConfig)file().get();
	}

	public static void save() {
		file().save();
	}
}
