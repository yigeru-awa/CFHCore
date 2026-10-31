package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.jetbrains.annotations.Nullable;

public final class StartupTime {
   private static final String MARK = "cfhgui.startup.mark.";
   private static boolean done;
   private static long tookMs = -1L;
   private static final Map<String, Long> slowed = new HashMap();
   private static @Nullable Field reloadField;
   private static boolean reloadFieldLooked;

   public static boolean running() {
      return !done;
   }

   public static long took() {
      return tookMs;
   }

   public static void mark(String stage) {
      String key = "cfhgui.startup.mark." + stage;
      if (System.getProperty(key) == null) {
         long began = processStart();
         if (began > 0L) {
            System.setProperty(key, Long.toString(Math.max(0L, System.currentTimeMillis() - began)));
         }

      }
   }

   static void screenSet(@Nullable Screen screen) {
      if (screen != null && !done && System.getProperty("cfhgui.startup.mark.end") == null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getOverlay() != null) {
            Overlay var3 = mc.getOverlay();
            if (var3 instanceof LoadingOverlay) {
               LoadingOverlay overlay = (LoadingOverlay)var3;
               if (!reloadDone(overlay)) {
                  CFHGui.LOGGER.debug("[CFHGui] a screen was set during the start's resource reload ({}): not yet its end", screen.getClass().getName());
                  return;
               }
            }

            mark("end");
            CFHGui.LOGGER.info("[CFHGui] the start's first screen is set ({}): the loading screens' bar is full", screen.getClass().getName());
         }
      }
   }

   private static boolean reloadDone(LoadingOverlay overlay) {
      try {
         if (!reloadFieldLooked) {
            reloadFieldLooked = true;

            for(Field f : LoadingOverlay.class.getDeclaredFields()) {
               if (f.getType() == ReloadInstance.class && !Modifier.isStatic(f.getModifiers())) {
                  f.setAccessible(true);
                  reloadField = f;
               }
            }
         }

         boolean var10000;
         if (reloadField != null) {
            Object var7 = reloadField.get(overlay);
            if (var7 instanceof ReloadInstance) {
               ReloadInstance reload = (ReloadInstance)var7;
               if (!reload.isDone()) {
                  var10000 = false;
                  return var10000;
               }
            }
         }

         var10000 = true;
         return var10000;
      } catch (RuntimeException | ReflectiveOperationException var5) {
         return true;
      }
   }

   static void tick(Minecraft mc) {
      if (!done && mc.getOverlay() == null && mc.screen != null) {
         done = true;
         long began = processStart();
         if (began > 0L) {
            tookMs = Math.max(0L, System.currentTimeMillis() - began);
            long tenths = tookMs / 100L;
            String time = String.format(Locale.ROOT, "%d:%02d.%d", tenths / 600L, tenths / 10L % 60L, tenths % 10L);
            CFHGui.LOGGER.info("[CFHGui] the game took {} to start (from its process start to the first screen)", time);
            if (CFHGui.config().startupTimer) {
               CFHGuiToasts.show(Component.translatable("cfhgui.startup.done.title"), Component.translatable("cfhgui.startup.done", new Object[]{time}), Icon.CLOCK);
            }

         }
      }
   }

   public static void slow(String stage) {
      slow(stage, Long.MAX_VALUE);
   }

   public static void slow(String stage, long slice) {
      String spec = System.getProperty("cfhgui.startupSlow");
      if (spec != null && !spec.isBlank()) {
         long ms;
         synchronized(slowed) {
            Long left = (Long)slowed.get(stage);
            if (left == null) {
               left = 0L;

               for(String part : spec.split("[;,]")) {
                  int colon = part.indexOf(58);
                  if (colon >= 0 && part.substring(0, colon).trim().equalsIgnoreCase(stage)) {
                     try {
                        left = Math.max(0L, Math.min(Long.parseLong(part.substring(colon + 1).trim()), 300000L));
                        CFHGui.LOGGER.info("[CFHGui] development: the start's {} stage is made {} ms slower (cfhgui.startupSlow)", stage, left);
                     } catch (NumberFormatException var16) {
                        CFHGui.LOGGER.warn("[CFHGui] cfhgui.startupSlow: not a number of milliseconds for {}: {}", stage, part);
                     }
                     break;
                  }
               }
            }

            ms = Math.min(left, slice);
            slowed.put(stage, left - ms);
         }

         if (ms > 0L) {
            try {
               Thread.sleep(ms);
            } catch (InterruptedException var15) {
               Thread.currentThread().interrupt();
            }

         }
      }
   }

   private static long processStart() {
      try {
         return ManagementFactory.getRuntimeMXBean().getStartTime();
      } catch (LinkageError | RuntimeException var1) {
         return 0L;
      }
   }

   private StartupTime() {
   }
}
