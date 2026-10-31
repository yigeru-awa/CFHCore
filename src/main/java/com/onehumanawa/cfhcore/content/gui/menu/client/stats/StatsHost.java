package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import org.jetbrains.annotations.Nullable;

public interface StatsHost {
   StatsFeed feed();

   boolean canChat();

   boolean canRank();

   void rankBy(String var1);

   void viewPlayer(StatsLink.@Nullable PlayerRef var1);

   void openTab(StatsTab var1);

   boolean onPanel();
}
