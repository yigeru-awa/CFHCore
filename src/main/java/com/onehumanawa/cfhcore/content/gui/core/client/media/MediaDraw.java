package com.onehumanawa.cfhcore.content.gui.core.client.media;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.media.MediaCache;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class MediaDraw {
   public static int[] fit(int w, int h, int maxW, int maxH, boolean allowUpscale) {
      if (w > 0 && h > 0) {
         float f = Math.min((float)maxW / (float)w, (float)maxH / (float)h);
         if (!allowUpscale) {
            f = Math.min(1.0F, f);
         }

         return new int[]{Math.max(1, Math.round((float)w * f)), Math.max(1, Math.round((float)h * f))};
      } else {
         return new int[]{1, 1};
      }
   }

   public static void drawFrame(GuiGraphics g, MediaCache.Entry e, int x, int y, int w, int h, float alpha) {
      if (e.ready() && !e.frames.isEmpty() && e.width > 0) {
         MediaCache.Frame f = MediaCache.currentFrame(e.frames);
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         g.blit(f.texture(), x, y, w, h, 0.0F, 0.0F, e.width, e.height, e.width, e.height);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public static @Nullable Drawn drawFit(GuiGraphics g, MediaCache.Entry e, int x, int y, int maxW, int maxH, float alpha, boolean center, boolean allowUpscale) {
      if (e.ready() && !e.frames.isEmpty() && e.width > 0) {
         int[] s = fit(e.width, e.height, maxW, maxH, allowUpscale);
         int dx = center ? x + (maxW - s[0]) / 2 : x;
         int dy = center ? y + (maxH - s[1]) / 2 : y;
         drawFrame(g, e, dx, dy, s[0], s[1], alpha);
         return new Drawn(dx, dy, s[0], s[1]);
      } else {
         drawPlaceholder(g, e, x, y, maxW, maxH, alpha);
         return null;
      }
   }

   public static void drawPlaceholder(GuiGraphics g, MediaCache.Entry e, int x, int y, int w, int h, float alpha) {
      Theme t = Theme.current();
      if (e.status == MediaCache.Status.FAILED) {
         Component text = Component.translatable("cfhgui.media.failed");
         int tw = CFHGuiDraw.width(text);
         if (tw <= w - 4 && h >= 10) {
            g.drawString(CFHGuiDraw.font(), text, x + (w - tw) / 2, y + (h - 8) / 2, Colors.scaleAlpha(t.palette().textDim(), alpha), t.isVanilla());
         }

      } else {
         int size = Math.max(6, Math.min(12, Math.min(w, h) - 2));
         CFHGuiSpinner.draw(g, x + (w - size) / 2, y + (h - size) / 2, size, Colors.scaleAlpha(t.isVanilla() ? -1 : t.palette().textMuted(), alpha));
      }
   }

   public static Component statusText(MediaCache.Entry e) {
      MutableComponent var10000;
      switch (e.status) {
         case READY -> var10000 = Component.empty();
         case FAILED -> var10000 = e.error.isEmpty() ? Component.translatable("cfhgui.media.failed") : Component.translatable("cfhgui.media.failed_reason", new Object[]{e.error});
         default -> var10000 = Component.translatable("cfhgui.media.loading");
      }

      return var10000;
   }

   private MediaDraw() {
   }

   public static record Drawn(int x, int y, int w, int h) {
      public boolean contains(double mx, double my) {
         return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
      }
   }
}
