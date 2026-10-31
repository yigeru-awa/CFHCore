package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CFHGuiCard extends AbstractContainerWidget {
   private final List<Child> children = new ArrayList();
   private final List<AbstractWidget> widgets = new ArrayList();
   protected final Anim hoverAnim;
   protected final Anim enterAnim;
   private long enterStartMs;
   private @Nullable Runnable onClick;
   private @Nullable Runnable onRightClick;
   private boolean selected;
   private boolean flat;
   private int liftPx;
   private int fillOverride;
   private boolean propagateAlpha;

   public CFHGuiCard(int x, int y, int width, int height) {
      super(x, y, width, height, Component.empty());
      this.hoverAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.enterAnim = new Anim(1.0F, 220, Ease.OUT_CUBIC);
      this.liftPx = 1;
      this.propagateAlpha = true;
   }

   public <T extends AbstractWidget> T add(T widget, int relX, int relY) {
      this.children.add(new Child(widget, relX, relY));
      this.widgets.add(widget);
      widget.setX(this.getX() + relX);
      widget.setY(this.getY() + relY);
      return widget;
   }

   public void clearChildren() {
      this.children.clear();
      this.widgets.clear();
      this.setFocused((GuiEventListener)null);
   }

   public List<AbstractWidget> widgets() {
      return this.widgets;
   }

   public CFHGuiCard onClick(Runnable r) {
      this.onClick = r;
      return this;
   }

   public CFHGuiCard onRightClick(Runnable r) {
      this.onRightClick = r;
      return this;
   }

   public CFHGuiCard selected(boolean s) {
      this.selected = s;
      return this;
   }

   public boolean isSelected() {
      return this.selected;
   }

   public CFHGuiCard flat() {
      this.flat = true;
      return this;
   }

   public CFHGuiCard lift(int px) {
      this.liftPx = Math.max(0, px);
      return this;
   }

   public CFHGuiCard fill(int argb) {
      this.fillOverride = argb;
      return this;
   }

   public CFHGuiCard keepChildAlpha() {
      this.propagateAlpha = false;
      return this;
   }

   public void playEntrance(int delayMs) {
      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.enterAnim.snap(1.0F);
         this.enterStartMs = 0L;
      } else {
         this.enterAnim.snap(0.0F);
         this.enterStartMs = Clock.nowMs() + (long)Math.max(0, Math.round((float)delayMs * motion));
      }
   }

   public float hover() {
      return this.hoverAnim.get();
   }

   protected float enterProgress() {
      if (this.enterStartMs != 0L) {
         if (Theme.current().motion() <= 0.0F) {
            this.enterStartMs = 0L;
            this.enterAnim.snap(1.0F);
            return 1.0F;
         }

         if (Clock.nowMs() < this.enterStartMs) {
            return 0.0F;
         }

         this.enterStartMs = 0L;
         this.enterAnim.set(1.0F, 220);
      }

      return Math.min(1.0F, Math.max(0.0F, this.enterAnim.get()));
   }

   private int dy() {
      int lift = this.flat ? 0 : Math.round(-this.hoverAnim.get() * (float)this.liftPx);
      return lift + Math.round((1.0F - this.enterProgress()) * 6.0F);
   }

   private void layout() {
      int dy = this.dy();

      for(Child c : this.children) {
         c.widget.setX(this.getX() + c.relX);
         c.widget.setY(this.getY() + c.relY + dy);
      }

   }

   public List<? extends GuiEventListener> children() {
      return this.widgets;
   }

   private boolean inside(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   private boolean clickable() {
      return this.onClick != null || this.onRightClick != null;
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.inside(mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.active && this.inside(mouseX, mouseY)) {
         this.layout();
         if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
         } else if (button == 0 && this.onClick != null) {
            CFHGuiSounds.click();
            this.onClick.run();
            return true;
         } else if (button == 1 && this.onRightClick != null) {
            this.onRightClick.run();
            return true;
         } else {
            return this.clickable();
         }
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!this.inside(mouseX, mouseY)) {
         return false;
      } else {
         this.layout();
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (!this.active || this.onClick == null || this.getFocused() != null || keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         CFHGuiSounds.click();
         this.onClick.run();
         return true;
      }
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean hov = this.active && this.clickable() && this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
      this.hoverAnim.set(hov && !this.flat);
      float h = this.hoverAnim.get();
      float a = this.alpha * this.enterProgress();
      this.layout();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.dy();
         int w = this.getWidth();
         int hh = this.getHeight();
         if (t.isVanilla()) {
            g.fill(x, y, x + w, y + hh, Colors.scaleAlpha(this.selected ? -1879048192 : 1610612736, a));
            CFHGuiDraw.outline(g, x, y, w, hh, Colors.scaleAlpha(this.selected ? -1 : Colors.lerp(-12566464, -6250336, h), a), 0);
         } else {
            if (!this.flat) {
               CFHGuiDraw.shadow(g, x, y, w, hh, (0.3F + 0.3F * h) * a);
            }

            int fill = this.fillOverride != 0 ? this.fillOverride : t.panel(Colors.lerp(this.selected ? p.surfaceActive() : p.surface(), p.surfaceHover(), h));
            CFHGuiDraw.pixelRound(g, x, y, w, hh, Colors.scaleAlpha(fill, a), t.radius());
            CFHGuiDraw.outline(g, x, y, w, hh, Colors.scaleAlpha(this.selected ? p.accent() : Colors.lerp(p.border(), p.borderStrong(), h), a), t.radius());
            if (this.isFocused() && this.getFocused() == null && this.clickable()) {
               CFHGuiDraw.focusRing(g, x, y, w, hh, a);
            }
         }

         Theme.enterPanel();

         try {
            this.renderContent(g, x, y, w, hh, mouseX, mouseY, partialTick);

            for(AbstractWidget wdg : this.widgets) {
               if (wdg.visible) {
                  if (this.propagateAlpha) {
                     wdg.setAlpha(a);
                  }

                  wdg.render(g, mouseX, mouseY, partialTick);
               }
            }

            this.renderOverlay(g, x, y, w, hh, mouseX, mouseY, partialTick);
         } finally {
            Theme.exitPanel();
         }

      }
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
   }

   protected void renderOverlay(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
   }

   private static record Child(AbstractWidget widget, int relX, int relY) {
   }
}
