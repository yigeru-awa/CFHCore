package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Sync {
	private static final List<String> ORDER = List.of("hello", "self", "warps", "requests", "online", "mail");
	private static final String NAMED_WARP = "cfhgui_server.warp.to.";
	private static final String NAMED_KIT = "cfhgui_server.kit.use.";
	private final ServerService s;
	private final Map<UUID, Set<String>> dirty = new HashMap<>();
	int sent;

	Sync(ServerService s) {
		this.s = s;
	}

	void mark(UUID player, String... kinds) {
		if (this.s.sessions.containsKey(player)) {
			Set<String> set = this.dirty.computeIfAbsent(player, k -> new HashSet<>());

			for (String kind : kinds) {
				set.add(kind);
			}
		}
	}

	void markAll(String... kinds) {
		for (UUID id : this.s.sessions.keySet()) {
			this.mark(id, kinds);
		}
	}

	void forget(UUID player) {
		this.dirty.remove(player);
	}

	boolean marked(UUID player, String kind) {
		Set<String> set = this.dirty.get(player);
		return set != null && set.contains(kind);
	}

	void joined(Session session) {
		this.mark(session.id, ORDER.toArray(new String[0]));
		this.flush(session);
	}

	void flush() {
		if (!this.dirty.isEmpty()) {
			for (UUID id : new ArrayList<>(this.dirty.keySet())) {
				Session session = this.s.sessions.get(id);
				if (session == null) {
					this.dirty.remove(id);
				} else {
					this.flush(session);
				}
			}
		}
	}

	private void flush(Session session) {
		Set<String> kinds = this.dirty.remove(session.id);
		if (kinds != null && !kinds.isEmpty()) {
			ServerPlayer p = session.player;
			if (this.s.live && p != null && ServerNet.has(p)) {
				Actor actor = session.actor();

				for (String kind : ORDER) {
					if (kinds.contains(kind)) {
						Object state = switch (kind) {
							case "hello" -> this.hello(actor);
							case "self" -> this.self(session);
							case "warps" -> this.warps(actor);
							case "requests" -> this.s.requests.view(session.id);
							case "online" -> this.online(actor);
							default -> mail(session.record);
						};
						ServerNet.send(p, ServerNet.State.of(kind, state));
						this.sent++;
					}
				}
			}
		}
	}

	Hello hello(Actor actor) {
		ServerRules rules = this.s.rules();
		Hello h = new Hello();
		h.features = rules.features();
		if (Thirst.present()) {
			h.features.add("thirst");
		}

		for (Node node : Perms.declared()) {
			if (!node.number()
				&& node.module().equals("cfhgui_server")
				&& !node.id().startsWith("cfhgui_server.warp.to.")
				&& !node.id().startsWith("cfhgui_server.kit.use.")
				&& this.s.may(actor, node)) {
				h.may.add(node.id());
			}
		}

		h.numbers.put(Nodes.HOMES_MAX.id(), this.s.limit(actor, Nodes.HOMES_MAX, rules.maxHomes));
		h.numbers.put(Nodes.TP_WARMUP.id(), this.s.limit(actor, Nodes.TP_WARMUP, rules.warmupSeconds));
		h.numbers.put(Nodes.TP_COOLDOWN.id(), this.s.limit(actor, Nodes.TP_COOLDOWN, rules.cooldownSeconds));
		h.numbers.put(Nodes.RTP_COOLDOWN.id(), this.s.limit(actor, Nodes.RTP_COOLDOWN, rules.randomCooldownSeconds));
		if (!actor.console()) {
			for (Role role : this.s.perms.held(actor.id(), actor.level())) {
				h.roles.add(new Role.Tag(role));
			}
		}

		h.rules = new ArrayList<>(rules.rules);
		h.links = new ArrayList<>(rules.links);
		h.featuredRankings = new ArrayList<>(rules.featuredRankings);
		h.maintenance = rules.maintenance;
		return h;
	}

	SelfState self(Session session) {
		ServerRules rules = this.s.rules();
		PlayerRecord rec = session.record;
		Actor actor = session.actor();
		long now = this.s.now();
		SelfState st = new SelfState();
		st.nick = rec.nick;
		st.homes = new ArrayList<>(rec.homes);
		if (rules.back && rec.back != null && (!rec.backIsDeath || rules.backToDeath && this.s.may(actor, Nodes.BACK_DEATH))) {
			st.back = rec.back;
			st.backIsDeath = rec.backIsDeath;
		}

		if (rules.deathPlace && rec.death != null) {
			st.death = rec.death;
			st.deathAt = rec.deathAt;
		}

		st.afk = session.afk;
		st.vanished = rec.vanished;
		st.god = rec.god;
		st.fly = rec.fly;
		st.frozen = rec.active(Punishment.Kind.FREEZE, now) != null;
		Punishment mute = rec.active(Punishment.Kind.MUTE, now);
		st.mutedUntil = mute == null ? 0L : (mute.expiresAt == 0L ? -1L : mute.expiresAt);
		st.acceptsRequests = rec.acceptsRequests;
		st.statsHidden = rec.statsHidden;
		st.unreadMail = rec.unreadMail();

		for (String key : new String[]{"tp", "rtp"}) {
			Long until = rec.cooldowns.get(key);
			if (until != null && until > now) {
				st.cooldowns.put(key, until);
			}
		}

		Teleports.Pending pending = session.pending;
		if (pending != null) {
			st.pending = pending.label;
			st.pendingAt = pending.at;
		}

		st.kits = this.s.people.kitViews(session);
		return st;
	}

	WarpList warps(Actor actor) {
		ServerRules rules = this.s.rules();
		WarpList out = new WarpList();
		if (rules.warps && (this.s.may(actor, Nodes.WARP_USE) || this.s.may(actor, Nodes.WARP_SET))) {
			boolean all = this.s.may(actor, Nodes.WARP_SET);

			for (Warp w : this.s.store.warps()) {
				if (all || !w.restricted || this.s.mayNamed(actor, w.node())) {
					out.warps.add(w);
				}
			}
		}

		out.spawn = rules.spawn ? this.s.travel.spawn() : null;
		return out;
	}

	OnlinePlayer[] online(Actor viewer) {
		List<OnlinePlayer> out = new ArrayList<>(this.s.sessions.size());

		for (Session other : this.s.sessions.values()) {
			if (this.s.visible(viewer, other)) {
				OnlinePlayer o = new OnlinePlayer();
				o.id = other.id;
				o.name = other.name;
				o.nick = other.record.nick;
				o.role = this.tag(other);
				o.afk = other.afk;
				o.vanished = other.record.vanished;
				o.acceptsRequests = other.record.acceptsRequests;
				o.joinedAt = other.joinedAt;
				out.add(o);
			}
		}

		return out.toArray(new OnlinePlayer[0]);
	}

	@Nullable
	Role.Tag tag(Session session) {
		return tag(this.s.perms.held(session.id, session.actor().level()));
	}

	@Nullable
	static Role.Tag tag(List<Role> held) {
		if (held.isEmpty()) {
			return null;
		} else {
			Role top = held.get(0);
			return top.everyone && top.prefix.isEmpty() ? null : new Role.Tag(top);
		}
	}

	static PlayerRecord.Mail[] mail(PlayerRecord rec) {
		PlayerRecord.Mail[] out = new PlayerRecord.Mail[rec.mail.size()];

		for (int i = 0; i < out.length; i++) {
			out[i] = rec.mail.get(out.length - 1 - i);
		}

		return out;
	}

	static Home copy(Home h) {
		Home c = new Home();
		c.name = h.name;
		c.place = h.place.copy();
		c.icon = h.icon;
		c.color = h.color;
		c.createdAt = h.createdAt;
		return c;
	}

	TeleportRequest[] requests(UUID player) {
		return this.s.requests.view(player);
	}
}
