package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

final class TitleTiles extends CFHGuiCard {
   static final int PAD = 6;
   static final int GAP = 5;
   private static final float PER_PIXEL = 0.03125F;
   private static final float FILL = 0.86F;
   private static final float FOV = 9.0F;
   private final Stage stage;
   private final List<Column> columns = new ArrayList();
   private boolean timed;

   TitleTiles(Screen owner, int x, int y, int w, int h) {
      super(x, y, w, h);
      Stage s = (new Stage()).bind(owner);
      this.stage = s;
      s.background(0);
      s.soft(true);
      s.finish(StageFinish.glow());
      s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.focusOutline(false);
      s.lighting().key(1.5F, 3.5F, 6.0F).ambientColor(0.46F, 0.45F, 0.47F).sun(0.3F, 1.0F, 0.7F);
      this.fill(Colors.withAlpha(Theme.current().palette().bg(), 180));
   }

   TitleTile tile(TitleTile tile, int relX, int relY, Function<PivotNode, ChestButton.HoverListener> thing) {
      this.add(tile, relX, relY);
      PivotNode holder = (PivotNode)this.stage.add(new PivotNode());
      Column c = new Column(tile, holder);
      c.listener = (ChestButton.HoverListener)thing.apply(holder);
      this.columns.add(c);
      return tile;
   }

   Stage stage() {
      return this.stage;
   }

   boolean entering() {
      if (this.enterProgress() < 1.0F) {
         return true;
      } else {
         for(AbstractWidget w : this.widgets()) {
            if (w instanceof CFHGuiWidget) {
               CFHGuiWidget sw = (CFHGuiWidget)w;
               if (sw.isEntering()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   void comeIn(int delayMs) {
      this.timed = true;
      super.playEntrance(delayMs);
   }

   public void playEntrance(int delayMs) {
      if (!this.timed) {
         super.playEntrance(delayMs);
      }

   }

   void comeInNow() {
      super.playEntrance(0);

      for(AbstractWidget w : this.widgets()) {
         if (w instanceof CFHGuiWidget sw) {
            sw.playEntrance(0);
         }
      }

   }

   void close() {
      this.stage.close();
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      if (!Theme.current().isVanilla()) {
         int r = Theme.current().radius();
         CFHGuiDraw.hline(g, x + r + 1, y + 1, w - 2 * r - 2, Colors.scaleAlpha(352321535, this.alpha * this.enterProgress()));
      }
   }

   protected void renderOverlay(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      float a = this.alpha * this.enterProgress();
      if (!(a <= 0.004F) && !this.stage.isClosed()) {
         float tall = (float)h * 0.03125F;
         float dist = tall / (2.0F * (float)Math.tan(Math.toRadians((double)4.5F)));
         this.stage.camera().at(0.0F, 0.0F, dist).lookAt(0.0F, 0.0F, 0.0F).fov(9.0F).clip(dist * 0.5F, dist * 1.5F + 20.0F);
         float midX = (float)x + (float)w / 2.0F;
         float midY = (float)y + (float)h / 2.0F;

         for(Column c : this.columns) {
            Rect win = c.tile.window();
            float shown = Mth.clamp(c.tile.shown(), 0.0F, 1.0F);
            float lift = c.tile.lift();
            c.holder.visible(shown > 0.01F);
            float pop = Theme.current().motion() > 0.0F ? Ease.OUT_BACK.apply(shown) : 1.0F;
            float size = (float)Math.min(win.w(), win.h()) * 0.03125F * 0.86F / 0.47F;
            c.holder.scale(Math.max(0.001F, size * pop * (1.0F + 0.07F * lift) * (1.0F - 0.05F * c.tile.press())));
            c.holder.at(((float)win.x() + (float)win.w() / 2.0F - midX) * 0.03125F, (midY - ((float)win.y() + (float)win.h() / 2.0F)) * 0.03125F, 0.0F);
            boolean pointed = lift > 0.5F;
            c.listener.hover(lift, pointed && !c.pointed);
            c.pointed = pointed;
         }

         this.stage.alpha(a);
         g.enableScissor(x, y, x + w, y + h);
         this.stage.render(g, x, y, w, h, (double)-1.0E9F, (double)-1.0E9F, partialTick);
         g.disableScissor();
      }
   }

   private static final class Column {
      final TitleTile tile;
      final PivotNode holder;
      ChestButton.HoverListener listener = (amount, entered) -> {
      };
      boolean pointed;

      Column(TitleTile tile, PivotNode holder) {
         this.tile = tile;
         this.holder = holder;
      }
   }
}
