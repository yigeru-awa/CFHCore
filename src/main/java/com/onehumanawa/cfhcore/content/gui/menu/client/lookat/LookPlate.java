package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class LookPlate {
   private static final int MEASURE = 0;
   private static final int FILL = 1;
   private static final int WORDS = 2;
   private static final int ICONS = 3;
   private static final int EFFECTS = 4;
   private static final int TEXT = 0;
   private static final int MUTED = 1;
   private static final int GOLD = 2;
   private static final int PAD = 5;
   static final int WELL_X = 5;
   static final int WELL_Y = 5;
   static final int WELL_W = 26;
   static final int WELL_H = 28;
   private static final int WELL_GAP = 5;
   private static final int ROW = 10;
   private static final int ICON = 8;
   private static final int TEXT_X = 11;
   private static final int BAR = 4;
   private static final int BAR_ROW = 7;
   private static final int COLUMN_MIN = 72;
   private static final int COLUMN_MAX = 140;
   private static final int EFFECT = 9;
   private static final int EFFECT_GAP = 2;
   private static final int ABSORPTION = -998324;
   private static final int STEEL = -4931380;
   private static final int PALE = -856346;
   static int width;
   static int height;
   static boolean well;
   private static final Glide FILLED = new Glide();
   private static final Glide TRAIL = new Glide();
   private static int pass;
   private static @Nullable GuiGraphics graphics;
   private static @Nullable Font font;
   private static boolean vanilla;
   private static boolean portrait;
   private static int radius;
   private static final int[] INK = new int[3];
   private static int plateInk;
   private static int borderInk;
   private static int shadowInk;
   private static int wellInk;
   private static int wellEdge;
   private static int faceInk;
   private static int faceEdge;
   private static int trackInk;
   private static int healthInk;
   private static int paleInk;
   private static int steelInk;
   private static int iconInk;
   private static int iconShade;
   private static int spriteInk;
   private static int column;
   private static int textX;
   private static int cy;
   private static int need;

   private LookPlate() {
   }

   static void measure(LookAtConfig cfg, boolean figure) {
      portrait = cfg.portrait && figure;
      pass = 0;
      need = 0;
      column = 140;
      walk(cfg);
      int rows = cy - 5;
      well = portrait;
      if (rows <= 0 && !well) {
         height = 0;
         width = 0;
      } else {
         column = rows <= 0 ? 0 : Mth.clamp(need, 72, 140);
         width = 5 + (well ? 26 : 0) + (well && rows > 0 ? 5 : 0) + column + 5;
         height = 5 + Math.max(well ? 28 : 0, rows - 1) + 5;
      }
   }

   static void step(float quick, float slow) {
      FILLED.to(LookFacts.health);
      float filled = FILLED.step(quick);
      TRAIL.to(LookFacts.health);
      if (TRAIL.step(slow) < filled) {
         TRAIL.snap(filled);
      }

   }

   static void settle() {
      FILLED.snap(LookFacts.health);
      TRAIL.snap(LookFacts.health);
   }

   static void begin(Theme theme, Font f, float alpha) {
      font = f;
      vanilla = theme.isVanilla();
      radius = theme.radius();
      Palette p = theme.palette();
      if (vanilla) {
         INK[0] = Colors.scaleAlpha(-1, alpha);
         INK[1] = Colors.scaleAlpha(-4934476, alpha);
         INK[2] = Colors.scaleAlpha(-10402, alpha);
         plateInk = Colors.scaleAlpha(-1879048192, alpha);
         faceEdge = 0;
         shadowInk = 0;
         borderInk = 0;
         wellInk = Colors.scaleAlpha(2013265920, alpha);
         wellEdge = Colors.scaleAlpha(822083583, alpha);
         faceInk = Colors.scaleAlpha(1879048192, alpha);
         trackInk = Colors.scaleAlpha(956301311, alpha);
         healthInk = Colors.scaleAlpha(-43691, alpha);
         paleInk = Colors.scaleAlpha(-1, alpha);
         steelInk = iconInk = Colors.scaleAlpha(-2302756, alpha);
      } else {
         INK[0] = Colors.scaleAlpha(p.text(), alpha);
         INK[1] = Colors.scaleAlpha(p.textMuted(), alpha);
         INK[2] = Colors.scaleAlpha(-998324, alpha);
         plateInk = Colors.scaleAlpha(Colors.withAlpha(p.bg(), theme.translucentPanels() ? 196 : 236), alpha);
         borderInk = Colors.scaleAlpha(p.border(), alpha);
         shadowInk = Colors.scaleAlpha(p.shadow(), alpha);
         wellInk = Colors.scaleAlpha(Colors.withAlpha(Colors.brighten(p.bg(), -0.4F), 255), alpha);
         wellEdge = faceEdge = borderInk;
         faceInk = Colors.scaleAlpha(Colors.withAlpha(p.surface(), 255), alpha);
         trackInk = Colors.scaleAlpha(Colors.withAlpha(p.text(), 42), alpha);
         healthInk = Colors.scaleAlpha(Colors.withAlpha(p.danger(), 255), alpha);
         paleInk = Colors.scaleAlpha(-856346, alpha);
         steelInk = Colors.scaleAlpha(-4931380, alpha);
         iconInk = Colors.scaleAlpha(p.textMuted(), alpha);
      }

      iconShade = Colors.scaleAlpha(-12961222, alpha);
      spriteInk = Colors.scaleAlpha(-1, alpha);
   }

   static void paint(GuiGraphics g, LookAtConfig cfg) {
      graphics = g;
      LookCanvas.begin(g);

      try {
         if (vanilla) {
            CFHGuiDraw.rect(g, 0, 0, width, height, plateInk);
         } else {
            CFHGuiDraw.shadow(g, 0, 0, width, height, shadowInk, radius);
            CFHGuiDraw.pixelRound(g, 0, 0, width, height, plateInk, radius);
            CFHGuiDraw.outline(g, 0, 0, width, height, borderInk, radius);
         }

         if (well) {
            int r = vanilla ? 0 : 1;
            CFHGuiDraw.pixelRound(g, 5, 5, 26, 28, wellInk, r);
            CFHGuiDraw.outline(g, 5, 5, 26, 28, wellEdge, r);
         }

         pass = 1;
         walk(cfg);
         pass = 2;
         walk(cfg);
         LookCanvas.texture(Icons.ATLAS);
         pass = 3;
         walk(cfg);
         if (cfg.effects && LookFacts.effects > 0 && LookFacts.EFFECT[0] != null) {
            LookCanvas.texture(LookFacts.EFFECT[0].atlasLocation());
            pass = 4;
            walk(cfg);
         }
      } finally {
         LookCanvas.end();
         graphics = null;
      }

   }

   private static void walk(LookAtConfig cfg) {
      textX = 5 + (portrait ? 31 : 0);
      cy = 5;
      headline(cfg);
      if (cfg.kind && has(LookFacts.KIND)) {
         if (pass == 2) {
            word(LookFacts.KIND, textX, cy + 1, 1);
         }

         row(LookFacts.KIND.w);
      }

      vitals(cfg);
      if (cfg.owner && LookFacts.tamed) {
         boolean known = has(LookFacts.OWNER);
         line(Icon.USER, known ? LookFacts.L_OWNER : LookFacts.L_TAMED, 1, known ? LookFacts.OWNER : null, 0);
      }

      if (cfg.villager && has(LookFacts.VILLAGER)) {
         line(Icon.STAR, LookFacts.VILLAGER, 0, (Line)null, 0);
      }

      if (cfg.mount && LookFacts.mount) {
         mount();
      }

      if (cfg.effects && LookFacts.effects > 0) {
         effects();
      }

   }

   private static void row(int w, int h) {
      if (w > need) {
         need = w;
      }

      cy += h;
   }

   private static void row(int w) {
      row(w, 10);
   }

   private static boolean has(@Nullable Line line) {
      return line != null && !line.s.isEmpty();
   }

   private static void word(Line line, int x, int y, int ink) {
      int color = INK[ink];
      if (Colors.alpha(color) >= 8) {
         LookCanvas.word(font, line.seq, x, y, color, vanilla);
      }

   }

   private static void wordRight(Line line, int right, int y, int ink) {
      word(line, right - line.w + 1, y, ink);
   }

   private static void icon(Icon icon, int x, int y, int color) {
      if (vanilla) {
         LookCanvas.icon(icon, x + 1, y + 1, 8, Colors.scaleAlpha(iconShade, (float)Colors.alpha(color) / 255.0F));
      }

      LookCanvas.icon(icon, x, y, 8, color);
   }

   private static void headline(LookAtConfig cfg) {
      boolean name = cfg.name && has(LookFacts.NAME);
      boolean young = cfg.baby && LookFacts.baby && has(LookFacts.L_BABY);
      if (name || young) {
         int chip = young ? LookFacts.L_BABY.w + 5 : 0;
         int chipX = name ? textX + column - chip : textX;
         switch (pass) {
            case 1:
               if (young) {
                  int r = vanilla ? 0 : Math.min(2, radius);
                  CFHGuiDraw.pixelRound(graphics, chipX, cy, chip, 10, faceInk, r);
                  CFHGuiDraw.outline(graphics, chipX, cy, chip, 10, faceEdge, r);
               }
               break;
            case 2:
               if (name) {
                  word(LookFacts.NAME, textX, cy + 1, 0);
               }

               if (young) {
                  word(LookFacts.L_BABY, chipX + 3, cy + 1, 1);
               }
         }

         row((name ? LookFacts.NAME.w : 0) + (name && young ? 4 : 0) + chip, 11);
      }
   }

   private static void vitals(LookAtConfig cfg) {
      boolean health = cfg.health && has(LookFacts.HEALTH);
      boolean armor = cfg.armor && has(LookFacts.ARMOR);
      if (health || armor) {
         boolean extra = health && has(LookFacts.ABSORPTION);
         int numbers = health ? 11 + Math.max(LookFacts.HEALTH.w, LookFacts.healthRoom) + (extra ? 2 + LookFacts.ABSORPTION.w : 0) : 0;
         int shield = armor ? 10 + LookFacts.ARMOR.w : 0;
         int shieldX = health ? textX + column - shield : textX;
         if (pass == 2) {
            if (health) {
               word(LookFacts.HEALTH, textX + 11, cy + 1, 0);
               if (extra) {
                  word(LookFacts.ABSORPTION, textX + 11 + LookFacts.HEALTH.w + 2, cy + 1, 2);
               }
            }

            if (armor) {
               word(LookFacts.ARMOR, shieldX + 11 - 1, cy + 1, 0);
            }
         } else if (pass == 3) {
            if (health) {
               icon(Icon.HEART_FILLED, textX, cy, healthInk);
            }

            if (armor) {
               icon(Icon.SHIELD, shieldX, cy, steelInk);
            }
         }

         row(numbers + (health && armor ? 6 : 0) + shield);
         if (health) {
            if (pass == 1) {
               bar(textX, cy + 1, column);
            }

            row(0, 7);
         }
      }
   }

   private static void line(Icon icon, Line a, int inkA, @Nullable Line b, int inkB) {
      if (pass == 3) {
         icon(icon, textX, cy, iconInk);
      } else if (pass == 2) {
         word(a, textX + 11, cy + 1, inkA);
         if (has(b)) {
            word(b, textX + 11 + a.w + 3, cy + 1, inkB);
         }
      }

      row(11 + a.w + (has(b) ? 3 + b.w : 0));
   }

   private static void mount() {
      Line speed = LookFacts.SPEED;
      Line jump = LookFacts.JUMP;
      int jumpX = textX + column - 11 - jump.w + 1;
      if (pass == 3) {
         icon(Icon.UPG_SPEED, textX, cy, iconInk);
         icon(Icon.ARROW_UP, jumpX, cy, iconInk);
      } else if (pass == 2) {
         word(speed, textX + 11, cy + 1, 0);
         wordRight(jump, textX + column, cy + 1, 0);
      }

      row(11 + speed.w + 6 + 11 + jump.w);
   }

   private static void effects() {
      int n = LookFacts.effects;
      if (pass == 4) {
         for(int i = 0; i < n; ++i) {
            if (LookFacts.EFFECT[i] != null) {
               LookCanvas.sprite(LookFacts.EFFECT[i], textX + i * 11, cy + 1, 9, spriteInk);
            }
         }
      } else if (pass == 2 && has(LookFacts.MORE)) {
         word(LookFacts.MORE, textX + n * 11 + 1, cy + 2, 1);
      }

      row(n * 11 - 2 + (has(LookFacts.MORE) ? 3 + LookFacts.MORE.w : 0), 11);
   }

   private static void bar(int x, int y, int w) {
      if (w > 2) {
         int cut = vanilla ? 0 : 1;
         piece(x, y, w, 0, w, cut, trackInk);
         float filled = Mth.clamp(FILLED.value, 0.0F, 1.0F);
         int red = filled <= 0.0F ? 0 : Math.max(1, Math.round((float)w * filled));
         int pale = Math.round((float)w * Mth.clamp(TRAIL.value, 0.0F, 1.0F));
         if (pale > red) {
            piece(x, y, w, red, pale, cut, paleInk);
         }

         piece(x, y, w, 0, red, cut, healthInk);
         int gold = Math.min(w, red + Math.round((float)w * LookFacts.absorption));
         if (gold > red) {
            piece(x, y, w, red, gold, cut, INK[2]);
         }

      }
   }

   private static void piece(int x, int y, int w, int from, int to, int cut, int color) {
      if (to > from) {
         CFHGuiDraw.rect(graphics, x + from, y + cut, to - from, 4 - cut * 2, color);
         if (cut != 0) {
            int a = Math.max(from, cut);
            int b = Math.min(to, w - cut);
            if (b > a) {
               CFHGuiDraw.rect(graphics, x + a, y, b - a, cut, color);
               CFHGuiDraw.rect(graphics, x + a, y + 4 - cut, b - a, cut, color);
            }

         }
      }
   }
}
