package com.onehumanawa.cfhcore.content.gui.menu.client.screenshots;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.media.ClipboardImages;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
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
import com.onehumanawa.cfhcore.content.gui.menu.api.ScreenshotShareProvider;
import com.onehumanawa.cfhcore.content.gui.menu.api.CFHGuiMenuApi;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiScreenshotsScreen extends CFHGuiScreen {
   private @Nullable List<Screenshots.Shot> all;
   private List<Screenshots.Shot> shown = List.of();
   private boolean loading;
   private boolean loadedOnce;
   private String query = "";
   private Sort sort;
   private Screenshots.@Nullable Shot selected;
   private Screenshots.@Nullable Shot lastClick;
   private long lastClickMs;
   private int lastMx;
   private int lastMy;
   private @Nullable CFHGuiScrollPanel grid;
   private @Nullable CFHGuiSearchField search;
   private final Map<Screenshots.Shot, ScreenshotCard> cards = new IdentityHashMap();
   private Rect gridRect = new Rect(0, 0, 0, 0);
   private int cols = 1;
   private final List<CFHGuiButton> selectionButtons = new ArrayList();

   public CFHGuiScreenshotsScreen(@Nullable Screen parent) {
      super(Component.translatable("cfhgui.screenshots.title"), parent);
      Sort s = CFHGuiScreenshotsScreen.Sort.NEWEST;

      try {
         s = CFHGuiScreenshotsScreen.Sort.valueOf(CFHGuiMenu.config().screenshotsSort.toUpperCase(Locale.ROOT));
      } catch (Exception var4) {
      }

      this.sort = s;
   }

   protected void build() {
      this.cards.clear();
      this.selectionButtons.clear();
      this.grid = null;
      Rect c = this.contentRect();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("cfhgui.refresh"), this::reload));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.FOLDER, Component.translatable("cfhgui.screenshots.open_folder"), () -> Util.getPlatform().openPath(Screenshots.dir())));
      if (this.width >= 480) {
         this.addHeaderAction(new CFHGuiDropdown<Sort>(0, 0, 96, Arrays.asList(CFHGuiScreenshotsScreen.Sort.values()), this.sort, (s) -> {
            String var10000 = s.name();
            return Component.translatable("cfhgui.screenshots.sort." + var10000.toLowerCase(Locale.ROOT));
         }, (s) -> {
            this.sort = s;
            CFHGuiMenu.configFile().update((x) -> x.screenshotsSort = s.name());
            this.rebuild();
         }));
      }

      CFHGuiSearchField sf = new CFHGuiSearchField(0, 0, this.width >= 600 ? 150 : 96, (q) -> {
         this.query = q;
         this.rebuild();
      });
      sf.onEnter(() -> {
         if (this.selected != null) {
            this.view(this.selected);
         }

      });
      this.search = (CFHGuiSearchField)this.addHeaderAction(sf);
      sf.setValue(this.query);
      int bottomH = 30;
      this.gridRect = new Rect(c.x(), c.y() + 4, c.w(), c.h() - 4 - 30);
      this.grid = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(this.gridRect.x(), this.gridRect.y(), this.gridRect.w(), this.gridRect.h())).scrollStep(60));
      int by = c.bottom() - 22;
      int bx = c.right();
      ScreenshotShareProvider share = CFHGuiMenuApi.screenshotShareProvider();
      if (share != null) {
         bx -= 66;
         this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(bx, by, 62, Component.translatable("cfhgui.screenshots.share"), () -> {
            if (this.selected != null) {
               share.share(this.selected.path());
            }

         })).icon(Icon.SEND)));
         bx -= 4;
      }

      bx -= 20;
      this.selectionButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.TRASH, Component.translatable("cfhgui.screenshots.delete"), () -> {
         if (this.selected != null) {
            this.confirmDelete(this.selected);
         }

      })));
      bx -= 22;
      this.selectionButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.EDIT, Component.translatable("cfhgui.screenshots.rename"), () -> {
         if (this.selected != null) {
            this.rename(this.selected);
         }

      })));
      bx -= 22;
      this.selectionButtons.add((CFHGuiButton)this.add(new CFHGuiIconButton(bx, by, 20, Icon.COPY, Component.translatable("cfhgui.screenshots.copy"), () -> {
         if (this.selected != null) {
            copy(this.selected);
         }

      })));
      bx -= 66;
      this.selectionButtons.add((CFHGuiButton)this.add((new CFHGuiButton(bx, by, 62, Component.translatable("cfhgui.screenshots.view"), () -> {
         if (this.selected != null) {
            this.view(this.selected);
         }

      })).variant(Variant.PRIMARY).icon(Icon.EYE)));
      if (this.all == null && !this.loading) {
         this.load();
      } else {
         this.rebuild();
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
      this.loading = true;
      Screenshots.scan().whenCompleteAsync((shots, err) -> {
         this.loading = false;
         this.loadedOnce = true;
         if (err != null) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] screenshot scan failed", err);
         }

         Path selPath = this.selected == null ? null : this.selected.path();
         this.all = shots == null ? new ArrayList() : new ArrayList(shots);
         this.selected = null;
         if (selPath != null) {
            for(Screenshots.Shot s : this.all) {
               if (s.path().equals(selPath)) {
                  this.selected = s;
               }
            }
         }

         this.rebuild();
         this.updateSelection();
      }, this.minecraft);
   }

   private void reload() {
      if (!this.loading) {
         this.load();
      }

   }

   private void rebuild() {
      if (this.grid != null) {
         this.grid.clear();
         this.cards.clear();
         List<Screenshots.Shot> list = new ArrayList();
         if (this.all != null) {
            String q = this.query == null ? "" : this.query.toLowerCase(Locale.ROOT).trim();

            for(Screenshots.Shot s : this.all) {
               if (q.isEmpty() || s.fileName().toLowerCase(Locale.ROOT).contains(q)) {
                  list.add(s);
               }
            }
         }

         switch (this.sort.ordinal()) {
            case 1 -> list.sort(Comparator.comparingLong(Screenshots.Shot::modified));
            case 2 -> list.sort(Comparator.comparing((sx) -> sx.fileName().toLowerCase(Locale.ROOT)));
            default -> list.sort(Comparator.comparingLong(Screenshots.Shot::modified).reversed());
         }

         this.shown = list;
         MenuConfig cfg = CFHGuiMenu.config();
         this.cols = cfg.screenshotColumns > 0 ? cfg.screenshotColumns : Math.max(1, (this.gridRect.w() - 6) / 176);
         int cardW = (this.gridRect.w() - 8 - (this.cols - 1) * 8) / this.cols;
         int cardH = (cardW - 8) * 9 / 16 + 8 + 26;
         int i = 0;

         for(Screenshots.Shot s : this.shown) {
            ScreenshotCard card = new ScreenshotCard(0, 0, cardW, cardH, s, this);
            card.selected(s == this.selected);
            this.grid.add(card, i % this.cols * (cardW + 8), i / this.cols * (cardH + 8));
            this.cards.put(s, card);
            ++i;
         }

         this.grid.setContentHeight((this.shown.size() + this.cols - 1) / this.cols * (cardH + 8));
      }
   }

   void clicked(ScreenshotCard card) {
      long now = Clock.nowMs();
      boolean dbl = card.shot() == this.lastClick && now - this.lastClickMs < 400L;
      this.lastClickMs = now;
      this.lastClick = card.shot();
      this.select(card.shot());
      if (dbl) {
         this.view(card.shot());
      }

   }

   private void select(Screenshots.@Nullable Shot s) {
      this.selected = s;

      for(Map.Entry<Screenshots.Shot, ScreenshotCard> e : this.cards.entrySet()) {
         ((ScreenshotCard)e.getValue()).selected(e.getKey() == s);
      }

      this.updateSelection();
   }

   private void updateSelection() {
      for(CFHGuiButton b : this.selectionButtons) {
         b.active = this.selected != null;
      }

   }

   void contextMenu(ScreenshotCard card) {
      this.select(card.shot());
      Screenshots.Shot s = card.shot();
      List<MenuPopup.Item> items = new ArrayList();
      items.add(Item.of(Component.translatable("cfhgui.screenshots.view"), Icon.EYE, () -> this.view(s)));
      items.add(Item.of(Component.translatable("cfhgui.screenshots.copy"), Icon.COPY, () -> copy(s)));
      items.add(Item.of(Component.translatable("cfhgui.screenshots.rename"), Icon.EDIT, () -> this.rename(s)));
      ScreenshotShareProvider share = CFHGuiMenuApi.screenshotShareProvider();
      if (share != null) {
         items.add(Item.of(Component.translatable("cfhgui.screenshots.share"), Icon.SEND, () -> share.share(s.path())));
      }

      items.add(Item.of(Component.translatable("cfhgui.screenshots.open_folder"), Icon.FOLDER, () -> Util.getPlatform().openPath(Screenshots.dir())));
      items.add(Item.of(Component.translatable("cfhgui.screenshots.copy_path"), Icon.LINK, () -> {
         this.minecraft.keyboardHandler.setClipboard(s.path().toAbsolutePath().toString());
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), Component.literal(s.fileName()), Icon.COPY);
      }));
      items.add(Item.sep());
      items.add(Item.danger(Component.translatable("cfhgui.screenshots.delete"), Icon.TRASH, () -> this.confirmDelete(s)));
      CFHGuiContextMenu.open((double)this.lastMx, (double)this.lastMy, items);
   }

   private void view(Screenshots.Shot s) {
      int index = this.shown.indexOf(s);
      this.minecraft.setScreen(new ScreenshotViewer(this, new ArrayList(this.shown), Math.max(0, index)));
   }

   static void copy(Screenshots.Shot s) {
      ClipboardImages.copy(s.path(), (ok) -> {
         if (ok) {
            CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.copied"), Component.literal(s.fileName()), Icon.COPY);
         } else {
            CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.copy_unsupported"), Component.translatable("cfhgui.screenshots.copy_unsupported_body"), Icon.WARNING);
         }

      });
   }

   private void rename(Screenshots.Shot s) {
      CFHGuiModal.prompt(Component.translatable("cfhgui.screenshots.rename"), Component.translatable("cfhgui.screenshots.rename_body"), s.name(), (name) -> {
         if (!name.isBlank() && !name.trim().equals(s.name())) {
            if (Screenshots.rename(s, name)) {
               this.reload();
            } else {
               CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.rename_failed"), Component.literal(name), Icon.WARNING);
            }

         }
      });
   }

   private void confirmDelete(Screenshots.Shot s) {
      CFHGuiModal.confirmDanger(Component.translatable("cfhgui.screenshots.delete"), Component.translatable("cfhgui.screenshots.delete_body", new Object[]{s.fileName()}), Component.translatable("cfhgui.screenshots.delete"), () -> {
         if (Screenshots.delete(s)) {
            if (this.all != null) {
               this.all.remove(s);
            }

            if (this.selected == s) {
               this.selected = null;
            }

            this.rebuild();
            this.updateSelection();
            CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.deleted"), Component.literal(s.fileName()), Icon.TRASH);
         }

      });
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean typing = this.search != null && this.search.isFocused();
      if (!typing && keyCode == 47 && this.search != null) {
         this.setFocused(this.search);
         this.search.setFocused(true);
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
         this.view(this.selected);
         return true;
      } else if (hasControlDown() && keyCode == 67 && this.selected != null) {
         copy(this.selected);
         return true;
      } else {
         if (!this.shown.isEmpty()) {
            int i = this.selected == null ? -1 : this.shown.indexOf(this.selected);
            int var10000;
            switch (keyCode) {
               case 262 -> var10000 = i + 1;
               case 263 -> var10000 = i - 1;
               case 264 -> var10000 = i < 0 ? 0 : i + this.cols;
               case 265 -> var10000 = i - this.cols;
               default -> var10000 = Integer.MIN_VALUE;
            }

            int next = var10000;
            if (next != Integer.MIN_VALUE) {
               next = Math.max(0, Math.min(this.shown.size() - 1, next));
               Screenshots.Shot s = (Screenshots.Shot)this.shown.get(next);
               this.select(s);
               ScreenshotCard card = (ScreenshotCard)this.cards.get(s);
               if (card != null && this.grid != null) {
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
         CFHGuiSpinner.draw(g, this.gridRect.centerX() - 8, this.gridRect.centerY() - 20, 16, van ? -1 : p.accentInk());
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.screenshots.loading"), this.gridRect.centerX(), this.gridRect.centerY() + 2, muted);
      } else if (this.all != null && this.all.isEmpty()) {
         Component h = Fonts.heading(Component.translatable("cfhgui.screenshots.empty"));
         g.drawString(this.font, h, this.gridRect.centerX() - this.font.width(h) / 2, this.gridRect.centerY() - 16, p.text(), van);
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.screenshots.empty_hint", new Object[]{this.minecraft.options.keyScreenshot.getTranslatedKeyMessage()}), this.gridRect.centerX(), this.gridRect.centerY() - 2, muted);
      } else if (this.shown.isEmpty() && this.all != null) {
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.screenshots.none_match"), this.gridRect.centerX(), this.gridRect.centerY() - 4, muted);
      }

      if (this.all != null) {
         Component count = Component.translatable("cfhgui.screenshots.count", new Object[]{this.shown.size(), this.all.size()});
         Rect c = this.contentRect();
         g.drawString(this.font, count, c.x(), c.bottom() - 16, van ? -6250336 : p.textDim(), van);
      }

   }

   public static enum Sort {
      NEWEST,
      OLDEST,
      NAME;
   }
}
