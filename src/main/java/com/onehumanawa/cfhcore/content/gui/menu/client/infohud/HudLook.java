package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class HudLook {
   static int variant = Mth.clamp(Integer.getInteger("cfhgui.infohud.variant", 0), 0, 2);
   private static final int MEASURE = 0;
   private static final int FILL = 1;
   private static final int WORDS = 2;
   private static final int ICONS = 3;
   private static final int TEXT = 0;
   private static final int MUTED = 1;
   private static final int DIM = 2;
   private static final int GOOD = 3;
   private static final int WARN = 4;
   private static final int BAD = 5;
   static final int WIDTH = 156;
   private static final int PAD = 6;
   private static final int TOP = 6;
   private static final int BOTTOM = 6;
   private static final int GAP = 4;
   private static final int INNER = 144;
   private static final int ROW = 11;
   private static final int ICON = 8;
   private static final int TEXT_X = 12;
   private static final int HEAD = 14;
   private static final int FACE_H = 11;
   private static final int HERO = 27;
   private static final int TILE = 72;
   private static final int WELL = 14;
   private static final int WELL_ROW = 17;
   private static final int WELL_LABEL = 40;
   private static final int METER = 16;
   private static final int METER_GAP = 4;
   private static final int BAR = 5;
   private static final int EXTRA_ROOM = 46;
   private static final int RULE = 9;
   private static final int CHIP_H = 13;
   private static final int CHIP_GAP = 3;
   private static final int CHIP_PAD = 4;
   private static final int MINI_BAR = 22;
   private static final int POSITION = 0;
   private static final int BIOME = 1;
   private static final int DIMENSION = 2;
   private static final int FPS = 3;
   private static final int NET = 4;
   private static final int RAM = 5;
   private static final int VRAM = 6;
   private static final int KINDS = 7;
   private static final int SECOND_ROW = 3;
   private static final int BIOME_MAX_CHIP = 96;
   private static int pass;
   private static Font font;
   private static boolean vanilla;
   private static boolean shadowed;
   private static boolean rightSide;
   private static boolean compact;
   private static float alpha;
   private static float solid;
   private static float pulse;
   private static int radius;
   private static final int[] INK = new int[6];
   private static int iconInk;
   private static int iconShade;
   private static int plateInk;
   private static int borderInk;
   private static int shadowInk;
   private static int ruleInk;
   private static int trackInk;
   private static int wellInk;
   private static int wellEdge;
   private static int faceInk;
   private static int sparkInk;
   private static int barFine;
   private static int barWarn;
   private static int barBad;
   private static final double READS = (double)3.0F;
   private static @Nullable Palette tonesOf;
   private static int accentInk;
   private static int accentFill;
   private static int ramLevel;
   private static int vramLevel;
   private static final Smooth RAM_WARMTH = new Smooth();
   private static final Smooth VRAM_WARMTH = new Smooth();
   private static final int CARDS = 3;
   private static final Smooth[] HEIGHT = new Smooth[]{new Smooth(), new Smooth(), new Smooth()};
   private static final int[] need = new int[3];
   private static int card = -1;
   private static int top;
   private static int cy;
   private static int clip;
   private static int rows;
   private static int tail;
   private static int stackY;
   private static int rowY;
   private static boolean rule;
   private static final Smooth[] CHIP_W = new Smooth[7];
   private static final int[] chipNeed = new int[7];
   private static final int[] chipW = new int[7];
   private static final int[] chipX = new int[7];
   private static final int[] chipY = new int[7];
   private static boolean twoRows;
   private static int maxRow = 320;
   private static int boxWidth;
   private static int boxHeight;
   static int targetWidth;
   static int targetHeight;
   private static final Smooth GRAPH_TOP = new Smooth();
   private static final int[] column = new int[130];
   private static final int[] columnInk = new int[130];
   private static Seg biomeShown;
   private static final Seg BIOME_CUT;
   private static String biomeCutOf;
   private static int biomeCutFor;

   private HudLook() {
   }

   static void begin(Theme theme, InfoHudConfig cfg, Font f, float a, boolean right, float beat) {
      font = f;
      alpha = a;
      pulse = beat;
      rightSide = right;
      compact = cfg.mode() == InfoHudConfig.Mode.COMPACT;
      Palette p = theme.palette();
      vanilla = theme.isVanilla();
      radius = theme.radius();
      solid = (float)Mth.clamp(cfg.opacity, 0, 100) / 100.0F;
      shadowed = vanilla || cfg.opacity < 30;
      if (p != tonesOf) {
         tonesOf = p;
         accentInk = Colors.withAlpha(p.accentInk(), 255);
         int accent = Colors.withAlpha(p.accent(), 255);
         int track = vanilla ? -13619152 : Colors.withAlpha(p.surfaceHover(), 255);
         accentFill = Colors.contrast(accent, track) >= (double)3.0F ? accent : accentInk;
      }

      INK[0] = Colors.scaleAlpha(vanilla ? -1 : p.text(), a);
      INK[1] = Colors.scaleAlpha(vanilla ? -4934476 : p.textMuted(), a);
      INK[2] = Colors.scaleAlpha(vanilla ? -7566196 : Colors.lerp(p.textDim(), p.textMuted(), 0.4F), a);
      INK[3] = Colors.scaleAlpha(accentInk, a);
      INK[4] = Colors.scaleAlpha(vanilla ? -10402 : p.warning(), a);
      INK[5] = Colors.scaleAlpha(vanilla ? -43691 : p.danger(), a);
      iconInk = Colors.scaleAlpha(vanilla ? -2302756 : p.textMuted(), a);
      iconShade = Colors.scaleAlpha(-12961222, a);
      if (vanilla) {
         plateInk = Colors.scaleAlpha(-16777216, a * solidity(0.66F));
         shadowInk = 0;
         borderInk = 0;
         ruleInk = Colors.scaleAlpha(1090519039, a);
         trackInk = Colors.scaleAlpha(-802148304, a);
         wellInk = Colors.scaleAlpha(2013265920, a);
         wellEdge = Colors.scaleAlpha(822083583, a);
         faceInk = Colors.scaleAlpha(1879048192, a);
      } else {
         float plate = solidity(1.0F);
         plateInk = Colors.scaleAlpha(Colors.withAlpha(p.bg(), 255), a * plate);
         borderInk = Colors.scaleAlpha(p.border(), a * Math.min(1.0F, plate * 1.5F));
         shadowInk = Colors.scaleAlpha(p.shadow(), a * plate);
         ruleInk = Colors.scaleAlpha(p.border(), a);
         trackInk = Colors.scaleAlpha(Colors.withAlpha(p.surfaceHover(), 255), a);
         wellInk = Colors.scaleAlpha(Colors.withAlpha(Colors.brighten(p.bg(), -0.4F), 255), a * Math.max(solid, 0.5F));
         wellEdge = Colors.scaleAlpha(p.border(), a);
         faceInk = Colors.scaleAlpha(Colors.withAlpha(p.surface(), 255), a);
      }

      sparkInk = INK[3];
      barFine = accentFill;
      barWarn = vanilla ? -10402 : p.warning();
      barBad = vanilla ? -43691 : p.danger();
   }

   private static float solidity(float most) {
      if (solid <= 0.0F) {
         return 0.0F;
      } else {
         float least = 0.101960786F;
         return 0.101960786F + (most - 0.101960786F) * solid;
      }
   }

   static void measure(InfoHudConfig cfg, int rowRoom, boolean snap) {
      maxRow = Math.max(120, rowRoom);
      compact = cfg.mode() == InfoHudConfig.Mode.COMPACT;

      for(int i = 0; i < 3; ++i) {
         need[i] = 0;
      }

      if (compact) {
         chipNeeds(cfg);
         flow(true);
      } else {
         for(int i = 0; i < 7; ++i) {
            chipNeed[i] = 0;
         }

         pass = 0;
         full(cfg);
         int total = 0;

         for(int i = 0; i < 3; ++i) {
            if (need[i] > 0) {
               total += need[i] + 4;
            }
         }

         targetWidth = total > 0 ? 156 : 0;
         targetHeight = Math.max(0, total - 4);
      }

      for(int i = 0; i < 3; ++i) {
         if (snap) {
            HEIGHT[i].snap((float)need[i]);
         } else {
            HEIGHT[i].to((float)need[i]);
         }
      }

      if (snap) {
         for(int i = 0; i < 7; ++i) {
            CHIP_W[i].snap((float)chipNeed[i]);
         }

         if (compact) {
            flow(false);
         }
      }

      graphTop(cfg);
   }

   static void step(InfoHudConfig cfg, float quick, float easy) {
      for(int i = 0; i < 3; ++i) {
         HEIGHT[i].step(quick);
      }

      GRAPH_TOP.step(easy);
      ramLevel = level(HudReadings.ramPressure, ramLevel);
      vramLevel = level(InfoHud.VRAM_BAR.value, vramLevel);
      RAM_WARMTH.to((float)ramLevel);
      VRAM_WARMTH.to((float)vramLevel);
      RAM_WARMTH.step(quick);
      VRAM_WARMTH.step(quick);
      if (compact) {
         chipNeeds(cfg);

         for(int i = 0; i < 7; ++i) {
            CHIP_W[i].to((float)chipNeed[i]);
            CHIP_W[i].step(quick);
         }

         flow(false);
      }

   }

   static int width() {
      return compact ? boxWidth : 156;
   }

   static int height() {
      if (compact) {
         return boxHeight;
      } else {
         int h = 0;
         int last = 0;

         for(int i = 0; i < 3; ++i) {
            int hi = Math.round(HEIGHT[i].value);
            if (hi > 0) {
               last = Math.min(4, hi);
               h += hi + last;
            }
         }

         return h - last;
      }
   }

   private static void graphTop(InfoHudConfig cfg) {
      if (cfg.fps && cfg.graph && !compact) {
         float worst = 0.0F;
         int bars = Math.min(127, 144);

         for(int i = 1; i <= bars; ++i) {
            worst = Math.max(worst, FrameTimes.slice(i));
         }

         float usual = Math.max(2.0F, FrameTimes.frameMs());
         GRAPH_TOP.to(Mth.clamp(worst * 1.1F, usual * 2.0F, usual * 4.0F));
      }
   }

   static void paint(InfoHudConfig cfg) {
      HudCanvas.flat();

      for(pass = 1; pass <= 3; ++pass) {
         if (pass == 3) {
            HudCanvas.icons();
         }

         walk(cfg);
         if (pass == 2) {
            while(HudCanvas.nextTexture()) {
               walk(cfg);
            }
         }
      }

   }

   private static void walk(InfoHudConfig cfg) {
      if (compact) {
         chips(cfg);
      } else {
         full(cfg);
      }

   }

   private static void full(InfoHudConfig cfg) {
      boolean place = !HudReadings.reduced;
      boolean elsewhere = cfg.dimension && !HudReadings.overworld && !HudReadings.DIMENSION.empty();
      boolean vram = cfg.videoMemory && HudReadings.vramKnown;
      stackY = 0;
      card = -1;
      open(0);
      if (place) {
         headline(cfg);
      }

      if (place && cfg.chunk) {
         line(Icon.GRID, HudReadings.L_CHUNK, 1, HudReadings.CHUNK, 0, HudReadings.L_WITHIN, 1, HudReadings.WITHIN, 0);
      }

      if (place && cfg.biome) {
         if (pass == 0) {
            biomeShown = fitted(132 - (elsewhere ? HudReadings.DIMENSION.w + 8 : 0));
         }

         line(Icon.TREE, biomeShown, 0, (Seg)null, 0, elsewhere ? HudReadings.DIMENSION : null, 1, (Seg)null, 0);
      } else if (elsewhere) {
         line(Icon.WORLD, HudReadings.DIMENSION, 0, (Seg)null, 0, (Seg)null, 0, (Seg)null, 0);
      }

      if (place && cfg.light) {
         line(Icon.BULB, HudReadings.L_LIGHT, 1, HudReadings.LIGHT, 0, HudReadings.hasSky ? HudReadings.L_SKY : null, 1, HudReadings.hasSky ? HudReadings.SKY : null, 0);
      }

      if (cfg.time) {
         line(HudReadings.night ? Icon.MOON : Icon.SUN, HudReadings.CLOCK, 0, (Seg)null, 0, place ? HudReadings.L_DAY : null, 1, place ? HudReadings.DAY : null, 0);
      }

      if (cfg.speed && HudReadings.speedShown) {
         line(Icon.UPG_SPEED, HudReadings.L_SPEED, 1, (Seg)null, 0, HudReadings.SPEED, 0, (Seg)null, 0);
      }

      open(1);
      if (variant == 2) {
         performanceBeside(cfg);
      } else {
         performance(cfg);
      }

      open(2);
      if (cfg.memory) {
         meter(Icon.UPG_MEMORY, HudReadings.L_RAM, HudReadings.RATE, HudReadings.RAM, InfoHud.RAM_BAR.value, InfoHud.RAM_GHOST.value, RAM_WARMTH.value, ramLevel);
      }

      if (vram) {
         if (cfg.memory) {
            cy += 4;
         }

         meter(Icon.GPU, HudReadings.L_VRAM, (Seg)null, HudReadings.VRAM, InfoHud.VRAM_BAR.value, -1.0F, VRAM_WARMTH.value, vramLevel);
      }

      close();
   }

   private static void open(int index) {
      if (variant == 1 && index > 0) {
         if (rows > 0) {
            rule = true;
         }

      } else {
         close();
         card = index;
         top = stackY;
         cy = top + 6;
         rows = 0;
         tail = 0;
         rule = false;
         if (pass != 0) {
            int h = Math.round(HEIGHT[index].value);
            clip = top + h - 3;
            if (pass == 1 && h >= radius * 2 + 3) {
               plate(0, top, 156, h, radius);
            }

         }
      }
   }

   private static void close() {
      if (card >= 0) {
         int h;
         if (pass == 0) {
            h = rows == 0 ? 0 : cy - tail - top + 6;
            need[card] = h;
         } else {
            h = Math.round(HEIGHT[card].value);
         }

         if (h > 0) {
            stackY = top + h + Math.min(4, h);
         }

         card = -1;
      }
   }

   private static boolean row(int h, int blank) {
      if (rule) {
         rule = false;
         if (rows > 0) {
            int at = cy - tail + 4;
            if (pass == 1 && at + 4 <= clip) {
               HudCanvas.hline(6, at, 144, ruleInk);
            }

            cy = at + 1 + 4;
         }
      }

      ++rows;
      rowY = cy;
      cy += h;
      tail = blank;
      return pass != 0 && cy - blank <= clip;
   }

   private static boolean has(@Nullable Seg seg) {
      return seg != null && !seg.s.isEmpty();
   }

   private static void word(Seg seg, int x, int y, int ink) {
      int color = INK[ink];
      if (Colors.alpha(color) >= 8) {
         HudCanvas.word(font, seg, x, y, color, shadowed);
      }
   }

   private static void wordRight(Seg seg, int right, int y, int ink) {
      word(seg, right - seg.w + 1, y, ink);
   }

   private static void icon(Icon icon, int x, int y, int color) {
      if (shadowed) {
         HudCanvas.icon(icon, (float)(x + 1), (float)(y + 1), 8.0F, Colors.scaleAlpha(iconShade, (float)Colors.alpha(color) / 255.0F));
      }

      HudCanvas.icon(icon, (float)x, (float)y, 8.0F, color);
   }

   private static void headline(InfoHudConfig cfg) {
      if (cfg.position || cfg.facing) {
         if (row(14, 3)) {
            int y = rowY;
            if (!cfg.position) {
               if (pass == 3) {
                  icon(Icon.COMPASS, 6, y + 1, iconInk);
               } else if (pass == 2) {
                  word(HudReadings.FACING, 18, y + 2, 0);
                  wordRight(HudReadings.FACING_AXIS, 150, y + 2, 1);
               }

            } else {
               Seg position = HudReadings.POSITION;
               Seg point = HudReadings.FACING_SHORT;
               Seg axis = HudReadings.FACING_AXIS;
               int whole = 4 + point.w + 3 + axis.w + 3;
               int small = 4 + point.w + 3;
               int chip = cfg.facing && has(point) ? (12 + position.w + 6 + whole <= 144 ? whole : (12 + position.w + 6 + small <= 144 ? small : 0)) : 0;
               boolean mark = 12 + position.w + (chip > 0 ? 6 + chip : 0) <= 145;
               int x = mark ? 18 : 6;
               switch (pass) {
                  case 1:
                     if (chip > 0) {
                        int cx = 150 - chip;
                        if (vanilla) {
                           HudCanvas.rect(cx, y, chip, 11, faceInk);
                        } else {
                           HudCanvas.pixelRound(cx, y, chip, 11, faceInk, Math.min(2, radius));
                           HudCanvas.outline(cx, y, chip, 11, borderInk == 0 ? ruleInk : borderInk, Math.min(2, radius));
                        }
                     }
                     break;
                  case 2:
                     word(position, x, y + 2, 0);
                     if (chip > 0) {
                        int cx = 150 - chip;
                        word(point, cx + 4, y + 2, 0);
                        if (chip == whole) {
                           word(axis, cx + 4 + point.w + 3, y + 2, 1);
                        }
                     }
                     break;
                  case 3:
                     if (mark) {
                        icon(Icon.LOCATION, 6, y + 1, iconInk);
                     }
               }

            }
         }
      }
   }

   private static void line(Icon icon, @Nullable Seg a, int inkA, @Nullable Seg b, int inkB, @Nullable Seg c, int inkC, @Nullable Seg d, int inkD) {
      boolean hasA = has(a);
      boolean hasB = has(b);
      boolean hasC = has(c);
      boolean hasD = has(d);
      if (hasA || hasB || hasC || hasD) {
         if (row(11, 3)) {
            int y = rowY;
            if (pass == 3) {
               icon(icon, 6, y, iconInk);
            } else if (pass == 2) {
               int left = (hasA ? a.w + 3 : 0) + (hasB ? b.w : 0);
               int right = (hasC ? c.w + 3 : 0) + (hasD ? d.w : 0);
               if ((hasA || hasB) && 12 + left + 6 + right > 145) {
                  hasD = false;
                  hasC = false;
               }

               int x = 18;
               if (hasA) {
                  word(a, x, y + 1, inkA);
                  x += a.w + 3;
               }

               if (hasB) {
                  word(b, x, y + 1, inkB);
               }

               int end = 150;
               if (hasD) {
                  wordRight(d, end, y + 1, inkD);
                  end -= d.w + 3;
               }

               if (hasC) {
                  wordRight(c, end, y + 1, inkC);
               }
            }

         }
      }
   }

   private static Seg fitted(int room) {
      Seg whole = HudReadings.BIOME;
      if (whole.w > room && font != null) {
         if (!whole.s.equals(biomeCutOf) || biomeCutFor != room) {
            biomeCutOf = whole.s;
            biomeCutFor = room;
            String dots = "...";
            BIOME_CUT.set(font.plainSubstrByWidth(whole.s, Math.max(0, room - font.width("..."))).stripTrailing() + "...", font);
         }

         return BIOME_CUT;
      } else {
         return whole;
      }
   }

   private static void performance(InfoHudConfig cfg) {
      if (cfg.fps || cfg.latency) {
         if (row(27, 1)) {
            int x = 6;
            if (cfg.fps) {
               Seg caption = !cfg.graph && !cfg.latency ? HudReadings.FRAME : HudReadings.LOW;
               int column = Math.max(72, HudReadings.FPS.w * 2 + 1 + HudReadings.L_FPS.w + 8);
               hero(x, HudReadings.FPS, HudReadings.fpsInk, HudReadings.L_FPS, fpsIcon(), fpsIconInk(), caption, cfg.latency ? column - 4 : 144);
               x += column;
            }

            if (cfg.latency) {
               hero(x, HudReadings.NET, HudReadings.netInk, HudReadings.L_MS, netIcon(), netIconInk(), HudReadings.local ? HudReadings.L_SERVER : HudReadings.L_PING, 150 - x);
            }
         }

         if (cfg.fps && cfg.graph && row(17, 0)) {
            Seg now = HudReadings.FRAME_MS;
            int room = Math.max(40, now.w + 5);
            graph(6, rowY + 3, 144 - room, 14);
            if (pass == 2 && has(now)) {
               wordRight(now, 150, rowY + 3 + 3, 1);
            }
         }

      }
   }

   private static void hero(int x, Seg numeral, int ink, Seg unit, Icon icon, int iconColor, @Nullable Seg caption, int room) {
      int y = rowY;
      boolean marked = has(caption) && 12 + caption.w - 1 <= room;
      if (pass == 2) {
         numeral(numeral, x, y, ink);
         word(unit, x + numeral.w * 2 + 1, y + 7, 1);
         if (has(caption)) {
            word(caption, marked ? x + 12 : x, y + 19, 1);
         }
      } else if (pass == 3 && marked) {
         icon(icon, x, y + 18, iconColor);
      }

   }

   private static void numeral(Seg seg, int x, int y, int ink) {
      int color = INK[ink];
      if (Colors.alpha(color) >= 8) {
         HudCanvas.word(font, seg, x, y, 2, color, shadowed);
      }

   }

   private static boolean wrong(int ink) {
      return ink == 4 || ink == 5;
   }

   private static Icon fpsIcon() {
      return wrong(HudReadings.fpsInk) ? Icon.WARNING : Icon.GAUGE;
   }

   private static int fpsIconInk() {
      return wrong(HudReadings.fpsInk) ? INK[HudReadings.fpsInk] : iconInk;
   }

   private static Icon netIcon() {
      return wrong(HudReadings.netQuality) ? Icon.WARNING : (HudReadings.local ? Icon.SERVER : Icon.SIGNAL);
   }

   private static int netIconInk() {
      return HudReadings.netQuality == 1 ? iconInk : INK[HudReadings.netQuality];
   }

   private static void performanceBeside(InfoHudConfig cfg) {
      if (cfg.fps) {
         if (row(16, 2)) {
            Seg numeral = HudReadings.FPS;
            if (pass == 2) {
               numeral(numeral, 6, rowY, HudReadings.fpsInk);
               word(HudReadings.L_FPS, 6 + numeral.w * 2 + 1, rowY + 7, 1);
            }

            int from = numeral.w * 2 + 1 + HudReadings.L_FPS.w + 6;
            if (cfg.graph && 144 - from > 24) {
               graph(6 + from, rowY, 144 - from, 14);
            }
         }

         if (has(HudReadings.FRAME) && row(10, 2) && pass == 2) {
            word(HudReadings.FRAME, 6, rowY + 1, 1);
         }
      }

      if (cfg.latency) {
         if (cfg.fps) {
            rule = true;
         }

         if (row(11, 3)) {
            if (pass == 3) {
               icon(netIcon(), 6, rowY, netIconInk());
            } else if (pass == 2) {
               word(HudReadings.local ? HudReadings.L_SERVER : HudReadings.L_PING, 18, rowY + 1, 1);
               wordRight(HudReadings.L_MS, 150, rowY + 1, 1);
               wordRight(HudReadings.NET, 150 - HudReadings.L_MS.w - 3, rowY + 1, HudReadings.netInk);
            }
         }
      }

   }

   private static void graph(int x, int y, int w, int h) {
      if (pass == 1 && w >= 8) {
         if (vanilla) {
            HudCanvas.rect(x, y, w, h, wellInk);
            HudCanvas.outline(x, y, w, h, wellEdge, 0);
         } else {
            HudCanvas.pixelRound(x, y, w, h, wellInk, 1);
            HudCanvas.outline(x, y, w, h, wellEdge, 1);
         }

         int n = Math.min(127, w - 4);
         int rise = h - 4;
         int x0 = x + w - 2 - n;
         int base = y + h - 2;
         float topMs = Math.max(1.0F, GRAPH_TOP.value);

         for(int i = 0; i < n; ++i) {
            float ms = FrameTimes.slice(n - i);
            column[i + 1] = ms <= 0.0F ? 0 : Mth.clamp(Math.round(ms / topMs * (float)rise), 1, rise);
            columnInk[i + 1] = ms > 50.0F ? INK[5] : (ms > 33.4F ? INK[4] : sparkInk);
         }

         column[0] = column[1];
         column[n + 1] = column[n];
         int from = 0;
         int height = -1;
         int foot = 0;
         int ink = 0;

         for(int i = 0; i <= n; ++i) {
            int hi = -1;
            int lo = 0;
            int color = 0;
            if (i < n) {
               hi = column[i + 1];
               lo = Math.min(hi, Math.min(column[i], column[i + 2]) + 1);
               color = columnInk[i + 1];
            }

            if (hi != height || lo != foot || color != ink) {
               if (height > 0) {
                  int rx = x0 + from;
                  int rw = i - from;
                  HudCanvas.rect(rx, base - height, rw, height - foot + 1, ink);
                  if (foot > 1) {
                     HudCanvas.rect(rx, base - foot + 1, rw, foot - 1, Colors.scaleAlpha(ink, ink == sparkInk ? 0.2F : 0.36F));
                  }
               }

               from = i;
               height = hi;
               foot = lo;
               ink = color;
            }
         }

      }
   }

   private static void meter(Icon icon, Seg label, @Nullable Seg extra, Seg value, float fill, float ghost, float warmth, int level) {
      if (row(16, 0)) {
         int y = rowY;
         switch (pass) {
            case 1:
               bar(6, y + 16 - 5, 144, 5, fill, ghost, barInk(warmth));
               break;
            case 2:
               word(label, 18, y + 1, 1);
               if (has(extra) && 12 + label.w + 4 + Math.max(extra.w, 46) + 6 + value.w <= 145) {
                  word(extra, 18 + label.w + 4, y + 1, 2);
               }

               wordRight(value, 150, y + 1, level == 0 ? 0 : levelInk(level));
               break;
            case 3:
               barIcon(icon, 6, y, level);
         }

      }
   }

   private static int level(float fraction, int was) {
      if (!(fraction >= 0.95F) && (was != 2 || !(fraction >= 0.935F))) {
         return !(fraction >= 0.85F) && (was < 1 || !(fraction >= 0.835F)) ? 0 : 1;
      } else {
         return 2;
      }
   }

   private static int levelInk(int level) {
      return level >= 2 ? 5 : 4;
   }

   private static void barIcon(Icon icon, int x, int y, int level) {
      if (level == 0) {
         icon(icon, x, y, iconInk);
      } else {
         icon(Icon.WARNING, x, y, INK[levelInk(level)]);
      }

   }

   private static int barInk(float warmth) {
      int c;
      if (warmth <= 1.0F) {
         c = Colors.lerp(barFine, barWarn, warmth);
      } else if (warmth < 1.99F) {
         c = Colors.lerp(barWarn, barBad, warmth - 1.0F);
      } else {
         c = Colors.brighten(barBad, 0.22F * pulse);
      }

      return Colors.scaleAlpha(c, alpha);
   }

   private static void bar(int x, int y, int w, int h, float fill, float ghost, int ink) {
      if (w > 2) {
         int cut = vanilla ? 0 : 1;
         capsule(x, y, w, h, w, cut, trackInk);
         if (ghost > 0.0F) {
            capsule(x, y, w, h, Math.round((float)w * Mth.clamp(ghost, 0.0F, 1.0F)), cut, Colors.scaleAlpha(ink, 0.3F));
         }

         float f = Mth.clamp(fill, 0.0F, 1.0F);
         if (f > 0.0F) {
            capsule(x, y, w, h, Math.max(2, Math.round((float)w * f)), cut, ink);
         }

      }
   }

   private static void capsule(int x, int y, int w, int h, int length, int cut, int color) {
      if (length > 0) {
         if (cut == 0) {
            HudCanvas.rect(x, y, length, h, color);
         } else {
            HudCanvas.rect(x, y + cut, length, h - cut * 2, color);
            int from = x + cut;
            int to = Math.min(x + length, x + w - cut);
            if (to > from) {
               HudCanvas.rect(from, y, to - from, cut, color);
               HudCanvas.rect(from, y + h - cut, to - from, cut, color);
            }

         }
      }
   }

   private static void plate(int x, int y, int w, int h, int r) {
      if (!(solid <= 0.0F)) {
         if (vanilla) {
            HudCanvas.rect(x, y, w, h, plateInk);
         } else {
            HudCanvas.shadow(x, y, w, h, shadowInk, r);
            HudCanvas.pixelRound(x, y, w, h, plateInk, r);
            HudCanvas.outline(x, y, w, h, borderInk, r);
         }
      }
   }

   private static void chipNeeds(InfoHudConfig cfg) {
      boolean place = !HudReadings.reduced;
      boolean elsewhere = cfg.dimension && !HudReadings.overworld && !HudReadings.DIMENSION.empty();
      int lead = 15;
      int end = 4;
      if (place && cfg.position) {
         chipNeed[0] = 15 + HudReadings.POSITION.w + (cfg.facing && has(HudReadings.FACING_SHORT) ? 4 + HudReadings.FACING_SHORT.w : 0) + 4;
      } else if (place && cfg.facing && has(HudReadings.FACING)) {
         chipNeed[0] = 15 + HudReadings.FACING.w + 4 + HudReadings.FACING_AXIS.w + 4;
      } else {
         chipNeed[0] = 0;
      }

      if (place && cfg.biome && has(HudReadings.BIOME)) {
         biomeShown = fitted(96);
         chipNeed[1] = 15 + biomeShown.w + 4;
      } else {
         chipNeed[1] = 0;
      }

      chipNeed[2] = elsewhere ? 15 + HudReadings.DIMENSION.w + 4 : 0;
      chipNeed[3] = cfg.fps && has(HudReadings.FPS) ? 15 + HudReadings.FPS.w + 2 + HudReadings.L_FPS.w + 4 : 0;
      chipNeed[4] = cfg.latency && has(HudReadings.NET) ? 15 + HudReadings.NET.w + 2 + HudReadings.L_MS.w + 4 : 0;
      chipNeed[5] = cfg.memory ? 42 : 0;
      chipNeed[6] = cfg.videoMemory && HudReadings.vramKnown ? 42 : 0;
   }

   private static void flow(boolean wanted) {
      int all = 0;
      int first = 0;
      int second = 0;

      for(int i = 0; i < 7; ++i) {
         int w = wanted ? chipNeed[i] : Math.round(CHIP_W[i].value);
         int shown = w >= 16 ? w : 0;
         if (!wanted) {
            chipW[i] = shown;
         }

         if (shown != 0) {
            all += shown + 3;
            if (i < 3) {
               first += shown + 3;
            } else {
               second += shown + 3;
            }
         }
      }

      if (wanted) {
         if (all - 3 > maxRow) {
            twoRows = true;
         } else if (all - 3 < maxRow - 16) {
            twoRows = false;
         }
      }

      boolean two = twoRows && first > 0 && second > 0;
      int width = Math.max(0, (two ? Math.max(first, second) : all) - 3);
      int height = all == 0 ? 0 : (two ? 29 : 13);
      if (wanted) {
         targetWidth = width;
         targetHeight = height;
      } else {
         boxWidth = width;
         boxHeight = height;
         int x = rightSide ? width - (two ? first : all) + 3 : 0;
         int y = 0;

         for(int i = 0; i < 7; ++i) {
            if (two && i == 3) {
               x = rightSide ? width - second + 3 : 0;
               y = 16;
            }

            chipX[i] = x;
            chipY[i] = y;
            if (chipW[i] > 0) {
               x += chipW[i] + 3;
            }
         }

      }
   }

   private static void chips(InfoHudConfig cfg) {
      boolean place = !HudReadings.reduced;
      if (chip(0)) {
         if (place && cfg.position) {
            chipWords(0, Icon.LOCATION, iconInk, HudReadings.POSITION, 0, cfg.facing ? HudReadings.FACING_SHORT : null, 1, 4);
         } else {
            chipWords(0, Icon.COMPASS, iconInk, HudReadings.FACING, 0, HudReadings.FACING_AXIS, 1, 4);
         }
      }

      if (chip(1)) {
         chipWords(1, Icon.TREE, iconInk, biomeShown, 0, (Seg)null, 0, 0);
      }

      if (chip(2)) {
         chipWords(2, Icon.WORLD, iconInk, HudReadings.DIMENSION, 0, (Seg)null, 0, 0);
      }

      if (chip(3)) {
         chipWords(3, fpsIcon(), fpsIconInk(), HudReadings.FPS, HudReadings.fpsInk, HudReadings.L_FPS, 1, 2);
      }

      if (chip(4)) {
         chipWords(4, netIcon(), netIconInk(), HudReadings.NET, HudReadings.netInk, HudReadings.L_MS, 1, 2);
      }

      if (chip(5)) {
         chipBar(5, Icon.UPG_MEMORY, InfoHud.RAM_BAR.value, InfoHud.RAM_GHOST.value, RAM_WARMTH.value, ramLevel);
      }

      if (chip(6)) {
         chipBar(6, Icon.GPU, InfoHud.VRAM_BAR.value, -1.0F, VRAM_WARMTH.value, vramLevel);
      }

   }

   private static boolean chip(int kind) {
      int w = chipW[kind];
      if (w <= 0) {
         return false;
      } else {
         if (pass == 1) {
            plate(chipX[kind], chipY[kind], w, 13, Math.min(2, radius));
         }

         return w >= chipNeed[kind] - 6;
      }
   }

   private static void chipWords(int kind, Icon icon, int iconColor, Seg value, int ink, @Nullable Seg more, int moreInk, int gap) {
      int x = chipX[kind];
      int y = chipY[kind];
      if (pass == 3) {
         icon(icon, x + 4, y + 2, iconColor);
      } else if (pass == 2) {
         int tx = x + 4 + 8 + 3;
         word(value, tx, y + 3, ink);
         if (has(more)) {
            word(more, tx + value.w + gap, y + 3, moreInk);
         }
      }

   }

   private static void chipBar(int kind, Icon icon, float fill, float ghost, float warmth, int level) {
      int x = chipX[kind];
      int y = chipY[kind];
      if (pass == 3) {
         barIcon(icon, x + 4, y + 2, level);
      } else if (pass == 1) {
         bar(x + 4 + 8 + 3, y + 4, 22, 5, fill, ghost, barInk(warmth));
      }

   }

   static {
      biomeShown = HudReadings.BIOME;
      BIOME_CUT = new Seg();
      biomeCutOf = "";
      biomeCutFor = -1;

      for(int i = 0; i < 7; ++i) {
         CHIP_W[i] = new Smooth();
      }

   }
}
