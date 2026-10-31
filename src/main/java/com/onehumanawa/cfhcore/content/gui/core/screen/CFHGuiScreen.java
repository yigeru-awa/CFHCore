package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public abstract class CFHGuiScreen extends Screen {
   public static final int PAD = 12;
   public static final int HEADER_H = 32;
   public static final int STAGGER_MS = 18;
   public static final int STAGGER_MAX_MS = 200;
   protected final @Nullable Screen parent;
   protected boolean showHeader = true;
   protected boolean showBack = true;
   protected boolean panoramaBackground = false;
   protected int maxContentWidth = 0;
   protected @Nullable EditBox slashFocusTarget;
   private boolean firstBuild = true;
   private final List<AbstractWidget> headerActions = new ArrayList();
   private int entranceIndex;
   private final List<Renderable> cfhguiRenderables = new ArrayList();

   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
      this.cfhguiRenderables.add(widget);
      return (T)super.addRenderableWidget(widget);
   }

   protected <T extends Renderable> T addRenderableOnly(T renderable) {
      this.cfhguiRenderables.add(renderable);
      return (T)super.addRenderableOnly(renderable);
   }

   protected void removeWidget(GuiEventListener listener) {
      this.cfhguiRenderables.remove(listener);
      this.headerActions.remove(listener);
      super.removeWidget(listener);
   }

   protected void clearWidgets() {
      this.cfhguiRenderables.clear();
      this.headerActions.clear();
      this.slashFocusTarget = null;
      super.clearWidgets();
   }

   public List<Renderable> renderableList() {
      return this.cfhguiRenderables;
   }

   protected CFHGuiScreen(Component title, @Nullable Screen parent) {
      super(title);
      this.parent = parent;
   }

   public Rect contentRect() {
      int w = this.width - 24;
      int x = 12;
      if (this.maxContentWidth > 0 && w > this.maxContentWidth) {
         w = this.maxContentWidth;
         x = (this.width - w) / 2;
      }

      int top = this.showHeader ? 36 : 12;
      return new Rect(x, top, w, this.height - top - 12);
   }

   public Rect headerRect() {
      return new Rect(0, 0, this.width, 32);
   }

   protected int headerTitleX() {
      return 12 + (this.showBack && this.parent != null ? 22 : 0);
   }

   protected final void init() {
      this.headerActions.clear();
      this.entranceIndex = 0;
      this.build();
      if (this.showHeader && this.showBack && this.parent != null) {
         this.add(new CFHGuiIconButton(8, 6, 20, Icon.ARROW_LEFT, Component.translatable("gui.back"), this::back));
      }

      if (this.firstBuild) {
         this.firstBuild = false;
         int i = 0;

         for(Renderable r : this.cfhguiRenderables) {
            i = entrance(r, i);
         }
      }

   }

   protected abstract void build();

   protected <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
      return (T)this.addRenderableWidget(widget);
   }

   protected <T extends AbstractWidget> T addHeaderAction(T widget) {
      int x = this.width - 12;

      for(AbstractWidget a : this.headerActions) {
         x = a.getX();
      }

      widget.setX(x - widget.getWidth() - (this.headerActions.isEmpty() ? 0 : 4));
      widget.setY((32 - widget.getHeight()) / 2);
      this.headerActions.add(widget);
      this.addRenderableWidget(widget);
      return widget;
   }

   protected int headerActionsLeft() {
      int x = this.width - 12;

      for(AbstractWidget a : this.headerActions) {
         x = Math.min(x, a.getX());
      }

      return x;
   }

   protected void reflowHeaderActions() {
      int x = this.width - 12;

      for(int i = 0; i < this.headerActions.size(); ++i) {
         AbstractWidget a = (AbstractWidget)this.headerActions.get(i);
         x -= a.getWidth() + (i == 0 ? 0 : 4);
         a.setX(x);
      }

   }

   public static int entrance(Renderable r, int index) {
      if (r instanceof CFHGuiWidget w) {
         w.playEntrance(Math.min(200, index++ * 18));
      } else if (r instanceof CFHGuiCard c) {
         c.playEntrance(Math.min(200, index++ * 18));
      } else if (r instanceof CFHGuiScrollPanel panel) {
         for(GuiEventListener child : panel.children()) {
            if (child instanceof Renderable cr) {
               index = entrance(cr, index);
            }
         }
      }

      return index;
   }

   public void back() {
      Popups.closeAll();
      this.minecraft.setScreen(this.parent);
   }

   public void onClose() {
      this.back();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (keyCode == 256 && this.shouldCloseOnEsc()) {
         this.back();
         return true;
      } else if (keyCode == 47 && this.slashFocusTarget != null) {
         GuiEventListener var5 = this.getFocused();
         if (var5 instanceof EditBox) {
            EditBox eb = (EditBox)var5;
            if (eb.isFocused()) {
               return false;
            }
         }

         this.setFocused(this.slashFocusTarget);
         EditBox var6 = this.slashFocusTarget;
         if (var6 instanceof CFHGuiTextField) {
            CFHGuiTextField f = (CFHGuiTextField)var6;
            f.setFocused(true);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isPauseScreen() {
      return this.minecraft == null || this.minecraft.level == null || super.isPauseScreen();
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      boolean inWorld = this.minecraft.level != null;
      boolean scene = !inWorld && MenuBackdrop.active();
      if (t.isVanilla()) {
         if (!inWorld && this.panoramaBackground && !scene) {
            this.renderPanorama(g, partialTick);
            CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.3F);
         } else {
            super.renderBackground(g, mouseX, mouseY, partialTick);
         }
      } else {
         if (inWorld || scene && MenuBackdrop.paint(g, this.width, this.height, partialTick)) {
            if (inWorld && t.blurInGame()) {
               this.renderBlurredBackground(partialTick);
            }

            if (inWorld) {
               g.fill(0, 0, this.width, this.height, t.palette().overlay());
            } else {
               MenuBackdrop.drawVeil(g, 0, 0, this.width, this.height);
            }

            if (this.showHeader) {
               CFHGuiDraw.rect(g, 0, 0, this.width, 32, t.chrome(t.palette().bg()));
            }
         } else if (this.panoramaBackground) {
            this.renderPanorama(g, partialTick);
            g.fill(0, 0, this.width, this.height, Colors.withAlpha(t.bg(), 153));
            CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.45F);
         } else {
            g.fill(0, 0, this.width, this.height, t.bg());
            CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.3F);
         }

      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (this.showHeader) {
         this.renderHeader(g);
      }

      this.renderContent(g, mouseX, mouseY, partialTick);
   }

   protected void renderHeader(GuiGraphics g) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int tx = this.headerTitleX();
      int right = this.width - 12;

      for(AbstractWidget a : this.headerActions) {
         right = Math.min(right, a.getX() - 8);
      }

      int ty = CFHGuiDraw.textY(0, 32);
      if (t.isVanilla()) {
         g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), right - tx), tx, ty, -1, true);
         CFHGuiDraw.vanillaSeparator(g, 0, 30, this.width, true, MenuBackdrop.worldBehind());
      } else {
         FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), right - tx);
         g.drawString(this.font, title, tx, ty, p.text(), false);
         CFHGuiDraw.hline(g, 0, 31, this.width, p.border());
         CFHGuiDraw.accentCap(g, tx, 30, Math.max(16, Math.min(this.font.width(title), 48)), 1.0F);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
   }

   protected int nextEntranceDelay() {
      return Math.min(200, this.entranceIndex++ * 18);
   }

   protected Theme theme() {
      return Theme.current();
   }

   protected Palette palette() {
      return Theme.current().palette();
   }

   protected boolean inWorld() {
      return this.minecraft != null && this.minecraft.level != null;
   }
}
