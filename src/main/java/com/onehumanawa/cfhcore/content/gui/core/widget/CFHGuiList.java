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
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CFHGuiList<T> extends CFHGuiWidget {
   public static final int BAR_W = 4;
   public static final int BAR_HIT = 8;
   private final List<T> items = new ArrayList();
   private int rowHeight;
   private int gap = 2;
   private final RowRenderer<T> renderer;
   private final Anim scroll;
   private int selected;
   private @Nullable Consumer<T> onSelect;
   private @Nullable Consumer<T> onActivate;
   private @Nullable Consumer<T> onRightClick;
   private long lastClickMs;
   private int lastClickIndex;
   private boolean draggingBar;
   private boolean barHover;
   private double dragStartY;
   private double dragStartScroll;
   private @Nullable Component emptyText;
   private boolean rowBackgrounds;

   public CFHGuiList(int x, int y, int width, int height, int rowHeight, RowRenderer<T> renderer) {
      super(x, y, width, height, Component.empty());
      this.scroll = new Anim(0.0F, 180, Ease.OUT_CUBIC);
      this.selected = -1;
      this.lastClickIndex = -1;
      this.rowBackgrounds = true;
      this.rowHeight = rowHeight;
      this.renderer = renderer;
   }

   public CFHGuiList<T> items(List<T> newItems) {
      this.items.clear();
      this.items.addAll(newItems);
      if (this.selected >= this.items.size()) {
         this.selected = -1;
      }

      this.scroll.snap(Math.min(this.scroll.target(), (float)this.maxScroll()));
      return this;
   }

   public List<T> items() {
      return this.items;
   }

   public CFHGuiList<T> gap(int px) {
      this.gap = px;
      return this;
   }

   public CFHGuiList<T> rowHeight(int px) {
      this.rowHeight = px;
      return this;
   }

   public int rowHeight() {
      return this.rowHeight;
   }

   public CFHGuiList<T> onSelect(Consumer<T> c) {
      this.onSelect = c;
      return this;
   }

   public CFHGuiList<T> onActivate(Consumer<T> c) {
      this.onActivate = c;
      return this;
   }

   public CFHGuiList<T> onRightClick(Consumer<T> c) {
      this.onRightClick = c;
      return this;
   }

   public CFHGuiList<T> emptyText(Component text) {
      this.emptyText = text;
      return this;
   }

   public CFHGuiList<T> plainRows() {
      this.rowBackgrounds = false;
      return this;
   }

   public @Nullable T selectedItem() {
      return (T)(this.selected >= 0 && this.selected < this.items.size() ? this.items.get(this.selected) : null);
   }

   public int selectedIndex() {
      return this.selected;
   }

   public void select(int index) {
      this.selected = index >= 0 && index < this.items.size() ? index : -1;
      if (this.selected >= 0) {
         this.ensureVisible(this.selected);
         if (this.onSelect != null) {
            this.onSelect.accept(this.items.get(this.selected));
         }
      }

   }

   public void select(T item) {
      this.select(this.items.indexOf(item));
   }

   public void clearSelection() {
      this.selected = -1;
   }

   private int stride() {
      return this.rowHeight + this.gap;
   }

   private int contentHeight() {
      return this.items.isEmpty() ? 0 : this.items.size() * this.stride() - this.gap;
   }

   public int maxScroll() {
      return Math.max(0, this.contentHeight() - this.getHeight());
   }

   public double scrollAmount() {
      return (double)this.scroll.get();
   }

   private boolean scrollbarVisible() {
      return this.maxScroll() > 0;
   }

   public int rowsWidth() {
      return this.scrollbarVisible() ? this.getWidth() - 8 : this.getWidth();
   }

   public void scrollTo(double y) {
      this.scroll.set((float)Math.max((double)0.0F, Math.min((double)this.maxScroll(), y)));
   }

   public void snapScroll(double y) {
      this.scroll.snap((float)Math.max((double)0.0F, Math.min((double)this.maxScroll(), y)));
   }

   public void ensureVisible(int index) {
      int top = index * this.stride();
      double s = (double)this.scroll.target();
      if ((double)top < s) {
         this.scrollTo((double)top);
      } else if ((double)(top + this.rowHeight) > s + (double)this.getHeight()) {
         this.scrollTo((double)(top + this.rowHeight - this.getHeight()));
      }

   }

   private int indexAt(double mouseY) {
      if (!this.contains((double)(this.getX() + 1), mouseY)) {
         return -1;
      } else {
         int rel = (int)(mouseY - (double)this.getY() + (double)this.scroll.get());
         int i = rel / this.stride();
         if (i >= 0 && i < this.items.size()) {
            return rel - i * this.stride() < this.rowHeight ? i : -1;
         } else {
            return -1;
         }
      }
   }

   private int rowY(int i) {
      return this.getY() + i * this.stride() - Math.round(this.scroll.get());
   }

   private int barHeight() {
      int h = this.getHeight();
      return Math.max(12, Math.min(h, (int)((long)h * (long)h / (long)Math.max(1, this.contentHeight()))));
   }

   private int barY() {
      return this.getY() + (int)((float)(this.getHeight() - this.barHeight()) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
   }

   private boolean overBar(double mx, double my) {
      return this.scrollbarVisible() && mx >= (double)(this.getX() + this.getWidth() - 8) && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.active && this.visible && this.contains(mouseX, mouseY)) {
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
         } else {
            int i = this.indexAt(mouseY);
            if (i < 0) {
               return true;
            } else if (this.renderer.click(this.items.get(i), i, this.getX(), this.rowY(i), this.rowsWidth(), this.rowHeight, mouseX, mouseY, button)) {
               return true;
            } else if (button == 1) {
               if (this.selected != i) {
                  this.selected = i;
                  if (this.onSelect != null) {
                     this.onSelect.accept(this.items.get(i));
                  }
               }

               if (this.onRightClick != null) {
                  this.onRightClick.accept(this.items.get(i));
               }

               return true;
            } else if (button != 0) {
               return true;
            } else {
               long now = Clock.nowMs();
               boolean dbl = i == this.lastClickIndex && now - this.lastClickMs < 350L;
               this.lastClickMs = now;
               this.lastClickIndex = i;
               if (this.selected != i) {
                  this.selected = i;
                  CFHGuiSounds.tick();
                  if (this.onSelect != null) {
                     this.onSelect.accept(this.items.get(i));
                  }
               }

               if (dbl && this.onActivate != null) {
                  this.onActivate.accept(this.items.get(i));
                  this.lastClickIndex = -1;
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingBar = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingBar) {
         double ratio = (double)this.maxScroll() / (double)Math.max(1, this.getHeight() - this.barHeight());
         this.snapScroll(this.dragStartScroll + (mouseY - this.dragStartY) * ratio);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.contains(mouseX, mouseY) && this.scrollbarVisible()) {
         if (scrollY > (double)0.0F && this.scroll.target() <= 0.0F) {
            return false;
         } else if (scrollY < (double)0.0F && this.scroll.target() >= (float)this.maxScroll()) {
            return false;
         } else {
            this.scrollTo((double)this.scroll.target() - scrollY * (double)this.stride());
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible && !this.items.isEmpty()) {
         int page = Math.max(1, this.getHeight() / this.stride());
         switch (keyCode) {
            case 257:
            case 335:
               if (this.selected >= 0 && this.onActivate != null) {
                  this.onActivate.accept(this.items.get(this.selected));
                  return true;
               }
            default:
               return false;
            case 264:
               this.select(Math.min(this.items.size() - 1, this.selected + 1));
               return true;
            case 265:
               this.select(Math.max(0, this.selected - 1));
               return true;
            case 266:
               this.select(Math.max(0, (this.selected < 0 ? 0 : this.selected) - page));
               return true;
            case 267:
               this.select(Math.min(this.items.size() - 1, (this.selected < 0 ? 0 : this.selected) + page));
               return true;
            case 268:
               this.select(0);
               return true;
            case 269:
               this.select(this.items.size() - 1);
               return true;
         }
      } else {
         return false;
      }
   }

   public void playDownSound(SoundManager handler) {
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int x = this.getX();
      int y = this.getY();
      int w = this.rowsWidth();
      int h = this.getHeight();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         if (this.items.isEmpty()) {
            if (this.emptyText != null) {
               CFHGuiDraw.textCentered(g, this.emptyText, x + this.getWidth() / 2, y + h / 2 - 4, Colors.scaleAlpha(p.textDim(), a), t.isVanilla());
            }

         } else {
            boolean over = this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
            this.barHover = over && this.overBar((double)mouseX, (double)mouseY);
            int s = Math.round(this.scroll.get());
            int hoverIdx = over && !this.barHover ? this.indexAt((double)mouseY) : -1;
            CFHGuiDraw.scissor(g, x, y, this.getWidth(), h);
            int first = Math.max(0, s / this.stride());
            int last = Math.min(this.items.size() - 1, (s + h) / this.stride() + 1);

            for(int i = first; i <= last; ++i) {
               int ry = y + i * this.stride() - s;
               boolean hov = i == hoverIdx;
               boolean sel = i == this.selected;
               if (this.rowBackgrounds) {
                  if (t.isVanilla()) {
                     if (hov || sel) {
                        g.fill(x, ry, x + w, ry + this.rowHeight, Colors.scaleAlpha(sel ? 1073741824 : 553648127, a));
                     }

                     if (sel) {
                        CFHGuiDraw.outline(g, x, ry, w, this.rowHeight, Colors.scaleAlpha(this.isFocused() ? -1 : -8355712, a), 0);
                     }
                  } else {
                     int fill = sel ? p.surfaceActive() : (hov ? p.surfaceHover() : 0);
                     if (fill != 0) {
                        CFHGuiDraw.pixelRound(g, x, ry, w, this.rowHeight, Colors.scaleAlpha(t.panel(fill), a), t.radius());
                     }

                     if (sel) {
                        CFHGuiDraw.rect(g, x, ry + 2, 2, this.rowHeight - 4, Colors.scaleAlpha(p.accent(), a));
                        if (this.isFocused()) {
                           CFHGuiDraw.outline(g, x, ry, w, this.rowHeight, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 128), a), t.radius());
                        }
                     }
                  }
               }

               this.renderer.render(g, this.items.get(i), i, x, ry, w, this.rowHeight, hov, sel, mouseX, mouseY);
            }

            CFHGuiDraw.unscissor(g);
            if (this.scrollbarVisible()) {
               int bx = x + this.getWidth() - 4;
               int barH = this.barHeight();
               int barY = this.barY();
               if (t.isVanilla()) {
                  g.fill(bx, y, bx + 4, y + h, -16777216);
                  g.fill(bx, barY, bx + 4, barY + barH, !this.draggingBar && !this.barHover ? -8355712 : -6250336);
                  g.fill(bx, barY, bx + 4 - 1, barY + barH - 1, !this.draggingBar && !this.barHover ? -4144960 : -2039584);
               } else {
                  CFHGuiDraw.pixelRound(g, bx, y, 4, h, Colors.withAlpha(p.surface(), 128), 1);
                  CFHGuiDraw.pixelRound(g, bx, barY, 4, barH, this.draggingBar ? p.accent() : (this.barHover ? p.textMuted() : p.borderStrong()), 1);
               }
            }

         }
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      if (this.selected >= 0 && this.selected < this.items.size()) {
         out.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", new Object[]{this.selected + 1, this.items.size()}));
      }

   }

   @FunctionalInterface
   public interface RowRenderer<T> {
      void render(GuiGraphics var1, T var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, int var10, int var11);

      default boolean click(T item, int index, int x, int y, int w, int h, double mouseX, double mouseY, int button) {
         return false;
      }
   }
}
