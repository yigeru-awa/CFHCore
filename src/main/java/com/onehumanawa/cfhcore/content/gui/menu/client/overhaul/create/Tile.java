package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class Tile extends AbstractContainerWidget {
   static final int HEIGHT = 42;
   private static final int LINE = 20;
   private static final int PAD = 8;
   private final List<AbstractWidget> children = new ArrayList();
   private final AbstractWidget control;
   private @Nullable AbstractWidget extra;
   private @Nullable Supplier<Component> note;
   private @Nullable Supplier<Component> tip;
   private @Nullable Supplier<Boolean> enabled;
   private final Anim hover;
   private final Anim enter;
   private long enterStartMs;

   Tile(int width, Component label, AbstractWidget control) {
      super(0, 0, width, 42, label);
      this.hover = new Anim(0.0F, 150, Ease.OUT_CUBIC);
      this.enter = new Anim(1.0F, 220, Ease.OUT_CUBIC);
      this.control = control;
      this.children.add(control);
   }

   Tile extra(AbstractWidget widget) {
      this.extra = widget;
      this.children.add(widget);
      return this;
   }

   Tile note(Supplier<Component> text) {
      this.note = text;
      return this;
   }

   Tile tip(Supplier<Component> text) {
      this.tip = text;
      return this;
   }

   Tile tip(Component text) {
      this.tip = () -> text;
      return this;
   }

   Tile enabled(Supplier<Boolean> on) {
      this.enabled = on;
      return this;
   }

   AbstractWidget control() {
      return this.control;
   }

   void playEntrance(int delayMs) {
      for(AbstractWidget c : this.children) {
         if (c instanceof CFHGuiWidget w) {
            w.playEntrance(delayMs);
         }
      }

      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.enter.snap(1.0F);
         this.enterStartMs = 0L;
      } else {
         this.enter.snap(0.0F);
         this.enterStartMs = Clock.nowMs() + (long)Math.max(0, Math.round((float)delayMs * motion));
      }
   }

   private float enterProgress() {
      if (this.enterStartMs != 0L) {
         if (Theme.current().motion() <= 0.0F) {
            this.enterStartMs = 0L;
            this.enter.snap(1.0F);
            return 1.0F;
         }

         if (Clock.nowMs() < this.enterStartMs) {
            return 0.0F;
         }

         this.enterStartMs = 0L;
         this.enter.set(1.0F, 220);
      }

      return Math.min(1.0F, Math.max(0.0F, this.enter.get()));
   }

   private void layout() {
      int x = this.getX() + 8;
      int y = this.getY() + 20 - 1 + (20 - this.control.getHeight()) / 2;
      int w = this.getWidth() - 16;
      if (this.extra != null) {
         w -= this.extra.getWidth() + 4;
         this.extra.setX(x + w + 4);
         this.extra.setY(y + (this.control.getHeight() - this.extra.getHeight()) / 2);
      }

      if (this.control.getWidth() != w && this.resizable()) {
         this.control.setWidth(w);
      }

      this.control.setX(x);
      this.control.setY(y);
      boolean on = this.enabled == null || Boolean.TRUE.equals(this.enabled.get());
      this.control.active = on;
      if (this.extra != null) {
         this.extra.active = on;
      }

   }

   private boolean resizable() {
      return !(this.control instanceof CFHGuiToggle);
   }

   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   private boolean inside(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.inside(mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.inside(mouseX, mouseY)) {
         this.layout();

         for(AbstractWidget c : this.children) {
            if (c.mouseClicked(mouseX, mouseY, button)) {
               this.setFocused(c);
               if (button == 0) {
                  this.setDragging(true);
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!this.inside(mouseX, mouseY)) {
         return false;
      } else {
         this.layout();

         for(AbstractWidget c : this.children) {
            if (c.isMouseOver(mouseX, mouseY) && c.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
               return true;
            }
         }

         return false;
      }
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      this.layout();
      float ea = this.enterProgress();
      if (!(ea <= 0.01F)) {
         int x = this.getX();
         int w = this.getWidth();
         int h = this.getHeight();
         int y = this.getY() + Math.round((1.0F - ea) * 6.0F);
         boolean hov = this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
         this.hover.set(hov);
         float hv = this.hover.get();
         if (van) {
            g.fill(x, y, x + w, y + h, Colors.scaleAlpha(Colors.lerp(1342177280, 1881153568, hv), ea));
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-16777216, -1, hv), ea), 0);
         } else {
            CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(Colors.lerp(p.surface(), p.surfaceHover(), hv)), ea), t.radius());
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), Colors.withAlpha(p.accent(), 160), hv), ea), t.radius());
         }

         boolean on = this.control.active;
         int fg = van ? (on ? -1 : -6250336) : (on ? p.text() : p.textDim());
         if (ea > 0.03F) {
            Font font = CFHGuiDraw.font();
            int ty = y + 5 + 2;
            Component said = this.note == null ? null : (Component)this.note.get();
            int noteW = said != null && !said.getString().isEmpty() ? Math.min(font.width(said), Math.max(0, w - 16 - font.width(this.getMessage()) - 10)) : 0;
            g.drawString(font, CFHGuiDraw.truncate(this.getMessage(), w - 16 - (noteW > 0 ? noteW + 8 : 0)), x + 8, ty, Colors.scaleAlpha(fg, ea), van);
            if (noteW > 24 && said != null) {
               FormattedCharSequence cut = CFHGuiDraw.truncate(said, noteW);
               g.drawString(font, cut, x + w - 8 - font.width(cut), ty, Colors.scaleAlpha(van ? -6250336 : p.textDim(), ea), van);
            }
         }

         Theme.enterPanel();

         try {
            for(AbstractWidget c : this.children) {
               if (c.visible) {
                  c.render(g, mouseX, mouseY, partialTick);
               }
            }
         } finally {
            Theme.exitPanel();
         }

         if (hov && this.tip != null && mouseY < y + 20) {
            Component said = (Component)this.tip.get();
            if (said != null && !said.getString().isEmpty()) {
               List<Component> lines = new ArrayList();

               for(FormattedText line : CFHGuiDraw.font().getSplitter().splitLines(said, 190, Style.EMPTY)) {
                  lines.add(Component.literal(line.getString()));
               }

               CFHGuiTooltips.request(lines, this);
            }
         }

      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, this.getMessage());
   }
}
