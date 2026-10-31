package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class MenuBackdrop {
   private static final int VEIL_TOP = 460811;
   private static final int VEIL_BOTTOM = 1644309;
   private static final int VEIL_ALPHA = 240;
   private static final float VEIL_VIGNETTE = 0.45F;
   private static volatile @Nullable Painter painter;
   private static volatile boolean failed;
   private static int suspended;

   public static void suspend() {
      ++suspended;
   }

   public static void resume() {
      if (suspended > 0) {
         --suspended;
      }

   }

   public static void painter(@Nullable Painter p) {
      painter = p;
   }

   public static Kind kind() {
      return MenuBackdrop.Kind.of(CFHGui.config().menuBackground);
   }

   public static boolean active() {
      if (painter != null && !failed && suspended <= 0 && kind() != MenuBackdrop.Kind.OFF) {
         Minecraft mc = Minecraft.getInstance();
         return mc.level == null && !Reskin.leftToOther(mc.screen);
      } else {
         return false;
      }
   }

   public static boolean active(Kind kind) {
      return active() && kind() == kind;
   }

   public static boolean worldBehind() {
      return Minecraft.getInstance().level != null || active();
   }

   public static boolean veiled() {
      return active() && !Theme.current().isVanilla();
   }

   public static void drawVeil(GuiGraphics g, int x, int y, int w, int h) {
      CFHGuiDraw.vgradient(g, x, y, w, h, Colors.withAlpha(460811, 240), Colors.withAlpha(1644309, 240));
      CFHGuiDraw.vignette(g, x, y, w, h, 0.45F);
   }

   /** @deprecated */
   @Deprecated
   public static int veil(int overWorld) {
      return Colors.withAlpha(Colors.lerp(-16316405, -15132907, 0.5F), 240);
   }

   public static boolean paint(GuiGraphics g, int width, int height, float partialTick) {
      Painter p = painter;
      if (p != null && !failed && suspended <= 0) {
         Kind kind = kind();
         if (kind == MenuBackdrop.Kind.OFF) {
            return false;
         } else {
            try {
               return p.paint(kind, g, width, height, partialTick);
            } catch (LinkageError | RuntimeException e) {
               failed = true;
               CFHGui.LOGGER.error("[CFHGui] the scene behind the menus failed; menus draw their own backgrounds for the rest of the session", e);
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private MenuBackdrop() {
   }

   public static enum Kind {
      SKY("sky"),
      TITLE("title"),
      OFF("off");

      private final String id;

      private Kind(final String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }

      public Component title() {
         return Component.translatable("cfhgui.menu_background." + this.id);
      }

      public static Kind of(@Nullable String id) {
         if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);

            for(Kind k : values()) {
               if (k.id.equals(key)) {
                  return k;
               }
            }
         }

         return SKY;
      }
   }

   @FunctionalInterface
   public interface Painter {
      boolean paint(Kind var1, GuiGraphics var2, int var3, int var4, float var5);
   }
}
