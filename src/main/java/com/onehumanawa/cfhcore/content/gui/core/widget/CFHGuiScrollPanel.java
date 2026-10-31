package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class CFHGuiScrollPanel extends AbstractContainerWidget {
   public static final int BAR_W = 4;
   public static final int BAR_HIT = 8;
   private final List<Child> children = new ArrayList();
   private final List<AbstractWidget> widgets = new ArrayList();
   private final Anim scroll;
   private int contentHeight;
   private int padding;
   private boolean drawBackground;
   private boolean edgeFades;
   private boolean draggingBar;
   private boolean barHover;
   private double dragStartY;
   private double dragStartScroll;
   private int scrollStep;

   public CFHGuiScrollPanel(int x, int y, int width, int height) {
      super(x, y, width, height, Component.empty());
      this.scroll = new Anim(0.0F, 180, Ease.OUT_CUBIC);
      this.padding = 0;
      this.edgeFades = true;
      this.scrollStep = 24;
   }

   public <T extends AbstractWidget> T add(T widget, int relX, int relY) {
      this.children.add(new Child(widget, relX, relY));
      this.widgets.add(widget);
      this.contentHeight = Math.max(this.contentHeight, relY + widget.getHeight());
      widget.setX(this.getX() + relX);
      widget.setY(this.getY() + relY - Math.round(this.scroll.get()));
      return widget;
   }

   public <T extends AbstractWidget> T add(T widget) {
      return (T)this.add(widget, widget.getX() - this.getX(), widget.getY() - this.getY());
   }

   public <T extends AbstractWidget> T addRelative(T widget) {
      return (T)this.add(widget, widget.getX(), widget.getY());
   }

   public void clear() {
      this.children.clear();
      this.widgets.clear();
      this.contentHeight = 0;
      this.scroll.snap(0.0F);
      this.setFocused((GuiEventListener)null);
   }

   public CFHGuiScrollPanel padding(int p) {
      this.padding = p;
      return this;
   }

   public CFHGuiScrollPanel background(boolean draw) {
      this.drawBackground = draw;
      return this;
   }

   public CFHGuiScrollPanel edgeFades(boolean on) {
      this.edgeFades = on;
      return this;
   }

   public CFHGuiScrollPanel scrollStep(int px) {
      this.scrollStep = px;
      return this;
   }

   public void setContentHeight(int h) {
      this.contentHeight = h;
   }

   public int contentHeight() {
      return this.contentHeight;
   }

   public int innerWidth() {
      return this.getWidth() - 8;
   }

   public int maxScroll() {
      return Math.max(0, this.contentHeight + this.padding - this.getHeight());
   }

   public double scrollAmount() {
      return (double)this.scroll.get();
   }

   public void scrollTo(double y) {
      this.scroll.set((float)Math.max((double)0.0F, Math.min((double)this.maxScroll(), y)));
   }

   public void scrollBy(double dy) {
      this.scrollTo((double)this.scroll.target() + dy);
   }

   public void snapScroll(double y) {
      this.scroll.snap((float)Math.max((double)0.0F, Math.min((double)this.maxScroll(), y)));
   }

   public void ensureVisible(AbstractWidget w) {
      for(Child c : this.children) {
         if (c.widget == w) {
            double s = (double)this.scroll.target();
            if ((double)c.relY < s + (double)4.0F) {
               this.scrollTo((double)(c.relY - 4));
            } else if ((double)(c.relY + w.getHeight()) > s + (double)this.getHeight() - (double)4.0F) {
               this.scrollTo((double)(c.relY + w.getHeight() - this.getHeight() + 4));
            }
         }
      }

   }

   private void layout() {
      int s = Math.round(this.scroll.get());

      for(Child c : this.children) {
         c.widget.setX(this.getX() + c.relX);
         c.widget.setY(this.getY() + c.relY - s);
      }

   }

   private boolean visibleChild(AbstractWidget w) {
      return w.visible && w.getY() + w.getHeight() > this.getY() && w.getY() < this.getY() + this.getHeight();
   }

   public List<? extends GuiEventListener> children() {
      return this.widgets;
   }

   private boolean inside(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   private boolean scrollbarVisible() {
      return this.maxScroll() > 0;
   }

   private int barX() {
      return this.getX() + this.getWidth() - 4;
   }

   private int barHeight() {
      int h = this.getHeight();
      return Math.max(12, Math.min(h, (int)((long)h * (long)h / (long)Math.max(1, this.contentHeight + this.padding))));
   }

   private int barY() {
      int h = this.getHeight();
      int bh = this.barHeight();
      return this.getY() + (int)((float)(h - bh) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
   }

   private boolean overBar(double mx, double my) {
      return this.scrollbarVisible() && mx >= (double)(this.getX() + this.getWidth() - 8) && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.active) {
         this.layout();
         if (button == 0 && this.overBar(mouseX, mouseY)) {
            int by = this.barY();
            int bh = this.barHeight();
            if (mouseY < (double)by || mouseY >= (double)(by + bh)) {
               double f = (mouseY - (double)this.getY() - (double)bh / (double)2.0F) / (double)Math.max(1, this.getHeight() - bh);
               this.snapScroll(f * (double)this.maxScroll());
            }

            this.draggingBar = true;
            this.dragStartY = mouseY;
            this.dragStartScroll = (double)this.scroll.target();
            return true;
         } else if (!this.inside(mouseX, mouseY)) {
            return false;
         } else {
            for(AbstractWidget w : this.widgets) {
               if (this.visibleChild(w) && w.mouseClicked(mouseX, mouseY, button)) {
                  this.setFocused(w);
                  if (button == 0) {
                     this.setDragging(true);
                  }

                  return true;
               }
            }

            this.setFocused((GuiEventListener)null);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingBar = false;
      this.layout();
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingBar) {
         double ratio = (double)this.maxScroll() / (double)Math.max(1, this.getHeight() - this.barHeight());
         this.snapScroll(this.dragStartScroll + (mouseY - this.dragStartY) * ratio);
         return true;
      } else {
         this.layout();
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.visible && this.inside(mouseX, mouseY)) {
         this.layout();

         for(AbstractWidget w : this.widgets) {
            if (this.visibleChild(w) && w.isMouseOver(mouseX, mouseY) && w.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
               return true;
            }
         }

         if (!this.scrollbarVisible()) {
            return false;
         } else if (scrollY > (double)0.0F && this.scroll.target() <= 0.0F) {
            return false;
         } else if (scrollY < (double)0.0F && this.scroll.target() >= (float)this.maxScroll()) {
            return false;
         } else {
            this.scrollBy(-scrollY * (double)this.scrollStep);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (!this.scrollbarVisible()) {
         return false;
      } else {
         switch (keyCode) {
            case 266:
               this.scrollBy((double)(-(this.getHeight() - this.scrollStep)));
               break;
            case 267:
               this.scrollBy((double)(this.getHeight() - this.scrollStep));
               break;
            case 268:
               if (this.getFocused() != null) {
                  return false;
               }

               this.scrollTo((double)0.0F);
               break;
            case 269:
               if (this.getFocused() != null) {
                  return false;
               }

               this.scrollTo((double)this.maxScroll());
               break;
            default:
               return false;
         }

         return true;
      }
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.inside(mouseX, mouseY);
   }

   public void setFocused(GuiEventListener listener) {
      super.setFocused(listener);
      if (listener instanceof AbstractWidget w) {
         this.ensureVisible(w);
      }

   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      this.layout();
      boolean inWorld = Minecraft.getInstance().level != null;
      if (this.drawBackground) {
         if (t.isVanilla()) {
            CFHGuiDraw.vanillaListBackground(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), inWorld);
         } else {
            CFHGuiDraw.panel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), t.panel(p.bg2()), p.border());
         }
      }

      boolean over = this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
      this.barHover = over && this.overBar((double)mouseX, (double)mouseY);
      int mx = over && !this.barHover ? mouseX : -9999;
      int my = over && !this.barHover ? mouseY : -9999;
      CFHGuiDraw.scissor(g, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      if (this.drawBackground) {
         Theme.enterPanel();
      }

      try {
         for(AbstractWidget w : this.widgets) {
            if (this.visibleChild(w)) {
               w.render(g, mx, my, partialTick);
            }
         }
      } finally {
         if (this.drawBackground) {
            Theme.exitPanel();
         }

      }

      CFHGuiDraw.unscissor(g);
      if (this.edgeFades && !t.isVanilla() && this.scrollbarVisible()) {
         int fadeH = 10;
         int s = Math.round(this.scroll.get());
         int shade = Colors.withAlpha(p.bg(), 144);
         if (s > 0) {
            CFHGuiDraw.vgradient(g, this.getX(), this.getY(), this.getWidth() - (this.scrollbarVisible() ? 8 : 0), 10, shade, 0);
         }

         if (s < this.maxScroll()) {
            CFHGuiDraw.vgradient(g, this.getX(), this.getY() + this.getHeight() - 10, this.getWidth() - (this.scrollbarVisible() ? 8 : 0), 10, 0, shade);
         }
      }

      if (this.scrollbarVisible()) {
         this.drawScrollbar(g, p);
      }

   }

   private void drawScrollbar(GuiGraphics g, Palette p) {
      int x = this.barX();
      int y = this.getY();
      int h = this.getHeight();
      int barH = this.barHeight();
      int barY = this.barY();
      if (Theme.current().isVanilla()) {
         g.fill(x, y, x + 4, y + h, -16777216);
         g.fill(x, barY, x + 4, barY + barH, !this.draggingBar && !this.barHover ? -8355712 : -6250336);
         g.fill(x, barY, x + 4 - 1, barY + barH - 1, !this.draggingBar && !this.barHover ? -4144960 : -2039584);
      } else {
         CFHGuiDraw.pixelRound(g, x, y, 4, h, Colors.withAlpha(p.surface(), 128), 1);
         int thumb = this.draggingBar ? p.accent() : (this.barHover ? p.textMuted() : p.borderStrong());
         CFHGuiDraw.pixelRound(g, x, barY, 4, barH, thumb, 1);
      }

   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
   }

   private static record Child(AbstractWidget widget, int relX, int relY) {
   }
}
