package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

final class ForceLoads {
	private final TeamService teams;

	ForceLoads(TeamService teams) {
		this.teams = teams;
	}

	private boolean wanted(ClaimMap.Claim c, @Nullable UUID leaving) {
		return c.forced && this.teams.cfg().claims ? !this.teams.cfg().forceLoadOnlyWhileOnline || this.teams.onlineCount(c.team, leaving) > 0 : false;
	}

	void apply(ClaimMap.Dim d, long chunk, ClaimMap.Claim c, @Nullable UUID leaving) {
		if (this.wanted(c, leaving)) {
			this.hold(d, chunk, c);
		} else {
			this.release(d, chunk, c);
		}
	}

	void refresh(TeamData t, @Nullable UUID leaving) {
		List<ClaimMap.Entry> forced = this.teams.claims.forcedOf(t.id);
		if (!forced.isEmpty()) {
			for (ClaimMap.Entry e : forced) {
				this.apply(e.dim(), e.chunk(), e.claim(), leaving);
			}
		}
	}

	void refreshAll(@Nullable UUID leaving) {
		for (List<ClaimMap.Entry> list : new ArrayList<>(this.teams.claims.allForced())) {
			for (ClaimMap.Entry e : list) {
				this.apply(e.dim(), e.chunk(), e.claim(), leaving);
			}
		}
	}

	private void hold(ClaimMap.Dim d, long chunk, ClaimMap.Claim c) {
		if (!c.held && this.teams.live) {
			ServerLevel level = this.teams.server.getLevel(d.key);
			if (level != null && !level.getForcedChunks().contains(chunk)) {
				level.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true);
				c.held = true;
				this.teams.claims.heldChanged();
			}
		}
	}

	void release(ClaimMap.Dim d, long chunk, ClaimMap.Claim c) {
		if (c.held) {
			c.held = false;
			this.teams.claims.heldChanged();
			if (this.teams.live) {
				ServerLevel level = this.teams.server.getLevel(d.key);
				if (level != null) {
					level.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
				}
			}
		}
	}

	void stopping() {
		if (this.teams.cfg().forceLoadOnlyWhileOnline) {
			for (List<ClaimMap.Entry> list : new ArrayList<>(this.teams.claims.allForced())) {
				for (ClaimMap.Entry e : list) {
					this.release(e.dim(), e.chunk(), e.claim());
				}
			}
		}
	}
}
