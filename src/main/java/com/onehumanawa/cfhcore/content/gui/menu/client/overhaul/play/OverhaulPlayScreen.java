package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.CubePlanetNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map.MapTiles;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map.MapView;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map.Waypoints;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerMeta;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldFavorites;
import com.onehumanawa.cfhcore.content.gui.menu.client.servers.ServerDialog;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.PlayPlates;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.SamplePlay;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class OverhaulPlayScreen extends CFHGuiScreen {
   private static final int STRIP_H = 54;
   private static final int DETAILS_GAP = 7;
   private final boolean startOnServers;
   private @Nullable Stage stage;
   private @Nullable Ring worlds;
   private @Nullable Ring servers;
   private boolean onServers;
   private String query = "";
   private Sort sort;
   private @Nullable List<RingEntry.World> allWorlds;
   private boolean loadingWorlds;
   private @Nullable Component worldsError;
   private @Nullable ServerList serverList;
   private final List<RingEntry.Server> allServers = new ArrayList();
   private final ServerPinger pinger = new ServerPinger();
   private Rect stageRect = new Rect(0, 0, 0, 0);
   private Rect stripRect = new Rect(0, 0, 0, 0);
   private Rect paneRect = new Rect(0, 0, 0, 0);
   private @Nullable CFHGuiSearchField search;
   private @Nullable CFHGuiButton playButton;
   private @Nullable CFHGuiButton addButton;
   private @Nullable CFHGuiButton paneEdit;
   private @Nullable CFHGuiButton paneFirst;
   private @Nullable CFHGuiButton paneSecond;
   private @Nullable CFHGuiIconButton starButton;
   private @Nullable CFHGuiIconButton prevButton;
   private @Nullable CFHGuiIconButton nextButton;
   private @Nullable CFHGuiIconButton trashButton;
   private @Nullable CFHGuiIconButton moreButton;
   private @Nullable MapView map;
   private @Nullable DetailsList details;
   private @Nullable CFHGuiIconButton plate;
   private @Nullable CFHGuiIconButton plateGhost;
   private @Nullable RingEntry plateFor;
   private int plateSize = 20;
   private final Vector3f plateAt = new Vector3f();
   private double costSum;
   private int costFrames;
   private @Nullable RingEntry shownEntry;
   private @Nullable LevelInfo levelInfo;
   private @Nullable MapTiles tiles;
   private final Map<String, LevelInfo> infoCache = new HashMap();
   private final Anim nameSwap;
   private Component shownName;
   private int lastMouseX;
   private int lastMouseY;
   private final Vector3f captionAt;

   public OverhaulPlayScreen(@Nullable Screen parent, boolean servers) {
      super(Component.translatable("selectWorld.title"), parent);
      this.nameSwap = new Anim(1.0F, 200, Ease.OUT_CUBIC);
      this.shownName = Component.empty();
      this.captionAt = new Vector3f();
      this.startOnServers = servers;
      this.onServers = servers;
      Sort s = OverhaulPlayScreen.Sort.RECENT;

      try {
         s = OverhaulPlayScreen.Sort.valueOf(CFHGuiMenu.config().playSort.toUpperCase(Locale.ROOT));
      } catch (Exception var5) {
      }

      this.sort = s;
   }

   public Component getTitle() {
      return Component.translatable(this.onServers ? "cfhgui.play.select_server" : "selectWorld.title");
   }

   protected void build() {
      int split = Math.max(150, Math.round((float)this.width * 0.54F));
      this.stageRect = new Rect(0, 32, split, Math.max(40, this.height - 32));
      this.stripRect = new Rect(0, this.height - 54, split, 54);
      this.paneRect = new Rect(split + 1, 32, this.width - split - 1, this.height - 32);
      if (this.stage == null || this.stage.isClosed()) {
         this.createScene();
      }

      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("cfhgui.refresh"), this::reload));
      List<Sort> orders = this.onServers ? List.of(OverhaulPlayScreen.Sort.RECENT, OverhaulPlayScreen.Sort.NAME) : Arrays.asList(OverhaulPlayScreen.Sort.values());
      if (!orders.contains(this.sort)) {
         this.sort = OverhaulPlayScreen.Sort.RECENT;
      }

      if (this.width >= 340) {
         this.addHeaderAction(new CFHGuiDropdown<Sort>(0, 0, this.width >= 520 ? 104 : 84, orders, this.sort, (s) -> {
            String var10000 = s.name();
            return Component.translatable("cfhgui.play.sort." + var10000.toLowerCase(Locale.ROOT));
         }, (s) -> {
            this.sort = s;
            CFHGuiMenu.configFile().update((c) -> c.playSort = s.name());
            this.applyFilter();
         }));
      }

      CFHGuiSearchField sf = new CFHGuiSearchField(0, 0, this.width >= 640 ? 150 : (this.width >= 440 ? 110 : 84), (q) -> {
         this.query = q;
         this.applyFilter();
      });
      sf.onEnter(this::playSelected);
      this.search = (CFHGuiSearchField)this.addHeaderAction(sf);
      sf.setValue(this.query);
      this.slashFocusTarget = sf;
      StageWidget view = new StageWidget(this.stageRect.x(), this.stageRect.y(), this.stageRect.w(), this.stageRect.h(), this.stage, this.getTitle());
      this.addRenderableOnly(view);
      this.addRenderableOnly((g, mx, my, pt) -> {
         Theme t = Theme.current();
         CFHGuiDraw.vgradient(g, 0, this.stripRect.y() - 26, this.stripRect.w(), 80, 0, t.isVanilla() ? -1342177280 : Colors.withAlpha(t.bg(), 230));
      });
      this.plateSize = Mth.clamp(Math.round((float)this.stageRect.h() * 0.037F) * 2, 16, 22);
      this.plate = PlayPlates.create(this.plateSize, this::playSelected);
      this.plateGhost = PlayPlates.create(this.plateSize, () -> {
      });
      this.plateGhost.visible = true;
      this.plateFor = null;
      this.addRenderableOnly(this::drawPlates);
      int row = this.height - 12 + 2 - 20;
      int mid = this.stripRect.centerX();
      this.prevButton = (CFHGuiIconButton)this.add(new CFHGuiIconButton(10, row, 20, Icon.CHEVRON_LEFT, Component.translatable("cfhgui.play.previous"), () -> this.step(-1)));
      this.nextButton = (CFHGuiIconButton)this.add(new CFHGuiIconButton(split - 12 - 18, row, 20, Icon.CHEVRON_RIGHT, Component.translatable("cfhgui.play.next"), () -> this.step(1)));
      int playW = Mth.clamp(split - 150, 56, 96);
      this.playButton = (CFHGuiButton)this.add((new CFHGuiButton(mid - playW / 2, row, playW, Component.translatable(this.onServers ? "cfhgui.play.join" : "cfhgui.play.play"), this::playSelected)).variant(Variant.PRIMARY).icon(Icon.PLAY));
      this.addButton = (CFHGuiButton)this.add(new CFHGuiIconButton(mid + playW / 2 + 4, row, 20, Icon.PLUS, Component.translatable(this.onServers ? "cfhgui.play.add_server" : "selectWorld.create"), this::createNew));
      this.starButton = (CFHGuiIconButton)this.add(new CFHGuiIconButton(mid - playW / 2 - 24, row, 20, Icon.STAR, Component.translatable("cfhgui.favorite"), this::toggleFavorite));
      int px = this.paneRect.x() + 12;
      int pw = this.paneRect.w() - 24;
      int top = this.paneRect.y() + 8;
      int mapH = Mth.clamp(Math.round((float)this.paneRect.h() * 0.4F), 48, Math.max(48, Math.round((float)pw * 0.62F)));
      this.map = (MapView)this.add(new MapView(px, top, pw, mapH));
      int detailsY = top + mapH + 6;
      this.details = (DetailsList)this.add((new DetailsList(px, detailsY, pw, Math.max(20, row - 7 - detailsY))).gapBelow(7));
      int bx = px + pw - 20;
      int moreX = bx;
      this.moreButton = (CFHGuiIconButton)this.add(new CFHGuiIconButton(bx, row, 20, Icon.DOTS, Component.translatable("cfhgui.more"), () -> this.menu(moreX, row)));
      bx -= 24;
      this.trashButton = (CFHGuiIconButton)this.add(new CFHGuiIconButton(bx, row, 20, Icon.TRASH, Component.translatable("selectWorld.delete"), this::deleteSelected));
      this.trashButton.variant(Variant.DANGER);
      CFHGuiButton edit = (new CFHGuiButton(px, row, 40, Component.translatable("selectWorld.edit"), this::editSelected)).icon(Icon.EDIT);
      int editW = edit.preferredWidth() + 4;
      int left;
      if (bx - 4 - px - editW - 4 >= 56) {
         edit.setWidth(editW);
         this.paneEdit = (CFHGuiButton)this.add(edit);
         left = px + editW + 4;
      } else {
         this.paneEdit = (CFHGuiButton)this.add(new CFHGuiIconButton(px, row, 20, Icon.EDIT, Component.translatable("selectWorld.edit"), this::editSelected));
         left = px + 24;
      }

      Component firstName = Component.translatable(this.onServers ? "cfhgui.refresh" : "cfhgui.worlds.open_folder");
      Component secondName = Component.translatable(this.onServers ? "cfhgui.play.copy_address" : "cfhgui.worlds.backup");
      Icon firstIcon = this.onServers ? Icon.REFRESH : Icon.FOLDER;
      Icon secondIcon = this.onServers ? Icon.COPY : Icon.BACKUP;
      CFHGuiButton first = (new CFHGuiButton(left, row, 40, firstName, this::paneFirst)).icon(firstIcon);
      CFHGuiButton second = (new CFHGuiButton(left, row, 40, secondName, this::paneSecond)).icon(secondIcon);
      int room = bx - 4 - left - 4;
      int each = room / 2;
      int firstW = first.preferredWidth();
      int secondW = second.preferredWidth();
      if (firstW + secondW <= room) {
         int w1 = Math.max(firstW, secondW) <= each ? each : firstW + (room - firstW - secondW) / 2;
         first.setWidth(w1);
         second.setX(left + w1 + 4);
         second.setWidth(room - w1);
         this.paneFirst = (CFHGuiButton)this.add(first);
         this.paneSecond = (CFHGuiButton)this.add(second);
      } else {
         this.paneFirst = (CFHGuiButton)this.add(new CFHGuiIconButton(left, row, 20, firstIcon, firstName, this::paneFirst));
         this.paneSecond = (CFHGuiButton)this.add(new CFHGuiIconButton(left + 24, row, 20, secondIcon, secondName, this::paneSecond));
      }

      this.addWidget(this.plate);
      this.addWidget(view);
      if (this.allWorlds == null && !this.loadingWorlds) {
         this.loadWorlds();
      }

      if (this.serverList == null) {
         this.loadServers();
      }

      this.shownEntry = null;
      this.syncSelection();
   }

   private void createScene() {
      Theme theme = Theme.current();
      Stage s = (new Stage()).bind(this);
      this.stage = s;
      s.background(0);
      s.finish(StageFinish.glow());
      s.soft(true);
      s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.camera().at(0.0F, 5.2F, 10.0F).lookAt(0.0F, -0.92F, 0.6F).fov(42.0F).clip(0.05F, 140.0F);
      s.camera().idle(IdleMotion.sway(21000.0F, 1.4F, 0.4F, 0.02F));
      s.lighting().key(3.4F, 7.5F, 8.5F).sun(0.3F, 1.0F, 0.7F);
      int warm = Colors.lerp(-5688, theme.accent(), theme.isVanilla() ? 0.0F : 0.35F);
      ((FxNode)s.add(FxNode.sparkles(150, 48.0F, 18.0F, 6.0F, 0.055F, -1596135681, 5L))).at(0.0F, 5.0F, -17.0F);
      ((FxNode)s.add(FxNode.glow(11.0F, 1882205739).fade(2.2F))).at(0.0F, -1.15F, 0.0F);
      ((FxNode)s.add(FxNode.glow(4.6F, Colors.withAlpha(warm, 38)).fade(1.8F))).at(0.0F, -1.14F, 0.0F);
      ((FxNode)s.add(FxNode.glow(2.1F, Colors.withAlpha(warm, 72)).fade(1.7F))).at(0.0F, -1.13F, 3.3F);
      ((MotesNode)s.add(new MotesNode(44, 9.0F, 3.4F, 9.0F, 0.026F, 1442834370, 9L))).at(0.0F, -0.6F, 0.0F);
      this.worlds = new Ring(s, false, false, () -> this.swapTo(false), Component.translatable("cfhgui.play.to_worlds"), this::createWorld, Component.translatable("selectWorld.create"));
      this.servers = new Ring(s, true, false, () -> this.swapTo(true), Component.translatable("cfhgui.play.to_servers"), this::addServer, Component.translatable("cfhgui.play.add_server"));
      this.worlds.capacity(CFHGuiMenu.config().ringPlanets);
      this.servers.capacity(CFHGuiMenu.config().ringPlanets);
      this.worlds.onPick(this::picked);
      this.servers.onPick(this::picked);
      this.current().active(true);
      if (this.allWorlds != null) {
         this.applyFilter();
      }

   }

   private Ring current() {
      return this.onServers ? this.servers : this.worlds;
   }

   private Ring other() {
      return this.onServers ? this.worlds : this.servers;
   }

   private void loadWorlds() {
      if (!SamplePlay.on()) {
         this.loadingWorlds = true;
         this.worldsError = null;
         WorldActions.loadAll().whenCompleteAsync((summaries, err) -> {
            this.loadingWorlds = false;
            List<RingEntry.World> fresh = new ArrayList();
            if (err != null) {
               CFHGuiMenu.LOGGER.error("[CFHGui Menu] could not load worlds", err);
               this.worldsError = Component.translatable("selectWorld.unable_to_load");
            } else {
               for(LevelSummary s : summaries) {
                  fresh.add(new RingEntry.World(new WorldEntry(s)));
               }
            }

            if (this.worlds != null) {
               this.worlds.clear();
            }

            this.allWorlds = fresh;
            this.applyFilter();
         }, this.minecraft);
      } else {
         List<RingEntry.World> made = new ArrayList();

         for(WorldEntry e : SamplePlay.worlds()) {
            made.add(new RingEntry.World(e));
         }

         if (this.worlds != null) {
            this.worlds.clear();
         }

         this.allWorlds = made;
         this.applyFilter();
      }
   }

   private void loadServers() {
      this.serverList = new ServerList(this.minecraft);
      this.serverList.load();
      if (this.servers != null) {
         this.servers.clear();
      }

      this.allServers.clear();
      if (SamplePlay.on()) {
         long ago = 10800000L;

         for(ServerData d : SamplePlay.servers()) {
            this.allServers.add(new RingEntry.Server(d, System.currentTimeMillis() - ago));
            ago *= 5L;
         }

         this.applyFilter();
      } else {
         Map<String, Long> joined = new HashMap();

         for(MenuConfig.RecentServer r : CFHGuiMenu.config().recentServers) {
            if (r != null && r.address != null) {
               joined.put(ServerActions.normalize(r.address), r.lastJoinedAt);
            }
         }

         for(int i = 0; i < this.serverList.size(); ++i) {
            ServerData d = this.serverList.get(i);
            d.setState(State.INITIAL);
            this.allServers.add(new RingEntry.Server(d, (Long)joined.getOrDefault(ServerActions.normalize(d.ip), 0L)));
            this.pinger.ping(d, () -> {
               RingEntry patt0$temp = this.shownEntry;
               if (patt0$temp instanceof RingEntry.Server s) {
                  if (s.data == d) {
                     this.answered(s);
                  }
               }

            });
         }

         this.applyFilter();
      }
   }

   private void reload() {
      this.infoCache.clear();
      if (!this.loadingWorlds) {
         this.loadWorlds();
      }

      this.loadServers();
   }

   private void applyFilter() {
      if (this.worlds != null && this.servers != null) {
         Comparator<RingEntry> byName = Comparator.comparing((RingEntry ex) -> ex.name().toLowerCase(Locale.ROOT));
         Comparator<RingEntry> byRecent = Comparator.comparingLong(RingEntry::lastPlayed).reversed();
         Comparator<RingEntry> bySize = Comparator.comparingLong((RingEntry ex) -> {
            long var10000;
            if (ex instanceof RingEntry.World w) {
               var10000 = w.world.size;
            } else {
               var10000 = 0L;
            }

            return var10000;
         }).reversed();
         Comparator<RingEntry> order = Comparator.comparing((RingEntry ex) -> !ex.favorite()).thenComparing(this.sort == OverhaulPlayScreen.Sort.NAME ? byName : (this.sort == OverhaulPlayScreen.Sort.SIZE ? bySize.thenComparing(byRecent) : byRecent.thenComparing(byName)));
         List<RingEntry> w = new ArrayList();
         if (this.allWorlds != null) {
            for(RingEntry.World e : this.allWorlds) {
               if (this.sort == OverhaulPlayScreen.Sort.SIZE) {
                  e.world.requestSize();
               }

               if (e.matches(this.query)) {
                  w.add(e);
               }
            }
         }

         w.sort(order);
         RingEntry keptWorld = this.worlds.selected();
         this.worlds.show(w, keptWorld == null ? null : keptWorld.id());
         List<RingEntry> sv = new ArrayList();

         for(RingEntry.Server e : this.allServers) {
            if (e.matches(this.query)) {
               sv.add(e);
            }
         }

         sv.sort(order);
         RingEntry keptServer = this.servers.selected();
         this.servers.show(sv, keptServer == null ? null : keptServer.id());
         this.worlds.offerNew(this.noWorlds());
         this.servers.offerNew(this.allServers.isEmpty());
         this.syncSelection();
      }
   }

   private boolean noWorlds() {
      return this.allWorlds != null && this.allWorlds.isEmpty() && this.worldsError == null;
   }

   private void picked(RingEntry e) {
      Ring ring = this.current();
      if (ring != null) {
         if (ring.selected() == e) {
            this.playSelected();
         } else {
            ring.select(e);
         }

      }
   }

   private void step(int by) {
      Ring ring = this.current();
      if (ring != null) {
         ring.step(by);
      }

   }

   private void swapTo(boolean toServers) {
      if (this.onServers != toServers && this.worlds != null && this.servers != null) {
         this.current().active(false);
         this.onServers = toServers;
         this.current().active(true);
         if (this.sort == OverhaulPlayScreen.Sort.SIZE && this.onServers) {
            this.sort = OverhaulPlayScreen.Sort.RECENT;
         }

         this.rebuildWidgets();
      }
   }

   private void syncSelection() {
      Ring ring = this.current();
      RingEntry now = ring == null ? null : ring.selected();
      boolean has = now != null;

      for(AbstractWidget b : new AbstractWidget[]{this.playButton, this.starButton, this.paneEdit, this.paneFirst, this.paneSecond, this.trashButton, this.moreButton}) {
         if (b != null) {
            b.active = has;
         }
      }

      boolean several = ring != null && ring.shown().size() > 1;
      if (this.prevButton != null) {
         this.prevButton.active = several;
      }

      if (this.nextButton != null) {
         this.nextButton.active = several;
      }

      if (this.starButton != null && has) {
         this.starButton.setIcon(now.favorite() ? Icon.STAR_FILLED : Icon.STAR);
         this.starButton.tip(Component.translatable(now.favorite() ? "cfhgui.unfavorite" : "cfhgui.favorite"));
      }

      if (now != this.shownEntry) {
         this.shownEntry = now;
         this.nameSwap.snap(0.0F);
         this.nameSwap.set(1.0F);
         this.shownName = now == null ? Component.empty() : Component.literal(now.name());
         this.levelInfo = null;
         if (this.tiles != null) {
            this.tiles.close();
            this.tiles = null;
         }

         if (this.map != null) {
            this.map.clearMarks().show((MapTiles)null, (double)0.0F, (double)0.0F).placeholder(Component.empty(), (Textures.Loaded)null);
         }

         if (now == null) {
            if (this.details != null) {
               this.details.show(Component.empty(), Component.empty(), List.of(), List.of());
            }

         } else {
            if (now instanceof RingEntry.World) {
               RingEntry.World w = (RingEntry.World)now;
               this.openWorld(w);
            } else if (now instanceof RingEntry.Server) {
               RingEntry.Server s = (RingEntry.Server)now;
               this.openServer(s);
            }

         }
      }
   }

   private void openWorld(RingEntry.World w) {
      Path folder = SamplePlay.folder(w.id());
      w.world.requestSize();
      this.fillDetails(w);
      LevelInfo cached = (LevelInfo)this.infoCache.get(w.id());
      if (cached != null) {
         this.worldInfo(w, folder, cached);
      } else {
         LevelInfo.read(folder).thenAccept((info) -> {
            this.infoCache.put(w.id(), info);
            if (this.shownEntry == w) {
               this.worldInfo(w, folder, info);
            }

         });
      }

   }

   private void worldInfo(RingEntry.World w, Path folder, LevelInfo info) {
      this.levelInfo = info;
      this.fillDetails(w);
      if (this.map != null) {
         this.tiles = MapTiles.ofWorld(folder);
         this.map.clearMarks().show(this.tiles, (double)info.centreX() + (double)0.5F, (double)info.centreZ() + (double)0.5F);
         if (info.known()) {
            this.map.spawn(info.spawnX(), info.spawnZ());
            if (info.hasPlayer() && info.overworld()) {
               this.map.player(info.playerX(), info.playerZ());
            }
         }

         if (Waypoints.mapModInstalled()) {
            CompletableFuture.supplyAsync(() -> Waypoints.ofWorld(w.id(), w.name()), PlayIo.POOL).thenAcceptAsync((list) -> {
               if (this.shownEntry == w && this.map != null) {
                  this.map.waypoints(list);
               }

            }, this.minecraft);
         }

      }
   }

   private void openServer(RingEntry.Server s) {
      this.fillDetails(s);
      this.pinger.ping(s.data, () -> this.answered(s));
      if (this.map != null) {
         Path day = Waypoints.journeyMapInstalled() ? Waypoints.journeyMapTilesOfServer(s.name()) : null;
         this.tiles = day == null ? null : MapTiles.journeyMap(day);
         if (this.tiles != null) {
            this.map.clearMarks().show(this.tiles, (double)0.5F, (double)0.5F);
         } else {
            this.serverPicture(s);
         }

         if (Waypoints.mapModInstalled()) {
            CompletableFuture.supplyAsync(() -> Waypoints.ofServer(s.data.ip, s.name()), PlayIo.POOL).thenAcceptAsync((list) -> {
               if (this.shownEntry == s && this.map != null) {
                  this.map.waypoints(list);
               }

            }, this.minecraft);
         }

      }
   }

   private void serverPicture(RingEntry.Server s) {
      if (this.map != null) {
         byte[] icon = s.data.getIconBytes();
         Textures.Loaded var10000;
         if (icon == null) {
            var10000 = null;
         } else {
            String var10001 = s.id();
            var10000 = (Textures.Loaded)Textures.fromBytes(icon, "cfhgui:favicon:" + var10001 + ":" + icon.length).orElse(null);
         }

         Textures.Loaded picture = var10000;
         this.map.clearMarks().show((MapTiles)null, (double)0.0F, (double)0.0F).placeholder(Component.translatable(Waypoints.mapModInstalled() ? "cfhgui.play.map.none_yet" : "cfhgui.play.map.needs_mod"), picture);
      }
   }

   private void answered(RingEntry.Server s) {
      if (this.shownEntry == s) {
         this.fillDetails(s);
         if (this.tiles == null) {
            this.serverPicture(s);
         }

      }
   }

   private static Component t(String key, Object... args) {
      return Component.translatable("cfhgui.play." + key, args);
   }

   private void fillDetails(RingEntry e) {
      if (this.details != null) {
         Palette p = Theme.current().palette();
         List<DetailsList.Chip> chips = new ArrayList();
         List<DetailsList.Row> rows = new ArrayList();
         Component subtitle;
         if (e instanceof RingEntry.World) {
            RingEntry.World w = (RingEntry.World)e;
            LevelSummary s = w.world.summary;
            subtitle = Component.literal(w.id());
            chips.add(new DetailsList.Chip(s.getGameMode().getShortDisplayName(), p.accent()));
            if (s.isHardcore()) {
               chips.add(new DetailsList.Chip(Component.translatable("cfhgui.worlds.hardcore"), p.danger()));
            }

            if (s.hasCommands()) {
               chips.add(new DetailsList.Chip(Component.translatable("cfhgui.worlds.cheats"), p.warning()));
            }

            if (s.isExperimental()) {
               chips.add(new DetailsList.Chip(Component.translatable("cfhgui.worlds.experimental"), p.warning()));
            }

            if (w.favorite()) {
               chips.add(new DetailsList.Chip(t("favourite"), -1457046));
            }

            if (s.isLocked() || s.isDisabled() || s.requiresManualConversion()) {
               rows.add(DetailsList.Row.colored(t("state"), (Component)(s.isLocked() ? Component.translatable("selectWorld.locked") : s.getInfo()), p.warning()));
            }

            rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.last_played"), (Component)Fmt.ago(s.getLastPlayed())));
            rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.mode"), (Component)s.getGameMode().getLongDisplayName()));
            LevelInfo info = this.levelInfo;
            if (info != null && info.known() && info.difficulty() >= 0) {
               rows.add(DetailsList.Row.of(t("difficulty"), Difficulty.byId(info.difficulty()).getDisplayName()));
            }

            rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.version"), (Component)s.getWorldVersionName()));
            rows.add(DetailsList.Row.heading(t("more")));
            rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.size"), (String)(w.world.size >= 0L ? Fmt.bytes(w.world.size) : "...")));
            if (info != null && info.known()) {
               rows.add(DetailsList.Row.of(t("days"), Long.toString(info.days())));
               rows.add(DetailsList.Row.of(t("played"), Fmt.duration(info.gameTime() * 50L)));
               if (info.hasSeed()) {
                  rows.add(DetailsList.Row.copy(t("seed"), Long.toString(info.seed())));
               }

               Component var10001 = t("spawn");
               int var10002 = info.spawnX();
               rows.add(DetailsList.Row.of(var10001, var10002 + ", " + info.spawnY() + ", " + info.spawnZ()));
               if (info.hasPlayer()) {
                  var10001 = t("position");
                  var10002 = (int)Math.floor(info.playerX());
                  rows.add(DetailsList.Row.of(var10001, var10002 + ", " + (int)Math.floor(info.playerY()) + ", " + (int)Math.floor(info.playerZ())));
                  rows.add(DetailsList.Row.of(t("dimension"), dimensionName(info.dimension())));
                  rows.add(DetailsList.Row.of(t("health"), Math.round(info.health()) + " / 20"));
                  rows.add(DetailsList.Row.of(t("level"), Integer.toString(info.xpLevel())));
               }

               rows.add(DetailsList.Row.of(t("weather"), t(info.thundering() ? "weather.thunder" : (info.raining() ? "weather.rain" : "weather.clear"))));
               if (!info.dataPacks().isEmpty()) {
                  rows.add(DetailsList.Row.of(t("data_packs"), String.join(", ", info.dataPacks())));
               }
            }

            rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.last_played"), (String)Fmt.date(s.getLastPlayed())));
            rows.add(DetailsList.Row.copy(t("folder"), w.id()));
            if (!w.world.tags.isEmpty()) {
               rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.tags"), (String)String.join(", ", w.world.tags)));
            }
         } else if (e instanceof RingEntry.Server) {
            RingEntry.Server sv = (RingEntry.Server)e;
            ServerData d = sv.data;
            subtitle = Component.literal(d.ip);
            boolean online = sv.online();
            DetailsList.Chip var21;
            String var10003;
            switch (d.state()) {
               case SUCCESSFUL -> var10003 = "status.online";
               case INCOMPATIBLE -> var10003 = "status.outdated";
               case UNREACHABLE -> var10003 = "status.offline";
               default -> var10003 = "status.pinging";
            }

            var21 = new DetailsList.Chip(t(var10003), sv.statusColor());
            chips.add(var21);
            if (sv.favorite()) {
               chips.add(new DetailsList.Chip(t("favourite"), -1457046));
            }

            if (d.isLan()) {
               chips.add(new DetailsList.Chip(t("lan"), p.accent()));
            }

            if (online && d.players != null) {
               Component var22 = t("players");
               int var25 = d.players.online();
               rows.add(DetailsList.Row.of(var22, var25 + " / " + d.players.max()));
            }

            if (online) {
               rows.add(DetailsList.Row.of(t("ping"), d.ping + " ms"));
            }

            if (online && d.version != null && !d.version.getString().isEmpty()) {
               rows.add(DetailsList.Row.of(Component.translatable("cfhgui.worlds.version"), (Component)d.version));
            }

            if (!online && d.state() == State.UNREACHABLE && d.motd != null && !d.motd.getString().isBlank()) {
               rows.add(DetailsList.Row.colored(t("state"), Component.literal(d.motd.getString().trim()), p.danger()));
            }

            if (sv.lastJoined > 0L) {
               rows.add(DetailsList.Row.of(t("last_joined"), Fmt.ago(sv.lastJoined)));
            }

            rows.add(DetailsList.Row.heading(t("more")));
            rows.add(DetailsList.Row.copy(t("address"), d.ip));
            if (online && d.motd != null && !d.motd.getString().isBlank()) {
               for(String line : d.motd.getString().split("\n")) {
                  if (!line.isBlank()) {
                     rows.add(DetailsList.Row.of(t("motd"), line.trim()));
                  }
               }
            }

            if (online && d.players != null && !d.players.sample().isEmpty()) {
               StringBuilder names = new StringBuilder();

               for(GameProfile profile : d.players.sample()) {
                  if (names.length() > 0) {
                     names.append(", ");
                  }

                  names.append(profile.getName());
               }

               rows.add(DetailsList.Row.of(t("playing"), names.toString()));
            }

            String group = ServerMeta.group(d.ip);
            if (!group.isEmpty()) {
               rows.add(DetailsList.Row.of(t("group"), group));
            }

            Component var23 = t("resource_packs");
            String var26;
            switch (d.getResourcePackStatus()) {
               case ENABLED -> var26 = "enabled";
               case DISABLED -> var26 = "disabled";
               default -> var26 = "prompt";
            }

            rows.add(DetailsList.Row.of(var23, (Component)Component.translatable("addServer.resourcePack." + var26)));
         } else {
            subtitle = Component.empty();
         }

         this.details.show(Component.literal(e.name()), subtitle, chips, rows);
      }
   }

   private static Component dimensionName(String id) {
      if (!id.isEmpty() && !id.endsWith("overworld")) {
         if (id.endsWith("the_nether")) {
            return t("dimension.nether");
         } else {
            return (Component)(id.endsWith("the_end") ? t("dimension.end") : Component.literal(id));
         }
      } else {
         return t("dimension.overworld");
      }
   }

   private void playSelected() {
      label39: {
         RingEntry e = this.current() == null ? null : this.current().selected();
         if (e == null) {
            if (this.onServers) {
               if (this.allServers.isEmpty()) {
                  break label39;
               }
            } else if (this.noWorlds()) {
               break label39;
            }
         }

         if (e instanceof RingEntry.World w) {
            if (!w.world.summary.primaryActionActive()) {
               CFHGuiToasts.show(Component.translatable("cfhgui.worlds.cannot_play"), w.world.summary.getInfo(), Icon.WARNING);
               return;
            }

            if (SamplePlay.isSample(w.id())) {
               WorldActions.play((String)w.id(), this);
            } else {
               WorldActions.play((LevelSummary)w.world.summary, this);
            }
         } else if (e instanceof RingEntry.Server s) {
            ServerActions.join(s.data, this);
         }

         return;
      }

      this.createNew();
   }

   private void editSelected() {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e instanceof RingEntry.World w) {
         if (w.world.summary.canEdit()) {
            WorldActions.edit(w.world.summary, this, this::reload);
         }
      } else if (e instanceof RingEntry.Server s) {
         if (this.serverList != null) {
            ServerDialog.open(Component.translatable("addServer.title"), s.data, (edited) -> {
               s.data.name = edited.name;
               s.data.ip = edited.ip;
               s.data.setResourcePackStatus(edited.getResourcePackStatus());
               this.serverList.save();
               this.loadServers();
            });
         }
      }

   }

   private void deleteSelected() {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e instanceof RingEntry.World w) {
         if (!w.world.summary.canDelete()) {
            return;
         }

         CFHGuiModal.confirmDanger(Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", new Object[]{w.name()}), Component.translatable("selectWorld.deleteButton"), () -> {
            if (WorldActions.delete(w.world.summary) && this.allWorlds != null) {
               this.allWorlds.remove(w);
               this.applyFilter();
               CFHGuiToasts.show(Component.translatable("cfhgui.worlds.deleted"), Component.literal(w.name()), Icon.TRASH);
            }

         });
      } else if (e instanceof RingEntry.Server s) {
         if (this.serverList != null) {
            CFHGuiModal.confirmDanger(Component.translatable("selectServer.deleteQuestion"), Component.translatable("selectServer.deleteWarning", new Object[]{s.name()}), Component.translatable("selectServer.deleteButton"), () -> {
               this.serverList.remove(s.data);
               this.serverList.save();
               ServerMeta.forget(s.data.ip);
               this.loadServers();
            });
         }
      }

   }

   private void toggleFavorite() {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e != null) {
         e.favorite(!e.favorite());
         this.applyFilter();
         this.fillDetails(e);
      }
   }

   private void paneFirst() {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e instanceof RingEntry.World w) {
         WorldActions.openFolder(w.world.summary);
      } else if (e instanceof RingEntry.Server s) {
         this.pinger.refresh(s.data, () -> this.answered(s));
         this.fillDetails(s);
      }

   }

   private void paneSecond() {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e instanceof RingEntry.World w) {
         WorldActions.backup(w.world.summary);
      } else if (e instanceof RingEntry.Server s) {
         ServerActions.copyAddress(s.data.ip);
      }

   }

   private void createNew() {
      if (this.onServers) {
         this.addServer();
      } else {
         this.createWorld();
      }

   }

   private void createWorld() {
      WorldActions.createNew(this);
   }

   private void addServer() {
      ServerDialog.open(Component.translatable("addServer.title"), (ServerData)null, (added) -> {
         if (this.serverList != null) {
            this.serverList.add(added, false);
            this.serverList.save();
            this.loadServers();
            if (this.servers != null) {
               for(RingEntry e : this.servers.shown()) {
                  if (e instanceof RingEntry.Server) {
                     RingEntry.Server s = (RingEntry.Server)e;
                     if (s.data.ip.equals(added.ip)) {
                        this.servers.select(e);
                     }
                  }
               }
            }

         }
      });
   }

   private void menu(int x, int y) {
      RingEntry e = this.current() == null ? null : this.current().selected();
      if (e != null) {
         List<MenuPopup.Item> items = new ArrayList();
         items.add(Item.of(Component.translatable(e.favorite() ? "cfhgui.unfavorite" : "cfhgui.favorite"), e.favorite() ? Icon.STAR_FILLED : Icon.STAR, this::toggleFavorite));
         if (e instanceof RingEntry.World) {
            RingEntry.World w = (RingEntry.World)e;
            LevelSummary s = w.world.summary;
            items.add(Item.of(Component.translatable("cfhgui.worlds.duplicate"), Icon.DUPLICATE, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.worlds.duplicate"), Component.translatable("cfhgui.worlds.duplicate_body"), w.name() + " copy", (name) -> WorldActions.duplicate(s, name, this::reload))));
            items.add(Item.of(Component.translatable("cfhgui.worlds.backup"), Icon.BACKUP, () -> WorldActions.backup(s)));
            items.add(s.canRecreate() ? Item.of(Component.translatable("selectWorld.recreate"), Icon.REFRESH, () -> WorldActions.recreate(s, this)) : Item.disabled(Component.translatable("selectWorld.recreate"), Icon.REFRESH));
            items.add(Item.of(Component.translatable("cfhgui.worlds.edit_tags"), Icon.TAG, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.worlds.edit_tags"), Component.translatable("cfhgui.worlds.edit_tags_body"), String.join(", ", w.world.tags), (text) -> {
                  WorldFavorites.setTags(w.id(), Arrays.asList(text.split(",")));
                  w.world.reloadMeta();
                  this.fillDetails(w);
               })));
            items.add(Item.sep());
            items.add(Item.of(Component.translatable("cfhgui.worlds.open_folder"), Icon.FOLDER, () -> WorldActions.openFolder(s)));
            items.add(Item.of(Component.translatable("cfhgui.worlds.open_saves"), Icon.FOLDER, WorldActions::openSavesFolder));
         } else if (e instanceof RingEntry.Server) {
            RingEntry.Server sv = (RingEntry.Server)e;
            items.add(Item.of(Component.translatable("cfhgui.refresh"), Icon.REFRESH, () -> {
               this.pinger.refresh(sv.data, () -> this.answered(sv));
               this.fillDetails(sv);
            }));
            items.add(Item.of(t("copy_address"), Icon.COPY, () -> ServerActions.copyAddress(sv.data.ip)));
         }

         items.add(Item.sep());
         items.add(Item.of(Component.translatable(this.onServers ? "cfhgui.play.add_server" : "selectWorld.create"), Icon.PLUS, this::createNew));
         CFHGuiContextMenu.open((double)x, (double)(y - 4), items);
      }
   }

   public void mouseMoved(double mouseX, double mouseY) {
      if (this.worlds != null) {
         this.worlds.armHover();
      }

      if (this.servers != null) {
         this.servers.armHover();
      }

      super.mouseMoved(mouseX, mouseY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else if (scrollY != (double)0.0F && mouseX < (double)this.paneRect.x() && mouseY >= (double)32.0F) {
         this.step(scrollY > (double)0.0F ? -1 : 1);
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean typing = this.search != null && this.search.isFocused();
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (typing) {
         return false;
      } else {
         switch (keyCode) {
            case 257:
            case 335:
               this.playSelected();
               return true;
            case 258:
               if ((modifiers & 2) == 0 && !hasControlDown()) {
                  return false;
               }

               this.swapTo(!this.onServers);
               return true;
            case 261:
               this.deleteSelected();
               return true;
            case 262:
               this.step(1);
               return true;
            case 263:
               this.step(-1);
               return true;
            case 294:
               this.reload();
               return true;
            default:
               return false;
         }
      }
   }

   public void tick() {
      super.tick();
      this.pinger.tick();
      this.syncSelection();
   }

   public void removed() {
      super.removed();
      this.pinger.close();
      if (this.tiles != null) {
         this.tiles.close();
         this.tiles = null;
      }

      if (this.worlds != null) {
         this.worlds.clear();
      }

      if (this.servers != null) {
         this.servers.clear();
      }

      if (this.stage != null && this.costFrames > 0 && DevHarness.active()) {
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] play screen: {} frames, {} ms a frame ({} at the end), {}x{}", new Object[]{this.stage.frames(), "%.2f".formatted(this.costSum / (double)this.costFrames), "%.2f".formatted(this.stage.frameMs()), this.stage.targetWidth(), this.stage.targetHeight()});
      }

      if (this.stage != null) {
         this.stage.close();
         this.stage = null;
      }

      this.worlds = null;
      this.servers = null;
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      Theme t = Theme.current();
      if (!t.isVanilla() && !MenuBackdrop.active()) {
         CFHGuiDraw.vgradient(g, 0, 32, this.paneRect.x(), this.height - 32, -16316405, -15132907);
         CFHGuiDraw.vignette(g, 0, 32, this.paneRect.x(), this.height - 32, 0.45F);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      this.syncSelection();
      super.render(g, mouseX, mouseY, partialTick);
      if (this.stage != null && this.stage.frames() > 30L && DevHarness.active()) {
         this.costSum += (double)this.stage.lastFrameMs();
         ++this.costFrames;
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      CFHGuiDraw.vline(g, this.paneRect.x() - 1, 32, this.height - 32, van ? -16777216 : p.border());
      float in = this.nameSwap.get();
      Component name = Fonts.heading(this.shownName);
      int room = this.stripRect.w() - 24;
      FormattedCharSequence line = CFHGuiDraw.truncate(name, room);
      g.drawString(this.font, line, this.stripRect.centerX() - this.font.width(line) / 2, this.stripRect.y() + 6 + Math.round((1.0F - in) * 4.0F), Colors.scaleAlpha(van ? -1 : p.text(), in), van);
      Ring ring = this.current();
      int cx = this.stageRect.centerX();
      int cy = this.stageRect.y() + (this.stripRect.y() - this.stageRect.y()) / 2;
      int muted = van ? -4144960 : p.textMuted();
      if (!this.onServers && this.loadingWorlds && this.allWorlds == null) {
         CFHGuiSpinner.draw(g, cx - 8, cy - 20, 16, van ? -1 : p.accentInk());
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.worlds.loading"), cx, cy + 2, muted);
      } else if (!this.onServers && this.worldsError != null) {
         CFHGuiDraw.textCentered(g, this.worldsError, cx, cy - 4, p.danger());
      } else if (ring != null && ring.shown().isEmpty() && ring.activeAmount() > 0.9F) {
         boolean none = this.onServers ? this.allServers.isEmpty() : this.allWorlds != null && this.allWorlds.isEmpty();
         Component head = t(none ? (this.onServers ? "empty.servers" : "empty.worlds") : "empty.search");
         int y = this.stripRect.y() - 30;
         g.drawString(this.font, Fonts.heading(head), cx - this.font.width(Fonts.heading(head)) / 2, y, van ? -1 : p.text(), van);
         if (none) {
            CFHGuiDraw.textCentered(g, t(this.onServers ? "empty.servers_hint" : "empty.worlds_hint"), cx, y + 13, muted);
         }
      }

      this.miniCaption(g, this.other(), this.onServers ? "ring.worlds" : "ring.servers");
   }

   private void drawPlates(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      CFHGuiIconButton real = this.plate;
      CFHGuiIconButton ghost = this.plateGhost;
      if (real != null && ghost != null) {
         Ring now = this.current();
         RingEntry front = now == null ? null : now.selected();
         boolean shown = false;
         if (this.stage != null && !this.stage.isClosed()) {
            shown = this.drawPlates(g, this.worlds, now, front, mouseX, mouseY, partialTick);
            shown |= this.drawPlates(g, this.servers, now, front, mouseX, mouseY, partialTick);
         }

         if (!shown) {
            real.visible = false;
         }

      }
   }

   private boolean drawPlates(GuiGraphics g, @Nullable Ring ring, @Nullable Ring now, @Nullable RingEntry front, int mouseX, int mouseY, float partialTick) {
      CFHGuiIconButton real = this.plate;
      CFHGuiIconButton ghost = this.plateGhost;
      if (ring != null && real != null && ghost != null && this.minecraft != null) {
         double scale = this.minecraft.getWindow().getGuiScale();
         boolean shown = false;

         for(RingEntry e : ring.shown()) {
            PivotNode holder = e.holder;
            boolean chosen = ring == now && e == front;
            boolean here = chosen && holder != null && holder.visible() && ring.activeAmount() > 0.9F;
            CubePlanetNode planet = e.planet;
            e.play.set(here && (planet != null && (planet.hovered() || planet.focused()) || real.isFocused() || real.visible && PlayPlates.over(real, (double)mouseX, (double)mouseY)));
            float k = e.play.get();
            if (holder != null && (here || !(k <= 0.01F)) && this.onScreen(holder, this.plateAt)) {
               CFHGuiIconButton b = chosen ? real : ghost;
               float left = (float)Math.round((double)(this.plateAt.x - (float)this.plateSize / 2.0F) * scale) / (float)scale;
               float top = (float)Math.round((double)(this.plateAt.y - (float)this.plateSize / 2.0F) * scale) / (float)scale;
               int x = Mth.floor(left);
               int y = Mth.floor(top);
               b.setX(x);
               b.setY(y);
               if (chosen) {
                  if (this.plateFor != e) {
                     this.plateFor = e;
                     PlayPlates.label(real, Component.translatable(e instanceof RingEntry.Server ? "cfhgui.worlds.join_this" : "cfhgui.worlds.play_this", new Object[]{e.name()}));
                  }

                  boolean var10001;
                  label77: {
                     if (e instanceof RingEntry.World) {
                        RingEntry.World w = (RingEntry.World)e;
                        if (!w.world.summary.primaryActionActive()) {
                           var10001 = false;
                           break label77;
                        }
                     }

                     var10001 = true;
                  }

                  real.active = var10001;
                  real.visible = true;
                  shown = true;
               }

               if (k > 0.01F) {
                  PlayPlates.draw(g, b, k, 1.0F, left - (float)x, top - (float)y, chosen ? mouseX : -10000, chosen ? mouseY : -10000, partialTick);
               }
            }
         }

         return shown;
      } else {
         return false;
      }
   }

   private boolean onScreen(PivotNode holder, Vector3f out) {
      if (this.stage == null) {
         return false;
      } else {
         Matrix4f m = holder.model;
         if (!this.stage.camera().project(m.m30(), m.m31(), m.m32(), out)) {
            return false;
         } else {
            out.set((float)this.stageRect.x() + (out.x + 1.0F) / 2.0F * (float)this.stageRect.w(), (float)this.stageRect.y() + (1.0F - out.y) / 2.0F * (float)this.stageRect.h(), 0.0F);
            return true;
         }
      }
   }

   private void miniCaption(GuiGraphics g, @Nullable Ring ring, String key) {
      float settled = ring != null && this.stage != null ? Mth.clamp((0.55F - ring.activeAmount()) / 0.55F, 0.0F, 1.0F) : 0.0F;
      if (!(settled <= 0.02F) && this.stage.camera().project(0.0F, 0.19F, 0.0F, this.captionAt)) {
         float a = settled * settled * (3.0F - 2.0F * settled);
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         float hot = ring.miniHover();
         Component text = Fonts.heading(t(key));
         int w = 6 + this.font.width(text) + 4 + 7 + 5;
         int h = 16;
         int x = Math.round((float)this.stageRect.x() + (this.captionAt.x + 1.0F) / 2.0F * (float)this.stageRect.w()) - w / 2;
         int y = Math.round((float)this.stageRect.y() + (1.0F - this.captionAt.y) / 2.0F * (float)this.stageRect.h()) - 8;
         int fill = van ? Colors.withAlpha(0, Math.round(144.0F + 64.0F * hot)) : Colors.lerp(Colors.withAlpha(p.surface(), 184), Colors.withAlpha(p.surfaceHover(), 240), hot);
         int border = van ? Colors.lerp(-8355712, -1, hot) : Colors.lerp(p.border(), p.accent(), hot);
         int ink = van ? -1 : Colors.lerp(p.text(), -1, hot);
         int arrow = van ? Colors.lerp(-4144960, -1, hot) : Colors.lerp(p.textMuted(), p.accent(), hot);
         CFHGuiDraw.pixelRound(g, x, y, w, 16, Colors.scaleAlpha(fill, a), 3);
         CFHGuiDraw.outline(g, x, y, w, 16, Colors.scaleAlpha(border, a), 3);
         g.drawString(this.font, text, x + 6, y + 4, Colors.scaleAlpha(ink, a), van);
         Icons.draw(g, Icon.CHEVRON_RIGHT, x + w - 12 + Math.round(2.0F * hot), y + 4, 8, Colors.scaleAlpha(arrow, a));
      }
   }

   public static enum Sort {
      RECENT,
      NAME,
      SIZE;
   }
}
