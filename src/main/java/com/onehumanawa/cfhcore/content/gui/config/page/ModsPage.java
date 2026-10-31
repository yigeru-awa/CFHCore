package com.onehumanawa.cfhcore.content.gui.config.page;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModConfigTargets;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModLibraries;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchIndex;
import com.onehumanawa.cfhcore.content.gui.config.ui.CategoryHost;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigSearchField;
import com.onehumanawa.cfhcore.content.gui.config.ui.Enterable;
import com.onehumanawa.cfhcore.content.gui.config.ui.Entrance;
import com.onehumanawa.cfhcore.content.gui.config.ui.TabPanel;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class ModsPage extends SidebarPage implements Enterable {
   private static final long CHANGES_TTL_MS = 2000L;
   private static final int COMPACT_BELOW = 300;
   private static final String DOT = "  ·  ";
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
   private String search = "";
   private final Set<String> expanded = new HashSet();
   private final Map<String, ModConfigTargets.Found> configs = new HashMap();
   private @Nullable List<ModInfo> mods;
   private final Map<String, Long> changes = new HashMap();
   private long changesReadMs;
   private boolean ownRebuild;
   private @Nullable String refocus;
   private final List<String> shown = new ArrayList();
   private SidebarScreen screen;
   private double keepScroll = (double)-1.0F;
   private @Nullable CFHGuiScrollPanel panel;
   private int enterDir;
   private @Nullable ConfigSearchField searchField;

   public ModsPage() {
      super("mods", Component.translatable("cfhgui_config.page.mods"), Icon.MODS);
   }

   private List<ModInfo> mods() {
      if (this.mods == null) {
         this.mods = new ArrayList(CFHGuiPlatform.get().allMods());
         this.mods.sort(Comparator.comparing((m) -> m.name().toLowerCase(Locale.ROOT)));
      }

      return this.mods;
   }

   private ModConfigTargets.Found configOf(ModInfo m) {
      return (ModConfigTargets.Found)this.configs.computeIfAbsent(m.id(), ModConfigTargets::find);
   }

   private static Sort sort() {
      try {
         return ModsPage.Sort.valueOf(String.valueOf(ConfigSettings.get().modsSort).toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var1) {
         return ModsPage.Sort.NAME;
      }
   }

   private static Component sortName(Sort sort) {
      String var10000 = sort.name();
      return Component.translatable("cfhgui_config.mods.sort." + var10000.toLowerCase(Locale.ROOT));
   }

   public void filter(String text) {
      String t = text == null ? "" : text;
      if (!t.equals(this.search)) {
         this.search = t;
         this.keepScroll = (double)0.0F;
         if (this.screen != null) {
            if (this.screen.currentPage() != this) {
               SidebarPage var4 = this.screen.currentPage();
               if (!(var4 instanceof CategoryHost)) {
                  return;
               }

               CategoryHost c = (CategoryHost)var4;
               if (c.activeChild() != this) {
                  return;
               }
            }

            this.ownRebuild = true;
            this.screen.refreshPage();
         }

      }
   }

   public void filterSilently(String text) {
      this.search = text == null ? "" : text;
   }

   public String search() {
      return this.search;
   }

   public List<String> shown() {
      return List.copyOf(this.shown);
   }

   public void enterFrom(int dir) {
      this.enterDir = Integer.signum(dir);
      this.keepScroll = (double)0.0F;
   }

   private void rebuild() {
      this.keepScroll = this.panel != null ? this.panel.scrollAmount() : (double)-1.0F;
      if (this.screen != null) {
         this.ownRebuild = true;
         this.screen.refreshPage();
      }
   }

   private void rebuildFromTop() {
      if (this.screen != null) {
         this.keepScroll = (double)0.0F;
         this.ownRebuild = true;
         this.screen.refreshPage();
      }
   }

   private void setSort(Sort sort) {
      ConfigSettings.file().update((c) -> c.modsSort = sort.name());
      this.refocus = "sort";
      this.rebuildFromTop();
   }

   private void setHideLibraries(boolean hide) {
      ConfigSettings.file().update((c) -> c.modsHideLibraries = hide);
      this.refocus = "hide";
      this.rebuildFromTop();
   }

   public void togglePin(String modId) {
      ConfigSettings.togglePinnedMod(modId);
      this.refocus = "pin:" + modId;
      this.rebuild();
   }

   private void toggleExpanded(String modId) {
      if (!this.expanded.remove(modId)) {
         this.expanded.add(modId);
      }

      this.refocus = "fold:" + modId;
      this.rebuild();
   }

   private void readChanges(boolean fromOutside) {
      long now = Util.getMillis();
      if (this.changesReadMs == 0L || fromOutside && now - this.changesReadMs >= 2000L) {
         this.changesReadMs = Math.max(1L, now);
         this.changes.clear();

         for(ModInfo m : this.mods()) {
            this.changes.put(m.id(), ModConfigTargets.lastModified(this.configOf(m).files()));
         }

      }
   }

   private long changed(ModInfo m) {
      return (Long)this.changes.getOrDefault(m.id(), 0L);
   }

   private static Component ago(long epochMs) {
      long seconds = Math.max(0L, System.currentTimeMillis() - epochMs) / 1000L;
      long minutes = seconds / 60L;
      long hours = minutes / 60L;
      long days = hours / 24L;
      if (seconds < 45L) {
         return Component.translatable("cfhgui_config.time.just_now");
      } else if (minutes < 60L) {
         return Component.translatable("cfhgui_config.time.minutes_ago", new Object[]{minutes});
      } else if (hours < 24L) {
         return Component.translatable("cfhgui_config.time.hours_ago", new Object[]{hours});
      } else if (days < 7L) {
         return Component.translatable("cfhgui_config.time.days_ago", new Object[]{days});
      } else {
         return days < 30L ? Component.translatable("cfhgui_config.time.weeks_ago", new Object[]{days / 7L}) : Component.literal(DATE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())));
      }
   }

   private static boolean matches(ModInfo m, String query) {
      String var10000 = m.id();
      String hay = (var10000 + " " + m.name() + " " + String.join(" ", m.authors()) + " " + m.description()).toLowerCase(Locale.ROOT);
      return hay.contains(query);
   }

   public void build(SidebarScreen screen, Rect area) {
      this.screen = screen;
      boolean own = this.ownRebuild;
      this.ownRebuild = false;
      String focusOn = own && Minecraft.getInstance().getLastInputType().isKeyboard() ? this.refocus : null;
      this.refocus = null;
      int w = area.w() - 8;
      Sort sort = sort();
      boolean hideLibraries = ConfigSettings.get().modsHideLibraries;
      if (sort == ModsPage.Sort.LAST_MODIFIED) {
         this.readChanges(!own);
      }

      String q = this.search.toLowerCase(Locale.ROOT).trim();
      Set<String> pins = new LinkedHashSet(ConfigSettings.pinnedMods());
      Map<String, ModInfo> pinnedById = new HashMap();
      List<ModInfo> rest = new ArrayList();
      int hidden = 0;

      for(ModInfo m : this.mods()) {
         if (q.isEmpty() || matches(m, q)) {
            if (pins.contains(m.id())) {
               pinnedById.put(m.id(), m);
            } else if (hideLibraries && q.isEmpty() && ModLibraries.isLibrary(m.id())) {
               ++hidden;
            } else {
               rest.add(m);
            }
         }
      }

      List<ModInfo> pinned = new ArrayList();

      for(String id : pins) {
         ModInfo m = (ModInfo)pinnedById.get(id);
         if (m != null) {
            pinned.add(m);
         }
      }

      if (sort == ModsPage.Sort.LAST_MODIFIED) {
         rest.sort(Comparator.comparingLong((ModInfo mx) -> this.changed(mx)).reversed());
      }

      Toolbar bar = this.toolbar(screen, area, w, sort, hideLibraries, hidden);
      TabPanel p = new TabPanel(area.x(), area.y() + 26, area.w(), area.h() - 26);
      this.panel = p;
      Map<String, CFHGuiCard> cards = new HashMap();
      this.shown.clear();
      int y = 0;
      if (!pinned.isEmpty()) {
         p.add(new CFHGuiSectionHeader(0, y, w, Component.translatable("cfhgui_config.mods.pinned"), pinned.size(), false, (Runnable)null), 0, y);
         y += 22;

         for(ModInfo m : pinned) {
            y = this.card(p, cards, m, w, y, true, sort);
         }

         if (!rest.isEmpty()) {
            p.add(new CFHGuiSectionHeader(0, y, w, Component.translatable("cfhgui_config.mods.others"), rest.size(), false, (Runnable)null), 0, y);
            y += 22;
         }
      }

      for(ModInfo m : rest) {
         y = this.card(p, cards, m, w, y, false, sort);
      }

      if (pinned.isEmpty() && rest.isEmpty() && hidden == 0) {
         p.add((new CFHGuiLabel(4, 8, w - 8, Component.translatable("cfhgui_config.search.no_matches"))).style(Style.MUTED), 4, 8);
      }

      if (hidden > 0) {
         p.add((new CFHGuiLabel(4, y + 2, w - 8, Component.translatable("cfhgui_config.mods.hidden", new Object[]{hidden}))).style(Style.MUTED), 4, y + 2);
         y += 14;
      }

      p.setContentHeight(y + 4);
      screen.addPageWidget(p);
      if (this.keepScroll >= (double)0.0F) {
         p.snapScroll(this.keepScroll);
         this.keepScroll = (double)-1.0F;
      }

      if (this.enterDir != 0) {
         p.slideFrom(this.enterDir);
         int i = Entrance.play(bar.search(), 0);
         i = Entrance.play(bar.order(), i);
         i = Entrance.play(bar.libraries(), i);
         Entrance.play(p, i);
         this.enterDir = 0;
      }

      if (focusOn != null) {
         focus(screen, p, bar, cards, focusOn);
      }

   }

   private Toolbar toolbar(SidebarScreen screen, Rect area, int w, Sort sort, boolean hideLibraries, int hidden) {
      Component sortLabel = Component.translatable("cfhgui_config.mods.sort");
      Component hideLabel = Component.translatable("cfhgui_config.mods.hide_libraries");
      int sortW = 0;

      for(Sort s : ModsPage.Sort.values()) {
         sortW = Math.max(sortW, CFHGuiDraw.width(Component.empty().append(sortLabel).append(": ").append(sortName(s))));
      }

      sortW += 30;
      int hideW = CFHGuiDraw.width(hideLabel) + 22 + 10;
      boolean tight = w < 120 + sortW + hideW + 16;
      int right = area.x() + w;
      List<Component> hideTip = new ArrayList();
      hideTip.add(Component.translatable("cfhgui_config.mods.hide_libraries.tip"));
      if (hidden > 0) {
         hideTip.add(Component.translatable("cfhgui_config.mods.hidden", new Object[]{hidden}).withStyle(ChatFormatting.GRAY));
      }

      CFHGuiWidget order;
      CFHGuiWidget libraries;
      if (tight) {
         CFHGuiIconButton hide = (new CFHGuiIconButton(right - 20, area.y(), 20, Icon.FILTER, hideLabel, () -> this.setHideLibraries(!hideLibraries))).toggled(hideLibraries);
         hideTip.add(0, hideLabel);
         hide.tip(hideTip);
         CFHGuiIconButton pick = new CFHGuiIconButton(right - 44, area.y(), 20, Icon.SORT, Component.empty().append(sortLabel).append(": ").append(sortName(sort)), (Runnable)null);
         pick.onPress(() -> this.sortMenu(pick, sort));
         order = pick;
         libraries = hide;
      } else {
         CFHGuiToggle hide = new CFHGuiToggle(right - hideW, area.y(), hideW, hideLabel, hideLibraries, this::setHideLibraries);
         hide.tip(hideTip);
         CFHGuiDropdown<Sort> pick = (new CFHGuiDropdown<Sort>(right - hideW - 8 - sortW, area.y(), sortW, List.of(ModsPage.Sort.values()), sort, ModsPage::sortName, this::setSort)).label(sortLabel);
         order = pick;
         libraries = hide;
      }

      ConfigSearchField field = new ConfigSearchField(area.x(), area.y(), Math.max(60, Math.min(260, order.getX() - 8 - area.x())), this::filter);
      field.setValue(this.search);
      field.placeholder(Component.translatable("cfhgui_config.mods.search"));
      boolean hadFocus = this.searchField != null && this.searchField.isFocused();
      this.searchField = field;
      screen.addPageWidget(field);
      if (hadFocus) {
         screen.setFocused(field);
      }

      screen.addPageWidget(order);
      screen.addPageWidget(libraries);
      return new Toolbar(field, order, libraries);
   }

   private void sortMenu(AbstractWidget under, Sort current) {
      List<MenuPopup.Item> items = new ArrayList();

      for(Sort s : ModsPage.Sort.values()) {
         items.add(Item.checked(sortName(s), s == current, () -> {
            if (s != current) {
               this.setSort(s);
            }

         }));
      }

      MenuPopup menu = new MenuPopup(under.getX(), under.getY() + under.getHeight() + 1, items, 110);
      menu.highlight(current.ordinal());
      Popups.open(menu);
   }

   private int card(TabPanel p, Map<String, CFHGuiCard> cards, ModInfo m, int w, int y, boolean pinned, Sort sort) {
      boolean open = this.expanded.contains(m.id());
      boolean compact = w < 300;
      int textW = Math.max(60, w - 40 - (compact ? 88 : 162));
      String description = m.description() == null ? "" : m.description().trim();
      int lines = open ? CFHGuiDraw.font().split(Component.literal(description), textW).size() : 0;
      int h = open ? 44 + Math.max(1, lines) * 10 + 4 : 44;
      CFHGuiCard card = (new CFHGuiCard(0, y, w, h)).flat();
      card.add(new ModIcon(m), 8, 10);
      card.add((new CFHGuiLabel(40, 7, textW, Component.literal(m.name()))).style(Style.TITLE), 40, 7);
      card.add((new CFHGuiLabel(40, 19, textW, this.meta(m, sort))).style(Style.CAPTION), 40, 19);
      if (!open) {
         Component first = description.isEmpty() ? Component.empty() : Component.literal(description.split("\\r?\\n")[0]);
         card.add((new CFHGuiLabel(40, 30, textW, first)).style(Style.MUTED), 40, 30);
      } else {
         card.add((new CFHGuiLabel(40, 32, textW, Component.literal(description))).style(Style.MUTED).wrap(true), 40, 32);
      }

      List<ModConfigTargets.Target> ts = this.configOf(m).targets();
      Component configure = Component.translatable("cfhgui_config.mods.configure");
      CFHGuiButton cfg = (CFHGuiButton)(compact ? (new CFHGuiIconButton(w - 76, 8, 20, Icon.SETTINGS, configure, () -> this.openTargets(m, ts))).variant(Variant.SECONDARY) : (new CFHGuiButton(w - 150, 8, 96, configure, () -> this.openTargets(m, ts))).icon(Icon.SETTINGS));
      cfg.enabled(!ts.isEmpty());
      if (ts.isEmpty()) {
         cfg.tip(Component.translatable("cfhgui_config.mods.no_config"));
      } else if (ts.size() > 1) {
         Component count = Component.translatable("cfhgui_config.mods.count", new Object[]{ts.size()});
         cfg.tip(compact ? List.of(configure, count) : List.of(count));
      }

      card.add(cfg, compact ? w - 76 : w - 150, 8);
      card.add((new CFHGuiIconButton(w - 48, 10, 16, Icon.PIN, Component.translatable(pinned ? "cfhgui_config.mods.unpin" : "cfhgui_config.mods.pin"), () -> this.togglePin(m.id()))).toggled(pinned), w - 48, 10);
      card.add(new CFHGuiIconButton(w - 26, 10, 16, open ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN, Component.translatable(open ? "cfhgui_config.mods.collapse" : "cfhgui_config.mods.expand"), () -> this.toggleExpanded(m.id())), w - 26, 10);
      card.onRightClick(() -> this.cardMenu(m, ts, pinned, open));
      p.add(card, 0, y);
      cards.put(m.id(), card);
      this.shown.add(m.id());
      return y + h + 6;
   }

   private Component meta(ModInfo m, Sort sort) {
      MutableComponent meta = Component.literal("v" + m.version());
      if (ModLibraries.isLibrary(m.id())) {
         meta.append("  ·  ").append(Component.translatable("cfhgui_config.mods.library"));
      }

      if (sort == ModsPage.Sort.LAST_MODIFIED && this.changed(m) > 0L) {
         meta.append("  ·  ").append(Component.translatable("cfhgui_config.mods.changed", new Object[]{ago(this.changed(m))}));
      }

      if (!m.authors().isEmpty()) {
         meta.append("  ·  " + String.join(", ", m.authors()));
      }

      return meta;
   }

   private void cardMenu(ModInfo m, List<ModConfigTargets.Target> ts, boolean pinned, boolean open) {
      List<MenuPopup.Item> items = new ArrayList();
      items.add(Item.of(Component.translatable(pinned ? "cfhgui_config.mods.unpin" : "cfhgui_config.mods.pin"), Icon.PIN, () -> this.togglePin(m.id())));
      if (!ts.isEmpty()) {
         items.add(Item.sep());
         if (ts.size() == 1) {
            items.add(Item.of(Component.translatable("cfhgui_config.mods.configure"), Icon.SETTINGS, ((ModConfigTargets.Target)ts.get(0)).open()));
         } else {
            for(ModConfigTargets.Target t : ts) {
               items.add(Item.of(t.label(), t.icon(), t.open()));
            }
         }
      }

      items.add(Item.sep());
      items.add(Item.of(Component.translatable(open ? "cfhgui_config.mods.collapse" : "cfhgui_config.mods.expand"), open ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN, () -> this.toggleExpanded(m.id())));
      menuAtPointer(items);
   }

   private void openTargets(ModInfo m, List<ModConfigTargets.Target> ts) {
      if (!ts.isEmpty()) {
         if (ts.size() == 1) {
            ((ModConfigTargets.Target)ts.get(0)).open().run();
         } else {
            List<MenuPopup.Item> items = new ArrayList();

            for(ModConfigTargets.Target t : ts) {
               items.add(Item.of(t.label(), t.icon(), t.open()));
            }

            menuAtPointer(items);
         }
      }
   }

   private static void menuAtPointer(List<MenuPopup.Item> items) {
      Minecraft mc = Minecraft.getInstance();
      double mx = mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth();
      double my = mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight();
      Popups.open(new MenuPopup((int)mx, (int)my, items, 160));
   }

   private static void focus(SidebarScreen screen, CFHGuiScrollPanel panel, Toolbar bar, Map<String, CFHGuiCard> cards, String key) {
      if (key.equals("sort")) {
         screen.setFocused(bar.order());
      } else if (key.equals("hide")) {
         screen.setFocused(bar.libraries());
      } else {
         int colon = key.indexOf(58);
         CFHGuiCard card = colon < 0 ? null : (CFHGuiCard)cards.get(key.substring(colon + 1));
         if (card != null) {
            List<AbstractWidget> widgets = card.widgets();
            AbstractWidget button = (AbstractWidget)widgets.get(widgets.size() - (key.startsWith("pin:") ? 2 : 1));
            screen.setFocused(panel);
            panel.setFocused(card);
            card.setFocused(button);
         }
      }
   }

   public List<SearchIndex.Entry> searchEntries() {
      return this.searchEntries(this.id(), this.title(), (Component)null);
   }

   public List<SearchIndex.Entry> searchEntries(String path, Component pageTitle, @Nullable Component tabTitle) {
      List<SearchIndex.Entry> out = new ArrayList();

      for(ModInfo m : this.mods()) {
         out.add(new SearchIndex.Entry(path, pageTitle, tabTitle, Component.translatable("cfhgui_config.page.mods"), Binding.of("mod:" + m.id(), OptionType.ACTION, (Component)Component.literal(m.name())).tooltip((Component)(m.description() != null && !m.description().isBlank() ? Component.literal(m.description().trim()) : null)).action(Component.translatable("cfhgui_config.mods.configure"), () -> this.filter(m.name())).searchWords(m.id() + " mod")));
      }

      return out;
   }

   private static enum Sort {
      NAME,
      LAST_MODIFIED;
   }

   private static record Toolbar(ConfigSearchField search, CFHGuiWidget order, CFHGuiWidget libraries) {
   }

   static final class ModIcon extends CFHGuiWidget {
      private final ModInfo mod;
      private Textures.@Nullable Loaded tex;
      private boolean requested;

      ModIcon(ModInfo mod) {
         super(0, 0, 24, 24, Component.literal(mod.name()));
         this.mod = mod;
         this.active = false;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         return false;
      }

      public ComponentPath nextFocusPath(FocusNavigationEvent event) {
         return null;
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g);
      }

      private void draw(GuiGraphics g) {
         if (!this.requested) {
            this.requested = true;
            this.mod.iconPath().ifPresent((p) -> Textures.load(p, (l) -> this.tex = l));
         }

         int x = this.getX();
         int y = this.getY();
         int s = this.getWidth();
         if (this.tex != null) {
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, this.effectiveAlpha());
            g.blit(this.tex.id(), x, y, s, s, 0.0F, 0.0F, this.tex.width(), this.tex.height(), this.tex.width(), this.tex.height());
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
         } else {
            int c = Theme.current().isVanilla() ? -12961222 : Theme.current().palette().surfaceActive();
            CFHGuiDraw.pixelRound(g, x, y, s, s, Colors.scaleAlpha(c, this.effectiveAlpha()), Theme.current().radius());
            Icons.draw(g, Icon.MODS, x + 4, y + 4, 16, Colors.scaleAlpha(Theme.current().palette().textDim(), this.effectiveAlpha()));
         }

      }
   }
}
