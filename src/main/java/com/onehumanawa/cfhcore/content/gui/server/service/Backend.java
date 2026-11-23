package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.storage.WorldStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Backend implements ServerApi.Backend {
	private final ServerService s;

	Backend(ServerService s) {
		this.s = s;
	}

	@Override
	public List<Home> homes(UUID player) {
		PlayerRecord rec = this.s.store.record(player);
		return rec == null ? List.of() : Collections.unmodifiableList(rec.homes);
	}

	@Override
	public List<Warp> warps() {
		return Collections.unmodifiableList(this.s.store.warps());
	}

	@Nullable
	@Override
	public Warp warp(String name) {
		return this.s.store.warp(name);
	}

	@Override
	public Place spawn() {
		return this.s.travel.spawn();
	}

	@Override
	public boolean teleport(ServerPlayer player, Place place, ServerApi.Cause cause) {
		return this.s.teleports.now(player, place, cause);
	}

	@Override
	public boolean afk(UUID player) {
		Session session = this.s.session(player);
		return session != null && session.afk;
	}

	@Override
	public boolean vanished(UUID player) {
		return this.s.vanish.is(player);
	}

	@Nullable
	@Override
	public Punishment active(UUID player, Punishment.Kind kind) {
		Session session = this.s.session(player);
		if (session != null) {
			return session.record.active(kind, this.s.now());
		} else {
			WorldStore.Known k = this.s.store.known(player);
			long now = this.s.now();
			if (k != null
				&& (kind == Punishment.Kind.MUTE ? k.muted(now) : (kind == Punishment.Kind.BAN ? k.banned(now) : kind == Punishment.Kind.FREEZE && k.frozen(now)))) {
				PlayerRecord rec = this.s.store.record(player);
				return rec == null ? null : rec.active(kind, now);
			} else {
				return null;
			}
		}
	}

	@Override
	public List<Role.Tag> roles(UUID player) {
		WorldStore.Known k = this.s.store.known(player);
		Session session = this.s.session(player);
		String name = session != null ? session.name : (k == null ? "" : k.name);
		List<Role.Tag> out = new ArrayList<>();

		for (Role role : this.s.perms.held(player, this.s.levelOf(player, name))) {
			out.add(new Role.Tag(role));
		}

		return out;
	}

	@Nullable
	@Override
	public String nick(UUID player) {
		Session session = this.s.session(player);
		String nick = session != null ? session.record.nick : (this.s.store.known(player) == null ? "" : this.s.store.known(player).nick);
		return nick != null && !nick.isEmpty() ? nick : null;
	}

	@Override
	public long extra(UUID player, String key) {
		return this.s.statistics.value(player, key);
	}

	@Override
	public void audit(@Nullable ServerPlayer actor, String action, @Nullable UUID target, String targetName, String detail) {
		this.s.audit(actor == null ? Actor.console("Server") : Actor.of(actor), action, target, targetName, detail);
	}
}
