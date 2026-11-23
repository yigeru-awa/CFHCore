package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class TeamsView {
	private static TeamsView.Pretend pretend = TeamsView.Pretend.NOTHING;
	private static TeamsView.Pretend pending = TeamsView.Pretend.NOTHING;

	public static void pretendNext(TeamsView.Pretend what) {
		pending = what;
	}

	public static TeamsView.Pretend takePending() {
		TeamsView.Pretend p = pending;
		pending = TeamsView.Pretend.NOTHING;
		return p;
	}

	public static void apply(TeamsView.Pretend what) {
		pretend = what;
	}

	public static TeamsView.Pretend pretending() {
		return pretend;
	}

	public static boolean available() {
		return pretend != TeamsView.Pretend.NO_SERVER && TeamsState.available();
	}

	public static boolean preview() {
		return TeamsState.preview();
	}

	public static ServerInfo info() {
		return TeamsState.info();
	}

	@Nullable
	public static TeamData team() {
		return pretend != TeamsView.Pretend.NOTHING ? null : TeamsState.team();
	}

	public static boolean hasTeam() {
		return team() != null;
	}

	@Nullable
	public static TeamData personal() {
		return pretend != TeamsView.Pretend.NOTHING ? null : TeamsState.personal();
	}

	@Nullable
	public static TeamData land() {
		return pretend != TeamsView.Pretend.NOTHING ? null : TeamsState.land();
	}

	public static boolean soloLand() {
		return available() && info().soloLand;
	}

	public static boolean claimsLand() {
		return land() != null || !hasTeam() && soloLand();
	}

	public static UUID self() {
		return TeamsState.self();
	}

	public static String selfName() {
		return Minecraft.getInstance().getUser().getName();
	}

	@Nullable
	public static Rank rank() {
		TeamData t = team();
		return t == null ? null : t.rankOf(self());
	}

	public static boolean may(Permission permission) {
		TeamData t = team();
		return t != null && t.may(self(), permission);
	}

	public static boolean mayClaim() {
		TeamData t = land();
		return t != null ? t.mayClaim(self()) : soloLand();
	}

	public static boolean mayLand(Permission permission) {
		TeamData t = land();
		return t != null && t.may(self(), permission);
	}

	public static List<TeamSummary> directory() {
		return pretend == TeamsView.Pretend.NO_SERVER ? List.of() : TeamsState.directory();
	}

	@Nullable
	public static TeamSummary summary(@Nullable UUID team) {
		return pretend == TeamsView.Pretend.NO_SERVER ? null : TeamsState.summary(team);
	}

	public static List<InviteView> invites() {
		return pretend == TeamsView.Pretend.NO_SERVER ? List.of() : TeamsState.invites();
	}

	public static Set<UUID> requested() {
		return pretend == TeamsView.Pretend.NO_SERVER ? Set.of() : TeamsState.requested();
	}

	public static boolean online(@Nullable UUID player) {
		return player == null ? false : player.equals(self()) || TeamsState.teammate(player) != null;
	}

	@Nullable
	public static TeammateState where(@Nullable UUID player) {
		return player == null ? null : TeamsState.teammate(player);
	}

	public static int onlineCount(TeamData team) {
		int n = 0;

		for (TeamData.Member m : team.members) {
			if (online(m.id)) {
				n++;
			}
		}

		return n;
	}

	public static TeamsView.Relation relation(@Nullable TeamSummary other) {
		if (other == null) {
			return TeamsView.Relation.NONE;
		} else {
			TeamData own = land();
			if (own != null && own.id.equals(other.id)) {
				return TeamsView.Relation.OWN;
			} else {
				TeamData mine = team();
				if (mine == null) {
					return TeamsView.Relation.NONE;
				} else {
					boolean ours = mine.isAlly(other.id);
					boolean theirs = other.allies.contains(mine.id);
					return ours && theirs ? TeamsView.Relation.MUTUAL : (ours ? TeamsView.Relation.OURS : (theirs ? TeamsView.Relation.THEIRS : TeamsView.Relation.NONE));
				}
			}
		}
	}

	public static TeamsView.Relation relation(@Nullable UUID other) {
		if (other == null) {
			return TeamsView.Relation.NONE;
		} else {
			TeamData own = land();
			if (own != null && own.id.equals(other)) {
				return TeamsView.Relation.OWN;
			} else {
				TeamData mine = team();
				if (mine == null) {
					return TeamsView.Relation.NONE;
				} else {
					TeamSummary s = summary(other);
					if (s != null) {
						return relation(s);
					} else {
						return mine.isAlly(other) ? TeamsView.Relation.OURS : TeamsView.Relation.NONE;
					}
				}
			}
		}
	}

	public static int held() {
		TeamData t = land();
		return t == null ? 0 : TeamsState.claimCount(t.id);
	}

	public static int forced() {
		TeamData t = land();
		return t == null ? 0 : TeamsState.forcedCount(t.id);
	}

	public static String dimension() {
		return TeamsState.currentDimension();
	}

	@Nullable
	public static Vec3 position() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			return mc.player.position();
		} else {
			TeamData t = TeamsState.team();
			return TeamsState.preview() && t != null && t.home != null ? new Vec3(t.home.x, t.home.y, t.home.z) : null;
		}
	}

	public static double distanceTo(TeamData.Waypoint waypoint) {
		Vec3 me = position();
		if (me != null && waypoint.dimension.equals(dimension())) {
			double dx = (double)waypoint.x + 0.5 - me.x;
			double dy = (double)waypoint.y - me.y;
			double dz = (double)waypoint.z + 0.5 - me.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		} else {
			return -1.0;
		}
	}

	public static boolean inWorld() {
		return Minecraft.getInstance().player != null;
	}

	public static String nameOf(@Nullable UUID player) {
		if (player == null) {
			return "";
		} else {
			TeamData t = TeamsState.team();
			TeamData.Member m = t == null ? null : t.member(player);
			if (m != null && !m.name.isEmpty()) {
				return m.name;
			} else {
				Minecraft mc = Minecraft.getInstance();
				if (mc.getConnection() != null) {
					PlayerInfo info = mc.getConnection().getPlayerInfo(player);
					if (info != null) {
						return info.getProfile().getName();
					}
				}

				return "";
			}
		}
	}

	private TeamsView() {
	}

	public static enum Pretend {
		NOTHING,
		NO_TEAM,
		NO_SERVER;
	}

	public static enum Relation {
		OWN,
		MUTUAL,
		OURS,
		THEIRS,
		NONE;

		public boolean ally() {
			return this == MUTUAL || this == OURS;
		}
	}
}
