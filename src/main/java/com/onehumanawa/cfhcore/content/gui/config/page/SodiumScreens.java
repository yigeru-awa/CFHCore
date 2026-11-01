package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Opens Sodium's own video settings screen. Sodium is an optional pack
 * dependency, so its GUI is reached through the same static factory that its
 * {@code OptionsScreenMixin} uses to replace the vanilla "Video Settings"
 * button: {@code VideoSettingsScreen.createScreen(Screen)}.
 */
final class SodiumScreens {
   private static final String VIDEO_SETTINGS_CLASS = "net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen";
   private static @Nullable Optional<Function<Screen, Screen>> cached;

   static boolean loaded() {
      return CFHGuiPlatform.get().isModLoaded("sodium");
   }

   static synchronized Optional<Function<Screen, Screen>> videoSettings() {
      if (cached != null) {
         return cached;
      } else {
         cached = Optional.empty();
         if (loaded()) {
            try {
               Class<?> cls = Class.forName(VIDEO_SETTINGS_CLASS);
               Method factory = cls.getMethod("createScreen", Screen.class);
               cached = Optional.of((parent) -> {
                  try {
                     return (Screen)factory.invoke(null, parent);
                  } catch (ReflectiveOperationException e) {
                     throw new IllegalStateException("cannot open Sodium video settings", e);
                  }
               });
            } catch (LinkageError | RuntimeException | ReflectiveOperationException ignored) {
            }
         }

         return cached;
      }
   }

   private SodiumScreens() {
   }
}
