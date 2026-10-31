package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.mixin.ToastComponentAccessor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiToasts {
   public static final int WIDTH = 160;
   public static final int PAD = 6;
   public static final int LIFE_MS = 5000;
   public static final int MAX_VISIBLE = 5;
   private static final int GAME_TOAST = 32;
   private static final List<Toast> TOASTS = new ArrayList();

   public static void show(Component title, @Nullable Component body, @Nullable Icon icon) {
      show(title, body, icon, (Runnable)null);
   }

   public static void show(Component title, @Nullable Component body, @Nullable Icon icon, @Nullable Runnable onClick) {
      show(title, body, icon, onClick, 5000);
   }

   public static void show(Component title, @Nullable Component body, @Nullable Icon icon, @Nullable Runnable onClick, int lifeMs) {
      if (Theme.current().toasts()) {
         synchronized(TOASTS) {
            TOASTS.add(new Toast(title, body, icon, onClick, lifeMs));
            int live = 0;

            for(int i = TOASTS.size() - 1; i >= 0; --i) {
               if (!((Toast)TOASTS.get(i)).closing) {
                  ++live;
                  if (live > 5) {
                     ((Toast)TOASTS.get(i)).dismiss();
                  }
               }
            }
         }

         if (RenderSystem.isOnRenderThread()) {
            CFHGuiSounds.chime();
         } else {
            Minecraft.getInstance().execute(CFHGuiSounds::chime);
         }

      }
   }

   private static List<FormattedCharSequence> titleLines(Component title, int width) {
      List<FormattedText> parts = CFHGuiDraw.font().getSplitter().splitLines(title, width, Style.EMPTY);
      List<FormattedCharSequence> out = new ArrayList(2);
      if (parts.isEmpty()) {
         return out;
      } else {
         out.add(Language.getInstance().getVisualOrder((FormattedText)parts.get(0)));
         if (parts.size() == 2) {
            out.add(Language.getInstance().getVisualOrder((FormattedText)parts.get(1)));
         } else if (parts.size() > 2) {
            StringBuilder rest = new StringBuilder(((FormattedText)parts.get(1)).getString());

            for(int i = 2; i < parts.size(); ++i) {
               rest.append(' ').append(((FormattedText)parts.get(i)).getString());
            }

            out.add(CFHGuiDraw.truncate(Component.literal(rest.toString()).withStyle(title.getStyle()), width));
         }

         return out;
      }
   }

   public static void clear() {
      synchronized(TOASTS) {
         TOASTS.clear();
      }
   }

   public static void dismiss(Component title) {
      synchronized(TOASTS) {
         for(Toast t : TOASTS) {
            if (t.title.equals(title)) {
               t.dismiss();
            }
         }

      }
   }

   public static void render(GuiGraphics g, int mouseX, int mouseY, int screenW) {
      List<Toast> snapshot;
      synchronized(TOASTS) {
         TOASTS.removeIf((txx) -> txx.closing && txx.slide.get() <= 0.01F);
         snapshot = new ArrayList(TOASTS);
      }

      if (!snapshot.isEmpty()) {
         Theme th = Theme.current();
         Palette p = th.palette();
         long now = Clock.nowMs();
         int y = 8 + gameToastsReach();
         g.pose().pushPose();
         g.pose().translate(0.0F, 0.0F, 450.0F);

         for(Toast t : snapshot) {
            int h = t.height();
            if (!t.placed) {
               t.yAnim.snap((float)y);
               t.placed = true;
            } else {
               t.yAnim.set((float)y);
            }

            int ty = Math.round(t.yAnim.get());
            float s = Math.max(0.0F, t.slide.get());
            int x = screenW - 8 - 160 + Math.round((1.0F - s) * 168.0F);
            t.drawnX = x;
            t.drawnY = ty;
            t.drawnH = h;
            boolean hov = mouseX >= x && mouseX < x + 160 && mouseY >= ty && mouseY < ty + h;
            if (hov && !t.closing) {
               t.bornMs += (long)Math.round(Clock.frameDelta());
            }

            if (!t.closing && now - t.bornMs > (long)t.lifeMs) {
               t.dismiss();
            }

            if (th.isVanilla()) {
               g.fill(x, ty, x + 160, ty + h, -267382768);
               CFHGuiDraw.outline(g, x, ty, 160, h, hov ? -1 : -7631989, 0);
               CFHGuiDraw.rect(g, x + 1, ty + 1, 2, h - 2, hov ? -1 : p.accent());
            } else {
               CFHGuiDraw.popupPlate(g, x, ty, 160, h, hov ? p.surfaceHover() : p.surface(), hov ? p.textDim() : p.borderStrong(), 0.5F, 1.0F);
               CFHGuiDraw.rect(g, x, ty + 3, 2, h - 6, p.accent());
            }

            int tx = x + 6 + 2;
            if (t.icon != null) {
               Icons.draw(g, t.icon, tx, ty + 6 - 2, 12, p.accentInk());
               tx += 16;
            }

            int ly = ty + 6;

            for(FormattedCharSequence line : t.titleLines) {
               g.drawString(CFHGuiDraw.font(), line, tx, ly, p.text(), th.isVanilla());
               ly += 10;
            }

            for(FormattedCharSequence line : t.bodyLines) {
               g.drawString(CFHGuiDraw.font(), line, tx, ly, p.textMuted(), th.isVanilla());
               ly += 10;
            }

            if (!t.closing) {
               float life = 1.0F - (float)(now - t.bornMs) / (float)t.lifeMs;
               CFHGuiDraw.rect(g, x + 4, ty + h - 2, Math.round(152.0F * Math.max(0.0F, life)), 1, Colors.withAlpha(th.isVanilla() ? 10526880 : p.textDim(), 128));
            }

            y += h + 4;
         }

         g.pose().popPose();
      }
   }

   public static int gameToastsReach() {
      try {
         return ((ToastComponentAccessor)Minecraft.getInstance().getToasts()).cfhgui$visible().size() * 32;
      } catch (RuntimeException var1) {
         return 0;
      }
   }

   public static boolean mouseClicked(double mx, double my, int screenW) {
      List<Toast> snapshot;
      synchronized(TOASTS) {
         snapshot = new ArrayList(TOASTS);
      }

      for(Toast t : snapshot) {
         if (!t.closing && mx >= (double)t.drawnX && mx < (double)(t.drawnX + 160) && my >= (double)t.drawnY && my < (double)(t.drawnY + t.drawnH)) {
            t.dismiss();
            if (t.onClick != null) {
               t.onClick.run();
            }

            return true;
         }
      }

      return false;
   }

   public static boolean anyVisible() {
      synchronized(TOASTS) {
         return !TOASTS.isEmpty();
      }
   }

   private CFHGuiToasts() {
   }

   private static final class Toast {
      final Component title;
      final @Nullable Component body;
      final @Nullable Icon icon;
      final @Nullable Runnable onClick;
      long bornMs = Clock.nowMs();
      final Anim slide;
      final Anim yAnim;
      final int lifeMs;
      boolean closing;
      boolean placed;
      int drawnX;
      int drawnY;
      int drawnH;
      @Nullable List<FormattedCharSequence> titleLines;
      @Nullable List<FormattedCharSequence> bodyLines;

      Toast(Component title, @Nullable Component body, @Nullable Icon icon, @Nullable Runnable onClick, int lifeMs) {
         this.slide = new Anim(0.0F, 260, Ease.OUT_BACK);
         this.yAnim = new Anim(0.0F, 200, Ease.OUT_CUBIC);
         this.title = title;
         this.body = body;
         this.icon = icon;
         this.onClick = onClick;
         this.lifeMs = lifeMs;
         this.slide.snap(0.0F);
         this.slide.set(1.0F);
      }

      int textWidth() {
         return 146 - (this.icon == null ? 0 : 16);
      }

      void layout() {
         if (this.titleLines == null) {
            int tw = this.textWidth();
            this.titleLines = CFHGuiToasts.titleLines(this.title, tw);
            this.bodyLines = this.body == null ? List.of() : CFHGuiDraw.font().split(this.body, tw);
         }
      }

      int height() {
         this.layout();
         return 12 + (this.titleLines.size() + this.bodyLines.size()) * 10;
      }

      void dismiss() {
         if (!this.closing) {
            this.closing = true;
            this.slide.ease(Ease.OUT_CUBIC);
            this.slide.set(0.0F, 200);
         }
      }
   }
}
