package com.onehumanawa.cfhcore.content.gui.menu.client.debug;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debugchart.AbstractDebugChart;
import net.minecraft.client.gui.components.debugchart.BandwidthDebugChart;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class DebugCharts {
   private static final int HEADER = 30;
   private static final int PAD = 6;
   private static final int CHART = 60;
   static final int ROW = 102;
   private static final String[] KEYS = new String[]{"frames", "ticks", "ping", "traffic"};
   private static final Icon[] ICONS;
   private static final FormattedCharSequence[] HEADINGS;
   private static @Nullable Theme headingsFor;
   private static @Nullable String headingsLanguage;
   private static boolean framing;
   private static int kind;
   private static final int GAME_GREEN = -16711936;
   private static final int GAME_YELLOW = -256;
   private static final int GAME_RED = -65536;
   private static final double READS = (double)2.0F;
   private static @Nullable Palette tonesOf;
   private static int fine;

   private DebugCharts() {
   }

   public static boolean begin(AbstractDebugChart chart, GuiGraphics g, int x, int width) {
      if (!DebugRestyle.active()) {
         return false;
      } else {
         kind = chart instanceof FpsDebugChart ? 0 : (chart instanceof TpsDebugChart ? 1 : (chart instanceof PingDebugChart ? 2 : (chart instanceof BandwidthDebugChart ? 3 : -1)));
         float s = DebugText.chartScale();
         float vw = (float)g.guiWidth() / s;
         float vh = (float)g.guiHeight() / s;
         float tx = x <= 0 ? (float)(10 - x) : vw - 4.0F - 2.0F - 6.0F - (float)(x + width);
         float ty = vh - 4.0F - 2.0F - 6.0F - (float)g.guiHeight();
         g.pose().pushPose();
         g.pose().scale(s, s, 1.0F);
         g.pose().translate((float)Math.round(tx), (float)Math.round(ty), 0.0F);
         framing = true;
         return true;
      }
   }

   public static void end(GuiGraphics g) {
      framing = false;
      g.pose().popPose();
   }

   public static boolean framing() {
      return framing;
   }

   public static boolean plate(GuiGraphics g, int x0, int y0, int x1, int y1) {
      if (!framing) {
         return false;
      } else {
         try {
            Theme t = DebugRestyle.theme();
            Palette p = t.palette();
            int radius = t.radius();
            int x = x0 - 6;
            int y = y0 - 30;
            int w = x1 - x0 + 12;
            int h = y1 - y0 + 30 + 6;
            CFHGuiDraw.shadow(g, x, y, w, h, Colors.scaleAlpha(p.shadow(), 0.7F), radius);
            CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.withAlpha(p.bg(), t.translucentPanels() ? 226 : 246), radius);
            CFHGuiDraw.outline(g, x, y, w, h, p.border(), radius);
            CFHGuiDraw.rect(g, x0, y0, x1 - x0, y1 - y0, Colors.withAlpha(Colors.brighten(p.bg(), -0.4F), 230));
            CFHGuiDraw.outline(g, x0, y0, x1 - x0, y1 - y0, p.border(), 0);
            FormattedCharSequence heading = heading();
            if (heading != null) {
               Font font = CFHGuiDraw.font();
               int from = x0 + 12 + font.width(heading) + 6;
               CFHGuiDraw.hline(g, from, y + 11, x1 - from, p.border());
               Icons.draw(g, ICONS[kind], x0, y + 7, 8, p.accentInk());
               g.drawString(font, heading, x0 + 12, y + 8, p.text(), false);
            }

            return true;
         } catch (RuntimeException e) {
            framing = false;
            DebugRestyle.failed("framing a chart", e);
            return false;
         }
      }
   }

   public static int number(GuiGraphics g, Font font, String text, int x, int y) {
      Palette p = DebugRestyle.palette();
      return g.drawString(font, text, x, y, Colors.lerp(p.textMuted(), p.text(), 0.35F), false);
   }

   public static int shade() {
      return Colors.withAlpha(DebugRestyle.palette().bg(), 200);
   }

   public static boolean recolours(int low, int mid, int high) {
      return framing && low == -16711936 && mid == -256 && high == -65536;
   }

   public static int sampleColor(double value, double mid, double high) {
      Palette p = DebugRestyle.palette();
      if (p != tonesOf) {
         tonesOf = p;
         int accent = Colors.withAlpha(p.accent(), 255);
         int well = Colors.withAlpha(Colors.brighten(p.bg(), -0.4F), 255);
         fine = Colors.contrast(accent, well) >= (double)2.0F ? accent : Colors.withAlpha(p.accentInk(), 255);
      }

      if (value < mid) {
         return kind == 1 ? Colors.withAlpha(fine, 128) : fine;
      } else {
         return Colors.lerp(p.warning(), p.danger(), high > mid ? (float)((value - mid) / (high - mid)) : 1.0F);
      }
   }

   private static @Nullable FormattedCharSequence heading() {
      if (kind < 0) {
         return null;
      } else {
         Theme theme = DebugRestyle.theme();
         String language = Minecraft.getInstance().getLanguageManager().getSelected();
         if (headingsFor != theme || !language.equals(headingsLanguage)) {
            Arrays.fill(HEADINGS, null);
            headingsFor = theme;
            headingsLanguage = language;
         }

         if (HEADINGS[kind] == null) {
            HEADINGS[kind] = Component.translatable("cfhgui_infohud.debug.chart." + KEYS[kind]).withStyle(theme.headingStyle()).getVisualOrderText();
         }

         return HEADINGS[kind];
      }
   }

   static {
      ICONS = new Icon[]{Icon.GAUGE, Icon.SERVER, Icon.SIGNAL, Icon.SORT};
      HEADINGS = new FormattedCharSequence[KEYS.length];
      kind = -1;
   }
}
