package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class LandNames {
	public static MutableComponent personal(String player) {
		return Component.translatable("cfhgui_teams.land.personal", new Object[]{player});
	}

	public static MutableComponent of(TeamSummary holder) {
		return holder.solo ? personal(holder.ownerName.isEmpty() ? holder.name : holder.ownerName) : Component.literal(holder.name);
	}

	public static MutableComponent of(TeamData holder) {
		if (!holder.solo) {
			return Component.literal(holder.name);
		} else {
			TeamData.Member owner = holder.owner();
			return personal(owner != null && !owner.name.isEmpty() ? owner.name : holder.name);
		}
	}

	@Nullable
	public static MutableComponent of(@Nullable UUID holder) {
		TeamData mine = TeamsState.land();
		if (mine != null && mine.id.equals(holder)) {
			return of(mine);
		} else {
			TeamSummary s = TeamsState.summary(holder);
			return s == null ? null : of(s);
		}
	}

	public static String plain(@Nullable UUID holder) {
		MutableComponent name = of(holder);
		return name == null ? "" : name.getString();
	}

	public static boolean isPersonal(@Nullable UUID holder) {
		TeamData mine = TeamsState.land();
		if (mine != null && mine.id.equals(holder)) {
			return mine.solo;
		} else {
			TeamSummary s = TeamsState.summary(holder);
			return s != null && s.solo;
		}
	}

	private LandNames() {
	}
}
