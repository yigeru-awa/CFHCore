package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Rows;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKeys;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsData;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Summary;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.overview.OverviewModel;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.overview.OverviewView;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.Column;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcon;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.StatTable;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.TableRow;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class StatsContent {
   private static final int PAGE = 50;
   private static final long SERVER_REFRESH_MS = 10000L;
   private final StatsHost host;
   private final Runnable relayout;
   private final Map<StatsTab, StatTable.State> tableStates = new EnumMap(StatsTab.class);
   private final OverviewView.State overviewState = new OverviewView.State();
   private @Nullable StatsTab built;
   private @Nullable Phase builtPhase;
   private @Nullable StatTable table;
   private @Nullable OverviewView overview;
   private @Nullable Summary summary;
   private int seenVersion = Integer.MIN_VALUE;
   private @Nullable StatsData tabled;
   private @Nullable List<StatsLink.PlayerRef> players;
   private long playersAskedMs;
   private boolean playersFresh;
   private String rankKey = StatKeys.custom("play_time");
   private final List<StatsLink.Row> boardRows = new ArrayList();
   private StatsLink.@Nullable Row boardSelf;
   private int boardTotal = -1;
   private boolean boardAsking;
   private boolean boardFresh;
   private long boardAskedMs;
   private long playersSinceMs;
   private long boardSinceMs;
   private int boardToken;

   public StatsContent(StatsHost host, Runnable relayout) {
      this.host = host;
      this.relayout = relayout;
   }

   public Summary summary() {
      if (this.summary == null) {
         this.summary = new Summary(this.host.feed().shown());
      }

      return this.summary;
   }

   public OverviewModel overviewModel() {
      return new OverviewModel(this.summary(), this.host.feed());
   }

   public Phase phase(StatsTab tab) {
      StatsFeed feed = this.host.feed();
      if (tab == StatsTab.PLAYERS) {
         return this.players == null ? StatsContent.Phase.LOADING : StatsContent.Phase.READY;
      } else if (tab == StatsTab.RANKINGS) {
         return this.boardTotal < 0 ? StatsContent.Phase.LOADING : StatsContent.Phase.READY;
      } else if (feed.loading()) {
         return StatsContent.Phase.LOADING;
      } else {
         return feed.missing() ? StatsContent.Phase.NOTHING : StatsContent.Phase.READY;
      }
   }

   public Component message(StatsTab tab) {
      StatsFeed feed = this.host.feed();
      if (this.phase(tab) == StatsContent.Phase.NOTHING) {
         return Component.translatable("cfhgui.stats.player_missing", new Object[]{feed.shownName()});
      } else if (tab.server) {
         long since = tab == StatsTab.PLAYERS ? this.playersSinceMs : this.boardSinceMs;
         boolean slow = since > 0L && System.currentTimeMillis() - since >= 5000L;
         return Component.translatable(slow ? "cfhgui.stats.still_waiting" : "cfhgui.stats.asking_server");
      } else {
         return Component.translatable(feed.slow() ? "cfhgui.stats.still_waiting" : "multiplayer.downloadingStats");
      }
   }

   public @Nullable CFHGuiSearchField search() {
      return this.table == null ? null : this.table.searchField();
   }

   public @Nullable AbstractWidget keyTarget() {
      if (this.table != null) {
         return this.table.list();
      } else {
         return this.overview == null ? null : this.overview.panel();
      }
   }

   public String rankKey() {
      return this.rankKey;
   }

   public void rankBy(String key) {
      if (!key.equals(this.rankKey) || this.boardTotal < 0) {
         this.rankKey = key;
         this.boardRows.clear();
         this.boardSelf = null;
         this.boardTotal = -1;
         ++this.boardToken;
         this.boardAsking = false;
         this.boardSinceMs = 0L;
         this.tableStates.remove(StatsTab.RANKINGS);
         if (this.built == StatsTab.RANKINGS) {
            this.askBoard(0);
            this.relayout.run();
         }

      }
   }

   public void build(StatsTab tab, Rect area, Consumer<AbstractWidget> add) {
      this.built = tab;
      this.table = null;
      this.tabled = null;
      this.overview = null;
      this.seenVersion = this.host.feed().version();
      this.summary = null;
      if (tab == StatsTab.PLAYERS && this.players == null && System.currentTimeMillis() - this.playersAskedMs > 1000L) {
         this.askPlayers();
      }

      if (tab == StatsTab.RANKINGS && this.boardTotal < 0 && !this.boardAsking) {
         this.askBoard(0);
      }

      this.builtPhase = this.phase(tab);
      if (this.builtPhase == StatsContent.Phase.READY) {
         StatsData data = this.host.feed().shown();
         this.tabled = data;
         switch (tab) {
            case OVERVIEW:
               this.overview = (new OverviewView(this.host, this.overviewState, this.overviewModel())).onRelayout(this.relayout);
               this.overview.build(area, add);
               break;
            case COMBAT:
               this.table(tab, area, add, List.of(Column.count(Component.translatable("cfhgui.stats.col.kills"), Component.translatable("cfhgui.stats.col.kills.tip"), 58), Column.count(Component.translatable("cfhgui.stats.col.deaths"), Component.translatable("cfhgui.stats.col.deaths.tip"), 58)), 0, Rows.mobs(data), "cfhgui.stats.empty.combat", true, true);
               break;
            case TRAVEL:
               this.table(tab, area, add, List.of(new Column(Component.translatable("cfhgui.stats.col.distance"), (Component)null, StatKind.DISTANCE, true, 10000, (ResourceLocation)null)), 0, Rows.ways(this.summary()), "cfhgui.stats.empty.travel", false, true);
               break;
            case BLOCKS:
               this.table(tab, area, add, List.of(Column.count(Component.translatable("cfhgui.stats.col.mined"), StatNames.typeName("minecraft:mined"), 52, "statistics/block_mined"), Column.count(Component.translatable("cfhgui.stats.col.placed"), Component.translatable("cfhgui.stats.col.placed.tip"), 52, "statistics/item_used"), Column.count(Component.translatable("cfhgui.stats.col.crafted"), StatNames.typeName("minecraft:crafted"), 52, "statistics/item_crafted"), Column.count(Component.translatable("cfhgui.stats.col.picked_up"), StatNames.typeName("minecraft:picked_up"), 56, "statistics/item_picked_up"), Column.count(Component.translatable("cfhgui.stats.col.dropped"), StatNames.typeName("minecraft:dropped"), 52, "statistics/item_dropped")), 0, Rows.blocks(data), "cfhgui.stats.empty.blocks", true, true);
               break;
            case ITEMS:
               this.table(tab, area, add, List.of(Column.count(Component.translatable("cfhgui.stats.col.used"), StatNames.typeName("minecraft:used"), 52, "statistics/item_used"), Column.count(Component.translatable("cfhgui.stats.col.crafted"), StatNames.typeName("minecraft:crafted"), 52, "statistics/item_crafted"), Column.count(Component.translatable("cfhgui.stats.col.broken"), StatNames.typeName("minecraft:broken"), 52, "statistics/item_broken"), Column.count(Component.translatable("cfhgui.stats.col.picked_up"), StatNames.typeName("minecraft:picked_up"), 56, "statistics/item_picked_up"), Column.count(Component.translatable("cfhgui.stats.col.dropped"), StatNames.typeName("minecraft:dropped"), 52, "statistics/item_dropped")), 0, Rows.items(data), "cfhgui.stats.empty.items", true, true);
               break;
            case GENERAL:
               this.table(tab, area, add, List.of(new Column(Component.translatable("cfhgui.stats.col.value"), (Component)null, StatKind.COUNT, false, 110, (ResourceLocation)null)), -1, Rows.general(data), "cfhgui.stats.empty.general", true, false);
               break;
            case PLAYERS:
               this.players(area, add);
               break;
            case RANKINGS:
               this.rankings(area, add);
         }

      }
   }

   private StatTable.State state(StatsTab tab) {
      return (StatTable.State)this.tableStates.computeIfAbsent(tab, (t) -> new StatTable.State());
   }

   private void table(StatsTab tab, Rect area, Consumer<AbstractWidget> add, List<Column> columns, int ownSort, List<TableRow> rows, String emptyKey, boolean searchable, boolean icons) {
      StatTable t = new StatTable(columns, this.state(tab), ownSort, new NumberRows());
      t.onPanel(this.host.onPanel()).empty(Component.translatable(emptyKey)).onRelayout(this.relayout);
      if (!searchable) {
         t.plain();
      }

      if (!icons) {
         t.noIcons();
      }

      t.rows(rows);
      t.build(area, add);
      this.table = t;
   }

   private void players(Rect area, Consumer<AbstractWidget> add) {
      StatTable t = new StatTable(List.of(), this.state(StatsTab.PLAYERS), Integer.MIN_VALUE, new StatTable.Listener() {
         public void menu(TableRow row, int column, double mouseX, double mouseY) {
            Object var8 = row.payload;
            if (var8 instanceof StatsLink.PlayerRef ref) {
               CFHGuiContextMenu.open(mouseX, mouseY, List.of(Item.of(Component.translatable("cfhgui.stats.menu.view"), Icon.USER, () -> StatsContent.this.view(ref))));
            }
         }

         public void activate(TableRow row) {
            Object var3 = row.payload;
            if (var3 instanceof StatsLink.PlayerRef ref) {
               StatsContent.this.view(ref);
            }

         }
      });
      t.pickOnClick().onPanel(this.host.onPanel()).empty(Component.translatable("cfhgui.stats.empty.players")).onRelayout(this.relayout);
      t.rows(this.playerRows());
      t.build(area, add);
      this.table = t;
   }

   private void view(StatsLink.PlayerRef ref) {
      this.host.viewPlayer(ref);
      this.host.openTab(StatsTab.OVERVIEW);
   }

   private List<TableRow> playerRows() {
      List<TableRow> out = new ArrayList();
      if (this.players == null) {
         return out;
      } else {
         StatsFeed feed = this.host.feed();

         for(StatsLink.PlayerRef ref : this.players) {
            boolean me = ref.id().equals(feed.selfId());
            TableRow row = TableRow.of(ref.id().toString(), "", Component.literal(ref.name()), new RowIcon.Face(ref.id(), ref.name(), ref.online()), new long[0], new String[0]).payload(ref);
            row.own = ref.id().equals(feed.shownId());
            row.note(Component.translatable(me ? "cfhgui.stats.note.you" : (ref.online() ? "cfhgui.stats.note.online" : "cfhgui.stats.note.offline")));
            out.add(row);
         }

         return out;
      }
   }

   private void askPlayers() {
      this.playersAskedMs = System.currentTimeMillis();
      Optional<StatsLink.Provider> provider = StatsFeed.provider();
      if (provider.isEmpty()) {
         this.players = List.of();
      } else {
         if (this.players == null && this.playersSinceMs == 0L) {
            this.playersSinceMs = this.playersAskedMs;
         }

         try {
            ((StatsLink.Provider)provider.get()).players((list) -> {
               List<StatsLink.PlayerRef> next = list == null ? List.of() : List.copyOf(list);
               if (!next.equals(this.players)) {
                  this.players = next;
                  this.playersFresh = true;
               }

            });
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the server's players could not be asked for: {}", e.toString());
            this.players = List.of();
         }

      }
   }

   private List<String> rankChoices() {
      List<String> out = new ArrayList();

      for(String path : new String[]{"play_time", "deaths", "mob_kills", "player_kills", "damage_dealt", "damage_taken", "walk_one_cm", "sprint_one_cm", "aviate_one_cm", "jump", "fish_caught", "animals_bred", "traded_with_villager", "enchant_item", "sleep_in_bed"}) {
         out.add(StatKeys.custom(path));
      }

      out.add(1, "afk_ticks");
      if ((Boolean)StatsFeed.provider().map(StatsLink.Provider::ranksTotals).orElse(false)) {
         int at = 5;

         for(String type : new String[]{"minecraft:mined", "minecraft:crafted", "minecraft:used", "minecraft:picked_up"}) {
            out.add(at++, StatsLink.total(type));
         }
      }

      if (!out.contains(this.rankKey)) {
         out.add(0, this.rankKey);
      }

      return out;
   }

   private void rankings(Rect area, Consumer<AbstractWidget> add) {
      int pickW = Math.min(area.w(), 220);
      CFHGuiDropdown<String> pick = new CFHGuiDropdown<String>(area.x(), area.y(), pickW, this.rankChoices(), this.rankKey, StatNames::ofKey, this::rankBy);
      pick.label(Component.translatable("cfhgui.stats.rank.by"));
      pick.tip(Component.translatable("cfhgui.stats.rank.by.tip"));
      add.accept(pick);
      StatKind kind = StatNames.kindOfKey(this.rankKey);
      StatTable t = new StatTable(List.of(new Column(Component.translatable("cfhgui.stats.col.value"), StatNames.ofKey(this.rankKey), kind, false, 110, (ResourceLocation)null)), this.state(StatsTab.RANKINGS), Integer.MIN_VALUE, new StatTable.Listener() {
         public void menu(TableRow row, int column, double mouseX, double mouseY) {
            StatTable own = StatsContent.this.table;
            StatFmt.Shown shown = own == null ? null : own.shownAt(row, 0);
            if (shown != null) {
               List<MenuPopup.Item> first = new ArrayList();
               Object var11 = row.payload;
               if (var11 instanceof StatsLink.PlayerRef) {
                  StatsLink.PlayerRef ref = (StatsLink.PlayerRef)var11;
                  first.add(Item.of(Component.translatable("cfhgui.stats.menu.view"), Icon.USER, () -> StatsContent.this.view(ref)));
               }

               Component label = Component.translatable("cfhgui.stats.rank.line", new Object[]{row.rank, row.name, StatNames.ofKey(StatsContent.this.rankKey)});
               StatMenu.open(StatsContent.this.host, mouseX, mouseY, new StatMenu.Subject(label, shown, (String)null), first);
            }
         }

         public void activate(TableRow row) {
            Object var3 = row.payload;
            if (var3 instanceof StatsLink.PlayerRef ref) {
               StatsContent.this.view(ref);
            }

         }

         public void copy(TableRow row, int column) {
            StatTable own = StatsContent.this.table;
            StatFmt.Shown shown = own == null ? null : own.shownAt(row, 0);
            if (shown != null) {
               StatMenu.copy(StatsContent.this.host, new StatMenu.Subject(Component.translatable("cfhgui.stats.rank.line", new Object[]{row.rank, row.name, StatNames.ofKey(StatsContent.this.rankKey)}), shown, (String)null));
            }
         }
      });
      t.plain().ranked().onPanel(this.host.onPanel()).empty(Component.translatable("cfhgui.stats.empty.rankings")).onRelayout(this.relayout);
      t.rows(this.boardTableRows());
      t.pinned(this.boardSelf == null ? null : this.boardRow(this.boardSelf));
      t.build(new Rect(area.x(), area.y() + 20 + 4, area.w(), area.h() - 20 - 4), add);
      this.table = t;
   }

   private TableRow boardRow(StatsLink.Row r) {
      StatsFeed feed = this.host.feed();
      TableRow row = TableRow.of(r.player().toString(), "", Component.literal(r.name()), new RowIcon.Face(r.player(), r.name(), (Boolean)null), new long[]{r.value()}, new String[]{null}).payload(new StatsLink.PlayerRef(r.player(), r.name(), false));
      row.kind(StatNames.kindOfKey(this.rankKey), StatNames.customIdOfKey(this.rankKey));
      row.rank = r.rank();
      row.own = r.player().equals(feed.selfId());
      return row;
   }

   private List<TableRow> boardTableRows() {
      List<TableRow> out = new ArrayList(this.boardRows.size());

      for(StatsLink.Row r : this.boardRows) {
         out.add(this.boardRow(r));
      }

      return out;
   }

   private void askBoard(int offset) {
      Optional<StatsLink.Provider> provider = StatsFeed.provider();
      this.boardAskedMs = System.currentTimeMillis();
      if (provider.isEmpty()) {
         this.boardTotal = 0;
      } else {
         this.boardAsking = true;
         if (this.boardTotal < 0 && this.boardSinceMs == 0L) {
            this.boardSinceMs = this.boardAskedMs;
         }

         int token = this.boardToken;
         int count = offset == 0 ? Math.max(50, this.boardRows.size()) : 50;

         try {
            ((StatsLink.Provider)provider.get()).board(this.rankKey, offset, count, (board) -> {
               if (token == this.boardToken) {
                  this.boardAsking = false;
                  if (board == null) {
                     this.boardTotal = Math.max(0, this.boardTotal);
                     this.boardFresh = true;
                  } else {
                     if (offset == 0) {
                        this.boardRows.clear();
                     }

                     while(this.boardRows.size() > offset) {
                        this.boardRows.remove(this.boardRows.size() - 1);
                     }

                     this.boardRows.addAll(board.rows());
                     this.boardSelf = board.self();
                     this.boardTotal = Math.max(board.total(), this.boardRows.size());
                     this.boardFresh = true;
                  }
               }
            });
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] a ranking could not be asked for: {}", e.toString());
            this.boardAsking = false;
            this.boardTotal = Math.max(0, this.boardTotal);
         }

      }
   }

   public void tick() {
      StatsTab tab = this.built;
      if (tab != null) {
         long now = System.currentTimeMillis();
         if (tab == StatsTab.PLAYERS && now - this.playersAskedMs >= 10000L) {
            this.askPlayers();
         }

         if (tab == StatsTab.RANKINGS) {
            if (this.boardAsking && now - this.boardAskedMs >= 10000L) {
               this.boardAsking = false;
            }

            if (!this.boardAsking) {
               if (now - this.boardAskedMs >= 10000L) {
                  this.askBoard(0);
               } else if (this.table != null && this.boardTotal > this.boardRows.size() && this.table.nearEnd()) {
                  this.askBoard(this.boardRows.size());
               }
            }
         }

         StatsFeed feed = this.host.feed();
         boolean numbers = feed.version() != this.seenVersion;
         if (numbers) {
            this.seenVersion = feed.version();
            this.summary = null;
         }

         if (this.builtPhase != this.phase(tab)) {
            this.relayout.run();
         } else if (this.builtPhase == StatsContent.Phase.READY) {
            if (tab != StatsTab.PLAYERS) {
               if (tab == StatsTab.RANKINGS) {
                  boolean fresh = this.boardFresh;
                  this.boardFresh = false;
                  if (fresh && this.table != null) {
                     this.table.rows(this.boardTableRows());
                     this.table.pinned(this.boardSelf == null ? null : this.boardRow(this.boardSelf));
                  }

               } else if (numbers) {
                  StatsData data = feed.shown();
                  switch (tab) {
                     case OVERVIEW:
                        if (this.overview != null) {
                           this.overview.model(this.overviewModel());
                        }
                        break;
                     case COMBAT:
                        if (this.table != null && this.differ(data, "minecraft:killed", "minecraft:killed_by")) {
                           this.table.rows(Rows.mobs(data));
                        }
                        break;
                     case TRAVEL:
                        if (this.table != null) {
                           this.table.rows(Rows.ways(this.summary()));
                        }
                        break;
                     case BLOCKS:
                        if (this.table != null && this.differ(data, "minecraft:mined", "minecraft:used", "minecraft:crafted", "minecraft:picked_up", "minecraft:dropped")) {
                           this.table.rows(Rows.blocks(data));
                        }
                        break;
                     case ITEMS:
                        if (this.table != null && this.differ(data, "minecraft:used", "minecraft:crafted", "minecraft:broken", "minecraft:picked_up", "minecraft:dropped")) {
                           this.table.rows(Rows.items(data));
                        }
                        break;
                     case GENERAL:
                        if (this.table != null) {
                           this.table.rows(Rows.general(data));
                        }
                  }

                  this.tabled = data;
               }
            } else {
               if ((this.playersFresh || numbers) && this.table != null) {
                  this.table.rows(this.playerRows());
               }

               this.playersFresh = false;
            }
         }
      }
   }

   private boolean differ(StatsData now, String... types) {
      StatsData before = this.tabled;
      if (before == null) {
         return true;
      } else {
         for(String type : types) {
            if (!before.type(type).equals(now.type(type))) {
               return true;
            }
         }

         return false;
      }
   }

   public void close() {
      this.built = null;
      this.table = null;
      this.tabled = null;
      this.overview = null;
      ++this.boardToken;
      this.boardAsking = false;
      this.playersSinceMs = 0L;
      this.boardSinceMs = 0L;
   }

   public static enum Phase {
      LOADING,
      NOTHING,
      READY;
   }

   private final class NumberRows implements StatTable.Listener {
      public void menu(TableRow row, int column, double mouseX, double mouseY) {
         StatTable t = StatsContent.this.table;
         if (t != null) {
            int c = column >= 0 ? column : t.mainColumn();
            StatFmt.Shown shown = t.shownAt(row, c);
            if (shown != null) {
               StatMenu.open(StatsContent.this.host, mouseX, mouseY, new StatMenu.Subject(t.labelAt(row, c), shown, row.key(c)), List.of());
            }
         }
      }

      public void activate(TableRow row) {
         StatTable t = StatsContent.this.table;
         AbstractWidget list = t == null ? null : t.list();
         if (list != null) {
            this.menu(row, -1, (double)(list.getX() + 80), (double)(list.getY() + 18));
         }
      }

      public void copy(TableRow row, int column) {
         StatTable t = StatsContent.this.table;
         StatFmt.Shown shown = t == null ? null : t.shownAt(row, column);
         if (shown != null) {
            StatMenu.copy(StatsContent.this.host, new StatMenu.Subject(t.labelAt(row, column), shown, row.key(column)));
         }

      }
   }
}
