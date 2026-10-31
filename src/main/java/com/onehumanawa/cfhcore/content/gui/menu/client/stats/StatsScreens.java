package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats.OverhaulStatsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.SampleStats;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import org.jetbrains.annotations.Nullable;

public final class StatsScreens {
   private static @Nullable StatsTab firstTab;

   public static void register() {
      ScreenIds.register(CFHGuiStatsScreen.class, "cfhgui:stats", "CFHGui statistics");
      MenuSlots.provide("minecraft:stats", Layout.CUSTOM, CFHGuiStatsScreen::replacing);
      AfkClock.init();
      if (SampleStats.on() || Boolean.getBoolean("cfhgui.stats.sampleProvider")) {
         SampleStats.register();
      }

      if (System.getProperty("cfhgui.autoScreens") != null || Boolean.getBoolean("cfhgui.autoWorld")) {
         StatsShots.register();
      }

      CoreActions.SCREEN_FACTORIES.put("cfhgui:stats", (p) -> open(p, (StatsTab)null, (Layout)null));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_custom", (p) -> open(p, (StatsTab)null, Layout.CUSTOM));

      for(StatsTab tab : StatsTab.values()) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stats/" + tab.id(), (p) -> open(p, tab, (Layout)null));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_custom/" + tab.id(), (p) -> open(p, tab, Layout.CUSTOM));
      }

   }

   public static Screen open(@Nullable Screen parent, @Nullable StatsTab tab, @Nullable Layout layout) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.getConnection() != null) {
         if (layout == Layout.OVERHAUL) {
            return new OverhaulStatsScreen(parent, mc.player.getStats(), false, tab);
         } else if (layout == Layout.CUSTOM) {
            return new CFHGuiStatsScreen(parent, mc.player.getStats(), false, tab);
         } else {
            firstTab = MenuSlots.effective("minecraft:stats") == Layout.VANILLA ? null : tab;
            return new StatsScreen(parent, mc.player.getStats());
         }
      } else {
         Layout shown = layout != null ? layout : MenuSlots.effective("minecraft:stats");
         return shown == Layout.OVERHAUL ? OverhaulStatsScreen.preview(parent, tab) : CFHGuiStatsScreen.preview(parent, tab);
      }
   }

   public static @Nullable StatsTab takeFirstTab() {
      StatsTab tab = firstTab;
      firstTab = null;
      return tab;
   }

   private StatsScreens() {
   }
}
