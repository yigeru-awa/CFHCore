package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.nio.ByteBuffer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class CFHGuiCursor {
   private static final String[] ARROW = new String[]{"O", "OO", "OFO", "OFFO", "OFFFO", "OFFFFO", "OFFFFFO", "OFFFFFFO", "OFFFFFFFO", "OFFFFFFFFO", "OFFFFFFFFFO", "OFFFFFFOOOOO", "OFFFOFFO", "OFFOOFFO", "OFO  OFFO", "OO   OFFO", "O     OFFO", "      OFFO", "       OO"};
   private static final int ART_W = 12;
   private static final int ART_H;
   private static final int SHADOW = 1711276032;
   private static long cursor;
   private static String made;
   private static boolean set;

   static void init() {
      CFHGuiEvents.SCREEN_OPENED.register(CFHGuiCursor::apply);
      CFHGuiEvents.SCREEN_INIT_POST.register(CFHGuiCursor::apply);
      Theme.onChange(() -> {
         if (RenderSystem.isOnRenderThread()) {
            apply(Minecraft.getInstance().screen);
         }

      });
   }

   public static Style style() {
      return CFHGuiCursor.Style.parse(CFHGui.config().cursor);
   }

   private static void apply(@Nullable Screen screen) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getWindow() != null) {
         long window = mc.getWindow().getWindow();
         Style style = style();
         if (style != CFHGuiCursor.Style.SYSTEM && screen != null) {
            Palette p = Theme.variant(com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style.CFHGUI).palette();
            int fill = style == CFHGuiCursor.Style.ACCENT ? p.accentInk() : p.text();
            int outline = p.bg();
            int px = Math.max(1, (int)Math.round(mc.getWindow().getGuiScale()) / 2);
            String var10000 = String.valueOf(style);
            String want = var10000 + "/" + Integer.toHexString(fill) + "/" + Integer.toHexString(outline) + "/" + px;
            if (cursor == 0L || !want.equals(made)) {
               long next = create(fill, outline, px);
               if (next == 0L) {
                  return;
               }

               long old = cursor;
               cursor = next;
               made = want;
               set = false;
               if (old != 0L) {
                  GLFW.glfwDestroyCursor(old);
               }
            }

            if (!set) {
               GLFW.glfwSetCursor(window, cursor);
               set = true;
            }

         } else {
            if (set) {
               GLFW.glfwSetCursor(window, 0L);
               set = false;
            }

         }
      }
   }

   private static long create(int fill, int outline, int px) {
      int w = 13 * px;
      int h = (ART_H + 1) * px;
      ByteBuffer pixels = MemoryUtil.memAlloc(w * h * 4);

      long var21;
      try {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            for(int y = 0; y < h; ++y) {
               for(int x = 0; x < w; ++x) {
                  int ax = x / px;
                  int ay = y / px;
                  int argb = colour(ax, ay, fill, outline);
                  if (argb == 0 && colour(ax - 1, ay - 1, fill, outline) != 0) {
                     argb = 1711276032;
                  }

                  pixels.put((byte)Colors.red(argb)).put((byte)Colors.green(argb)).put((byte)Colors.blue(argb)).put((byte)Colors.alpha(argb));
               }
            }

            pixels.flip();
            GLFWImage image = GLFWImage.malloc(stack);
            image.set(w, h, pixels);
            long handle = GLFW.glfwCreateCursor(image, 0, 0);
            if (handle == 0L) {
               CFHGui.LOGGER.warn("[CFHGui] the window could not take CFHGui's pointer: the system's stays");
            }

            var21 = handle;
         } catch (Throwable var17) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var16) {
                  var17.addSuppressed(var16);
               }
            }

            throw var17;
         }

         if (stack != null) {
            stack.close();
         }
      } finally {
         MemoryUtil.memFree(pixels);
      }

      return var21;
   }

   private static int colour(int x, int y, int fill, int outline) {
      if (x >= 0 && y >= 0 && y < ART_H && x < ARROW[y].length()) {
         int var10000;
         switch (ARROW[y].charAt(x)) {
            case 'F' -> var10000 = Colors.withAlpha(fill, 255);
            case 'O' -> var10000 = Colors.withAlpha(outline, 255);
            default -> var10000 = 0;
         }

         return var10000;
      } else {
         return 0;
      }
   }

   private CFHGuiCursor() {
   }

   static {
      ART_H = ARROW.length;
      made = "";
   }

   public static enum Style {
      SYSTEM,
      CFHGUI,
      ACCENT;

      public static Style parse(@Nullable String value) {
         if (value != null) {
            for(Style s : values()) {
               if (s.name().equalsIgnoreCase(value.trim())) {
                  return s;
               }
            }
         }

         return SYSTEM;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      public Component title() {
         return Component.translatable("cfhgui.cursor." + this.key());
      }
   }
}
