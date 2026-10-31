package com.onehumanawa.cfhcore.content.gui.menu.client.servers;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.CommunityServers;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.LanScanner;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerMeta;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.SamplePlay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiServersScreen extends CFHGuiScreen {
   private static final Set<String> COLLAPSED = new HashSet();
   private static final long RETRY_UNREACHABLE_MS = 300000L;
   private @Nullable ServerList servers;
   private final ServerPinger pinger = new ServerPinger();
   private @Nullable LanScanner lan;
   private List<LanServer> lanServers = new ArrayList();
   private final Map<String, ServerData> lanData = new LinkedHashMap();
   private final Map<String, ServerData> recentData = new LinkedHashMap();
   private final Map<String, ServerData> communityData = new LinkedHashMap();
   private final Map<String, String> communityDescriptions = new LinkedHashMap();
   private String query = "";
   private Sort sort;
   private @Nullable ServerData selectedData;
   private ServerCard.@Nullable Kind selectedKind;
   private @Nullable ServerData lastClickData;
   private long lastClickMs;
   private int lastMx;
   private int lastMy;
   private long lastRefreshMs = Clock.nowMs();
   private long lastRetryMs = Clock.nowMs();
   private @Nullable CFHGuiTextField quickField;
   private @Nullable CFHGuiButton quickJoin;
   private @Nullable ServerData quickData;
   private String quickAddress = "";
   private long quickTypedMs;
   private boolean quickDirty;
   private int quickStatusY;
   private static final int QUICK_LINE_H = 9;
   private final Anim quickLine;
   private int quickOpen;
   private int listTop;
   private int listBottom;
   private @Nullable CFHGuiScrollPanel panel;
   private @Nullable CFHGuiSearchField search;
   private Rect listRect;
   private final List<ServerCard> cards;
   private final List<CFHGuiButton> savedButtons;
   private final List<CFHGuiButton> anyButtons;
   private @Nullable CFHGuiButton addToListBtn;
   private boolean anyRows;

   public CFHGuiServersScreen(@Nullable Screen parent) {
      super(Component.translatable("multiplayer.title"), parent);
      this.quickLine = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.listRect = new Rect(0, 0, 0, 0);
      this.cards = new ArrayList();
      this.savedButtons = new ArrayList();
      this.anyButtons = new ArrayList();
      String s = CFHGuiMenu.config().serversSort;
      Sort parsed = CFHGuiServersScreen.Sort.MANUAL;

      try {
         parsed = CFHGuiServersScreen.Sort.valueOf(s == null ? "" : s.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var5) {
      }

      this.sort = parsed;
   }

   public ServerPinger pinger() {
      return this.pinger;
   }

   protected void build() {
      this.cards.clear();
      this.savedButtons.clear();
      this.anyButtons.clear();
      MenuConfig cfg = CFHGuiMenu.config();
      if (this.servers == null) {
         this.servers = new ServerList(this.minecraft);
         this.servers.load();
      }

      if (this.lan == null && cfg.showLan) {
         this.lan = new LanScanner();
      }

      Rect c = this.contentRect();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("selectServer.refresh"), () -> this.refreshAll(true)));
      if (this.width >= 500) {
         this.addHeaderAction(new CFHGuiDropdown<Sort>(0, 0, 100, Arrays.asList(CFHGuiServersScreen.Sort.values()), this.sort, (s) -> {
            String var10000 = s.name();
            return Component.translatable("cfhgui.servers.sort." + var10000.toLowerCase(Locale.ROOT));
         }, (s) -> {
            this.sort = s;
            CFHGuiMenu.configFile().update((x) -> x.serversSort = s.name());
            this.rebuildList();
         }));
      }

      CFHGuiSearchField sf = new CFHGuiSearchField(0, 0, this.width >= 640 ? 150 : 96, (q) -> {
         this.query = q;
         this.rebuildList();
      });
      this.search = (CFHGuiSearchField)this.addHeaderAction(sf);
      sf.setValue(this.query);
      int qy = c.y() + 4;
      int joinW = 62;
      CFHGuiTextField qf = new CFHGuiTextField(c.x(), qy, c.w() - 62 - 6, Component.translatable("cfhgui.servers.quick_connect"));
      qf.placeholder(Component.translatable("cfhgui.servers.quick_connect_hint")).icon(Icon.BOLT).text(this.quickAddress).maxLength(128);
      qf.onChange(this::quickChanged);
      qf.onEnter(this::quickJoin);
      this.quickField = (CFHGuiTextField)this.add(qf);
      this.quickJoin = (CFHGuiButton)this.add((new CFHGuiButton(c.right() - 62, qy, 62, Component.translatable("cfhgui.servers.join"), this::quickJoin)).variant(Variant.PRIMARY).icon(Icon.PLAY));
      this.quickJoin.active = ServerAddress.isValidAddress(this.quickAddress);
      this.quickStatusY = qy + 23;
      int bottomH = 30;
      this.listTop = qy + 28;
      this.listBottom = c.bottom() - 30;
      this.quickOpen = Math.round(9.0F * this.quickLine.get());
      this.listRect = new Rect(c.x(), this.listTop + this.quickOpen, c.w(), this.listBottom - this.listTop - this.quickOpen);
      this.panel = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(this.listRect.x(), this.listRect.y(), this.listRect.w(), this.listRect.h())).scrollStep(48));
      int by = c.bottom() - 22;
      this.add((new CFHGuiButton(c.x(), by, 96, Component.translatable("selectServer.add"), this::addServer)).variant(Variant.PRIMARY).icon(Icon.PLUS));
      int bx = c.right();
      if (this.width >= 460) {
         bx -= 20;
         this.savedButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.ARROW_DOWN, Component.translatable("cfhgui.servers.move_down"), () -> this.move(1))));
         bx -= 22;
         this.savedButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.ARROW_UP, Component.translatable("cfhgui.servers.move_up"), () -> this.move(-1))));
         bx -= 2;
      }

      bx -= 20;
      this.savedButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.TRASH, Component.translatable("selectServer.delete"), this::removeSelected)));
      bx -= 22;
      this.savedButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.EDIT, Component.translatable("selectServer.edit"), this::editSelected)));
      bx -= 22;
      this.anyButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.COPY, Component.translatable("cfhgui.servers.copy_address"), () -> {
         if (this.selectedData != null) {
            ServerActions.copyAddress(this.selectedData.ip);
         }

      })));
      bx -= 22;
      this.anyButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.DOTS, Component.translatable("cfhgui.more"), () -> {
         if (this.selectedCard() != null) {
            this.contextMenuAt(this.selectedCard(), this.lastMx, this.lastMy);
         }

      })));
      Component toList = Component.translatable("cfhgui.servers.add_to_list");
      CFHGuiButton joinButton = (new CFHGuiButton(0, by, 58, Component.translatable("cfhgui.servers.join"), this::joinSelected)).variant(Variant.PRIMARY).icon(Icon.PLAY);
      CFHGuiButton listButton = (new CFHGuiButton(0, by, 72, toList, this::addSelectedToList)).icon(Icon.PLUS);
      int joinButtonW = joinButton.preferredWidth() + 8;
      int listButtonW = listButton.preferredWidth() + 4;
      int room = bx - 4 - (c.x() + 96 + 8);
      if (joinButtonW + 4 + listButtonW <= room) {
         bx -= listButtonW + 4;
         listButton.setX(bx);
         listButton.setWidth(listButtonW);
         this.addToListBtn = (CFHGuiButton)this.add(listButton);
      } else {
         bx -= 22;
         this.addToListBtn = (CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.PLUS, toList, this::addSelectedToList));
      }

      bx -= joinButtonW + 4;
      joinButton.setX(bx);
      joinButton.setWidth(joinButtonW);
      this.anyButtons.add((CFHGuiButton)this.add(joinButton));
      this.rebuildList();
      this.updateSelection();
   }

   private boolean matches(ServerData d) {
      if (this.query != null && !this.query.isBlank()) {
         String q = this.query.toLowerCase(Locale.ROOT).trim();
         if (d.name != null && d.name.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
         } else if (d.ip != null && d.ip.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
         } else {
            return d.motd != null && d.motd.getString().toLowerCase(Locale.ROOT).contains(q);
         }
      } else {
         return true;
      }
   }

   private List<ServerData> filterSort(List<ServerData> in) {
      List<ServerData> out = new ArrayList();

      for(ServerData d : in) {
         if (this.matches(d)) {
            out.add(d);
         }
      }

      switch (this.sort.ordinal()) {
         case 1 -> out.sort(Comparator.comparing((ServerData dx) -> (dx.name == null ? dx.ip : dx.name).toLowerCase(Locale.ROOT)));
         case 2 -> out.sort(Comparator.comparing((ServerData dx) -> !ServerPinger.isOnline(dx)).thenComparingLong((ServerData dx) -> dx.ping));
         case 3 -> out.sort(Comparator.comparingInt((ServerData dx) -> dx.players == null ? -1 : dx.players.online()).reversed());
      }

      return out;
   }

   private boolean isSaved(String address) {
      if (this.servers == null) {
         return false;
      } else {
         String key = ServerActions.normalize(address);

         for(int i = 0; i < this.servers.size(); ++i) {
            if (ServerActions.normalize(this.servers.get(i).ip).equals(key)) {
               return true;
            }
         }

         return false;
      }
   }

   private void rebuildList() {
      if (this.panel != null && this.servers != null) {
         MenuConfig cfg = CFHGuiMenu.config();
         this.panel.clear();
         this.cards.clear();
         this.anyRows = false;
         int w = this.listRect.w() - 6;
         int y = 0;
         List<ServerData> saved = new ArrayList();
         if (SamplePlay.on()) {
            saved.addAll(SamplePlay.servers());
         } else {
            for(int i = 0; i < this.servers.size(); ++i) {
               saved.add(this.servers.get(i));
            }
         }

         List<ServerData> favs = new ArrayList();
         List<ServerData> rest = new ArrayList();
         Map<String, List<ServerData>> grouped = new LinkedHashMap();

         for(String g : ServerMeta.groups()) {
            grouped.put(g, new ArrayList());
         }

         for(ServerData d : saved) {
            if (ServerMeta.isFavorite(d.ip)) {
               favs.add(d);
            }

            String g = ServerMeta.group(d.ip);
            if (!g.isEmpty() && grouped.containsKey(g)) {
               ((List)grouped.get(g)).add(d);
            } else if (!ServerMeta.isFavorite(d.ip)) {
               rest.add(d);
            }
         }

         y = this.section("favorites", Component.translatable("cfhgui.servers.favorites"), favs, ServerCard.Kind.SAVED, y, w, false);

         for(Map.Entry<String, List<ServerData>> e : grouped.entrySet()) {
            y = this.section("group:" + (String)e.getKey(), Component.literal((String)e.getKey()), (List)e.getValue(), ServerCard.Kind.SAVED, y, w, false);
         }

         y = this.section("servers", Component.translatable("cfhgui.servers.saved_section"), rest, ServerCard.Kind.SAVED, y, w, saved.isEmpty());
         if (cfg.showRecent) {
            List<ServerData> recents = new ArrayList();

            for(MenuConfig.RecentServer r : cfg.recentServers) {
               if (r != null && r.address != null && !r.address.isBlank()) {
                  recents.add((ServerData)this.recentData.computeIfAbsent(ServerActions.normalize(r.address), (k) -> new ServerData(r.name != null && !r.name.isBlank() ? r.name : r.address, r.address, Type.OTHER)));
               }
            }

            y = this.section("recent", Component.translatable("cfhgui.servers.recent"), recents, ServerCard.Kind.RECENT, y, w, false);
         }

         if (cfg.showLan) {
            List<ServerData> lans = new ArrayList();

            for(LanServer l : this.lanServers) {
               ServerData d = (ServerData)this.lanData.computeIfAbsent(l.getAddress(), (k) -> new ServerData(I18n.get("lanServer.title", new Object[0]), l.getAddress(), Type.LAN));
               d.motd = Component.literal(l.getMotd() == null ? "" : l.getMotd());
               lans.add(d);
            }

            y = this.section("lan", Component.translatable("lanServer.scanning"), lans, ServerCard.Kind.LAN, y, w, false);
         }

         if (cfg.showCommunity) {
            List<ServerData> community = new ArrayList();

            for(CommunityServers.Entry e : CommunityServers.all()) {
               ServerData d = (ServerData)this.communityData.computeIfAbsent(ServerActions.normalize(e.address), (k) -> new ServerData(e.name != null && !e.name.isBlank() ? e.name : e.address, e.address, Type.OTHER));
               this.communityDescriptions.put(ServerActions.normalize(e.address), e.description == null ? "" : e.description);
               community.add(d);
            }

            y = this.section("community", Component.translatable("cfhgui.servers.community"), community, ServerCard.Kind.COMMUNITY, y, w, false);
         }

         this.panel.setContentHeight(y);
      }
   }

   private int section(String key, Component title, List<ServerData> entries, ServerCard.Kind kind, int y, int w, boolean showWhenEmpty) {
      List<ServerData> list = this.filterSort(entries);
      if (list.isEmpty() && !showWhenEmpty) {
         return y;
      } else if (this.panel == null) {
         return y;
      } else {
         boolean collapsed = COLLAPSED.contains(key);
         this.panel.add(new CFHGuiSectionHeader(0, 0, w, title, list.size(), collapsed, () -> {
            if (!COLLAPSED.remove(key)) {
               COLLAPSED.add(key);
            }

            this.minecraft.tell(this::rebuildList);
         }), 0, y);
         y += 22;
         if (!collapsed) {
            for(ServerData d : list) {
               ServerCard card = new ServerCard(0, 0, w, d, kind, this, kind == ServerCard.Kind.COMMUNITY ? (String)this.communityDescriptions.get(ServerActions.normalize(d.ip)) : null);
               if (kind == ServerCard.Kind.RECENT) {
                  card.savedHint(this.isSaved(d.ip));
               }

               card.selected(d == this.selectedData && kind == this.selectedKind);
               this.panel.add(card, 0, y);
               this.cards.add(card);
               this.anyRows = true;
               y += 48;
            }
         }

         return y + 6;
      }
   }

   private @Nullable ServerCard selectedCard() {
      for(ServerCard c : this.cards) {
         if (c.data() == this.selectedData && c.kind() == this.selectedKind) {
            return c;
         }
      }

      return null;
   }

   void clicked(ServerCard card) {
      long now = Clock.nowMs();
      boolean dbl = card.data() == this.lastClickData && now - this.lastClickMs < 400L;
      this.lastClickMs = now;
      this.lastClickData = card.data();
      this.select(card);
      if (dbl) {
         this.join(card.data(), card.kind());
      }

   }

   private void select(@Nullable ServerCard card) {
      this.selectedData = card == null ? null : card.data();
      this.selectedKind = card == null ? null : card.kind();

      for(ServerCard c : this.cards) {
         c.selected(c.data() == this.selectedData && c.kind() == this.selectedKind);
      }

      this.updateSelection();
   }

   private void updateSelection() {
      boolean has = this.selectedData != null;
      boolean saved = has && this.selectedKind == ServerCard.Kind.SAVED;

      for(CFHGuiButton b : this.savedButtons) {
         b.active = saved;
      }

      for(CFHGuiButton b : this.anyButtons) {
         b.active = has;
      }

      if (this.addToListBtn != null) {
         this.addToListBtn.active = has && !saved && !this.isSaved(this.selectedData.ip);
      }

   }

   void onPingChanged() {
      if (this.sort == CFHGuiServersScreen.Sort.PING || this.sort == CFHGuiServersScreen.Sort.PLAYERS) {
         this.rebuildList();
      }

   }

   void toggleFavorite(ServerData d) {
      ServerMeta.setFavorite(d.ip, !ServerMeta.isFavorite(d.ip));
      this.minecraft.tell(this::rebuildList);
   }

   void contextMenu(ServerCard card) {
      this.contextMenuAt(card, this.lastMx, this.lastMy);
   }

   private void contextMenuAt(ServerCard card, int x, int y) {
      this.select(card);
      ServerData d = card.data();
      List<MenuPopup.Item> items = new ArrayList();
      items.add(Item.of(Component.translatable("selectServer.select"), Icon.PLAY, () -> this.join(d, card.kind())));
      items.add(Item.of(Component.translatable("cfhgui.servers.copy_address"), Icon.COPY, () -> ServerActions.copyAddress(d.ip)));
      items.add(Item.of(Component.translatable("cfhgui.servers.refresh_one"), Icon.REFRESH, () -> this.pinger.refresh(d, this::onPingChanged)));
      items.add(Item.sep());
      if (card.kind() == ServerCard.Kind.SAVED) {
         boolean fav = ServerMeta.isFavorite(d.ip);
         items.add(Item.of(Component.translatable(fav ? "cfhgui.unfavorite" : "cfhgui.favorite"), fav ? Icon.STAR_FILLED : Icon.STAR, () -> this.toggleFavorite(d)));
         items.add(Item.of(Component.translatable("selectServer.edit"), Icon.EDIT, this::editSelected));
         if (this.sort == CFHGuiServersScreen.Sort.MANUAL) {
            items.add(Item.of(Component.translatable("cfhgui.servers.move_up"), Icon.ARROW_UP, () -> this.move(-1)));
            items.add(Item.of(Component.translatable("cfhgui.servers.move_down"), Icon.ARROW_DOWN, () -> this.move(1)));
         }

         items.add(Item.sep());
         String current = ServerMeta.group(d.ip);
         items.add(Item.checked(Component.translatable("cfhgui.servers.no_group"), current.isEmpty(), () -> {
            ServerMeta.setGroup(d.ip, "");
            this.minecraft.tell(this::rebuildList);
         }));

         for(String g : ServerMeta.groups()) {
            items.add(Item.checked(Component.literal(g), g.equals(current), () -> {
               ServerMeta.setGroup(d.ip, g);
               this.minecraft.tell(this::rebuildList);
            }));
         }

         items.add(Item.of(Component.translatable("cfhgui.servers.new_group"), Icon.PLUS, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.servers.new_group"), Component.translatable("cfhgui.servers.new_group_body"), "", (name) -> {
               if (!name.isBlank()) {
                  ServerMeta.setGroup(d.ip, name.trim());
                  this.rebuildList();
               }
            })));
         if (!current.isEmpty()) {
            items.add(Item.danger(Component.translatable("cfhgui.servers.delete_group", new Object[]{current}), Icon.TRASH, () -> {
               ServerMeta.removeGroup(current);
               this.minecraft.tell(this::rebuildList);
            }));
         }

         items.add(Item.sep());
         items.add(Item.danger(Component.translatable("selectServer.delete"), Icon.TRASH, this::removeSelected));
      } else {
         if (!this.isSaved(d.ip)) {
            items.add(Item.of(Component.translatable("cfhgui.servers.add_to_list"), Icon.PLUS, this::addSelectedToList));
         }

         if (card.kind() == ServerCard.Kind.RECENT) {
            items.add(Item.danger(Component.translatable("cfhgui.servers.forget_recent"), Icon.CLOSE, () -> {
               CFHGuiMenu.configFile().update((c) -> c.recentServers.removeIf((r) -> r != null && ServerActions.normalize(r.address).equals(ServerActions.normalize(d.ip))));
               this.recentData.remove(ServerActions.normalize(d.ip));
               this.minecraft.tell(this::rebuildList);
            }));
         }
      }

      CFHGuiContextMenu.open((double)x, (double)y, items);
   }

   private void join(ServerData d, ServerCard.Kind kind) {
      if (kind == ServerCard.Kind.LAN) {
         ServerActions.join(new ServerData(I18n.get("selectServer.defaultName", new Object[0]), d.ip, Type.LAN), this);
      } else if (kind == ServerCard.Kind.SAVED) {
         ServerActions.join(d, this);
      } else {
         ServerData target = (ServerData)ServerActions.find(d.ip).orElseGet(() -> {
            ServerList.saveSingleServer(d);
            return d;
         });
         ServerActions.join(target, this);
      }
   }

   private void joinSelected() {
      if (this.selectedData != null && this.selectedKind != null) {
         this.join(this.selectedData, this.selectedKind);
      }

   }

   void join(ServerCard card) {
      this.join(card.data(), card.kind());
   }

   private void addServer() {
      ServerDialog.open(Component.translatable("addServer.title"), (ServerData)null, (d) -> {
         if (this.servers != null) {
            this.servers.add(d, false);
            this.servers.save();
            this.rebuildList();
            CFHGuiToasts.show(Component.translatable("cfhgui.servers.added"), Component.literal(d.name), Icon.SERVER);
         }
      });
   }

   private void editSelected() {
      if (this.servers != null && this.selectedData != null && this.selectedKind == ServerCard.Kind.SAVED) {
         ServerData d = this.selectedData;
         String oldIp = d.ip;
         ServerDialog.open(Component.translatable("editServer.title"), d, (edited) -> {
            this.servers.save();
            if (!ServerActions.normalize(oldIp).equals(ServerActions.normalize(edited.ip))) {
               ServerMeta.forget(oldIp);
               edited.setState(State.INITIAL);
            }

            this.rebuildList();
         });
      }
   }

   private void removeSelected() {
      if (this.servers != null && this.selectedData != null && this.selectedKind == ServerCard.Kind.SAVED) {
         ServerData d = this.selectedData;
         CFHGuiModal.confirmDanger(Component.translatable("selectServer.deleteQuestion"), Component.translatable("selectServer.deleteWarning", new Object[]{d.name}), Component.translatable("selectServer.deleteButton"), () -> {
            this.servers.remove(d);
            this.servers.save();
            ServerMeta.forget(d.ip);
            this.select((ServerCard)null);
            this.rebuildList();
         });
      }
   }

   private void addSelectedToList() {
      if (this.servers != null && this.selectedData != null && this.selectedKind != ServerCard.Kind.SAVED && !this.isSaved(this.selectedData.ip)) {
         ServerData src = this.selectedData;
         ServerData d = new ServerData(src.name != null && !src.name.isBlank() ? src.name : src.ip, src.ip, Type.OTHER);
         this.servers.add(d, false);
         this.servers.save();
         CFHGuiToasts.show(Component.translatable("cfhgui.servers.added"), Component.literal(d.name), Icon.SERVER);
         this.rebuildList();
         this.updateSelection();
      }
   }

   private void move(int dir) {
      if (this.servers != null && this.selectedData != null && this.selectedKind == ServerCard.Kind.SAVED) {
         int idx = -1;

         for(int i = 0; i < this.servers.size(); ++i) {
            if (this.servers.get(i) == this.selectedData) {
               idx = i;
            }
         }

         int to = idx + dir;
         if (idx >= 0 && to >= 0 && to < this.servers.size()) {
            this.servers.swap(idx, to);
            this.servers.save();
            this.rebuildList();
         }
      }
   }

   private void refreshAll(boolean manual) {
      long now = Clock.nowMs();
      this.lastRefreshMs = now;
      boolean retry = manual || now - this.lastRetryMs > 300000L;
      if (retry) {
         this.lastRetryMs = now;
      }

      if (manual) {
         CommunityServers.reload();
      }

      List<ServerData> all = new ArrayList();
      if (this.servers != null) {
         for(int i = 0; i < this.servers.size(); ++i) {
            all.add(this.servers.get(i));
         }
      }

      all.addAll(this.recentData.values());
      all.addAll(this.communityData.values());
      if (this.quickData != null) {
         all.add(this.quickData);
      }

      for(ServerData d : all) {
         if (d.state() != State.PINGING && (d.state() != State.UNREACHABLE || retry)) {
            d.setState(State.INITIAL);
         }
      }

      this.rebuildList();
   }

   private void quickChanged(String text) {
      this.quickAddress = text == null ? "" : text.trim();
      this.quickTypedMs = Clock.nowMs();
      this.quickDirty = true;
      if (this.quickJoin != null) {
         this.quickJoin.active = ServerAddress.isValidAddress(this.quickAddress);
      }

      if (this.quickAddress.isEmpty()) {
         this.quickData = null;
      }

   }

   private void quickJoin() {
      if (!this.quickAddress.isEmpty() && ServerAddress.isValidAddress(this.quickAddress)) {
         ServerData d = (ServerData)ServerActions.find(this.quickAddress).orElseGet(() -> {
            ServerData n = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), this.quickAddress, Type.OTHER);
            ServerList.saveSingleServer(n);
            return n;
         });
         this.minecraft.options.lastMpIp = this.quickAddress;
         this.minecraft.options.save();
         ServerActions.join(d, this);
      } else {
         CFHGuiToasts.show(Component.translatable("cfhgui.servers.invalid_address"), Component.literal(this.quickAddress), Icon.WARNING);
      }
   }

   public void tick() {
      super.tick();
      this.pinger.tick();
      if (this.lan != null) {
         List<LanServer> l = this.lan.poll();
         if (l != null) {
            this.lanServers = l;
            this.rebuildList();
         }
      }

      long now = Clock.nowMs();
      if (this.quickDirty && now - this.quickTypedMs > 600L) {
         this.quickDirty = false;
         if (ServerAddress.isValidAddress(this.quickAddress)) {
            this.quickData = new ServerData(this.quickAddress, this.quickAddress, Type.OTHER);
            this.pinger.ping(this.quickData, () -> {
            });
         } else {
            this.quickData = null;
         }
      }

      MenuConfig cfg = CFHGuiMenu.config();
      if (cfg.serverAutoRefresh && now - this.lastRefreshMs > (long)Math.max(5, cfg.serverRefreshSeconds) * 1000L) {
         this.refreshAll(false);
      }

   }

   public void removed() {
      super.removed();
      this.pinger.close();
      if (this.lan != null) {
         this.lan.close();
         this.lan = null;
      }

   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean typing = this.search != null && this.search.isFocused() || this.quickField != null && this.quickField.isFocused();
      if (!typing && keyCode == 47 && this.search != null) {
         this.setFocused(this.search);
         this.search.setFocused(true);
         return true;
      } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (typing) {
         return false;
      } else if (keyCode == 294) {
         this.refreshAll(true);
         return true;
      } else if (keyCode == 261 && this.selectedKind == ServerCard.Kind.SAVED) {
         this.removeSelected();
         return true;
      } else if ((keyCode == 257 || keyCode == 335) && this.selectedData != null) {
         this.joinSelected();
         return true;
      } else if (hasControlDown() && keyCode == 265) {
         this.move(-1);
         return true;
      } else if (hasControlDown() && keyCode == 264) {
         this.move(1);
         return true;
      } else if ((keyCode == 264 || keyCode == 265) && !this.cards.isEmpty()) {
         int i = -1;

         for(int k = 0; k < this.cards.size(); ++k) {
            if (((ServerCard)this.cards.get(k)).data() == this.selectedData && ((ServerCard)this.cards.get(k)).kind() == this.selectedKind) {
               i = k;
            }
         }

         int next = Math.max(0, Math.min(this.cards.size() - 1, keyCode == 264 ? i + 1 : (i < 0 ? 0 : i - 1)));
         ServerCard card = (ServerCard)this.cards.get(next);
         this.select(card);
         if (this.panel != null) {
            this.panel.ensureVisible(card);
         }

         return true;
      } else {
         return false;
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.lastMx = mouseX;
      this.lastMy = mouseY;
      this.placeList();
      super.render(g, mouseX, mouseY, partialTick);
   }

   private boolean quickLineShown() {
      return this.quickData != null || !this.quickAddress.isEmpty() && !ServerAddress.isValidAddress(this.quickAddress);
   }

   private void placeList() {
      this.quickLine.set(this.quickLineShown());
      int open = Math.round(9.0F * this.quickLine.get());
      if (open != this.quickOpen && this.panel != null) {
         this.quickOpen = open;
         this.listRect = new Rect(this.listRect.x(), this.listTop + open, this.listRect.w(), this.listBottom - this.listTop - open);
         this.panel.setY(this.listRect.y());
         this.panel.setHeight(this.listRect.h());
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (this.quickField != null && !Theme.current().isVanilla()) {
         Rect c = this.contentRect();
         CFHGuiDraw.floatingPanel(g, c.x() - 6, this.quickField.getY() - 6, c.w() + 12, this.quickField.getHeight() + 12 + this.quickOpen, 1.0F);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int muted = van ? -4144960 : p.textMuted();
      Rect c = this.contentRect();
      float lineA = this.quickLine.get();
      int lineMuted = Colors.scaleAlpha(muted, lineA);
      int lineDanger = Colors.scaleAlpha(p.danger(), lineA);
      if (!(lineA <= 0.02F)) {
         if (this.quickData != null) {
            int sx = c.x() + 4;
            switch (this.quickData.state()) {
               case INITIAL:
               case PINGING:
                  CFHGuiSpinner.draw(g, sx, this.quickStatusY, 10, Colors.scaleAlpha(van ? -1 : p.accentInk(), lineA));
                  g.drawString(this.font, Component.translatable("multiplayer.status.pinging"), sx + 14, this.quickStatusY + 1, lineMuted, van);
                  break;
               case UNREACHABLE:
                  g.fill(sx + 2, this.quickStatusY + 3, sx + 6, this.quickStatusY + 7, lineDanger);
                  g.drawString(this.font, (Component)(this.quickData.motd == null ? Component.translatable("cfhgui.servers.offline") : this.quickData.motd), sx + 12, this.quickStatusY + 1, lineDanger, van);
                  break;
               default:
                  g.fill(sx + 2, this.quickStatusY + 3, sx + 6, this.quickStatusY + 7, Colors.scaleAlpha(this.quickData.state() == State.INCOMPATIBLE ? p.warning() : p.success(), lineA));
                  Component line = Component.empty().append(Component.translatable("cfhgui.servers.online_count", new Object[]{ServerPinger.players(this.quickData), this.quickData.ping})).append(this.quickData.state() == State.INCOMPATIBLE && this.quickData.version != null ? Component.empty().append(" · ").append(this.quickData.version) : Component.empty()).append(this.quickData.motd != null && !this.quickData.motd.getString().isBlank() ? Component.empty().append(" · ").append(this.quickData.motd) : Component.empty());
                  g.drawString(this.font, CFHGuiDraw.truncate(line, c.w() - 16), sx + 12, this.quickStatusY + 1, lineMuted, van);
            }
         } else if (!this.quickAddress.isEmpty() && !ServerAddress.isValidAddress(this.quickAddress)) {
            int sx = c.x() + 4;
            g.fill(sx + 2, this.quickStatusY + 3, sx + 6, this.quickStatusY + 7, lineDanger);
            g.drawString(this.font, Component.translatable("cfhgui.servers.invalid_address"), sx + 12, this.quickStatusY + 1, lineDanger, van);
         }
      }

      if (!this.anyRows) {
         Component empty = this.query.isBlank() ? Component.translatable("cfhgui.servers.empty") : Component.translatable("cfhgui.servers.none_match");
         CFHGuiDraw.textCentered(g, empty, this.listRect.centerX(), this.listRect.centerY() - 10, muted);
         if (this.query.isBlank()) {
            CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.servers.empty_hint"), this.listRect.centerX(), this.listRect.centerY() + 2, van ? -6250336 : p.textDim());
         }
      }

   }

   public static enum Sort {
      MANUAL,
      NAME,
      PING,
      PLAYERS;
   }
}
