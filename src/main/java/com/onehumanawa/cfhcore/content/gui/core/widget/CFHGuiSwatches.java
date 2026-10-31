package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class CFHGuiSwatches extends CFHGuiWidget {
   public static final int SIZE = 14;
   public static final int GAP = 4;
   private final List<Palette.AccentPreset> presets;
   private final IntConsumer onPick;
   private int selected = -1;
   private int hoverIdx = -1;
   private int cursor;

   public CFHGuiSwatches(int x, int y, int width, List<Palette.AccentPreset> presets, int selectedColor, IntConsumer onPick) {
      super(x, y, width, 14, Component.translatable("cfhgui.settings.accent_preset"));
      this.presets = new ArrayList(presets);
      this.onPick = onPick;
      this.setSelectedColor(selectedColor);
      this.cursor = Math.max(0, this.selected);
      this.setHeight(this.rows() * 18 - 4);
   }

   private int perRow() {
      return Math.max(1, (this.getWidth() + 4) / 18);
   }

   private int rows() {
      return (this.presets.size() + this.perRow() - 1) / this.perRow();
   }

   public void setSelectedColor(int argb) {
      this.selected = -1;

      for(int i = 0; i < this.presets.size(); ++i) {
         if ((((Palette.AccentPreset)this.presets.get(i)).color() & 16777215) == (argb & 16777215)) {
            this.selected = i;
            break;
         }
      }

      if (this.selected >= 0) {
         this.cursor = this.selected;
      }

   }

   public int selectedIndex() {
      return this.selected;
   }

   private int swatchX(int i) {
      return this.getX() + i % this.perRow() * 18;
   }

   private int swatchY(int i) {
      return this.getY() + i / this.perRow() * 18;
   }

   private int at(double mx, double my) {
      for(int i = 0; i < this.presets.size(); ++i) {
         int sx = this.swatchX(i);
         int sy = this.swatchY(i);
         if (mx >= (double)sx && mx < (double)(sx + 14) && my >= (double)sy && my < (double)(sy + 14)) {
            return i;
         }
      }

      return -1;
   }

   private void pick(int i) {
      if (i >= 0 && i < this.presets.size()) {
         this.cursor = i;
         if (i != this.selected) {
            this.selected = i;
            CFHGuiSounds.tick();
            if (this.onPick != null) {
               this.onPick.accept(((Palette.AccentPreset)this.presets.get(i)).color());
            }

         }
      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.pick(this.at(mouseX, mouseY));
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible && !this.presets.isEmpty()) {
         switch (keyCode) {
            case 32:
            case 257:
            case 335:
               this.pick(this.cursor);
               return true;
            case 262:
               this.cursor = Math.min(this.presets.size() - 1, this.cursor + 1);
               break;
            case 263:
               this.cursor = Math.max(0, this.cursor - 1);
               break;
            case 264:
               this.cursor = Math.min(this.presets.size() - 1, this.cursor + this.perRow());
               break;
            case 265:
               this.cursor = Math.max(0, this.cursor - this.perRow());
               break;
            default:
               return false;
         }

         return true;
      } else {
         return false;
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, true);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int dy = this.enterOffset();
         this.hoverIdx = this.isHovered() && this.active && g.containsPointInScissor(mouseX, mouseY) ? this.at((double)mouseX, (double)mouseY) : -1;
         if (this.hoverIdx >= 0) {
            CFHGuiTooltips.request(Component.literal(((Palette.AccentPreset)this.presets.get(this.hoverIdx)).name()), this);
         }

         int radius = vanilla ? 0 : Math.min(3, t.radius());

         for(int i = 0; i < this.presets.size(); ++i) {
            int sx = this.swatchX(i);
            int sy = this.swatchY(i) + dy;
            int c = ((Palette.AccentPreset)this.presets.get(i)).color();
            boolean sel = i == this.selected;
            boolean hov = i == this.hoverIdx;
            boolean cur = i == this.cursor && this.isFocused();
            int lift = hov ? -1 : 0;
            if (!vanilla && (hov || sel)) {
               CFHGuiDraw.shadow(g, sx, sy + lift, 14, 14, 0.35F * a);
            }

            CFHGuiDraw.pixelRound(g, sx, sy + lift, 14, 14, Colors.scaleAlpha(this.active ? c : Colors.withAlpha(c, 128), a), radius);
            CFHGuiDraw.hline(g, sx + 2, sy + lift + 1, 10, Colors.scaleAlpha(Colors.withAlpha(16777215, 48), a));
            int border = sel ? p.text() : (hov ? Colors.brighten(c, 0.35F) : (vanilla ? -16777216 : Colors.brighten(c, -0.35F)));
            CFHGuiDraw.outline(g, sx, sy + lift, 14, 14, Colors.scaleAlpha(border, a), radius);
            if (sel) {
               Icons.draw(g, Icon.CHECK, sx + 3, sy + lift + 3, 8, Colors.scaleAlpha(Colors.readableOn(c), a));
            }

            if (cur) {
               CFHGuiDraw.focusRing(g, sx, sy + lift, 14, 14, a);
            }
         }

      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, this.getMessage());
      if (this.cursor >= 0 && this.cursor < this.presets.size()) {
         out.add(NarratedElementType.HINT, Component.literal(((Palette.AccentPreset)this.presets.get(this.cursor)).name()));
      }

   }
}
