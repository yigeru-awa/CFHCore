package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public class CFHGuiTabStrip extends CFHGuiWidget {
   public static final int HEIGHT = 22;
   public static final int PILL_HEIGHT = 16;
   private static final int ARROW_W = 14;
   private static final int MORE_W = 16;
   private static final int PILL_GAP = 4;
   private final List<Tab> tabs = new ArrayList();
   private final List<Anim> tabHover = new ArrayList();
   private int index;
   private final @Nullable IntConsumer onChange;
   private Style style;
   private final Anim slide;
   private final Anim scroll;
   private final Anim leftHover;
   private final Anim rightHover;
   private final Anim moreHover;
   private static final int FADE_W = 14;
   private static final int PLATE_PAD = 2;

   public CFHGuiTabStrip(int x, int y, int width, List<Tab> tabs, int selected, @Nullable IntConsumer onChange) {
      super(x, y, width, 22, Component.empty());
      this.style = CFHGuiTabStrip.Style.UNDERLINE;
      this.slide = new Anim(0.0F, 220, Ease.OUT_CUBIC);
      this.scroll = new Anim(0.0F, 220, Ease.OUT_CUBIC);
      this.leftHover = new Anim(0.0F, 120, Ease.OUT_CUBIC);
      this.rightHover = new Anim(0.0F, 120, Ease.OUT_CUBIC);
      this.moreHover = new Anim(0.0F, 120, Ease.OUT_CUBIC);
      this.tabs.addAll(tabs);

      for(int i = 0; i < tabs.size(); ++i) {
         this.tabHover.add(new Anim(0.0F, 140, Ease.OUT_CUBIC));
      }

      this.index = Math.max(0, Math.min(tabs.size() - 1, selected));
      this.onChange = onChange;
      this.slide.snap((float)this.index);
      this.ensureVisible(this.index, false);
   }

   public CFHGuiTabStrip style(Style s) {
      this.style = s;
      this.height = s == CFHGuiTabStrip.Style.PILLS ? 16 : 22;
      this.ensureVisible(this.index, false);
      return this;
   }

   public Style style() {
      return this.style;
   }

   public int index() {
      return this.index;
   }

   public List<Tab> tabs() {
      return this.tabs;
   }

   public void setBadge(int i, int count) {
      if (i >= 0 && i < this.tabs.size()) {
         Tab t = (Tab)this.tabs.get(i);
         this.tabs.set(i, new Tab(t.label(), t.icon(), count));
      }
   }

   public void select(int i) {
      if (i >= 0 && i < this.tabs.size() && i != this.index) {
         this.setIndex(i);
         CFHGuiSounds.tick();
         if (this.onChange != null) {
            this.onChange.accept(i);
         }

      }
   }

   public void setIndex(int i) {
      if (i >= 0 && i < this.tabs.size()) {
         this.index = i;
         this.slide.set((float)i);
         this.ensureVisible(i, true);
      }
   }

   public CFHGuiTabStrip animateFrom(int fromIndex, double fromScroll) {
      if (fromIndex >= 0 && fromIndex < this.tabs.size()) {
         this.slide.snap((float)fromIndex);
      }

      this.slide.set((float)this.index);
      this.scroll.snap((float)this.clampScroll(fromScroll));
      this.ensureVisible(this.index, true);
      return this;
   }

   public double scrollOffset() {
      return (double)this.scroll.target();
   }

   public int contentWidth() {
      int w = 0;

      for(int i = 0; i < this.tabs.size(); ++i) {
         w += this.tabW(i) + (i > 0 ? this.gap() : 0);
      }

      return w;
   }

   public boolean overflows() {
      return this.contentWidth() > this.getWidth();
   }

   private int gap() {
      return this.style == CFHGuiTabStrip.Style.PILLS ? 4 : 0;
   }

   private static int badgeW(int badge) {
      return badge <= 0 ? 0 : Math.max(10, CFHGuiDraw.width(badge > 99 ? "99+" : Integer.toString(badge)) + 6) + 4;
   }

   private int tabW(int i) {
      Tab t = (Tab)this.tabs.get(i);
      boolean pills = this.style == CFHGuiTabStrip.Style.PILLS;
      return CFHGuiDraw.width(t.label()) + (pills ? 14 : 18) + (t.icon() != null ? (pills ? 12 : 14) : 0) + badgeW(t.badge());
   }

   private int tabCx(int i) {
      int x = 0;

      for(int k = 0; k < i; ++k) {
         x += this.tabW(k) + this.gap();
      }

      return x;
   }

   private int viewX0() {
      return this.getX() + (this.overflows() ? 16 : 0);
   }

   private int viewX1() {
      return this.getX() + this.getWidth() - (this.overflows() ? 34 : 0);
   }

   private int viewW() {
      return Math.max(1, this.viewX1() - this.viewX0());
   }

   private int maxScroll() {
      return Math.max(0, this.contentWidth() - this.viewW());
   }

   private double clampScroll(double s) {
      return Math.max((double)0.0F, Math.min((double)this.maxScroll(), s));
   }

   private int tabX(int i) {
      return this.viewX0() + this.tabCx(i) - Math.round(this.scroll.get());
   }

   private int rightArrowX() {
      return this.viewX1() + 2;
   }

   private int moreX() {
      return this.getX() + this.getWidth() - 16;
   }

   private void ensureVisible(int i, boolean animate) {
      if (i >= 0 && i < this.tabs.size()) {
         int cx = this.tabCx(i);
         int w = this.tabW(i);
         int vw = this.viewW();
         double s = (double)this.scroll.target();
         if ((double)(cx - 8) < s) {
            s = (double)(cx - 8);
         } else if ((double)(cx + w + 8) > s + (double)vw) {
            s = (double)(cx + w + 8 - vw);
         }

         s = this.clampScroll(s);
         if (animate) {
            this.scroll.set((float)s);
         } else {
            this.scroll.snap((float)s);
         }

      }
   }

   private int tabAt(double mx, double my) {
      if (!(my < (double)this.getY()) && !(my >= (double)(this.getY() + this.getHeight())) && !(mx < (double)this.viewX0()) && !(mx >= (double)this.viewX1())) {
         for(int i = 0; i < this.tabs.size(); ++i) {
            int tx = this.tabX(i);
            if (mx >= (double)tx && mx < (double)(tx + this.tabW(i))) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean overLeft(double mx) {
      return this.overflows() && mx >= (double)this.getX() && mx < (double)(this.getX() + 14);
   }

   private boolean overRight(double mx) {
      return this.overflows() && mx >= (double)this.rightArrowX() && mx < (double)(this.rightArrowX() + 14);
   }

   private boolean overMore(double mx) {
      return this.overflows() && mx >= (double)this.moreX() && mx < (double)(this.moreX() + 16);
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      if (this.overLeft(mouseX)) {
         this.scrollPage(-1);
      } else if (this.overRight(mouseX)) {
         this.scrollPage(1);
      } else if (this.overMore(mouseX)) {
         this.openMore();
      } else {
         this.select(this.tabAt(mouseX, mouseY));
      }
   }

   private void scrollPage(int dir) {
      double to = this.clampScroll((double)this.scroll.target() + (double)dir * Math.max((double)40.0F, (double)this.viewW() * 0.6));
      if (to != (double)this.scroll.target()) {
         this.scroll.set((float)to);
         CFHGuiSounds.tick();
      }
   }

   private void openMore() {
      List<MenuPopup.Item> items = new ArrayList();

      for(int i = 0; i < this.tabs.size(); ++i) {
         Tab t = (Tab)this.tabs.get(i);
         int at = i;
         items.add(MenuPopup.Item.checked(t.label(), i == this.index, () -> this.select(at)));
      }

      CFHGuiSounds.tick();
      Popups.open(new MenuPopup(Math.max(2, this.getX() + this.getWidth() - 130), this.getY() + this.getHeight() + 1, items, 130));
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.visible && this.active && this.overflows() && this.isMouseOver(mouseX, mouseY)) {
         double d = scrollX != (double)0.0F ? scrollX : scrollY;
         this.scroll.set((float)this.clampScroll((double)this.scroll.target() - d * (double)28.0F));
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible && !this.tabs.isEmpty()) {
         switch (keyCode) {
            case 262:
               this.select(Math.min(this.tabs.size() - 1, this.index + 1));
               break;
            case 263:
               this.select(Math.max(0, this.index - 1));
               break;
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               return false;
            case 268:
               this.select(0);
               break;
            case 269:
               this.select(this.tabs.size() - 1);
         }

         return true;
      } else {
         return false;
      }
   }

   private int hoveredTab(int mouseX, int mouseY, GuiGraphics g) {
      return this.isHovered() && this.active && g.containsPointInScissor(mouseX, mouseY) ? this.tabAt((double)mouseX, (double)mouseY) : -1;
   }

   private int[] indicator() {
      float s = Math.max(0.0F, Math.min((float)(this.tabs.size() - 1), this.slide.get()));
      int i0 = (int)Math.floor((double)s);
      int i1 = Math.min(this.tabs.size() - 1, i0 + 1);
      float f = s - (float)i0;
      int x = Math.round((float)this.tabX(i0) + (float)(this.tabX(i1) - this.tabX(i0)) * f);
      int w = Math.round((float)this.tabW(i0) + (float)(this.tabW(i1) - this.tabW(i0)) * f);
      return new int[]{x, w};
   }

   private void drawTab(GuiGraphics g, Tab tab, int tx, int tw, int y, int h, int fg, int iconColor, boolean shadow) {
      boolean pills = this.style == CFHGuiTabStrip.Style.PILLS;
      int iconSize = pills ? 8 : 10;
      int iconW = tab.icon() != null ? iconSize + 4 : 0;
      int badgeW = badgeW(tab.badge());
      FormattedCharSequence label = CFHGuiDraw.truncate(tab.label(), Math.max(0, tw - 8 - iconW - badgeW));
      int labelW = CFHGuiDraw.width(label);
      int cx = tx + (tw - (iconW + labelW + badgeW)) / 2;
      if (tab.icon() != null) {
         Icons.draw(g, tab.icon(), cx, y + (h - iconSize) / 2, iconSize, iconColor);
         cx += iconW;
      }

      g.drawString(CFHGuiDraw.font(), label, cx, CFHGuiDraw.textY(y, h), fg, shadow);
      cx += labelW;
      if (tab.badge() > 0) {
         CFHGuiBadge.drawCount(g, tab.badge(), cx + 4, y + (h - 10) / 2);
      }

   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F) && !this.tabs.isEmpty()) {
         Theme t = Theme.current();
         Palette p = t.palette();
         int y = this.getY() + this.enterOffset();
         int h = this.getHeight();
         int hov = this.hoveredTab(mouseX, mouseY, g);
         boolean pills = this.style == CFHGuiTabStrip.Style.PILLS;
         int plate = Colors.scaleAlpha(t.chrome(p.bg2()), a);
         if (pills) {
            int trackW = this.overflows() ? this.getWidth() : Math.min(this.getWidth(), this.contentWidth());
            CFHGuiDraw.pixelRound(g, this.getX() - 2, y - 2, trackW + 4, h + 4, plate, t.radius());
         } else {
            CFHGuiDraw.rect(g, this.getX(), y, this.getWidth(), h, plate);
         }

         if (!pills) {
            CFHGuiDraw.hline(g, this.getX(), y + h - 1, this.getWidth(), Colors.scaleAlpha(p.border(), a));
         }

         CFHGuiDraw.scissor(g, this.viewX0(), y - 1, this.viewW(), h + 2);
         if (pills) {
            int[] ind = this.indicator();
            int fill = Colors.lerp(p.surfaceActive(), p.accent(), 0.2F);

            for(int i = 0; i < this.tabs.size(); ++i) {
               Anim ha = (Anim)this.tabHover.get(i);
               ha.set(i == hov && i != this.index);
               float hv = ha.get();
               int tx = this.tabX(i);
               int tw = this.tabW(i);
               if (tx + tw >= this.viewX0() && tx <= this.viewX1() && hv > 0.01F) {
                  CFHGuiDraw.pixelRound(g, tx, y, tw, h, Colors.scaleAlpha(p.surfaceHover(), a * hv), t.radius());
               }
            }

            CFHGuiDraw.pixelRound(g, ind[0], y, ind[1], h, Colors.scaleAlpha(fill, a), t.radius());
            CFHGuiDraw.outline(g, ind[0], y, ind[1], h, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 144), a), t.radius());

            for(int i = 0; i < this.tabs.size(); ++i) {
               int tx = this.tabX(i);
               int tw = this.tabW(i);
               if (tx + tw >= this.viewX0() && tx <= this.viewX1()) {
                  float hv = ((Anim)this.tabHover.get(i)).get();
                  int fg = !this.active ? p.textDim() : (i == this.index ? p.text() : Colors.lerp(p.textMuted(), p.text(), hv));
                  this.drawTab(g, (Tab)this.tabs.get(i), tx, tw, y, h, Colors.scaleAlpha(fg, a), Colors.scaleAlpha(i == this.index ? p.accent() : fg, a), false);
               }
            }

            CFHGuiDraw.focusRing(g, this.tabX(this.index), y, this.tabW(this.index), h, this.focus() * a);
         } else {
            for(int i = 0; i < this.tabs.size(); ++i) {
               Anim ha = (Anim)this.tabHover.get(i);
               ha.set(i == hov && i != this.index);
               float hv = ha.get();
               int tx = this.tabX(i);
               int tw = this.tabW(i);
               if (tx + tw >= this.viewX0() && tx <= this.viewX1()) {
                  if (hv > 0.01F) {
                     CFHGuiDraw.pixelRound(g, tx + 2, y + 2, tw - 4, h - 6, Colors.scaleAlpha(p.surfaceHover(), a * hv), t.radius());
                  }

                  int fg = !this.active ? p.textDim() : (i == this.index ? p.text() : Colors.lerp(p.textDim(), p.textMuted(), Math.max(hv, 0.35F)));
                  this.drawTab(g, (Tab)this.tabs.get(i), tx, tw, y, h - 2, Colors.scaleAlpha(fg, a), Colors.scaleAlpha(i == this.index ? p.accent() : fg, a), false);
               }
            }

            int[] ind = this.indicator();
            CFHGuiDraw.rect(g, ind[0] + 5, y + h - 2, Math.max(2, ind[1] - 10), 2, Colors.scaleAlpha(this.active ? p.accent() : p.textDim(), a));
            CFHGuiDraw.focusRing(g, this.tabX(this.index) + 1, y + 1, this.tabW(this.index) - 2, h - 4, this.focus() * a);
         }

         CFHGuiDraw.unscissor(g);
         if (this.overflows()) {
            int s = Math.round(this.scroll.get());
            int shade = Colors.scaleAlpha(p.bg2(), a);
            if (s > 0) {
               fadeEdge(g, this.viewX0(), y, h - (pills ? 0 : 1), shade, true);
            }

            if (s < this.maxScroll()) {
               fadeEdge(g, this.viewX1() - 14, y, h - (pills ? 0 : 1), shade, false);
            }

            this.darkButton(g, this.getX(), y, 14, h, Icon.CHEVRON_LEFT, this.leftHover, this.overLeft((double)mouseX) && this.isHovered(), s > 0, a, p, t);
            this.darkButton(g, this.rightArrowX(), y, 14, h, Icon.CHEVRON_RIGHT, this.rightHover, this.overRight((double)mouseX) && this.isHovered(), s < this.maxScroll(), a, p, t);
            this.darkButton(g, this.moreX(), y, 16, h, Icon.CHEVRON_DOWN, this.moreHover, this.overMore((double)mouseX) && this.isHovered(), true, a, p, t);
         }

      }
   }

   private static void fadeEdge(GuiGraphics g, int x, int y, int h, int color, boolean fromLeft) {
      int alpha = color >>> 24 & 255;

      for(int i = 0; i < 14; ++i) {
         float t = 1.0F - ((float)i + 0.5F) / 14.0F;
         int col = Colors.withAlpha(color, Math.round((float)alpha * t * t));
         int cx = fromLeft ? x + i : x + 14 - 1 - i;
         g.fill(cx, y, cx + 1, y + h, col);
      }

   }

   private void darkButton(GuiGraphics g, int x, int y, int w, int h, Icon icon, Anim hoverA, boolean hovered, boolean enabled, float a, Palette p, Theme t) {
      hoverA.set(hovered && enabled);
      float hv = hoverA.get();
      int bh = this.style == CFHGuiTabStrip.Style.PILLS ? h : h - 4;
      int by = this.style == CFHGuiTabStrip.Style.PILLS ? y : y + 1;
      if (hv > 0.01F) {
         CFHGuiDraw.pixelRound(g, x, by, w, bh, Colors.scaleAlpha(p.surfaceHover(), a * hv), t.radius());
      }

      int c = !enabled ? Colors.withAlpha(p.textDim(), 112) : Colors.lerp(p.textMuted(), p.text(), hv);
      Icons.draw(g, icon, x + (w - 8) / 2, by + (bh - 8) / 2, 8, Colors.scaleAlpha(c, a));
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F) && !this.tabs.isEmpty()) {
         int y = this.getY() + this.enterOffset();
         int h = this.getHeight();
         int hov = this.hoveredTab(mouseX, mouseY, g);
         boolean pills = this.style == CFHGuiTabStrip.Style.PILLS;
         if (!pills) {
            g.setColor(1.0F, 1.0F, 1.0F, a);
            CFHGuiDraw.vanillaSeparator(g, this.getX(), y + h - 2, this.getWidth(), true, Minecraft.getInstance().level != null);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         CFHGuiDraw.scissor(g, this.viewX0(), y - 1, this.viewW(), h + 2);

         for(int i = 0; i < this.tabs.size(); ++i) {
            Anim ha = (Anim)this.tabHover.get(i);
            ha.set(i == hov && i != this.index);
            float hv = ha.get();
            int tx = this.tabX(i);
            int tw = this.tabW(i);
            if (tx + tw >= this.viewX0() && tx <= this.viewX1()) {
               boolean sel = i == this.index;
               if (pills) {
                  CFHGuiDraw.vanillaButton(g, tx, y, tw, h, sel ? 1.0F : hv, this.active, a);
                  int fg = !this.active ? -6250336 : (sel ? -96 : Colors.lerp(-2039584, -1, hv));
                  this.drawTab(g, (Tab)this.tabs.get(i), tx, tw, y, h, Colors.scaleAlpha(fg, a), Colors.scaleAlpha(fg, a), true);
                  if (sel) {
                     int lw = Math.min(CFHGuiDraw.width(((Tab)this.tabs.get(i)).label()), tw - 8);
                     CFHGuiDraw.rect(g, tx + (tw - lw) / 2, y + h - 3, lw, 1, Colors.scaleAlpha(-96, a * 0.8F));
                  }
               } else {
                  float lift = sel ? Math.max(0.6F, this.focus()) : hv * 0.6F;
                  CFHGuiDraw.vanillaTab(g, tx, y, tw, h, sel, lift, a);
                  int fg = Colors.scaleAlpha(!this.active ? -6250336 : (sel ? -1 : Colors.lerp(-4144960, -1, hv)), a);
                  if (sel) {
                     this.drawTab(g, (Tab)this.tabs.get(i), tx, tw, y, h, fg, fg, true);
                  } else {
                     this.drawTab(g, (Tab)this.tabs.get(i), tx, tw, y + 6, h - 8, fg, fg, true);
                  }
               }
            }
         }

         if (!pills && this.focus() > 0.5F) {
            int uw = Math.min(CFHGuiDraw.width(((Tab)this.tabs.get(this.index)).label()), this.tabW(this.index) - 4);
            CFHGuiDraw.rect(g, this.tabX(this.index) + (this.tabW(this.index) - uw) / 2, y + h - 4, uw, 1, Colors.scaleAlpha(-1, a));
         }

         CFHGuiDraw.unscissor(g);
         if (this.overflows()) {
            int s = Math.round(this.scroll.get());
            this.vanillaArrow(g, this.getX(), y, 14, h, Icon.CHEVRON_LEFT, this.leftHover, this.overLeft((double)mouseX) && this.isHovered(), s > 0, a);
            this.vanillaArrow(g, this.rightArrowX(), y, 14, h, Icon.CHEVRON_RIGHT, this.rightHover, this.overRight((double)mouseX) && this.isHovered(), s < this.maxScroll(), a);
            this.vanillaArrow(g, this.moreX(), y, 16, h, Icon.CHEVRON_DOWN, this.moreHover, this.overMore((double)mouseX) && this.isHovered(), true, a);
         }

      }
   }

   private void vanillaArrow(GuiGraphics g, int x, int y, int w, int h, Icon icon, Anim hoverA, boolean hovered, boolean enabled, float a) {
      hoverA.set(hovered && enabled);
      int bh = this.style == CFHGuiTabStrip.Style.PILLS ? h : h - 4;
      int by = this.style == CFHGuiTabStrip.Style.PILLS ? y : y + 1;
      CFHGuiDraw.vanillaButton(g, x, by, w, bh, hoverA.get(), enabled, a);
      Icons.draw(g, icon, x + (w - 8) / 2, by + (bh - 8) / 2, 8, Colors.scaleAlpha(enabled ? -1 : -6250336, a));
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      if (!this.tabs.isEmpty()) {
         out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", new Object[]{((Tab)this.tabs.get(this.index)).label()}));
         out.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", new Object[]{this.index + 1, this.tabs.size()}));
         if (this.active) {
            out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.slider.usage.focused" : "narration.slider.usage.hovered"));
         }

      }
   }

   public static enum Style {
      UNDERLINE,
      PILLS;
   }

   public static record Tab(Component label, @Nullable Icon icon, int badge) {
      public Tab(Component label) {
         this(label, (Icon)null, 0);
      }

      public Tab(Component label, Icon icon) {
         this(label, icon, 0);
      }
   }
}
