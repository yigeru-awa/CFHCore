package com.onehumanawa.cfhcore.content.gui.core.client.media;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.media.Attachment;
import com.onehumanawa.cfhcore.content.gui.core.media.GifSearch;
import com.onehumanawa.cfhcore.content.gui.core.media.GifStore;
import com.onehumanawa.cfhcore.content.gui.core.media.MediaCache;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class GifLibraryScreen extends CFHGuiScreen {
   private static final int CARD = 72;
   private static final int GAP = 6;
   private static Tab tab;
   private static List<String> searchResults;
   private static String lastQuery;
   private static boolean trendingLoaded;
   private final Consumer<String> onPick;
   private final Anim scroll;
   private CFHGuiTabStrip tabs;
   private CFHGuiTextField field;
   private CFHGuiButton action;
   private List<String> urls;
   private boolean searching;
   private GifSearch.Result.State searchState;
   private int gridTop;
   private int gridBottom;
   private int hoverIndex;

   public GifLibraryScreen(@Nullable Screen parent, Consumer<String> onPick) {
      super(Component.translatable("cfhgui.gifs.title"), parent);
      this.scroll = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.urls = List.of();
      this.searchState = GifSearch.Result.State.OK;
      this.hoverIndex = -1;
      this.onPick = onPick;
      this.maxContentWidth = 560;
   }

   protected void build() {
      Rect c = this.contentRect();
      this.tabs = (CFHGuiTabStrip)this.add(new CFHGuiTabStrip(c.x(), c.y() + 2, Math.min(c.w(), 240), List.of(new CFHGuiTabStrip.Tab(Component.translatable("cfhgui.gifs.search"), Icon.SEARCH), new CFHGuiTabStrip.Tab(Component.translatable("cfhgui.gifs.favorites"), Icon.STAR), new CFHGuiTabStrip.Tab(Component.translatable("cfhgui.gifs.recent"), Icon.HISTORY)), tab.ordinal(), (i) -> this.switchTab(GifLibraryScreen.Tab.values()[i])));
      int fy = c.y() + 30;
      this.field = (CFHGuiTextField)this.add(new CFHGuiTextField(c.x(), fy, c.w() - 92, 20, Component.translatable("cfhgui.gifs.title")));
      this.field.maxLength(512);
      this.field.onEnter(this::onAction);
      this.action = (CFHGuiButton)this.add((new CFHGuiButton(c.right() - 86, fy, 86, 20, Component.empty(), this::onAction)).variant(CFHGuiButton.Variant.PRIMARY));
      this.gridTop = fy + 28;
      this.gridBottom = c.bottom();
      this.applyTabChrome();
      this.refresh();
      if (tab == GifLibraryScreen.Tab.SEARCH && searchResults.isEmpty() && !trendingLoaded && GifSearch.hasKey()) {
         this.loadTrending();
      }

   }

   private void switchTab(Tab t) {
      tab = t;
      this.scroll.snap(0.0F);
      this.applyTabChrome();
      this.refresh();
      if (t == GifLibraryScreen.Tab.SEARCH && searchResults.isEmpty() && !trendingLoaded && GifSearch.hasKey()) {
         this.loadTrending();
      }

   }

   private void applyTabChrome() {
      if (tab == GifLibraryScreen.Tab.SEARCH) {
         this.field.placeholder(Component.translatable("cfhgui.gifs.search_hint"));
         this.field.icon(Icon.SEARCH);
         this.field.setValue(lastQuery);
         this.action.setMessage(Component.translatable("cfhgui.gifs.go"));
         this.action.icon(Icon.SEARCH);
      } else {
         this.field.placeholder(Component.translatable("cfhgui.gifs.url_hint"));
         this.field.icon(Icon.LINK);
         this.field.setValue("");
         this.action.setMessage(Component.translatable("cfhgui.gifs.add"));
         this.action.icon(Icon.PLUS);
      }

      this.field.setInvalid(false);
   }

   private void onAction() {
      if (tab == GifLibraryScreen.Tab.SEARCH && !this.field.getValue().trim().startsWith("http")) {
         this.runSearch();
      } else {
         String url = this.field.getValue().trim();
         if (url.startsWith("http")) {
            if (Attachment.parse(url, true).stream().noneMatch(Attachment::isImage)) {
               this.field.setInvalid(true);
               CFHGuiToasts.show(Component.translatable("cfhgui.gifs.not_a_file"), Component.translatable("cfhgui.gifs.not_a_file.body"), Icon.WARNING);
               return;
            }

            if (!GifStore.isFavorite(url)) {
               GifStore.toggleFavorite(url);
            }

            this.field.setValue("");
            this.switchTab(GifLibraryScreen.Tab.FAVORITES);
            this.tabs.select(GifLibraryScreen.Tab.FAVORITES.ordinal());
         } else {
            this.field.setInvalid(true);
         }

      }
   }

   private void runSearch() {
      String q = this.field.getValue().trim();
      if (!this.searching) {
         if (q.isEmpty()) {
            this.loadTrending();
         } else {
            lastQuery = q;
            this.searching = true;
            GifSearch.search(q, (result) -> {
               this.searching = false;
               this.searchState = result.state();
               searchResults = result.urls();
               if (tab == GifLibraryScreen.Tab.SEARCH) {
                  this.refresh();
               }

            });
         }
      }
   }

   private void loadTrending() {
      if (!this.searching) {
         this.searching = true;
         trendingLoaded = true;
         GifSearch.trending((result) -> {
            this.searching = false;
            this.searchState = result.state();
            if (result.state() != GifSearch.Result.State.OK) {
               trendingLoaded = false;
            }

            if (result.state() == GifSearch.Result.State.OK && searchResults.isEmpty()) {
               searchResults = result.urls();
            }

            if (tab == GifLibraryScreen.Tab.SEARCH) {
               this.refresh();
            }

         });
      }
   }

   private void refresh() {
      List var10001;
      switch (tab.ordinal()) {
         case 0 -> var10001 = searchResults;
         case 1 -> var10001 = GifStore.favorites();
         case 2 -> var10001 = GifStore.recent();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      this.urls = var10001;
      this.scroll.set(Math.min(this.scroll.target(), (float)this.maxScroll()));
   }

   private Rect grid() {
      Rect c = this.contentRect();
      return new Rect(c.x(), this.gridTop, c.w(), Math.max(0, this.gridBottom - this.gridTop));
   }

   private int columns() {
      return Math.max(1, (this.grid().w() + 6) / 78);
   }

   private int contentHeight() {
      int rows = (this.urls.size() + this.columns() - 1) / this.columns();
      return rows * 78;
   }

   private int maxScroll() {
      return Math.max(0, this.contentHeight() - this.grid().h());
   }

   private int cardX(int i) {
      Rect gr = this.grid();
      int cols = this.columns();
      int used = cols * 78 - 6;
      return gr.x() + (gr.w() - used) / 2 + i % cols * 78;
   }

   private int cardY(int i) {
      return this.grid().y() + i / this.columns() * 78 - Math.round(this.scroll.get());
   }

   private int cardAt(double mx, double my) {
      Rect gr = this.grid();
      if (!gr.contains(mx, my)) {
         return -1;
      } else {
         for(int i = 0; i < this.urls.size(); ++i) {
            int x = this.cardX(i);
            int y = this.cardY(i);
            if (mx >= (double)x && mx < (double)(x + 72) && my >= (double)y && my < (double)(y + 72)) {
               return i;
            }
         }

         return -1;
      }
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      Rect gr = this.grid();
      this.hoverIndex = this.cardAt((double)mouseX, (double)mouseY);
      if (!this.urls.isEmpty()) {
         CFHGuiDraw.scissor(g, gr.x(), gr.y(), gr.w(), gr.h());

         for(int i = 0; i < this.urls.size(); ++i) {
            int x = this.cardX(i);
            int y = this.cardY(i);
            if (y <= gr.bottom() && y + 72 >= gr.y()) {
               this.drawCard(g, (String)this.urls.get(i), x, y, i == this.hoverIndex, mouseX, mouseY);
            }
         }

         CFHGuiDraw.unscissor(g);
         if (this.maxScroll() > 0) {
            int bx = gr.right() - 3;
            int barH = Math.max(12, (int)((long)gr.h() * (long)gr.h() / (long)Math.max(1, this.contentHeight())));
            int barY = gr.y() + (int)((float)(gr.h() - barH) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
            CFHGuiDraw.pixelRound(g, bx, gr.y(), 3, gr.h(), Colors.withAlpha(t.isVanilla() ? -16777216 : p.surface(), 128), 1);
            CFHGuiDraw.pixelRound(g, bx, barY, 3, barH, t.isVanilla() ? -4144960 : p.borderStrong(), 1);
         }

         if (this.hoverIndex >= 0) {
            String url = (String)this.urls.get(this.hoverIndex);
            boolean onStar = this.onStar(this.hoverIndex, (double)mouseX, (double)mouseY);
            CFHGuiTooltips.request(Component.translatable(onStar ? (GifStore.isFavorite(url) ? "cfhgui.gifs.unstar" : "cfhgui.gifs.star") : "cfhgui.gifs.send"), (AbstractWidget)null);
         }

      } else {
         Component msg;
         if (tab == GifLibraryScreen.Tab.SEARCH) {
            msg = this.searching ? Component.translatable("cfhgui.media.loading") : (this.searchState == GifSearch.Result.State.NO_KEY ? Component.translatable("cfhgui.gifs.no_key") : (this.searchState == GifSearch.Result.State.ERROR ? Component.translatable("cfhgui.gifs.search_error") : (this.searchState == GifSearch.Result.State.UNAVAILABLE ? Component.translatable("cfhgui.gifs.search_unavailable") : Component.translatable("cfhgui.gifs.search_prompt"))));
         } else {
            msg = Component.translatable(tab == GifLibraryScreen.Tab.FAVORITES ? "cfhgui.gifs.empty_favorites" : "cfhgui.gifs.empty_recent");
         }

         int y = gr.centerY() - 4;

         for(FormattedCharSequence line : this.font.split(msg, Math.min(300, gr.w() - 20))) {
            g.drawString(this.font, line, gr.centerX() - this.font.width(line) / 2, y, p.textMuted(), t.isVanilla());
            y += 11;
         }

      }
   }

   private boolean onStar(int i, double mx, double my) {
      int x = this.cardX(i);
      int y = this.cardY(i);
      return mx >= (double)(x + 72 - 16) && mx < (double)(x + 72) && my >= (double)y && my < (double)(y + 16);
   }

   private void drawCard(GuiGraphics g, String url, int x, int y, boolean hover, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      if (t.isVanilla()) {
         g.fill(x, y, x + 72, y + 72, -1879048192);
         CFHGuiDraw.outline(g, x, y, 72, 72, hover ? -1 : -12566464, 0);
      } else {
         CFHGuiDraw.pixelRound(g, x, y, 72, 72, t.panel(hover ? p.surfaceHover() : p.surface()), t.radius());
         CFHGuiDraw.outline(g, x, y, 72, 72, hover ? p.accent() : p.border(), t.radius());
      }

      MediaCache.Entry e = MediaCache.get(new Attachment(Attachment.Kind.IMAGE_URL, Attachment.idForUrl(url), url, 0));
      CFHGuiDraw.scissor(g, x + 1, Math.max(this.grid().y(), y + 1), 70, 70);
      MediaDraw.drawFit(g, e, x + 2, y + 2, 68, 68, 1.0F, true, true);
      CFHGuiDraw.unscissor(g);
      boolean fav = GifStore.isFavorite(url);
      boolean starHover = hover && this.onStar(this.urls.indexOf(url), (double)mouseX, (double)mouseY);
      if (fav || hover) {
         g.fill(x + 72 - 15, y + 1, x + 72 - 1, y + 15, Integer.MIN_VALUE);
         Icons.draw(g, fav ? Icon.STAR_FILLED : Icon.STAR, x + 72 - 14, y + 2, 12, fav ? Colors.withAlpha(p.warning(), starHover ? 255 : 224) : Colors.withAlpha(-1, starHover ? 255 : 160));
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int i = this.cardAt(mouseX, mouseY);
      if (i >= 0 && button == 0) {
         String url = (String)this.urls.get(i);
         if (this.onStar(i, mouseX, mouseY)) {
            GifStore.toggleFavorite(url);
            CFHGuiSounds.tick();
            this.refresh();
            return true;
         } else {
            CFHGuiSounds.click();
            GifStore.noteRecent(url);
            if (this.onPick != null) {
               this.onPick.accept(url);
            }

            this.back();
            return true;
         }
      } else if (i >= 0 && button == 1) {
         GifStore.toggleFavorite((String)this.urls.get(i));
         CFHGuiSounds.tick();
         this.refresh();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
      if (super.mouseScrolled(mouseX, mouseY, dx, dy)) {
         return true;
      } else if (!this.grid().contains(mouseX, mouseY) && mouseY < (double)this.gridTop) {
         return false;
      } else {
         this.scroll.set(Mth.clamp((float)((double)this.scroll.target() - dy * (double)78.0F / (double)2.0F), 0.0F, (float)this.maxScroll()));
         return true;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   static {
      tab = GifLibraryScreen.Tab.FAVORITES;
      searchResults = List.of();
      lastQuery = "";
   }

   private static enum Tab {
      SEARCH,
      FAVORITES,
      RECENT;
   }
}
