package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class Expiry {
	static final long DAY_MS = 86400000L;
	static final int EVERY_TICKS = 72000;
	private static final UUID NOBODY = new UUID(0L, 0L);
	private final TeamService teams;

	Expiry(TeamService teams) {
		this.teams = teams;
	}

	void seen(TeamData t) {
		t.lastSeen = this.teams.now();
		this.teams.store.touch(t);
	}

	List<TeamData> run() {
		long now = this.teams.now();
		int days = this.teams.cfg().claimExpiryDays;
		List<TeamData> released = List.of();
		List<TeamData> bare = List.of();

		for (TeamData t : this.teams.store.all()) {
			if (t.lastSeen <= 0L || t.lastSeen > now || this.teams.anyOnline(t)) {
				t.lastSeen = now;
				this.teams.store.touch(t);
			} else if (days > 0 && now - t.lastSeen >= (long)days * 86400000L) {
				int chunks = this.teams.claims.count(t.id);
				if (chunks != 0) {
					long away = (now - t.lastSeen) / 86400000L;

					for (ClaimMap.Entry e : this.teams.claims.of(t)) {
						this.teams.free(e.dim().id, e.chunk(), e.claim());
					}

					this.teams.log(t, "expired", NOBODY, "", String.valueOf(away));
					this.teams.sync.directory(false);
					if (released.isEmpty()) {
						released = new ArrayList<>();
					}

					released.add(t);
					if (t.solo) {
						if (bare.isEmpty()) {
							bare = new ArrayList<>();
						}

						bare.add(t);
					}

					if (this.teams.live) {
						CFHGuiTeams.LOGGER.info("[CFHGui Teams] the land of {} was given up ({} chunk(s)): no member was here for {} day(s)", new Object[]{t.name, chunks, away});
					}
				}
			}
		}

		for (TeamData tx : bare) {
			this.teams.drop(tx);
		}

		return released;
	}

	List<Expiry.Standing> standings() {
		long now = this.teams.now();
		int days = this.teams.cfg().claimExpiryDays;
		List<Expiry.Standing> out = new ArrayList<>();

		for (TeamData t : this.teams.store.all()) {
			int chunks = this.teams.claims.count(t.id);
			if (chunks != 0) {
				boolean online = this.teams.anyOnline(t);
				long seen = t.lastSeen > 0L && t.lastSeen <= now ? t.lastSeen : now;
				out.add(new Expiry.Standing(t, chunks, online, seen, days > 0 && !online ? seen + (long)days * 86400000L : 0L));
			}
		}

		out.sort(
			Comparator.<Expiry.Standing>comparingLong(s -> s.due() == 0L ? Long.MAX_VALUE : s.due()).thenComparing(s -> s.team().name, String.CASE_INSENSITIVE_ORDER)
		);
		return out;
	}

	static record Standing(TeamData team, int chunks, boolean online, long lastSeen, long due) {
	}
}
