package com.onehumanawa.cfhcore.content.gui.menu;

import java.util.ArrayList;
import java.util.List;

public final class MenuConfig {
   public boolean titleScreen = true;
   public boolean worldsScreen = true;
   public boolean serversScreen = true;
   public boolean pauseScreen = true;
   public boolean optionsScreen = true;
   public boolean disconnectedScreen = true;
   public boolean layoutFlagsMigrated = false;
   public boolean loadingScreens = true;
   public boolean tabList = true;
   public boolean panorama = true;
   public boolean splash = true;
   public boolean showLogo = true;
   public boolean showContinueCard = true;
   public boolean showAccountCard = true;
   public boolean showFriendsPanel = false;
   public boolean showFooter = true;
   public boolean showModsButton = true;
   public boolean confirmQuitGame = true;
   public boolean skipOnboarding = true;
   public String titleScene = "hearth";
   public String titleTextures = "mixed";
   public boolean titleChests = true;
   public String titlePlayIcon = "block";
   public String worldsView = "GRID";
   public String worldsSort = "LAST_PLAYED";
   public boolean worldsShowDetails = true;
   public boolean serverAutoRefresh = true;
   public int serverRefreshSeconds = 30;
   public String serversSort = "MANUAL";
   public boolean showLan = true;
   public boolean showCommunity = true;
   public boolean showRecent = true;
   public int recentServersMax = 8;
   public String playSort = "RECENT";
   public int ringPlanets = 11;
   public boolean createPreviewMinimised = false;
   public int createPreviewSpan = 3072;
   public String screenshotsSort = "NEWEST";
   public int screenshotColumns = 0;
   public boolean confirmQuit = true;
   public boolean showSessionTime = true;
   public List<String> creativeTabOrder = new ArrayList();
   public int statsAfkMinutes = 2;
   public boolean statsRankings = true;
   public boolean friendsPanelOptIn = true;
   public String lastKind = "";
   public String lastWorld = "";
   public String lastWorldName = "";
   public String lastServerAddress = "";
   public String lastServerName = "";
   public long lastPlayedAt = 0L;
   public List<RecentServer> recentServers = new ArrayList();

   public static final class RecentServer {
      public String name = "";
      public String address = "";
      public long lastJoinedAt;

      public RecentServer() {
      }

      public RecentServer(String name, String address, long at) {
         this.name = name;
         this.address = address;
         this.lastJoinedAt = at;
      }
   }
}
