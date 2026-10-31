package com.onehumanawa.cfhcore.content.gui.menu.client.worlds;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldFavorites;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiWorldsScreen extends CFHGuiScreen {
   private static final int CARD_MIN_W = 236;
   private static final int GRID_RISE = 2;
   private static final int PANEL_PAD = 8;
   private @Nullable List<WorldEntry> all;
   private boolean loading;
   private boolean loadedOnce;
   private @Nullable Component loadError;
   private String query = "";
   private Sort sort;
   private View view;
   private @Nullable WorldEntry selected;
   private @Nullable WorldEntry lastClickEntry;
   private long lastClickMs;
   private int lastMx;
   private int lastMy;
   private List<WorldEntry> shown = List.of();
   private @Nullable CFHGuiScrollPanel grid;
   private @Nullable CFHGuiList<WorldEntry> list;
   private @Nullable CFHGuiSearchField search;
   private final Map<WorldEntry, WorldCard> cards = new IdentityHashMap();
   private Rect listRect = new Rect(0, 0, 0, 0);
   private Rect detailsRect = new Rect(0, 0, 0, 0);
   private boolean showDetails;
   private int detailsTextBottom;
   private static final int DETAILS_MIN_H = 190;
   private int gridCols = 1;
   private final List<CFHGuiButton> selectionButtons = new ArrayList();
   private @Nullable CFHGuiButton emptyCta;
   private @Nullable CFHGuiIconButton listPlate;
   private @Nullable WorldEntry listPlateFor;
   private boolean listPlateNew;
   private final Anim listPlay;
   private int hoverRow;
   private final Anim hoverRowEdge;

   public CFHGuiWorldsScreen(@Nullable Screen parent) {
      super(Component.translatable("selectWorld.title"), parent);
      this.listPlay = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.hoverRow = -1;
      this.hoverRowEdge = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      MenuConfig cfg = CFHGuiMenu.config();
      this.sort = (Sort)parse(Sort.class, cfg.worldsSort, CFHGuiWorldsScreen.Sort.LAST_PLAYED);
      this.view = (View)parse(View.class, cfg.worldsView, CFHGuiWorldsScreen.View.GRID);
   }

   private static <E extends Enum<E>> E parse(Class<E> cls, String s, E def) {
      try {
         return (E)Enum.valueOf(cls, s == null ? "" : s.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var4) {
         return def;
      }
   }

   protected void build() {
      this.cards.clear();
      this.selectionButtons.clear();
      this.grid = null;
      this.list = null;
      Rect c = this.contentRect();
      MenuConfig cfg = CFHGuiMenu.config();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("cfhgui.refresh"), this::reload));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, this.view == CFHGuiWorldsScreen.View.GRID ? Icon.LIST : Icon.GRID, Component.translatable("cfhgui.view_toggle"), this::toggleView));
      if (this.width >= 500) {
         this.addHeaderAction(new CFHGuiDropdown<Sort>(0, 0, 112, Arrays.asList(CFHGuiWorldsScreen.Sort.values()), this.sort, (Sort s) -> {
            String var10000 = s.name();
            return Component.translatable("cfhgui.sort." + var10000.toLowerCase(Locale.ROOT));
         }, (Sort s) -> {
            this.sort = s;
            CFHGuiMenu.configFile().update((x) -> x.worldsSort = s.name());
            this.rebuildList();
         }));
      }

      CFHGuiSearchField sf = new CFHGuiSearchField(0, 0, this.width >= 640 ? 160 : 100, (q) -> {
         this.query = q;
         this.rebuildList();
      });
      sf.onEnter(() -> {
         if (this.selected != null) {
            this.play(this.selected);
         }

      });
      this.search = (CFHGuiSearchField)this.addHeaderAction(sf);
      sf.setValue(this.query);
      int bottomH = 30;
      int listH = c.h() - 4 - 30;
      this.showDetails = cfg.worldsShowDetails && this.width >= 600 && listH >= 190;
      int detailsW = this.showDetails ? 204 : 0;
      this.listRect = new Rect(c.x(), c.y() + 4, c.w() - detailsW - (this.showDetails ? 10 : 0), listH);
      this.detailsRect = new Rect(this.listRect.right() + 10, this.listRect.y(), detailsW, this.listRect.h());
      this.listPlate = PlayPlates.create(18, () -> {
         if (this.selected != null) {
            this.play(this.selected);
         }

      });
      this.listPlateNew = true;
      if (this.view == CFHGuiWorldsScreen.View.GRID) {
         this.grid = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(this.listRect.x(), this.listRect.y() - 2, this.listRect.w(), this.listRect.h() + 2)).scrollStep(40));
      } else {
         this.list = (CFHGuiList<WorldEntry>)this.add((new CFHGuiList<WorldEntry>(this.listRect.x(), this.listRect.y(), this.listRect.w(), this.listRect.h(), 40, new RowRenderer())).gap(2).onSelect((WorldEntry e) -> this.select(e, false)).onActivate(this::play).onRightClick(this::contextMenu).emptyText(Component.translatable("cfhgui.worlds.none_match")));
      }

      int by = c.bottom() - 22;
      this.add((new CFHGuiButton(c.x(), by, 130, Component.translatable("selectWorld.create"), () -> WorldActions.createNew(this))).variant(Variant.PRIMARY).icon(Icon.NEW_WORLD));
      this.add(new CFHGuiIconButton(c.x() + 136, by, 20, Icon.FOLDER, Component.translatable("cfhgui.worlds.open_saves"), WorldActions::openSavesFolder));
      if (!this.showDetails) {
         int bx = c.right();
         bx -= 20;
          int finalBx = bx;
          this.selectionButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.DOTS, Component.translatable("cfhgui.more"), () -> {
            if (this.selected != null) {
               this.contextMenuAt(this.selected, finalBx, by);
            }

         })));
         bx -= 66;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(bx, by, 62, Component.translatable("selectWorld.edit"), () -> {
            if (this.selected != null) {
               this.edit(this.selected);
            }

         })).icon(Icon.EDIT)));
         bx -= 70;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(bx, by, 66, Component.translatable("cfhgui.worlds.play"), () -> {
            if (this.selected != null) {
               this.play(this.selected);
            }

         })).variant(Variant.PRIMARY).icon(Icon.PLAY)));
      } else {
         int px = this.detailsRect.x() + 8;
         int pw = this.detailsRect.w() - 16;
         int half = (pw - 4) / 2;
         int py = this.detailsRect.bottom() - 8 - 20;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px, py, pw, Component.translatable("selectWorld.delete"), () -> {
            if (this.selected != null) {
               this.confirmDelete(this.selected);
            }

         })).variant(Variant.DANGER).icon(Icon.TRASH)));
         py -= 24;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px, py, half, Component.translatable("cfhgui.worlds.open_folder"), () -> {
            if (this.selected != null) {
               WorldActions.openFolder(this.selected.summary);
            }

         })).icon(Icon.FOLDER)));
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px + half + 4, py, half, Component.translatable("cfhgui.worlds.tags"), () -> {
            if (this.selected != null) {
               this.editTags(this.selected);
            }

         })).icon(Icon.TAG)));
         py -= 24;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px, py, half, Component.translatable("cfhgui.worlds.duplicate_short"), () -> {
            if (this.selected != null) {
               this.duplicate(this.selected);
            }

         })).icon(Icon.DUPLICATE).tip(Component.translatable("cfhgui.worlds.duplicate"))));
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px + half + 4, py, half, Component.translatable("selectWorld.recreate"), () -> {
            if (this.selected != null) {
               WorldActions.recreate(this.selected.summary, this);
            }

         })).icon(Icon.REFRESH)));
         py -= 24;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px, py, half, Component.translatable("selectWorld.edit"), () -> {
            if (this.selected != null) {
               this.edit(this.selected);
            }

         })).icon(Icon.EDIT)));
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px + half + 4, py, half, Component.translatable("cfhgui.worlds.backup"), () -> {
            if (this.selected != null) {
               this.backup(this.selected);
            }

         })).icon(Icon.BACKUP)));
         py -= 26;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(px, py, pw, Component.translatable("selectWorld.select"), () -> {
            if (this.selected != null) {
               this.play(this.selected);
            }

         })).variant(Variant.PRIMARY).icon(Icon.PLAY)));
         this.detailsTextBottom = py - 6;
      }

      this.emptyCta = (CFHGuiButton)this.add((new CFHGuiButton(this.listRect.centerX() - 84, this.listRect.centerY() + 12, 168, Component.translatable("cfhgui.worlds.create_first"), () -> WorldActions.createNew(this))).variant(Variant.PRIMARY).icon(Icon.NEW_WORLD));
      this.emptyCta.visible = false;
      if (this.all == null && !this.loading) {
         this.load();
      } else {
         this.rebuildList();
      }

      this.updateSelection();
   }

   public void added() {
      super.added();
      if (this.loadedOnce && !this.loading) {
         this.reload();
      }

   }

   private void load() {
      if (SamplePlay.on()) {
         this.loadedOnce = true;
         this.all = new ArrayList(SamplePlay.worlds());
         if (this.selected != null && !this.all.contains(this.selected)) {
            this.selected = null;
         }

         this.rebuildList();
         this.updateSelection();
      } else {
         this.loading = true;
         this.loadError = null;
         WorldActions.loadAll().whenCompleteAsync((summaries, err) -> {
            this.loading = false;
            this.loadedOnce = true;
            List<WorldEntry> fresh = new ArrayList();
            if (err != null) {
               CFHGuiMenu.LOGGER.error("[CFHGui Menu] could not load worlds", err);
               this.loadError = Component.translatable("selectWorld.unable_to_load");
            } else {
               for(LevelSummary s : summaries) {
                  fresh.add(new WorldEntry(s));
               }
            }

            String selId = this.selected == null ? null : this.selected.id();
            this.all = fresh;
            this.selected = null;
            if (selId != null) {
               for(WorldEntry e : fresh) {
                  if (e.id().equals(selId)) {
                     this.selected = e;
                  }
               }
            }

            this.rebuildList();
            this.updateSelection();
         }, this.minecraft);
      }
   }

   public void reload() {
      if (!this.loading) {
         this.load();
      }
   }

   private List<WorldEntry> filtered() {
      if (this.all == null) {
         return List.of();
      } else {
         List<WorldEntry> out = new ArrayList();

         for(WorldEntry e : this.all) {
            if (e.matches(this.query)) {
               out.add(e);
            }
         }

         Comparator<WorldEntry> byPlayed = Comparator.comparingLong((WorldEntry ex) -> ex.summary.getLastPlayed()).reversed();
         switch (this.sort.ordinal()) {
            case 1 -> out.sort(Comparator.comparing((WorldEntry ex) -> ex.name().toLowerCase(Locale.ROOT)));
            case 2 -> out.sort(Comparator.comparingLong((WorldEntry ex) -> ex.size < 0L ? Long.MIN_VALUE : ex.size).reversed().thenComparing(byPlayed));
            case 3 -> out.sort(Comparator.comparing((WorldEntry ex) -> !ex.favorite).thenComparing(byPlayed));
            default -> out.sort(byPlayed);
         }

         return out;
      }
   }

   private void rebuildList() {
      if (this.grid != null || this.list != null) {
         this.shown = this.filtered();
         this.cards.clear();
         if (this.grid != null) {
            this.grid.clear();
            this.gridCols = Math.max(1, (this.listRect.w() - 6) / 236);
            int cardW = (this.listRect.w() - 8 - (this.gridCols - 1) * 8) / this.gridCols;
            int i = 0;

            for(WorldEntry e : this.shown) {
               int col = i % this.gridCols;
               int row = i / this.gridCols;
               WorldCard card = new WorldCard(0, 0, cardW, e, this);
               card.selected(e == this.selected);
               this.grid.add(card, col * (cardW + 8), 2 + row * 82);
               this.cards.put(e, card);
               ++i;
            }

            int rows = (this.shown.size() + this.gridCols - 1) / this.gridCols;
            this.grid.setContentHeight(2 + rows * 82);
         } else if (this.list != null) {
            this.list.items(this.shown);
            if (this.selected != null && this.shown.contains(this.selected)) {
               if (this.list.selectedItem() != this.selected) {
                  this.list.select(this.selected);
               }
            } else {
               this.list.clearSelection();
            }
         }

         if (this.emptyCta != null) {
            this.emptyCta.visible = !this.loading && this.all != null && this.all.isEmpty() && this.loadError == null;
         }

      }
   }

   void clicked(WorldEntry e) {
      long now = Clock.nowMs();
      boolean dbl = e == this.lastClickEntry && now - this.lastClickMs < 400L;
      this.lastClickMs = now;
      this.lastClickEntry = e;
      this.select(e, true);
      if (dbl) {
         this.play(e);
      }

   }

   private void select(@Nullable WorldEntry e, boolean syncList) {
      this.selected = e;

      for(Map.Entry<WorldEntry, WorldCard> c : this.cards.entrySet()) {
         ((WorldCard)c.getValue()).selected(c.getKey() == e);
      }

      if (syncList && this.list != null && e != null && this.list.selectedItem() != e) {
         this.list.select(e);
      }

      this.updateSelection();
   }

   private void updateSelection() {
      boolean has = this.selected != null;

      for(CFHGuiButton b : this.selectionButtons) {
         b.visible = this.showDetails ? has : true;
         b.active = has;
      }

   }

   void toggleFavorite(WorldEntry e) {
      e.favorite = WorldFavorites.toggleFavorite(e.id());
      WorldCard card = (WorldCard)this.cards.get(e);
      if (card != null) {
         card.refresh();
      }

      if (this.sort == CFHGuiWorldsScreen.Sort.FAVORITES) {
         this.rebuildList();
      }

   }

   void contextMenu(WorldEntry e) {
      this.contextMenuAt(e, this.lastMx, this.lastMy);
   }

   private void contextMenuAt(WorldEntry e, int x, int y) {
      this.select(e, true);
      LevelSummary s = e.summary;
      List<MenuPopup.Item> items = new ArrayList();
      items.add(s.primaryActionActive() ? Item.of(Component.translatable("cfhgui.worlds.play"), Icon.PLAY, () -> this.play(e)) : Item.disabled(Component.translatable("cfhgui.worlds.play"), Icon.PLAY));
      items.add(s.canEdit() ? Item.of(Component.translatable("selectWorld.edit"), Icon.EDIT, () -> this.edit(e)) : Item.disabled(Component.translatable("selectWorld.edit"), Icon.EDIT));
      items.add(Item.of(Component.translatable("cfhgui.worlds.backup"), Icon.BACKUP, () -> this.backup(e)));
      items.add(Item.of(Component.translatable("cfhgui.worlds.duplicate"), Icon.DUPLICATE, () -> this.duplicate(e)));
      items.add(s.canRecreate() ? Item.of(Component.translatable("selectWorld.recreate"), Icon.REFRESH, () -> WorldActions.recreate(s, this)) : Item.disabled(Component.translatable("selectWorld.recreate"), Icon.REFRESH));
      items.add(Item.sep());
      items.add(Item.of(Component.translatable(e.favorite ? "cfhgui.unfavorite" : "cfhgui.favorite"), e.favorite ? Icon.STAR_FILLED : Icon.STAR, () -> this.toggleFavorite(e)));
      items.add(Item.of(Component.translatable("cfhgui.worlds.edit_tags"), Icon.TAG, () -> this.editTags(e)));
      items.add(Item.of(Component.translatable("cfhgui.worlds.open_folder"), Icon.FOLDER, () -> WorldActions.openFolder(s)));
      items.add(Item.of(Component.translatable("cfhgui.worlds.copy_folder"), Icon.COPY, () -> {
         this.minecraft.keyboardHandler.setClipboard(e.id());
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), Component.literal(e.id()), Icon.COPY);
      }));
      items.add(Item.sep());
      items.add(s.canDelete() ? Item.danger(Component.translatable("selectWorld.delete"), Icon.TRASH, () -> this.confirmDelete(e)) : Item.disabled(Component.translatable("selectWorld.delete"), Icon.TRASH));
      CFHGuiContextMenu.open((double)x, (double)y, items);
   }

   void play(WorldEntry e) {
      if (!e.summary.primaryActionActive()) {
         CFHGuiToasts.show(Component.translatable("cfhgui.worlds.cannot_play"), e.summary.getInfo(), Icon.WARNING);
      } else {
         if (SamplePlay.isSample(e.id())) {
            WorldActions.play((String)e.id(), this);
         } else {
            WorldActions.play((LevelSummary)e.summary, this);
         }

      }
   }

   private void edit(WorldEntry e) {
      if (e.summary.canEdit()) {
         WorldActions.edit(e.summary, this, this::reload);
      }
   }

   private void backup(WorldEntry e) {
      WorldActions.backup(e.summary);
   }

   private void duplicate(WorldEntry e) {
      CFHGuiModal.prompt(Component.translatable("cfhgui.worlds.duplicate"), Component.translatable("cfhgui.worlds.duplicate_body"), e.name() + " copy", (name) -> WorldActions.duplicate(e.summary, name, this::reload));
   }

   private void editTags(WorldEntry e) {
      CFHGuiModal.prompt(Component.translatable("cfhgui.worlds.edit_tags"), Component.translatable("cfhgui.worlds.edit_tags_body"), String.join(", ", e.tags), (text) -> {
         WorldFavorites.setTags(e.id(), Arrays.asList(text.split(",")));
         e.reloadMeta();
         this.rebuildList();
      });
   }

   private void confirmDelete(WorldEntry e) {
      CFHGuiModal.confirmDanger(Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", new Object[]{e.name()}), Component.translatable("selectWorld.deleteButton"), () -> {
         if (WorldActions.delete(e.summary) && this.all != null) {
            this.all.remove(e);
            if (this.selected == e) {
               this.selected = null;
            }

            this.rebuildList();
            this.updateSelection();
            CFHGuiToasts.show(Component.translatable("cfhgui.worlds.deleted"), Component.literal(e.name()), Icon.TRASH);
         }

      });
   }

   private void toggleView() {
      this.view = this.view == CFHGuiWorldsScreen.View.GRID ? CFHGuiWorldsScreen.View.LIST : CFHGuiWorldsScreen.View.GRID;
      CFHGuiMenu.configFile().update((c) -> c.worldsView = this.view.name());
      this.rebuildWidgets();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean typing = this.search != null && this.search.isFocused();
      if (!typing && keyCode == 47) {
         if (this.search != null) {
            this.setFocused(this.search);
            this.search.setFocused(true);
         }

         return true;
      } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (typing) {
         return false;
      } else if (keyCode == 294) {
         this.reload();
         return true;
      } else if (keyCode == 261 && this.selected != null) {
         this.confirmDelete(this.selected);
         return true;
      } else if ((keyCode == 257 || keyCode == 335) && this.selected != null) {
         this.play(this.selected);
         return true;
      } else {
         if (this.grid != null && !this.shown.isEmpty()) {
            int i = this.selected == null ? -1 : this.shown.indexOf(this.selected);
            int var10000;
            switch (keyCode) {
               case 262 -> var10000 = i + 1;
               case 263 -> var10000 = i - 1;
               case 264 -> var10000 = i < 0 ? 0 : i + this.gridCols;
               case 265 -> var10000 = i - this.gridCols;
               default -> var10000 = Integer.MIN_VALUE;
            }

            int next = var10000;
            if (next != Integer.MIN_VALUE) {
               next = Math.max(0, Math.min(this.shown.size() - 1, next));
               WorldEntry e = (WorldEntry)this.shown.get(next);
               this.select(e, false);
               WorldCard card = (WorldCard)this.cards.get(e);
               if (card != null) {
                  this.grid.ensureVisible(card);
               }

               return true;
            }
         }

         return false;
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.lastMx = mouseX;
      this.lastMy = mouseY;
      super.render(g, mouseX, mouseY, partialTick);
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int muted = van ? -4144960 : p.textMuted();
      if (this.loading && this.all == null) {
         CFHGuiSpinner.draw(g, this.listRect.centerX() - 8, this.listRect.centerY() - 20, 16, van ? -1 : p.accentInk());
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.worlds.loading"), this.listRect.centerX(), this.listRect.centerY() + 2, muted);
      } else if (this.loadError != null) {
         CFHGuiDraw.textCentered(g, this.loadError, this.listRect.centerX(), this.listRect.centerY() - 4, p.danger());
      } else if (this.all != null && this.all.isEmpty()) {
         g.drawString(this.font, Fonts.heading(Component.translatable("cfhgui.worlds.empty")), this.listRect.centerX() - this.font.width(Fonts.heading(Component.translatable("cfhgui.worlds.empty"))) / 2, this.listRect.centerY() - 22, p.text(), van);
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.worlds.empty_hint"), this.listRect.centerX(), this.listRect.centerY() - 8, muted);
      } else if (this.shown.isEmpty() && this.grid != null) {
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.worlds.none_match"), this.listRect.centerX(), this.listRect.centerY() - 4, muted);
      }

      if (this.showDetails) {
         this.renderDetails(g, mouseX, mouseY);
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (this.showDetails) {
         Rect r = this.detailsRect;
         if (Theme.current().isVanilla()) {
            CFHGuiDraw.vanillaListBackground(g, r.x(), r.y(), r.w(), r.h(), this.minecraft.level != null);
            CFHGuiDraw.outline(g, r.x(), r.y(), r.w(), r.h(), -16777216, 0);
         } else {
            CFHGuiDraw.floatingPanel(g, r.x(), r.y(), r.w(), r.h(), 1.0F);
         }

      }
   }

   private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      Rect r = this.detailsRect;
      int muted = van ? -4144960 : p.textMuted();
      if (this.selected == null) {
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.worlds.select_one"), r.centerX(), r.centerY() - 4, muted);
      } else {
         WorldEntry e = this.selected;
         LevelSummary s = e.summary;
         e.requestIcon();
         e.requestSize();
         int is = 48;
         int ix = r.x() + 8;
         int iy = r.y() + 8;
         if (e.icon != null) {
            RenderSystem.enableBlend();
            g.blit(e.icon.id(), ix, iy, 48, 48, 0.0F, 0.0F, e.icon.width(), e.icon.height(), e.icon.width(), e.icon.height());
            RenderSystem.disableBlend();
         } else {
            CFHGuiDraw.pixelRound(g, ix, iy, 48, 48, van ? Integer.MIN_VALUE : p.surface(), t.radius());
            Icons.draw(g, Icon.WORLD, ix + 16, iy + 16, 16, muted);
         }

         CFHGuiDraw.outline(g, ix, iy, 48, 48, van ? -16777216 : p.border(), t.radius());
         int tx = ix + 48 + 8;
         int tw = r.right() - 8 - tx;
         int ty = iy + 5;
         g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(Component.literal(e.name())), tw), tx, ty, p.text(), van);
         g.drawString(this.font, CFHGuiDraw.truncate(Component.literal(e.id()), tw), tx, ty + 14, van ? -6250336 : p.textDim(), van);
         int cx = tx + CFHGuiBadge.draw(g, s.getGameMode().getShortDisplayName(), tx, ty + 28, p.accent()) + 3;
         if (s.isHardcore()) {
            cx += CFHGuiBadge.draw(g, Component.translatable("cfhgui.worlds.hardcore"), cx, ty + 28, p.danger()) + 3;
         }

         if (s.hasCommands() && cx + CFHGuiDraw.width(Component.translatable("cfhgui.worlds.cheats")) + 8 <= r.right() - 8) {
            CFHGuiBadge.draw(g, Component.translatable("cfhgui.worlds.cheats"), cx, ty + 28, p.warning());
         }

         int y = iy + 48 + 10;
         int lx = r.x() + 8;
         int lw = r.w() - 16;
         List<FormattedCharSequence> warning = !s.isLocked() && !s.isDisabled() && !s.requiresManualConversion() && !s.backupStatus().shouldBackup() ? List.of() : this.font.split((FormattedText)(s.isLocked() ? Component.translatable("selectWorld.locked") : s.getInfo()), lw);
         int limit = this.detailsTextBottom;
         int rowLimit = limit - warning.size() * 10;
         int var10000 = y + 14;
         Objects.requireNonNull(this.font);
         if (var10000 + 9 <= rowLimit) {
            CFHGuiDraw.sectionRule(g, Component.translatable("cfhgui.worlds.details"), lx, y, lw, 1.0F);
            y += 14;
         }

         y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.last_played"), Component.literal(Fmt.date(s.getLastPlayed())), muted);
         y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.version"), s.getWorldVersionName(), muted);
         y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.size"), Component.literal(e.size >= 0L ? Fmt.bytes(e.size) : "..."), muted);
         if (s.isExperimental()) {
            y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.experimental"), Component.translatable("options.on"), muted);
         }

         y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.mode"), s.getGameMode().getLongDisplayName(), muted);
         y = this.row(g, lx, y, lw, rowLimit, Component.translatable("cfhgui.worlds.cheats"), Component.translatable(s.hasCommands() ? "options.on" : "options.off"), muted);
         if (!e.tags.isEmpty() && y + 11 + 12 <= rowLimit) {
            g.drawString(this.font, Component.translatable("cfhgui.worlds.tags"), lx, y, muted, van);
            y += 11;
            int tx2 = lx;

            for(String tag : e.tags) {
               int bw = CFHGuiDraw.width(tag) + 8;
               if (tx2 + bw > r.right() - 8) {
                  tx2 = lx;
                  y += 12;
               }

               if (y + 12 > rowLimit) {
                  break;
               }

               tx2 += CFHGuiBadge.draw(g, Component.literal(tag), tx2, y, van ? -7631989 : p.borderStrong()) + 3;
            }

            y += 14;
         }

         for(FormattedCharSequence line : warning) {
            Objects.requireNonNull(this.font);
            if (y + 9 > limit) {
               break;
            }

            g.drawString(this.font, line, lx, y, p.warning(), van);
            y += 10;
         }

      }
   }

   private int row(GuiGraphics g, int x, int y, int w, int limit, Component label, Component value, int muted) {
      Objects.requireNonNull(this.font);
      if (y + 9 > limit) {
         return y;
      } else {
         boolean van = Theme.current().isVanilla();
         g.drawString(this.font, label, x, y, muted, van);
         FormattedCharSequence v = CFHGuiDraw.truncate(value, w - this.font.width(label) - 6);
         g.drawString(this.font, v, x + w - this.font.width(v) + 1, y, Theme.current().palette().text(), van);
         return y + 12;
      }
   }

   public static enum Sort {
      LAST_PLAYED,
      NAME,
      SIZE,
      FAVORITES;
   }

   public static enum View {
      GRID,
      LIST;
   }

   private final class RowRenderer implements CFHGuiList.RowRenderer<WorldEntry> {
      public void render(GuiGraphics g, WorldEntry item, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
         WorldCard.drawSummary(g, item, x + 4, y + 4, w - 8 - 22, 32, false, mouseX, mouseY);
         Theme t = Theme.current();
         Palette p = t.palette();
         Icons.draw(g, item.favorite ? Icon.STAR_FILLED : Icon.STAR, x + w - 16, y + (h - 10) / 2, 10, item.favorite ? p.accentInk() : (t.isVanilla() ? -6250336 : p.textDim()));
         if (hovered) {
            if (CFHGuiWorldsScreen.this.hoverRow != index) {
               CFHGuiWorldsScreen.this.hoverRow = index;
               CFHGuiWorldsScreen.this.hoverRowEdge.snap(0.0F);
               CFHGuiWorldsScreen.this.hoverRowEdge.set(1.0F);
            }

            if (!selected || !t.isVanilla()) {
               WorldCard.hoverEdge(g, x, y, w, h, CFHGuiWorldsScreen.this.hoverRowEdge.get());
            }
         } else if (CFHGuiWorldsScreen.this.hoverRow == index) {
            CFHGuiWorldsScreen.this.hoverRow = -1;
         }

         CFHGuiIconButton plate = CFHGuiWorldsScreen.this.listPlate;
         if (item == CFHGuiWorldsScreen.this.selected && plate != null) {
            if (CFHGuiWorldsScreen.this.listPlateFor != item || CFHGuiWorldsScreen.this.listPlateNew) {
               boolean same = CFHGuiWorldsScreen.this.listPlateFor != null && CFHGuiWorldsScreen.this.listPlateFor.id().equals(item.id());
               CFHGuiWorldsScreen.this.listPlateFor = item;
               CFHGuiWorldsScreen.this.listPlateNew = false;
               PlayPlates.label(plate, Component.translatable("cfhgui.worlds.play_this", new Object[]{item.name()}));
               if (!same) {
                  CFHGuiWorldsScreen.this.listPlay.snap(0.0F);
               }
            }

            CFHGuiWorldsScreen.this.listPlay.set(hovered);
            plate.active = item.summary.primaryActionActive();
            plate.visible = true;
            plate.setX(x + 4 + 7);
            plate.setY(y + 4 + 7);
            PlayPlates.draw(g, plate, CFHGuiWorldsScreen.this.listPlay.get(), 1.0F, 0.0F, 0.0F, mouseX, mouseY, 0.0F);
         }

      }

      public boolean click(WorldEntry item, int index, int x, int y, int w, int h, double mouseX, double mouseY, int button) {
         if (button == 0 && mouseX >= (double)(x + w - 22)) {
            CFHGuiWorldsScreen.this.toggleFavorite(item);
            return true;
         } else {
            CFHGuiIconButton plate = CFHGuiWorldsScreen.this.listPlate;
            if (button == 0 && plate != null && item == CFHGuiWorldsScreen.this.listPlateFor && item == CFHGuiWorldsScreen.this.selected && plate.isMouseOver(mouseX, mouseY)) {
               plate.activate();
               return true;
            } else {
               return false;
            }
         }
      }
   }
}
