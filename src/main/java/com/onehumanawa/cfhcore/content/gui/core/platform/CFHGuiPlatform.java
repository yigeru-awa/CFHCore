package com.onehumanawa.cfhcore.content.gui.core.platform;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;

public interface CFHGuiPlatform {
   static CFHGuiPlatform get() {
      return (CFHGuiPlatform)Services.load(CFHGuiPlatform.class);
   }

   Loader loader();

   boolean isModLoaded(String var1);

   Optional<ModInfo> modInfo(String var1);

   List<ModInfo> allMods();

   default Optional<Path> modFile(String modId, String path) {
      return Optional.empty();
   }

   Path configDir();

   Path gameDir();

   boolean isClient();

   boolean isDedicatedServer();

   boolean isDevelopmentEnvironment();

   Optional<Function<Screen, Screen>> otherModConfigScreen(String var1);

   void openUri(URI var1);
}
