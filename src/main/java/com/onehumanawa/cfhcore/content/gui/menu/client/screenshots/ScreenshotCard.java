package com.onehumanawa.cfhcore.content.gui.menu.client.screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ScreenshotCard extends CFHGuiCard {
   public static final int CAPTION_H = 26;
   private static final int THUMB_PX = 256;
   private final Screenshots.Shot shot;
   private Textures.@Nullable Loaded thumb;
   private boolean requested;

   public ScreenshotCard(int x, int y, int w, int h, Screenshots.Shot shot, CFHGuiScreenshotsScreen screen) {
      super(x, y, w, h);
      this.shot = shot;
      this.onClick(() -> screen.clicked(this));
      this.onRightClick(() -> screen.contextMenu(this));
   }

   public Screenshots.Shot shot() {
      return this.shot;
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      if (!this.requested) {
         this.requested = true;
         Textures.loadThumbnail(this.shot.path(), 256, (l) -> this.thumb = l);
      }

      int tx = x + 4;
      int ty = y + 4;
      int tw = w - 8;
      int th = h - 8 - 26;
      CFHGuiDraw.pixelRound(g, tx, ty, tw, th, van ? -16777216 : p.bg(), Math.max(0, t.radius() - 1));
      if (this.thumb != null) {
         float s = Math.min((float)tw / (float)this.thumb.width(), (float)th / (float)this.thumb.height());
         int dw = Math.max(1, Math.round((float)this.thumb.width() * s));
         int dh = Math.max(1, Math.round((float)this.thumb.height() * s));
         int dx = tx + (tw - dw) / 2;
         int dy = ty + (th - dh) / 2;
         RenderSystem.enableBlend();
         g.blit(this.thumb.id(), dx, dy, dw, dh, 0.0F, 0.0F, this.thumb.width(), this.thumb.height(), this.thumb.width(), this.thumb.height());
         RenderSystem.disableBlend();
      } else {
         CFHGuiSpinner.draw(g, tx + tw / 2 - 8, ty + th / 2 - 8, 16, van ? -1 : p.textDim());
      }

      if (this.isSelected()) {
         CFHGuiDraw.outline(g, tx, ty, tw, th, Colors.withAlpha(p.accent(), 192), Math.max(0, t.radius() - 1));
      }

      g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(this.shot.name()), w - 12), x + 6, y + h - 26 + 4, p.text(), van);
      Font var10001 = CFHGuiDraw.font();
      String var10002 = Fmt.date(this.shot.modified());
      g.drawString(var10001, CFHGuiDraw.truncate(Component.literal(var10002 + " · " + Fmt.bytes(this.shot.size())), w - 24), x + 6, y + h - 11, van ? -6250336 : p.textDim(), van);
      Icons.draw(g, Icon.IMAGE, x + w - 14, y + h - 12, 8, van ? -8355712 : p.textDim());
   }
}
