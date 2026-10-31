package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiTooltips {
   public static final int DELAY_MS = 350;
   private static final int MAX_WIDTH = 220;
   private static final int PAD = 4;
   private static final int FADE_MS = 90;
   private static List<Component> pending;
   private static @Nullable AbstractWidget anchor;
   private static int anchorX = -1;
   private static int anchorY = -1;
   private static int withinX;
   private static int withinY;
   private static int withinW;
   private static int withinH;
   private static @Nullable Object shownKey;
   private static long shownSinceMs;

   public static void request(List<Component> lines, @Nullable AbstractWidget owner) {
      pending = lines;
      anchor = owner;
      anchorY = -1;
      anchorX = -1;
      withinW = 0;
   }

   public static void requestWithin(List<Component> lines, int x, int y, int w, int h) {
      request(lines, (AbstractWidget)null);
      withinX = x;
      withinY = y;
      withinW = Math.max(0, w);
      withinH = Math.max(0, h);
   }

   public static void requestAt(List<Component> lines, int x, int y) {
      pending = lines;
      anchor = null;
      anchorX = x;
      anchorY = y;
      withinW = 0;
   }

   public static void request(Component line, @Nullable AbstractWidget owner) {
      request(List.of(line), owner);
   }

   public static boolean hasPending() {
      return pending != null;
   }

   public static void render(GuiGraphics g, int mouseX, int mouseY, int screenW, int screenH) {
      List<Component> lines = pending;
      pending = null;
      if (lines != null && !lines.isEmpty()) {
         Object key = anchor != null ? anchor : lines.stream().map(Component::getString).collect(Collectors.joining("|"));
         if (!key.equals(shownKey)) {
            shownKey = key;
            shownSinceMs = Clock.nowMs();
         }

         float fade = Theme.current().motion() <= 0.0F ? 1.0F : Math.min(1.0F, (float)(Clock.nowMs() - shownSinceMs) / (float)Theme.current().ms(90));
         int maxWidth = Math.min(220, screenW - 16);
         List<FormattedCharSequence> wrapped = new ArrayList();
         int w = 0;

         for(Component c : lines) {
            for(FormattedCharSequence s : CFHGuiDraw.font().split(c, maxWidth)) {
               wrapped.add(s);
               w = Math.max(w, CFHGuiDraw.font().width(s));
            }
         }

         int h = wrapped.size() * (CFHGuiDraw.lineHeight() + 1) - 1 + 8;
         w += 8;
         int left = 4;
         int right = screenW - 4;
         int top = 4;
         int bottom = screenH - 4;
         if (withinW >= w + 4 && withinH >= h + 4) {
            left = Math.max(left, withinX + 2);
            right = Math.min(right, withinX + withinW - 2);
            top = Math.max(top, withinY + 2);
            bottom = Math.min(bottom, withinY + withinH - 2);
         }

         withinW = 0;
         int x;
         int y;
         if (anchorX >= 0) {
            x = anchorX;
            y = anchorY;
         } else {
            x = mouseX + 10;
            y = mouseY - 6;
         }

         if (x + w > right) {
            x = Math.max(left, (anchorX >= 0 ? anchorX : mouseX) - w - 6);
         }

         if (y + h > bottom) {
            y = bottom - h;
         }

         if (y < top) {
            y = top;
         }

         if (x < left) {
            x = left;
         }

         Theme t = Theme.current();
         int alpha = Math.round(255.0F * fade);
         g.pose().pushPose();
         g.pose().translate(0.0F, 0.0F, 400.0F);
         if (t.isVanilla()) {
            g.fill(x, y, x + w, y + h, Colors.withAlpha(1048592, Math.round(240.0F * fade)));
            CFHGuiDraw.outline(g, x, y, w, h, Colors.withAlpha(5243135, alpha), 0);
            CFHGuiDraw.outline(g, x + 1, y + 1, w - 2, h - 2, Colors.withAlpha(2621567, alpha), 0);
         } else {
            CFHGuiDraw.popupPlate(g, x, y, w, h, t.palette().surfaceActive(), t.palette().borderStrong(), 0.6F, fade);
         }

         int ty = y + 4;
         int fg = Colors.withAlpha(t.text(), Math.max(4, alpha));

         for(FormattedCharSequence s : wrapped) {
            g.drawString(CFHGuiDraw.font(), s, x + 4, ty, fg, t.isVanilla());
            ty += CFHGuiDraw.lineHeight() + 1;
         }

         g.pose().popPose();
         anchor = null;
      } else {
         shownKey = null;
      }
   }

   private CFHGuiTooltips() {
   }
}
