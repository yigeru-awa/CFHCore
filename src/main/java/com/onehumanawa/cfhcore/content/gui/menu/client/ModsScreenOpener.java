package com.onehumanawa.cfhcore.content.gui.menu.client;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;

public final class ModsScreenOpener {
   private static final List<String> CANDIDATES = List.of("net.neoforged.neoforge.client.gui.ModListScreen", "com.terraformersmc.modmenu.gui.ModsScreen");
   private static Optional<Function<Screen, Screen>> cached;

   public static synchronized Optional<Function<Screen, Screen>> factory() {
      if (cached != null) {
         return cached;
      } else {
         cached = Optional.empty();

         for(String name : CANDIDATES) {
            try {
               Class<?> cls = Class.forName(name);
               Constructor<?> ctor = cls.getConstructor(Screen.class);
               cached = Optional.of((parent) -> {
                  try {
                     return (Screen)ctor.newInstance(parent);
                  } catch (ReflectiveOperationException e) {
                     throw new IllegalStateException("cannot open " + name, e);
                  }
               });
               CFHGuiMenu.LOGGER.info("[CFHGui Menu] mod list screen: {}", name);
               break;
            } catch (Throwable var4) {
            }
         }

         return cached;
      }
   }

   public static boolean available() {
      return factory().isPresent();
   }

   private ModsScreenOpener() {
   }
}
