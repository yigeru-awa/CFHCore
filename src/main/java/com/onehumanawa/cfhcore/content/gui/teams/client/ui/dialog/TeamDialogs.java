package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface TeamDialogs {
	void confirm(Component var1, Component var2, Component var3, boolean var4, Runnable var5);

	void confirmTyped(Component var1, Component var2, String var3, Component var4, Runnable var5);

	void foundTeam();

	void invite();

	void waypoint(@Nullable TeamData.Waypoint var1);

	void banner(BannerData var1, Consumer<BannerData> var2);

	void pickTeam(Component var1, List<TeamSummary> var2, Consumer<TeamSummary> var3);

	void pickMember(Component var1, List<TeamData.Member> var2, Consumer<TeamData.Member> var3);
}
