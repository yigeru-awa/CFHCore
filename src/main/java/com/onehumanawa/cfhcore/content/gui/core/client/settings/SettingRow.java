package com.onehumanawa.cfhcore.content.gui.core.client.settings;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class SettingRow extends CFHGuiCard {
   public static final int HEIGHT = 28;
   private static final int PAD = 6;
   private final Component label;
   private final @Nullable Component tooltip;
   private final @Nullable CFHGuiLabel description;

   private SettingRow(int width, Component label, @Nullable Component tooltip, AbstractWidget control, @Nullable Component description) {
      super(0, 0, width, 28);
      this.flat();
      this.label = label;
      this.tooltip = tooltip;
      int h = 28;
      if (description != null) {
         this.description = (new CFHGuiLabel(0, 0, width - 12, description)).style(CFHGuiLabel.Style.MUTED).wrap(true);
         this.add(this.description, 6, 30);
         h = 30 + this.description.getHeight() + 4;
      } else {
         this.description = null;
      }

      this.add(control, Math.max(0, width - 6 - control.getWidth()), (28 - control.getHeight()) / 2);
      this.setHeight(h);
   }

   public static SettingRow of(int width, Component label, @Nullable Component tooltip, AbstractWidget control) {
      return new SettingRow(width, label, tooltip, control, (Component)null);
   }

   public static SettingRow of(int width, Component label, @Nullable Component tooltip, AbstractWidget control, @Nullable Component description) {
      return new SettingRow(width, label, tooltip, control, description);
   }

   public static int controlWidth(int width) {
      int inner = width - 12;
      return Math.max(Math.min(inner - 80, 120), Math.min(320, inner * 3 / 5));
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.alpha * this.enterProgress();
      int controlW = this.widgets().isEmpty() ? 0 : ((AbstractWidget)this.widgets().get(this.widgets().size() - 1)).getWidth();
      int labelW = w - controlW - 8 - 12;
      int color = t.isVanilla() ? -1 : p.text();
      g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.label, labelW), x + 6, CFHGuiDraw.textY(y, 28), Colors.scaleAlpha(color, a), t.isVanilla());
      if (this.tooltip != null && mouseX >= x && mouseX < x + 6 + labelW && mouseY >= y && mouseY < y + 28) {
         CFHGuiTooltips.request(this.tooltip, this);
      }

   }
}
