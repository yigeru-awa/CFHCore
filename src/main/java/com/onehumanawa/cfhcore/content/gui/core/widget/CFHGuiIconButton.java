package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CFHGuiIconButton extends CFHGuiButton {
   private boolean toggled;
   private boolean showToggle;

   public CFHGuiIconButton(int x, int y, int size, Icon icon, Component label, Runnable onPress) {
      super(x, y, size, size, label, onPress);
      this.icon = icon;
      this.iconSize = size >= 24 ? 16 : (size >= 18 ? 12 : 8);
      this.variant = CFHGuiButton.Variant.GHOST;
      if (label != null && !label.getString().isEmpty()) {
         this.tip(label);
      }

   }

   public CFHGuiIconButton toggled(boolean on) {
      this.toggled = on;
      this.showToggle = true;
      return this;
   }

   public boolean isToggled() {
      return this.toggled;
   }

   public CFHGuiIconButton setIcon(Icon icon) {
      this.icon = icon;
      return this;
   }

   public CFHGuiIconButton variant(CFHGuiButton.Variant v) {
      super.variant(v);
      return this;
   }

   public CFHGuiIconButton iconSize(int size) {
      super.iconSize(size);
      return this;
   }

   public CFHGuiIconButton tip(Component tooltip) {
      super.tip(tooltip);
      return this;
   }

   public CFHGuiIconButton tip(List<Component> lines) {
      super.tip(lines);
      return this;
   }

   protected void drawContent(GuiGraphics g, int x, int y, int w, int h, int fg, boolean shadow) {
      if (this.icon != null) {
         Palette p = Theme.current().palette();
         int color = fg;
         if (this.showToggle && this.toggled && this.active) {
            color = Colors.scaleAlpha(p.accent(), this.effectiveAlpha());
         }

         Icons.draw(g, this.icon, x + (w - this.iconSize) / 2, y + (h - this.iconSize) / 2, this.iconSize, color);
         if (this.showToggle && this.toggled) {
            CFHGuiDraw.rect(g, x + w / 2 - 3, y + h - 3, 6, 1, color);
         }

      }
   }
}
