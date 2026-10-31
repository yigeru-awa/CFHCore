package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class ContainerMotion {
   public static final int MIN_MS = 60;
   public static final int MAX_MS = 300;
   public static final int DEFAULT_MS = 110;
   private static final float POP_FROM = 0.92F;
   private static final float SLIDE_FROM = 12.0F;
   private static final float HOLD = holdAt();
   private static @Nullable Screen forScreen;
   private static Style style;
   private static long startMs;
   private static int durationMs;
   private static boolean pushed;
   private static @Nullable Screen drawing;
   private static float scale;
   private static float shiftY;
   private static float centreX;
   private static float centreY;
   private static float eased;
   private static final Matrix4f BEFORE;
   private static final Matrix4f MOVED;
   private static boolean side;
   private static float sideX;
   private static float sideY;
   private static final Matrix4f REST;

   private ContainerMotion() {
   }

   private static float holdAt() {
      try {
         String v = System.getProperty("cfhgui.containerOpenAt");
         return v == null ? -1.0F : Mth.clamp(Float.parseFloat(v), 0.0F, 1.0F);
      } catch (RuntimeException var1) {
         return -1.0F;
      }
   }

   public static void onScreenChanged(@Nullable Screen previous, @Nullable Screen next) {
      if (next != previous) {
         forScreen = null;
         if (previous == null && next instanceof AbstractContainerScreen) {
            Theme theme = Theme.current();
            Style wanted = ContainerMotion.Style.parse(CFHGui.config().containerOpen);
            int ms = theme.ms(Mth.clamp(CFHGui.config().containerOpenMs, 60, 300));
            if (wanted != ContainerMotion.Style.OFF && ms > 0) {
               forScreen = next;
               style = wanted;
               durationMs = ms;
               startMs = -1L;
            }
         }
      }
   }

   public static void begin(Screen screen, GuiGraphics g) {
      if (!pushed && forScreen == screen) {
         long now = Clock.nowMs();
         if (startMs < 0L) {
            startMs = now;
         }

         float t = HOLD >= 0.0F ? HOLD : (float)(now - startMs) / (float)durationMs;
         if (t >= 1.0F) {
            forScreen = null;
         } else {
            float out = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
            eased = 1.0F - out * out * out;
            scale = style == ContainerMotion.Style.POP ? Mth.lerp(eased, 0.92F, 1.0F) : 1.0F;
            shiftY = style == ContainerMotion.Style.SLIDE ? (1.0F - eased) * 12.0F : 0.0F;
            centreX = (float)screen.width / 2.0F;
            centreY = (float)screen.height / 2.0F;
            BEFORE.set(g.pose().last().pose());
            g.pose().pushPose();
            g.pose().translate(centreX, centreY + shiftY, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.pose().translate(-centreX, -centreY, 0.0F);
            MOVED.set(g.pose().last().pose());
            pushed = true;
            drawing = screen;
         }
      }
   }

   public static void end(Screen screen, GuiGraphics g) {
      if (pushed && drawing == screen) {
         endSide(g);
         pushed = false;
         drawing = null;
         g.pose().popPose();
      }
   }

   public static void beginSide(GuiGraphics g, int dx, int dy, float travel) {
      if (pushed && !side) {
         float left = (1.0F - eased) * Math.max(0.0F, travel);
         sideX = (float)dx * left;
         sideY = (float)dy * left;
         REST.set(MOVED).invert().mul(g.pose().last().pose());
         g.pose().pushPose();
         g.pose().last().pose().set(BEFORE).translate(sideX, sideY, 0.0F).mul(REST);
         side = true;
      }
   }

   public static void endSide(GuiGraphics g) {
      if (side) {
         side = false;
         g.pose().popPose();
      }
   }

   public static boolean active() {
      return pushed;
   }

   public static float backdropAlpha() {
      return pushed ? eased : 1.0F;
   }

   public static void beginBackdrop(GuiGraphics g) {
      if (pushed) {
         g.pose().pushPose();
         g.pose().last().pose().set(BEFORE);
      }
   }

   public static void endBackdrop(GuiGraphics g) {
      if (pushed) {
         g.pose().popPose();
      }

   }

   public static int scissorX(int x) {
      if (!pushed) {
         return x;
      } else {
         return side ? Math.round((float)x + sideX) : Math.round(centreX + ((float)x - centreX) * scale);
      }
   }

   public static int scissorY(int y) {
      if (!pushed) {
         return y;
      } else {
         return side ? Math.round((float)y + sideY) : Math.round(centreY + shiftY + ((float)y - centreY) * scale);
      }
   }

   static {
      style = ContainerMotion.Style.OFF;
      startMs = -1L;
      scale = 1.0F;
      eased = 1.0F;
      BEFORE = new Matrix4f();
      MOVED = new Matrix4f();
      REST = new Matrix4f();
   }

   public static enum Style {
      OFF,
      POP,
      SLIDE;

      public static Style parse(@Nullable String name) {
         if (name != null) {
            for(Style s : values()) {
               if (s.name().equalsIgnoreCase(name.trim())) {
                  return s;
               }
            }
         }

         return POP;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }
}
