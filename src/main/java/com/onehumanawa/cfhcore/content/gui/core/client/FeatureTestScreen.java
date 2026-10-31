package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.module.KnownModule;
import com.onehumanawa.cfhcore.content.gui.core.module.KnownModules;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class FeatureTestScreen extends CFHGuiScreen {
   private static final int ROW_H = 30;
   private static final int LABEL_W = 120;
   private static final int GAP = 8;
   private final List<Row> rows = new ArrayList();
   private Rect grid = new Rect(0, 0, 0, 0);
   private int colW;

   public FeatureTestScreen(@Nullable Screen parent) {
      super(Component.translatable("cfhgui.feature_test.title"), parent);
      this.maxContentWidth = 620;
   }

   protected void build() {
      this.rows.clear();
      Rect c = this.contentRect();
      CFHGuiScrollPanel panel = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(c.x(), c.y(), c.w(), c.h())).padding(2));
      int inner = panel.innerWidth();
      this.colW = (inner - 120 - 24) / 3;
      this.grid = new Rect(c.x() + 2, c.y() + 2, inner, 0);
      int y = 22;

      for(KnownModule m : KnownModules.features()) {
         this.rows.add(new Row(m, y));
         int x = 128;

         for(Layout layout : Layout.values()) {
            CFHGuiButton b = (CFHGuiButton)Features.gate(m.id(), (Layout)layout, () -> (new CFHGuiButton(0, 0, this.colW, 20, Component.translatable("cfhgui.feature_test.open", new Object[]{m.name()}), () -> {
               })).icon(m.icon()));
            if (b != null) {
               panel.add(b, x, y + 5);
            }

            x += this.colW + 8;
         }

         y += 30;
      }

      panel.setContentHeight(y + 4);
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int muted = van ? -4144960 : p.textMuted();
      int dim = van ? -7631989 : p.textDim();
      int x = this.grid.x() + 120 + 8;
      Layout here = MenuSlots.effective("cfhgui:hub");

      for(Layout layout : Layout.values()) {
         Component head = Fonts.heading((Component)Component.translatable("cfhgui.layout." + layout.key()));
         g.drawString(this.font, head, x, this.grid.y() + 4, layout == here ? (van ? -1 : p.text()) : muted, van);
         if (layout == here) {
            CFHGuiDraw.accentCap(g, x, this.grid.y() + 15, Math.min(32, this.font.width(head)), 1.0F);
         }

         x += this.colW + 8;
      }

      for(Row r : this.rows) {
         int ry = this.grid.y() + r.y();
         boolean on = r.module().installed();
         Icons.draw(g, r.module().icon(), this.grid.x(), ry + 9, 12, on ? (van ? -1 : p.accentInk()) : dim);
         g.drawString(this.font, CFHGuiDraw.truncate(r.module().name(), 102), this.grid.x() + 16, ry + 6, on ? (van ? -1 : p.text()) : muted, van);
         g.drawString(this.font, Component.translatable(on ? "cfhgui.feature_test.installed" : "cfhgui.feature_test.missing"), this.grid.x() + 16, ry + 16, dim, van);
         int cx = this.grid.x() + 120 + 8;

         for(Layout layout : Layout.values()) {
            if (Features.state(r.module().id(), layout) == Features.State.HIDDEN) {
               CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.feature_test.hidden"), cx + this.colW / 2, ry + 11, Colors.withAlpha(dim, 160), van);
            }

            cx += this.colW + 8;
         }
      }

   }

   private static record Row(KnownModule module, int y) {
   }
}
