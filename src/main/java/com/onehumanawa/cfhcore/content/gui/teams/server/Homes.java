package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class Homes {
	private static final double STILL = 0.5625;
	private final TeamService teams;
	private final Map<UUID, Homes.Waiting> waiting = new HashMap<>();
	private final Object2LongOpenHashMap<UUID> restUntil = new Object2LongOpenHashMap();

	Homes(TeamService teams) {
		this.teams = teams;
	}

	OpResult start(ServerPlayer player, TeamData team) {
		UUID id = player.getUUID();
		long now = System.currentTimeMillis();
		long rest = this.restUntil.getLong(id) - now;
		if (rest > 0L) {
			return OpResult.fail("cooldown", (rest + 999L) / 1000L);
		} else if (this.waiting.containsKey(id)) {
			return OpResult.fail("already_going");
		} else if (team.home == null) {
			return OpResult.fail("no_home");
		} else if (this.teams.level(team.home.dimension) == null) {
			return OpResult.fail("home_gone");
		} else {
			int warmup = Math.max(0, this.teams.cfg().homeWarmupSeconds);
			if (warmup == 0) {
				this.arrive(player, team);
				return OpResult.OK;
			} else {
				this.waiting.put(id, new Homes.Waiting(player.position(), player.level().dimension().location().toString(), now + (long)warmup * 1000L, team.id));
				player.displayClientMessage(Msg.tr("cfhgui_teams.home.warmup", warmup), true);
				return OpResult.ok(warmup);
			}
		}
	}

	void cancel(UUID player) {
		this.waiting.remove(player);
	}

	void tick() {
		if (!this.waiting.isEmpty()) {
			long now = System.currentTimeMillis();
			Iterator<Entry<UUID, Homes.Waiting>> it = this.waiting.entrySet().iterator();

			while (it.hasNext()) {
				Entry<UUID, Homes.Waiting> e = it.next();
				Homes.Waiting w = e.getValue();
				ServerPlayer p = this.teams.online(e.getKey());
				if (p == null) {
					it.remove();
				} else {
					TeamData t = this.teams.store.ofPlayer(e.getKey());
					if (t != null && t.id.equals(w.team) && t.home != null) {
						boolean moved = !p.level().dimension().location().toString().equals(w.dimension) || p.position().distanceToSqr(w.from) > 0.5625;
						if (moved || p.isDeadOrDying()) {
							it.remove();
							p.displayClientMessage(Msg.tr("cfhgui_teams.home.cancelled"), true);
						} else if (now >= w.due) {
							it.remove();
							this.arrive(p, t);
						}
					} else {
						it.remove();
					}
				}
			}
		}
	}

	private void arrive(ServerPlayer player, TeamData team) {
		TeamData.Place home = team.home;
		ServerLevel level = home == null ? null : this.teams.level(home.dimension);
		if (home != null && level != null) {
			if (player.isPassenger()) {
				player.stopRiding();
			}

			player.teleportTo(level, home.x, home.y, home.z, home.yaw, home.pitch);
			this.restUntil.put(player.getUUID(), System.currentTimeMillis() + (long)Math.max(0, this.teams.cfg().homeCooldownSeconds) * 1000L);
			player.displayClientMessage(Msg.tr("cfhgui_teams.home.arrived", team.name), true);
		} else {
			player.displayClientMessage(OpResult.fail("home_gone").message(), true);
		}
	}

	private static record Waiting(Vec3 from, String dimension, long due, UUID team) {
	}
}
