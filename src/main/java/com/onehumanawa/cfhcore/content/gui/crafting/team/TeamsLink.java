package com.onehumanawa.cfhcore.content.gui.crafting.team;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public interface TeamsLink {
	@Nullable
	TeamsLink.TeamInfo teamOf(UUID var1);

	@Nullable
	TeamsLink.TeamInfo team(UUID var1);

	void listen(BiConsumer<UUID, UUID> var1, BiConsumer<UUID, UUID> var2, Consumer<UUID> var3);

	public static record TeamInfo(UUID id, String name, List<UUID> members, Set<UUID> officers) {
		public boolean isMember(UUID player) {
			return this.members.contains(player);
		}

		public boolean isOfficer(UUID player) {
			return this.officers.contains(player);
		}
	}
}
