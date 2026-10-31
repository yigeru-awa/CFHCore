package com.onehumanawa.cfhcore.content.gui.menu.client.debug;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class DebugText {
   static final int MARGIN = 4;
   static final int GAP = 4;
   private static final int PAD_X = 8;
   private static final int PAD_TOP = 7;
   private static final int PAD_BOTTOM = 6;
   private static final int LINE = 10;
   private static final int HEAD = 13;
   private static final int BREAK = 6;
   private static final int RULE_ONLY = 9;
   private static final int ICON = 8;
   private static final int ICON_GAP = 4;
   private static final int TAB_GAP = 10;
   private static final int HANG = 8;
   private static final int MAX_LABEL = 24;
   private static final int MAX_PLATES = 6;
   private static final int BLANK = 0;
   private static final int PLAIN = 1;
   private static final int PAIR = 2;
   private static final int TITLE = 3;
   private static final String UNDERLINE;
   private static final List<String> STRONG;
   private static final Column LEFT;
   private static final Column RIGHT;
   private static final Map<String, Heading> HEADINGS;
   private static @Nullable Theme headingsFor;
   private static @Nullable String headingsLanguage;
   private static double px;
   private static double chartPx;
   private static double lastGuiScale;
   private static int lastWidth;
   private static int lastHeight;
   private static int lastCharts;
   private static long lastDrawMs;

   private DebugText() {
   }

   static float chartScale() {
      double gs = Minecraft.getInstance().getWindow().getGuiScale();
      return !(chartPx <= (double)0.0F) && !(gs <= (double)0.0F) ? (float)Math.min((double)1.0F, chartPx / gs) : 1.0F;
   }

   private static double under(double pixels) {
      return pixels > Math.floor(pixels) ? Math.floor(pixels) : pixels - (double)1.0F;
   }

   static void draw(GuiGraphics g, List<String> source, boolean left) {
      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      Column c = left ? LEFT : RIGHT;
      read(c, source, font);
      double gs = Math.max((double)1.0F, mc.getWindow().getGuiScale());
      int gw = g.guiWidth();
      int gh = g.guiHeight();
      DebugScreenOverlay overlay = mc.getDebugOverlay();
      boolean frames = overlay.showFpsCharts();
      boolean network = overlay.showNetworkCharts();
      int charts = (frames ? 1 : 0) | (network ? 2 : 0);
      boolean chartUnder = left ? frames || network && !mc.isLocalServer() : frames || network;
      long now = Clock.nowMs();
      if (px <= (double)0.0F || now - lastDrawMs > 400L || gs != lastGuiScale || gw != lastWidth || gh != lastHeight || charts != lastCharts) {
         px = gs;
         chartPx = gs;
         lastGuiScale = gs;
         lastWidth = gw;
         lastHeight = gh;
         lastCharts = charts;
      }

      lastDrawMs = now;
      double least = Math.max((double)1.0F, Math.ceil(gs / (double)2.0F));
      double chartLeast = Math.min((double)2.0F, least);

      int vw;
      while(true) {
         vw = (int)Math.floor((double)gw * gs / px + 0.001);
         int vh = (int)Math.floor((double)gh * gs / px + 0.001);
         int chartRoom = chartUnder ? Mth.ceil((double)102.0F * chartPx / px) : 0;
         int inner = innerMax(c, vw, now);
         int avail = vh - 8 - chartRoom;
         layout(c, font, inner, avail, false);
         if (c.fits) {
            break;
         }

         if (chartUnder && chartPx > chartLeast) {
            chartPx = under(chartPx);
         } else {
            if (!(px > least)) {
               layout(c, font, inner, avail, true);
               int across = 8 + (c.plates - 1) * 4 + 4 + otherWidth(c, vw, now);

               for(int i = 0; i < c.plates; ++i) {
                  across += c.plateW[i];
               }

               if (across > vw) {
                  layout(c, font, inner, avail, false);
               }
               break;
            }

            px = under(px);
            chartPx = Math.min(chartPx, px);
         }
      }

      place(c, vw);
      c.drawnWidthPx = (float)(c.plates > 0 ? (double)c.plateW[0] * px : (double)0.0F);
      c.drawnMs = now;
      float s = (float)(px / gs);
      g.pose().pushPose();

      try {
         if (s != 1.0F) {
            g.pose().scale(s, s, 1.0F);
         }

         paint(g, c, font);
      } finally {
         g.pose().popPose();
      }

   }

   private static int innerMax(Column c, int vw, long now) {
      int chrome = 18;
      int whole = vw - 8 - 18;
      int beside = vw - 8 - 4 - otherWidth(c, vw, now) - 18;
      int least = Math.round((float)vw * (c.left ? 0.4F : 0.3F)) - 18;
      int most = Math.round((float)vw * (c.left ? 0.5F : 0.44F)) - 18;
      return Math.max(60, Math.min(Math.min(whole, most), Math.max(least, beside)));
   }

   private static int otherWidth(Column c, int vw, long now) {
      Column other = c.left ? RIGHT : LEFT;
      return now - other.drawnMs < 400L ? Mth.ceil((double)other.drawnWidthPx / px) : Math.round((float)vw * 0.42F);
   }

   private static void read(Column c, List<String> source, Font font) {
      int n = source.size();

      while(c.lines.size() < n) {
         c.lines.add(new Line());
      }

      c.count = n;

      for(int i = 0; i < n; ++i) {
         String s = (String)source.get(i);
         Line line = (Line)c.lines.get(i);
         String text = s == null ? "" : s;
         if (!line.source.equals(text)) {
            parse(line, text, font);
         }
      }

   }

   private static void parse(Line line, String s, Font font) {
      line.source = s;
      line.label = "";
      line.value = "";
      line.strong = false;
      line.lead = false;
      line.quiet = false;
      line.keyed = false;
      line.title = null;
      line.titleFor = null;
      line.wrapped = null;
      line.wrapFor = -1;
      line.labelW = line.valueW = 0;
      if (s.isEmpty()) {
         line.kind = 0;
         line.width = 0;
         line.section = null;
         line.formatted = false;
      } else {
         line.kind = 1;
         line.formatted = s.indexOf(167) >= 0;
         String plain = line.formatted ? stripped(s) : s;
         line.section = known(plain);
         line.keyed = line.formatted && plain.indexOf(": ") > 0;
         if (s.startsWith(UNDERLINE)) {
            int at = plain.indexOf(": ");
            line.kind = 3;
            line.label = at > 0 ? plain.substring(0, at) : plain;
            line.value = at > 0 ? plain.substring(at + 2).trim() : "";
            line.valueW = font.width(line.value);
         } else if (!line.formatted) {
            int at = s.indexOf(": ");
            if (at >= 2 && at <= 24 && s.indexOf(": ", at + 2) < 0 && labelLike(s, at) && at + 2 < s.length() && !s.substring(at + 2).isBlank()) {
               line.kind = 2;
               line.label = s.substring(0, at + 1);
               line.value = s.substring(at + 2).trim();
               line.labelW = font.width(line.label);
               line.valueW = font.width(line.value);
               line.strong = STRONG.contains(line.label);
            } else {
               int lead = frameRateLead(s);
               if (lead > 0) {
                  line.lead = true;
                  line.label = s.substring(0, lead);
                  line.value = s.substring(lead);
                  line.labelW = font.width(line.label);
               } else if (s.startsWith("Minecraft ")) {
                  line.strong = true;
               } else {
                  line.quiet = at >= 0 || s.charAt(0) == '#';
               }
            }
         }

         line.width = font.width(s);
      }
   }

   private static String stripped(String s) {
      String plain = ChatFormatting.stripFormatting(s);
      return plain == null ? s : plain;
   }

   private static boolean labelLike(String s, int end) {
      if (!Character.isLetter(s.charAt(0))) {
         return false;
      } else {
         for(int i = 1; i < end; ++i) {
            char ch = s.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != ' ' && ch != '-' && ch != '_' && ch != '.') {
               return false;
            }
         }

         return true;
      }
   }

   private static int frameRateLead(String s) {
      int i;
      for(i = 0; i < s.length() && i < 6 && Character.isDigit(s.charAt(i)); ++i) {
      }

      return i > 0 && s.startsWith(" fps", i) ? i + 4 : 0;
   }

   private static @Nullable String known(String s) {
      if (s.startsWith("Minecraft ")) {
         return "game";
      } else if (!s.startsWith("XYZ: ") && !s.startsWith("Chunk-relative: ")) {
         if (!s.startsWith("Debug charts: ") && !s.startsWith("For help: ")) {
            if (s.startsWith("Java: ")) {
               return "java";
            } else if (s.startsWith("CPU: ")) {
               return "processor";
            } else {
               return s.startsWith("Display: ") ? "graphics" : null;
            }
         } else {
            return "keys";
         }
      } else {
         return "position";
      }
   }

   private static Icon iconOf(String key) {
      Icon var10000;
      switch (key) {
         case "game" -> var10000 = Icon.WORLD;
         case "position" -> var10000 = Icon.LOCATION;
         case "keys" -> var10000 = Icon.KEYBOARD;
         case "java" -> var10000 = Icon.UPG_MEMORY;
         case "processor" -> var10000 = Icon.CHIP;
         case "graphics" -> var10000 = Icon.GPU;
         default -> var10000 = Icon.INFO;
      }

      return var10000;
   }

   private static Icon iconOfTitle(String label) {
      if (label.endsWith("Block")) {
         return Icon.BLOCK;
      } else {
         return label.endsWith("Entity") ? Icon.USER : Icon.EYE;
      }
   }

   private static Heading heading(String key, Font font) {
      Theme theme = DebugRestyle.theme();
      String language = Minecraft.getInstance().getLanguageManager().getSelected();
      if (headingsFor != theme || !language.equals(headingsLanguage)) {
         HEADINGS.clear();
         headingsFor = theme;
         headingsLanguage = language;
      }

      Heading h = (Heading)HEADINGS.get(key);
      if (h == null) {
         FormattedCharSequence text = Component.translatable("cfhgui_infohud.debug.heading." + key).withStyle(theme.headingStyle()).getVisualOrderText();
         h = new Heading(text, font.width(text), iconOf(key));
         HEADINGS.put(key, h);
      }

      return h;
   }

   private static FormattedCharSequence title(Line line) {
      Theme theme = DebugRestyle.theme();
      if (line.title == null || line.titleFor != theme) {
         line.title = Component.literal(line.label).withStyle(theme.headingStyle()).getVisualOrderText();
         line.titleFor = theme;
         line.labelW = Minecraft.getInstance().font.width(line.title);
      }

      return line.title;
   }

   private static Section section(Column c, int index) {
      while(c.sections.size() <= index) {
         c.sections.add(new Section());
      }

      return (Section)c.sections.get(index);
   }

   private static void layout(Column c, Font font, int innerMax, int avail, boolean flow) {
      int count = 0;

      Section sec;
      for(int i = 0; i < c.count; sec.to = i) {
         while(i < c.count && ((Line)c.lines.get(i)).kind == 0) {
            ++i;
         }

         if (i >= c.count) {
            break;
         }

         sec = section(c, count++);

         for(sec.from = i; i < c.count && ((Line)c.lines.get(i)).kind != 0; ++i) {
         }
      }

      c.sectionCount = count;

      for(int k = 0; k < count; ++k) {
         Section s = (Section)c.sections.get(k);
         Line head = (Line)c.lines.get(s.from);
         s.titled = false;
         s.valueBelow = false;
         s.heading = null;
         if (head.kind == 3) {
            title(head);
            int words = 12 + head.labelW;
            s.titled = words <= innerMax;
            s.valueBelow = s.titled && !head.value.isEmpty() && words + 6 + head.valueW > innerMax;
         }

         if (s.valueBelow) {
            int room = innerMax - 8 - 4;
            if (head.valueW <= room) {
               head.wrapped = null;
               head.wrapFor = -1;
               head.rows = 1;
            } else {
               wrapTo(head, font, head.value, room, -room, 0);
            }
         }

         if (!s.titled) {
            for(int r = s.from; r < s.to; ++r) {
               String key = ((Line)c.lines.get(r)).section;
               if (key != null) {
                  s.heading = heading(key, font);
                  break;
               }
            }
         }

         int tab = 0;
         int tabMax = Math.round((float)innerMax * 0.42F);

         for(int r = s.from; r < s.to; ++r) {
            Line line = (Line)c.lines.get(r);
            if (line.kind == 2 && line.labelW + 10 <= tabMax) {
               tab = Math.max(tab, line.labelW + 10);
            }
         }

         s.tab = tab;
         int body = s.valueBelow ? head.rows * 10 : 0;

         for(int r = s.titled ? s.from + 1 : s.from; r < s.to; ++r) {
            Line line = (Line)c.lines.get(r);
            line.subject = s.titled && r == s.from + 1 && line.kind == 1;
            wrap(line, font, innerMax, tab);
            body += line.rows * 10;
         }

         s.bodyH = body;
      }

      int room = flow ? Math.max(40, avail) : 1073741823;
      int plate = 0;
      int y = 7;
      boolean empty = true;

      for(int p = 0; p < 6; ++p) {
         c.plateW[p] = 0;
         c.plateH[p] = 0;
      }

      for(int k = 0; k < count; ++k) {
         Section s = (Section)c.sections.get(k);
         boolean worded = s.titled || s.heading != null;
         if (!empty && plate < 5 && y + (worded ? 19 : 9) + s.bodyH + 6 > room) {
            c.plateH[plate] = y + 6;
            ++plate;
            y = 7;
            empty = true;
         }

         int lead = !empty && worded ? 6 : 0;
         int headH = worded ? 13 : (empty ? 0 : 9);
         s.plate = plate;
         s.first = empty;
         s.y = y + lead;
         y += lead + headH;
         empty = false;
         if (!s.titled) {
            if (s.heading != null) {
               c.plateW[plate] = Math.max(c.plateW[plate], 12 + s.heading.width() + 16);
            }
         } else {
            Line head = (Line)c.lines.get(s.from);
            c.plateW[plate] = Math.max(c.plateW[plate], 12 + head.labelW + (!head.value.isEmpty() && !s.valueBelow ? 6 + head.valueW : 0) + 16);
            if (s.valueBelow) {
               head.plate = plate;
               head.y = y;
               y += head.rows * 10;
               c.plateW[plate] = Math.max(c.plateW[plate], 12 + (head.wrapped == null ? head.valueW : head.wrapW));
            }
         }

         for(int r = s.titled ? s.from + 1 : s.from; r < s.to; ++r) {
            Line line = (Line)c.lines.get(r);
            int h = line.rows * 10;
            if (y + h + 6 > room && y > 7 && plate < 5) {
               c.plateH[plate] = y + 6;
               ++plate;
               y = 7;
            }

            line.plate = plate;
            line.y = y;
            y += h;
            c.plateW[plate] = Math.max(c.plateW[plate], extent(line));
         }
      }

      c.plateH[plate] = y - 2 + 6;
      c.plates = count == 0 ? 0 : plate + 1;
      c.fits = plate == 0 && c.plateH[0] <= avail;

      for(int p = 0; p < c.plates; ++p) {
         c.plateW[p] = Math.min(innerMax, c.plateW[p]) + 16;
      }

   }

   private static int extent(Line line) {
      if (line.kind == 2 && line.pairSplit) {
         return line.valueX + (line.wrapped == null ? line.valueW : line.wrapW);
      } else {
         return line.wrapped == null ? line.width : line.wrapW;
      }
   }

   private static void wrap(Line line, Font font, int innerMax, int tab) {
      line.rows = 1;
      line.pairSplit = false;
      if (line.kind == 2) {
         line.valueX = line.labelW + 10 <= tab ? tab : line.labelW + 10;
         int room = innerMax - line.valueX;
         if (line.valueW <= room) {
            line.pairSplit = true;
            line.wrapped = null;
            line.wrapFor = -1;
            return;
         }

         if (room >= 70) {
            line.pairSplit = true;
            wrapTo(line, font, line.value, room, -room, 0);
            return;
         }
      }

      if (line.width <= innerMax) {
         line.wrapped = null;
         line.wrapFor = -1;
      } else {
         wrapTo(line, font, line.source, Math.max(40, innerMax - 8), innerMax, 8);
      }
   }

   private static void wrapTo(Line line, Font font, String text, int width, int key, int hang) {
      if (line.wrapped == null || line.wrapFor != key) {
         line.wrapped = font.split(FormattedText.of(text), width);
         line.wrapFor = key;
         int widest = 0;

         for(int i = 0; i < line.wrapped.size(); ++i) {
            widest = Math.max(widest, font.width((FormattedCharSequence)line.wrapped.get(i)) + (i == 0 ? 0 : hang));
         }

         line.wrapW = widest;
      }

      line.rows = Math.max(1, line.wrapped.size());
   }

   private static void place(Column c, int vw) {
      int x = c.left ? 4 : vw - 4 - 2;

      for(int p = 0; p < c.plates; ++p) {
         if (c.left) {
            c.plateX[p] = x;
            x += c.plateW[p] + 4;
         } else {
            x -= c.plateW[p];
            c.plateX[p] = x;
            x -= 4;
         }
      }

   }

   private static void paint(GuiGraphics g, Column c, Font font) {
      if (c.plates != 0) {
         Theme t = DebugRestyle.theme();
         Palette p = t.palette();
         int radius = t.radius();
         int fill = Colors.withAlpha(p.bg(), t.translucentPanels() ? 226 : 246);
         int rule = p.border();
         int top = 4;

         for(int i = 0; i < c.plates; ++i) {
            int x = c.plateX[i];
            int w = c.plateW[i];
            int h = c.plateH[i];
            CFHGuiDraw.shadow(g, x, 4, w, h, Colors.scaleAlpha(p.shadow(), 0.7F), radius);
            CFHGuiDraw.pixelRound(g, x, 4, w, h, fill, radius);
            CFHGuiDraw.outline(g, x, 4, w, h, p.border(), radius);
         }

         for(int k = 0; k < c.sectionCount; ++k) {
            Section s = (Section)c.sections.get(k);
            int x = c.plateX[s.plate] + 8;
            int right = c.plateX[s.plate] + c.plateW[s.plate] - 8;
            if (s.titled) {
               Line head = (Line)c.lines.get(s.from);
               int from = x + 8 + 4 + head.labelW + (!head.value.isEmpty() && !s.valueBelow ? 6 + head.valueW : 0) + 6;
               CFHGuiDraw.hline(g, from, 4 + s.y + 4, right - from, rule);
            } else if (s.heading != null) {
               int from = x + 8 + 4 + s.heading.width() + 6;
               CFHGuiDraw.hline(g, from, 4 + s.y + 4, right - from, rule);
            } else if (!s.first) {
               CFHGuiDraw.hline(g, x, 4 + s.y + 4 - 2, right - x, rule);
            }
         }

         int mark = p.accentInk();

         for(int k = 0; k < c.sectionCount; ++k) {
            Section s = (Section)c.sections.get(k);
            int x = c.plateX[s.plate] + 8;
            if (s.titled) {
               Icons.draw(g, iconOfTitle(((Line)c.lines.get(s.from)).label), x, 4 + s.y, 8, mark);
            } else if (s.heading != null) {
               Icons.draw(g, s.heading.icon(), x, 4 + s.y, 8, mark);
            }
         }

         int headInk = p.text();
         int labelInk = p.textMuted();
         int valueInk = Colors.lerp(p.textMuted(), p.text(), 0.6F);
         int quietInk = Colors.lerp(p.textDim(), p.textMuted(), 0.45F);
         int strongInk = p.text();

         for(int k = 0; k < c.sectionCount; ++k) {
            Section s = (Section)c.sections.get(k);
            int x = c.plateX[s.plate] + 8 + 8 + 4;
            if (s.titled) {
               Line head = (Line)c.lines.get(s.from);
               g.drawString(font, title(head), x, 4 + s.y + 1, headInk, false);
               if (s.valueBelow) {
                  if (head.wrapped == null) {
                     g.drawString(font, head.value, x, 4 + head.y, labelInk, false);
                  } else {
                     for(int w = 0; w < head.wrapped.size(); ++w) {
                        g.drawString(font, (FormattedCharSequence)head.wrapped.get(w), x, 4 + head.y + w * 10, labelInk, false);
                     }
                  }
               } else if (!head.value.isEmpty()) {
                  g.drawString(font, head.value, x + head.labelW + 6, 4 + s.y + 1, labelInk, false);
               }
            } else if (s.heading != null) {
               g.drawString(font, s.heading.text(), x, 4 + s.y + 1, headInk, false);
            }

            for(int r = s.titled ? s.from + 1 : s.from; r < s.to; ++r) {
               Line line = (Line)c.lines.get(r);
               int lx = c.plateX[line.plate] + 8;
               int ly = 4 + line.y;
               if (line.kind == 2 && line.pairSplit) {
                  g.drawString(font, line.label, lx, ly, labelInk, false);
                  int ink = line.strong ? strongInk : valueInk;
                  if (line.wrapped == null) {
                     g.drawString(font, line.value, lx + line.valueX, ly, ink, false);
                  } else {
                     for(int w = 0; w < line.wrapped.size(); ++w) {
                        g.drawString(font, (FormattedCharSequence)line.wrapped.get(w), lx + line.valueX, ly + w * 10, ink, false);
                     }
                  }
               } else {
                  int ink = !line.strong && !line.subject ? (line.quiet ? quietInk : (line.keyed ? labelInk : valueInk)) : strongInk;
                  if (line.wrapped != null) {
                     for(int w = 0; w < line.wrapped.size(); ++w) {
                        g.drawString(font, (FormattedCharSequence)line.wrapped.get(w), lx + (w == 0 ? 0 : 8), ly + w * 10, ink, false);
                     }
                  } else if (line.lead) {
                     g.drawString(font, line.label, lx, ly, strongInk, false);
                     g.drawString(font, line.value, lx + line.labelW, ly, valueInk, false);
                  } else {
                     g.drawString(font, line.source, lx, ly, ink, false);
                  }
               }
            }
         }

      }
   }

   static {
      UNDERLINE = ChatFormatting.UNDERLINE.toString();
      STRONG = List.of("XYZ:", "Block:", "Facing:", "Biome:", "Client Light:", "Chunk-relative:");
      LEFT = new Column(true);
      RIGHT = new Column(false);
      HEADINGS = new HashMap();
      px = (double)-1.0F;
      chartPx = (double)-1.0F;
   }

   private static final class Line {
      String source = "";
      int kind;
      String label = "";
      String value = "";
      int labelW;
      int valueW;
      int width;
      boolean strong;
      boolean formatted;
      boolean lead;
      boolean quiet;
      boolean keyed;
      @Nullable String section;
      @Nullable FormattedCharSequence title;
      @Nullable Theme titleFor;
      @Nullable List<FormattedCharSequence> wrapped;
      int wrapFor = -1;
      int wrapW;
      int plate;
      int y;
      int valueX;
      int rows;
      boolean pairSplit;
      boolean subject;
   }

   private static final class Section {
      int from;
      int to;
      boolean titled;
      boolean valueBelow;
      @Nullable Heading heading;
      int tab;
      int bodyH;
      int plate;
      int y;
      boolean first;
   }

   private static record Heading(FormattedCharSequence text, int width, Icon icon) {
   }

   private static final class Column {
      final boolean left;
      final ArrayList<Line> lines = new ArrayList();
      final ArrayList<Section> sections = new ArrayList();
      int count;
      int sectionCount;
      int plates;
      final int[] plateW = new int[6];
      final int[] plateH = new int[6];
      final int[] plateX = new int[6];
      boolean fits;
      float drawnWidthPx;
      long drawnMs;

      Column(boolean left) {
         this.left = left;
      }
   }
}
