package com.onehumanawa.cfhcore.content.gui.config.hub;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.curated.CuratedPages;
import com.onehumanawa.cfhcore.content.gui.config.page.AudioPage;
import com.onehumanawa.cfhcore.content.gui.config.page.CategoryPage;
import com.onehumanawa.cfhcore.content.gui.config.page.CompositePage;
import com.onehumanawa.cfhcore.content.gui.config.page.ConfigFilesPage;
import com.onehumanawa.cfhcore.content.gui.config.page.ControlsPage;
import com.onehumanawa.cfhcore.content.gui.config.page.CuratedPage;
import com.onehumanawa.cfhcore.content.gui.config.page.DeveloperPage;
import com.onehumanawa.cfhcore.content.gui.config.page.EssentialsPage;
import com.onehumanawa.cfhcore.content.gui.config.page.FavouritesPage;
import com.onehumanawa.cfhcore.content.gui.config.page.GameplayGeneralPage;
import com.onehumanawa.cfhcore.content.gui.config.page.InterfacePage;
import com.onehumanawa.cfhcore.content.gui.config.page.LanguagePage;
import com.onehumanawa.cfhcore.content.gui.config.page.ModsPage;
import com.onehumanawa.cfhcore.content.gui.config.page.PresetsPage;
import com.onehumanawa.cfhcore.content.gui.config.page.ResourcePacksPage;
import com.onehumanawa.cfhcore.content.gui.config.page.SimplePages;
import com.onehumanawa.cfhcore.content.gui.config.page.SkinPage;
import com.onehumanawa.cfhcore.content.gui.config.page.VideoPage;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchIndex;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchPopup;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigSearchField;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigTextField;
import com.onehumanawa.cfhcore.content.gui.config.ui.Entrance;
import com.onehumanawa.cfhcore.content.gui.config.ui.LiveGameView;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.PageColumns;
import com.onehumanawa.cfhcore.content.gui.config.ui.TabHost;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenSwaps;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ConfigHubScreen extends SidebarScreen implements PageColumns {
   public static final String GENERAL = "general";
   public static final String GAMEPLAY = "gameplay";
   public static final String MULTIPLAYER = "multiplayer";
   public static final String CUSTOMIZATION = "customization";
   public static final String LANGUAGE_ACCESSIBILITY = "language_accessibility";
   public static final String ADVANCED = "advanced";
   private static final Map<String, String> ALIASES = Map.ofEntries(Map.entry("chat", "multiplayer/chat"), Map.entry("online", "multiplayer/online"), Map.entry("mods", "customization/mods"), Map.entry("packs", "customization/packs"), Map.entry("resourcepacks", "customization/packs"), Map.entry("shaders", "customization/shaders"), Map.entry("shaderpacks", "customization/shaders"), Map.entry("skin", "multiplayer/skin"), Map.entry("language", "language_accessibility/language"), Map.entry("accessibility", "language_accessibility/accessibility"), Map.entry("essentials", "general"), Map.entry("gameplay/essentials", "general"), Map.entry("difficulty", "gameplay/general"), Map.entry("files", "advanced/files"), Map.entry("configs", "advanced/files"), Map.entry("keybinds", "controls/keys"), Map.entry("keys", "controls/keys"), Map.entry("sound", "audio"), Map.entry("sounds", "audio"));
   private @Nullable String pendingPage;
   private @Nullable ConfigSearchField searchField;
   private @Nullable CFHGuiIconButton resetButton;
   private @Nullable SearchIndex index;
   private @Nullable SearchPopup popup;
   private @Nullable OptionPageBase lastOptionPage;
   private String searchText;
   private @Nullable List<AbstractWidget> capture;
   private boolean firstBuild;
   private final Presentation presentation;
   private @Nullable LiveGameView liveView;
   private boolean devShown;

   public ConfigHubScreen(@Nullable Screen parent, @Nullable String page) {
      this(parent, page, ConfigHubScreen.Presentation.SIDEBAR);
   }

   public ConfigHubScreen(@Nullable Screen parent, @Nullable String page, Presentation presentation) {
      super(Component.translatable("cfhgui_config.hub.title"), parent, "cfhgui_config:hub");
      this.searchText = "";
      this.firstBuild = true;
      this.pendingPage = page;
      this.presentation = presentation;
   }

   public static ConfigHubScreen forLayout(@Nullable Screen parent, @Nullable String page) {
      boolean overhaul = MenuSlots.effective("minecraft:options") == Layout.OVERHAUL;
      return new ConfigHubScreen(parent, page, overhaul ? ConfigHubScreen.Presentation.OVERHAUL : ConfigHubScreen.Presentation.SIDEBAR);
   }

   public Presentation presentation() {
      return this.presentation;
   }

   private boolean overhaul() {
      return this.presentation == ConfigHubScreen.Presentation.OVERHAUL;
   }

   public int columns(int pageWidth) {
      return this.overhaul() && pageWidth >= 272 ? 2 : 1;
   }

   protected boolean overhaulNav() {
      return this.overhaul();
   }

   protected Component navTitle(SidebarPage p) {
      return (Component)(this.overhaul() && "language_accessibility".equals(p.id()) ? Component.translatable("cfhgui_config.page.accessibility_short") : p.title());
   }

   protected void definePages(List<SidebarPage> pages) {
      pages.add(new EssentialsPage("general", Component.translatable("cfhgui_config.category.general"), Icon.HOME));
      pages.add(new CategoryPage("gameplay", Component.translatable("cfhgui_config.category.gameplay"), Icon.GAMEPLAY, List.of(new GameplayGeneralPage())));
      pages.add(new VideoPage());
      pages.add(new ControlsPage());
      pages.add(new AudioPage());
      pages.add(new CategoryPage("multiplayer", Component.translatable("cfhgui_config.page.multiplayer"), Icon.MULTIPLAYER, List.of(new SimplePages.OnlinePage(), new SimplePages.ChatPage(), new SkinPage())));
      pages.add(new InterfacePage());
      pages.add(new CategoryPage("language_accessibility", Component.translatable("cfhgui_config.page.language_accessibility"), Icon.ACCESSIBILITY, List.of(new LanguagePage(), new SimplePages.AccessibilityPage())));
      List<SidebarPage> custom = new ArrayList(List.of(new ModsPage(), new ResourcePacksPage()));
      pages.add(new CategoryPage("customization", Component.translatable("cfhgui_config.page.customization"), Icon.CUSTOMIZE, custom));
      List<SidebarPage> advanced = new ArrayList();
      advanced.add((new VideoPage()).level(OptionPageBase.Level.ADVANCED));
      advanced.add((new ControlsPage()).level(OptionPageBase.Level.ADVANCED));
      advanced.add((new CompositePage("multiplayer", Component.translatable("cfhgui_config.page.multiplayer"), Icon.MULTIPLAYER, List.of(new SimplePages.OnlinePage(), new SimplePages.ChatPage()))).level(OptionPageBase.Level.ADVANCED));
      advanced.add((new InterfacePage()).level(OptionPageBase.Level.ADVANCED));
      advanced.add(new FavouritesPage());
      advanced.add(new PresetsPage());
      advanced.add(new ConfigFilesPage());

      for(CuratedPages.PageDef def : CuratedPages.load()) {
         advanced.add(new CuratedPage(def));
      }

      pages.add(new CategoryPage("advanced", Component.translatable("cfhgui_config.page.advanced"), Icon.WRENCH, advanced));
      this.devShown = CFHGui.config().devMode;
      if (this.devShown) {
         pages.add(new DeveloperPage());
      }

      this.index();
   }

   private void followDevMode() {
      this.devShown = CFHGui.config().devMode;
      String id = "developer";
      SidebarPage there = this.find("developer");
      if (this.devShown != (there != null)) {
         if (this.devShown) {
            this.pages().add(new DeveloperPage());
         } else {
            if (this.currentPage() == there) {
               this.open("advanced/interface");
            }

            this.pages().remove(there);
         }
      }
   }

   private boolean liveViewWanted() {
      if (this.presentation == ConfigHubScreen.Presentation.OVERHAUL && this.minecraft != null && this.minecraft.level != null) {
         SidebarPage p = this.currentPage();
         return p != null && ("general".equals(p.id()) || "video".equals(p.id()));
      } else {
         return false;
      }
   }

   public Rect pageRect() {
      Rect r = super.pageRect();
      if (!this.liveViewWanted()) {
         return r;
      } else {
         int h = LiveGameView.heightFor(r, ConfigSettings.get().liveViewMinimised);
         return new Rect(r.x(), r.y() + h + 6, r.w(), Math.max(20, r.h() - h - 6));
      }
   }

   private void mountLiveView() {
      this.liveView = null;
      if (this.liveViewWanted()) {
         Rect r = super.pageRect();
         boolean minimised = ConfigSettings.get().liveViewMinimised;
         this.liveView = (LiveGameView)this.addPageWidget(new LiveGameView(r.x(), r.y(), r.w(), LiveGameView.heightFor(r, minimised), minimised, () -> {
            ConfigSettings.file().update((c) -> c.liveViewMinimised = !c.liveViewMinimised);
            this.refreshPage();
         }));
      }
   }

   protected void build() {
      List<AbstractWidget> built = new ArrayList();
      this.capture = built;
      super.build();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.EXTERNAL, Component.translatable("cfhgui_config.hub.vanilla_options"), () -> {
         Minecraft mc = Minecraft.getInstance();
         ScreenSwaps.runUnswapped(() -> mc.setScreen(new OptionsScreen(this, mc.options)));
      }));
      this.resetButton = (CFHGuiIconButton)this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.UNDO, Component.translatable("cfhgui_config.hub.reset_tab"), this::resetTab));
      ConfigSearchField f = new ConfigSearchField(0, 0, Math.min(150, Math.max(90, this.width / 4)), this::onSearch);
      f.setValue(this.searchText);
      this.addHeaderAction(f);
      this.searchField = f;
      if (this.pendingPage != null) {
         String p = this.pendingPage;
         this.pendingPage = null;
         this.open(p);
      }

      this.capture = null;
      this.mountLiveView();
      if (this.firstBuild) {
         this.firstBuild = false;
         int i = 0;

         for(AbstractWidget w : built) {
            i = Entrance.play(w, i);
         }
      }

      this.trackOptionPage();
      this.updateResetButton();
   }

   public <T extends GuiEventListener & Renderable & NarratableEntry> T addPageWidget(T widget) {
      if (this.capture != null && widget instanceof AbstractWidget w) {
         this.capture.add(w);
      }

      return (T)super.addPageWidget(widget);
   }

   protected int navWidth() {
      if (this.narrow()) {
         return 30;
      } else {
         int widest = 0;

         for(SidebarPage p : this.pages()) {
            widest = Math.max(widest, this.font.width(p.title()));
         }

         return Math.max(118, Math.min(widest + 34, Math.max(118, this.width / 4)));
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (this.overhaul() && !Theme.current().isVanilla() && !MenuBackdrop.worldBehind()) {
         CFHGuiDraw.vgradient(g, 0, 32, this.width, this.height - 32, -16053233, -15067114);
         CFHGuiDraw.vignette(g, 0, 32, this.width, this.height - 32, 0.4F);
      }
   }

   protected boolean showPageTitle() {
      return false;
   }

   public Component getTitle() {
      SidebarPage page = this.pages().isEmpty() ? null : this.currentPage();
      if (page == null) {
         return super.getTitle();
      } else {
         String var10000 = super.getTitle().getString();
         return Component.literal(var10000 + (this.overhaul() ? " – " : " › ") + page.title().getString());
      }
   }

   public void open(String raw) {
      String path = resolve(raw);
      String[] parts = path.split("/", 3);
      SidebarPage page = this.find(parts[0]);
      if (page == null) {
         page = this.find("curated:" + parts[0]);
      }

      if (page != null) {
         if (parts.length > 1) {
            if (page instanceof CategoryPage) {
               CategoryPage cp = (CategoryPage)page;
               cp.select(parts[1]);
               if (parts.length > 2) {
                  SidebarPage var12 = cp.child(parts[1]);
                  if (var12 instanceof OptionPageBase) {
                     OptionPageBase o = (OptionPageBase)var12;
                     o.selectTab(parts[2]);
                  }
               }
            } else if (page instanceof OptionPageBase) {
               OptionPageBase o = (OptionPageBase)page;
               o.selectTab(parts[1]);
            }
         }

         if (page != this.currentPage()) {
            this.showPage(page.id());
         }

      } else {
         for(SidebarPage p : this.pages()) {
            if (p instanceof CategoryPage) {
               CategoryPage cp = (CategoryPage)p;
               if (cp.child(parts[0]) != null || cp.child("curated:" + parts[0]) != null) {
                  String child = cp.child(parts[0]) != null ? parts[0] : "curated:" + parts[0];
                  String var10001 = cp.id();
                  this.open(var10001 + "/" + child + (parts.length > 1 ? "/" + parts[1] : ""));
                  return;
               }
            }
         }

         CFHGuiConfig.LOGGER.warn("[CFHGui Config] no settings page '{}'", raw);
      }
   }

   public static String resolve(String raw) {
      String p = raw.trim();
      if (p.startsWith("curated:")) {
         return p;
      } else {
         int slash = p.indexOf(47);
         String head = (slash < 0 ? p : p.substring(0, slash)).toLowerCase(Locale.ROOT);
         String alias = (String)ALIASES.get(head);
         if (alias == null) {
            return p;
         } else {
            return slash < 0 ? alias : alias + p.substring(slash);
         }
      }
   }

   private @Nullable SidebarPage find(String id) {
      for(SidebarPage p : this.pages()) {
         if (p.id().equals(id)) {
            return p;
         }
      }

      return null;
   }

   public @Nullable SidebarPage leaf() {
      SidebarPage p = this.currentPage();
      SidebarPage var10000;
      if (p instanceof CategoryPage cp) {
         var10000 = cp.activeChild();
      } else {
         var10000 = p;
      }

      return var10000;
   }

   public String leafPath() {
      SidebarPage p = this.currentPage();
      if (p instanceof CategoryPage cp) {
         return cp.activePath();
      } else {
         return p == null ? "" : p.id();
      }
   }

   private void trackOptionPage() {
      SidebarPage var2 = this.leaf();
      if (var2 instanceof OptionPageBase o) {
         if (!(o instanceof FavouritesPage)) {
            this.lastOptionPage = o;
         }
      }

   }

   public @Nullable OptionPageBase lastOptionPage() {
      return this.lastOptionPage;
   }

   public void showPage(int index) {
      boolean changing = index != this.currentIndex() && index >= 0 && index < this.pages().size();
      if (changing) {
         this.applyFilterTo((SidebarPage)this.pages().get(index));
      }

      List<AbstractWidget> outer = this.capture;
      List<AbstractWidget> built = changing ? new ArrayList() : null;
      this.capture = built;
      super.showPage(index);
      this.capture = outer;
      if (changing) {
         this.mountLiveView();
      }

      if (built != null) {
         int i = 0;

         for(AbstractWidget w : built) {
            i = Entrance.play(w, i);
         }
      }

      this.trackOptionPage();
      this.updateResetButton();
   }

   public void refreshPage() {
      super.refreshPage();
      this.mountLiveView();
      this.trackOptionPage();
      this.updateResetButton();
   }

   private void applyFilterTo(@Nullable SidebarPage p) {
      if (p instanceof OptionPageBase o) {
         o.filter(this.searchText);
      } else if (p instanceof CategoryPage c) {
         c.filter(this.searchText);
      } else if (p instanceof ModsPage m) {
         m.filter(this.searchText);
      }

   }

   public void refreshOptionPages() {
      SidebarPage var2 = this.leaf();
      if (var2 instanceof OptionPageBase o) {
         o.refreshRows();
      }

   }

   private void updateResetButton() {
      if (this.resetButton != null) {
         this.resetButton.active = this.leaf() instanceof OptionPageBase;
      }

   }

   private SearchIndex index() {
      if (this.index == null) {
         SearchIndex ix = new SearchIndex();

         for(SidebarPage p : this.pages()) {
            try {
               if (p instanceof OptionPageBase) {
                  OptionPageBase o = (OptionPageBase)p;
                  ix.addAll(o.searchEntries());
               } else if (p instanceof CategoryPage) {
                  CategoryPage c = (CategoryPage)p;
                  ix.addAll(c.searchEntries());
               } else if (p instanceof ModsPage) {
                  ModsPage m = (ModsPage)p;
                  ix.addAll(m.searchEntries());
               } else if (p instanceof PresetsPage) {
                  PresetsPage pr = (PresetsPage)p;
                  ix.addAll(pr.searchEntries());
               }
            } catch (Exception e) {
               CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot index page {}: {}", p.id(), e.toString());
            }
         }

         this.index = ix;
      }

      return this.index;
   }

   private void onSearch(String text) {
      this.searchText = text;
      this.applyFilterTo(this.currentPage());
      String q = text.trim();
      if (q.length() >= 2 && this.searchField != null) {
         List<SearchIndex.Hit> hits = this.index().query(q, this.leafPath(), 24);
         if (hits.isEmpty()) {
            this.closePopup();
         } else {
            if (this.popup == null || Popups.top() != this.popup) {
               this.popup = new SearchPopup(this.searchField.getX(), this.searchField.getY() + this.searchField.getHeight() + 2, Math.max(230, this.searchField.getWidth() + 90), this::jumpTo);
               Popups.open(this.popup);
            }

            this.popup.setHits(hits);
         }
      } else {
         this.closePopup();
      }
   }

   private void closePopup() {
      if (this.popup != null) {
         Popups.close(this.popup);
         this.popup = null;
      }

   }

   private void jumpTo(SearchIndex.Hit hit) {
      this.searchText = "";
      if (this.searchField != null) {
         this.searchField.setValue("");
      }

      this.open(hit.entry().path());
      SidebarPage p = this.leaf();
      if (p instanceof OptionPageBase o) {
         o.focusRow(hit.entry().binding().id());
      } else if (p instanceof ModsPage m) {
         m.filter(hit.entry().binding().label().getString());
      } else if (p instanceof PresetsPage) {
         Runnable r = hit.entry().binding().action();
         if (r != null) {
            r.run();
         }
      }

   }

   private void resetTab() {
      Component scope;
      Runnable action;
      label18: {
         SidebarPage cur = this.currentPage();
         if (cur instanceof CategoryPage cp) {
            SidebarPage var7 = cp.activeChild();
            if (var7 instanceof OptionPageBase o) {
               scope = o.fullTitle();
               Objects.requireNonNull(o);
               action = o::resetAll;
               break label18;
            }
         }

         if (!(cur instanceof OptionPageBase)) {
            return;
         }

         OptionPageBase o = (OptionPageBase)cur;
         scope = o.currentTabTitle();
         Objects.requireNonNull(o);
         action = o::resetTab;
      }

      CFHGuiModal.confirmDanger(Component.translatable("cfhgui_config.hub.reset_tab"), Component.translatable("cfhgui_config.hub.reset_tab.body", new Object[]{scope}), Component.translatable("cfhgui_config.hub.reset_tab.confirm"), action);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      SidebarPage var5 = this.currentPage();
      if (var5 instanceof ControlsPage cp) {
         if (cp.captureKey(keyCode, scanCode, modifiers)) {
            return true;
         }
      }

      if (hasControlDown() && (keyCode == 266 || keyCode == 267)) {
         var5 = this.currentPage();
         if (var5 instanceof TabHost) {
            TabHost th = (TabHost)var5;
            th.cycleTab(keyCode == 267 ? 1 : -1, hasShiftDown());
            return true;
         }
      }

      if (this.typing() || this.searchField == null || keyCode != 47 && (keyCode != 70 || !hasControlDown())) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.setFocused(this.searchField);
         return true;
      }
   }

   private boolean typing() {
      GuiEventListener f = this.getFocused();

      ContainerEventHandler c;
      for(int guard = 0; f instanceof ContainerEventHandler; f = c.getFocused()) {
         c = (ContainerEventHandler)f;
         if (c.getFocused() == null || guard++ >= 8) {
            break;
         }
      }

      return f instanceof EditBox || f instanceof ConfigTextField;
   }

   public void debugSearch(String text) {
      if (this.searchField != null) {
         this.setFocused(this.searchField);
         this.searchField.setValue(text);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      SidebarPage var7 = this.currentPage();
      if (var7 instanceof ControlsPage cp) {
         if (cp.captureMouse(button)) {
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void tick() {
      super.tick();
      if (this.popup != null && this.searchField != null && !this.searchField.isFocused() && Popups.top() == this.popup) {
         this.closePopup();
      }

      if (CFHGui.config().devMode != this.devShown) {
         this.followDevMode();
      }

   }

   public void removed() {
      ApplyQueue.flush();
      super.removed();
   }

   public static enum Presentation {
      SIDEBAR,
      OVERHAUL;
   }
}
