package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Requests {
	private static final int MAX_OPEN = 5;
	private static final long GAP_MS = 2000L;
	private final ServerService s;
	private final List<Requests.Request> open = new ArrayList<>();

	Requests(ServerService s) {
		this.s = s;
	}

	public OpResult ask(Actor a, @Nullable ServerService.Target target, String wanted, boolean here) {
		ServerRules rules = this.s.rules();
		if (!rules.requests) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, here ? Nodes.TPA_HERE : Nodes.TPA_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session from = this.s.session(a);
			if (from == null) {
				return OpResult.fail("player_only");
			} else if (target == null) {
				return OpResult.fail("no_such_player", Text.line(wanted, 32));
			} else if (target.id().equals(a.id())) {
				return OpResult.fail("self");
			} else {
				Session to = target.session();
				if (to != null && this.s.visible(a, to)) {
					if (this.s.moderation.frozen(from)) {
						return OpResult.fail("frozen");
					} else if (!to.record.acceptsRequests) {
						return OpResult.fail("requests_off", Text.shown(to.record));
					} else {
						int mine = 0;

						for (Requests.Request r : this.open) {
							if (r.from.equals(from.id)) {
								if (r.to.equals(to.id)) {
									return OpResult.fail("already_requested", Text.shown(to.record));
								}

								mine++;
							}
						}

						long now = this.s.now();
						if (mine < 5 && from.allow("tpa", 2000L, now)) {
							this.open.add(new Requests.Request(from.id, to.id, here, now + (long)rules.requestSeconds * 1000L));
							this.changed(from.id, to.id);
							String asker = Text.shown(from.record);
							Component line = Msg.tr(here ? "cfhgui_server.chat.tpahere_received" : "cfhgui_server.chat.tpa_received", asker, rules.requestSeconds)
								.append(" ")
								.append(Travel.button("cfhgui_server.chat.button.accept", "/tpaccept " + from.name))
								.append(" ")
								.append(Travel.button("cfhgui_server.chat.button.deny", "/tpdeny " + from.name));
							this.s.notice(to, here ? "tpahere_received" : "tpa_received", 0, line, asker);
							return OpResult.ok(Text.shown(to.record), rules.requestSeconds);
						} else {
							return OpResult.fail("too_fast");
						}
					}
				} else {
					return OpResult.fail("player_offline", target.name());
				}
			}
		}
	}

	@Nullable
	private Requests.Request incoming(UUID to, @Nullable UUID from) {
		Requests.Request found = null;

		for (Requests.Request r : this.open) {
			if (r.to.equals(to) && (from == null || r.from.equals(from))) {
				found = r;
			}
		}

		return found;
	}

	public OpResult accept(Actor a, @Nullable UUID from) {
		if (!this.s.rules().requests) {
			return OpResult.fail("disabled");
		} else {
			Session me = this.s.session(a);
			if (me == null) {
				return OpResult.fail("player_only");
			} else {
				Requests.Request r = this.incoming(me.id, from);
				Session other = r == null ? null : this.s.session(r.from);
				if (r != null && other != null) {
					this.open.remove(r);
					this.changed(r.from, r.to);
					Session traveller = r.here ? me : other;
					Session host = r.here ? other : me;
					Place place = host.place();
					if (place == null) {
						return OpResult.fail("no_request");
					} else {
						this.s.notice(other, "tpa_accepted", 0, Msg.tr("cfhgui_server.chat.tpa_accepted", Text.shown(me.record)), Text.shown(me.record));
						Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.REQUEST, "player:" + host.name, place);
						OpResult result = this.s.teleports.begin(traveller, plan);
						if (traveller != me) {
							if (!result.ok()) {
								this.s.say(other, "result:" + result.code(), result.message());
							} else if (!result.args().isEmpty()) {
								this.s.say(other, "result:" + result.code(), result.message());
							}

							return OpResult.ok(Text.shown(other.record));
						} else {
							return result.ok() && result.args().isEmpty() ? OpResult.ok(Text.shown(other.record)) : result;
						}
					}
				} else {
					return OpResult.fail("no_request");
				}
			}
		}
	}

	public OpResult deny(Actor a, @Nullable UUID from) {
		if (!this.s.rules().requests) {
			return OpResult.fail("disabled");
		} else {
			Session me = this.s.session(a);
			if (me == null) {
				return OpResult.fail("player_only");
			} else {
				Requests.Request r = this.incoming(me.id, from);
				if (r == null) {
					return OpResult.fail("no_request");
				} else {
					this.open.remove(r);
					this.changed(r.from, r.to);
					Session other = this.s.session(r.from);
					if (other != null) {
						this.s.notice(other, "tpa_denied", 0, Msg.tr("cfhgui_server.chat.tpa_denied", Text.shown(me.record)), Text.shown(me.record));
					}

					return OpResult.ok(other == null ? "" : Text.shown(other.record));
				}
			}
		}
	}

	public OpResult cancel(Actor a, @Nullable UUID to) {
		Session me = this.s.session(a);
		if (me == null) {
			return OpResult.fail("player_only");
		} else {
			int n = 0;
			Iterator<Requests.Request> it = this.open.iterator();

			while (it.hasNext()) {
				Requests.Request r = it.next();
				if (r.from.equals(me.id) && (to == null || r.to.equals(to))) {
					it.remove();
					this.s.sync.mark(r.to, "requests");
					n++;
				}
			}

			if (n == 0) {
				return OpResult.fail("no_request");
			} else {
				this.s.sync.mark(me.id, "requests");
				return OpResult.ok(n);
			}
		}
	}

	public OpResult toggle(Actor a, @Nullable Boolean on) {
		if (!this.s.rules().requests) {
			return OpResult.fail("disabled");
		} else {
			Session me = this.s.session(a);
			if (me == null) {
				return OpResult.fail("player_only");
			} else {
				boolean accepts = on == null ? !me.record.acceptsRequests : on;
				if (accepts == me.record.acceptsRequests) {
					return OpResult.ok(accepts);
				} else if (!me.allow("tptoggle", 2000L, this.s.now())) {
					return OpResult.fail("too_fast");
				} else {
					me.record.acceptsRequests = accepts;
					this.s.touch(me.record);
					this.s.sync.mark(me.id, "self");
					this.s.sync.markAll("online");
					return OpResult.ok(accepts);
				}
			}
		}
	}

	void tick() {
		if (!this.open.isEmpty()) {
			long now = this.s.now();
			Iterator<Requests.Request> it = this.open.iterator();

			while (it.hasNext()) {
				Requests.Request r = it.next();
				if (now >= r.expiresAt) {
					it.remove();
					this.changed(r.from, r.to);
					Session from = this.s.session(r.from);
					Session to = this.s.session(r.to);
					if (from != null && to != null) {
						this.s.notice(from, "tpa_expired", 0, Msg.tr("cfhgui_server.chat.tpa_expired", Text.shown(to.record)), Text.shown(to.record));
					}
				}
			}
		}
	}

	void left(UUID player) {
		Iterator<Requests.Request> it = this.open.iterator();

		while (it.hasNext()) {
			Requests.Request r = it.next();
			if (r.from.equals(player) || r.to.equals(player)) {
				it.remove();
				this.changed(r.from, r.to);
			}
		}
	}

	private void changed(UUID a, UUID b) {
		this.s.sync.mark(a, "requests");
		this.s.sync.mark(b, "requests");
	}

	TeleportRequest[] view(UUID player) {
		List<TeleportRequest> out = new ArrayList<>(2);

		for (Requests.Request r : this.open) {
			boolean incoming = r.to.equals(player);
			if (incoming || r.from.equals(player)) {
				Session other = this.s.session(incoming ? r.from : r.to);
				if (other != null) {
					TeleportRequest v = new TeleportRequest();
					v.player = other.id;
					v.name = Text.shown(other.record);
					v.incoming = incoming;
					v.here = r.here;
					v.expiresAt = r.expiresAt;
					out.add(v);
				}
			}
		}

		return out.toArray(new TeleportRequest[0]);
	}

	List<String> askers(UUID to) {
		List<String> out = new ArrayList<>(2);

		for (Requests.Request r : this.open) {
			if (r.to.equals(to)) {
				Session from = this.s.session(r.from);
				if (from != null) {
					out.add(from.name);
				}
			}
		}

		return out;
	}

	int size() {
		return this.open.size();
	}

	static final class Request {
		final UUID from;
		final UUID to;
		final boolean here;
		final long expiresAt;

		Request(UUID from, UUID to, boolean here, long expiresAt) {
			this.from = from;
			this.to = to;
			this.here = here;
			this.expiresAt = expiresAt;
		}
	}
}
