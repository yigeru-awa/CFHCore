package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ScreenSwaps {
   private static final Map<Class<? extends Screen>, Function<Screen, Screen>> SWAPS = new LinkedHashMap();
   private static final Map<String, Function<Screen, Screen>> SWAPS_BY_NAME = new LinkedHashMap();
   private static boolean suspended;
   private static final Set<Screen> KEPT = Collections.newSetFromMap(new WeakHashMap());

   public static void register(Class<? extends Screen> vanilla, Function<Screen, Screen> replacement) {
      SWAPS.put(vanilla, replacement);
   }

   public static void unregister(Class<? extends Screen> vanilla) {
      SWAPS.remove(vanilla);
   }

   public static void registerByName(String className, Function<Screen, Screen> replacement) {
      SWAPS_BY_NAME.put(className, replacement);
   }

   public static void unregisterByName(String className) {
      SWAPS_BY_NAME.remove(className);
   }

   public static boolean has(Class<? extends Screen> cls) {
      return SWAPS.containsKey(cls);
   }

   public static void runUnswapped(Runnable r) {
      suspended = true;

      try {
         r.run();
      } finally {
         suspended = false;
      }

   }

   public static @Nullable Screen apply(@Nullable Screen screen) {
      if (screen == null) {
         return null;
      } else if (suspended) {
         KEPT.add(screen);
         return screen;
      } else if (!KEPT.contains(screen) && (!SWAPS.isEmpty() || !SWAPS_BY_NAME.isEmpty())) {
         Function<Screen, Screen> f = (Function)SWAPS.get(screen.getClass());
         if (f == null) {
            f = (Function)SWAPS_BY_NAME.get(screen.getClass().getName());
         }

         if (f == null) {
            return screen;
         } else {
            try {
               Screen s = (Screen)f.apply(screen);
               return s == null ? screen : s;
            } catch (Exception e) {
               CFHGui.LOGGER.error("[CFHGui] screen swap for {} failed; showing vanilla", screen.getClass().getName(), e);
               return screen;
            }
         }
      } else {
         return screen;
      }
   }

   private ScreenSwaps() {
   }
}
