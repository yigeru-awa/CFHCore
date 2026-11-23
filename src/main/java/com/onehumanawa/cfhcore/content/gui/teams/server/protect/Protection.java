package com.onehumanawa.cfhcore.content.gui.teams.server.protect;

import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.server.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.server.LandPins;
import com.onehumanawa.cfhcore.content.gui.teams.server.Msg;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamService;
import com.onehumanawa.cfhcore.content.gui.teams.server.platform.TeamsPlatform;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Protection {
	private static final UUID NOBODY = new UUID(0L, 0L);
	private static final long TELL_EVERY_MS = 2000L;
	private final TeamService teams;
	private final Object2LongOpenHashMap<UUID> lastTold = new Object2LongOpenHashMap();

	public Protection(TeamService teams) {
		this.teams = teams;
	}

	public Access.Relation relation(TeamData land, UUID actor) {
		if (this.teams.isServerLand(land)) {
			return Access.Relation.OUTSIDER;
		} else {
			TeamData own = this.teams.store().ofPlayer(actor);
			if (own == land) {
				return Access.Relation.MEMBER;
			} else {
				return own != null && land.isAlly(own.id) ? Access.Relation.ALLY : Access.Relation.OUTSIDER;
			}
		}
	}

	@Nullable
	public TeamData refuses(UUID actor, ResourceKey<Level> dimension, int chunkX, int chunkZ, Protection.Kind kind) {
		ClaimMap.Claim c = this.teams.claims().at(dimension, chunkX, chunkZ);
		return c == null ? null : this.refuses(actor, c.team, kind);
	}

	@Nullable
	public TeamData refuses(UUID actor, TeamData land, Protection.Kind kind) {
		LandPins pins = this.teams.pins();

		Access access = switch (kind) {
			case BUILD -> pins.build(land);
			case INTERACT -> pins.interact(land);
			case CONTAINERS -> pins.containers(land);
			case ENTITIES -> pins.entities(land);
		};
		return access.allows(this.relation(land, actor)) ? null : land;
	}

	@Nullable
	public TeamData refuses(Player player, TeamData land, Protection.Kind kind) {
		TeamData refusing = this.refuses(this.identity(player), land, kind);
		return refusing != null && this.isMachineOf(player, refusing) ? null : refusing;
	}

	private boolean isMachineOf(Player player, TeamData land) {
		if (!this.teams.isServerLand(land) && this.teams.cfg().fakePlayersFollowOwner && TeamsPlatform.get().isFakePlayer(player)) {
			ClaimMap.Claim at = this.teams.claims().at(player.level().dimension(), player.getBlockX() >> 4, player.getBlockZ() >> 4);
			return at != null && at.team == land;
		} else {
			return false;
		}
	}

	public boolean mayReachInto(@Nullable ClaimMap.Claim from, ClaimMap.Claim to) {
		if (from != null && from.team == to.team) {
			return true;
		} else {
			Access build = this.teams.pins().build(to.team);
			return build == Access.EVERYONE ? true : build == Access.ALLIES && from != null && to.team.isAlly(from.team.id);
		}
	}

	public boolean bypasses(Player player) {
		if (this.teams.cfg().operatorsBypass && player instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator()) && sp.hasPermissions(2)) {
			return true;
		}

		return false;
	}

	public UUID identity(Player player) {
		return !this.teams.cfg().fakePlayersFollowOwner && TeamsPlatform.get().isFakePlayer(player) ? NOBODY : player.getUUID();
	}

	public void tell(Player player, TeamData land) {
		if (player instanceof ServerPlayer sp && sp.connection != null && !TeamsPlatform.get().isFakePlayer(player)) {
			long now = System.currentTimeMillis();
			UUID id = sp.getUUID();
			if (this.lastTold.containsKey(id) && now - this.lastTold.getLong(id) < 2000L) {
				return;
			}

			this.lastTold.put(id, now);
			sp.displayClientMessage(Msg.tr(this.teams.isServerLand(land) ? "cfhgui_teams.protect.server" : "cfhgui_teams.protect.denied", Msg.team(land)), true);
			return;
		}
	}

	public void forget(UUID player) {
		this.lastTold.removeLong(player);
	}

	public static enum Kind {
		BUILD,
		INTERACT,
		CONTAINERS,
		ENTITIES;
	}
}
