package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.platform.Loader;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

public final class NeoForgePlatform implements CFHGuiPlatform {
   public Loader loader() {
      return Loader.NEOFORGE;
   }

   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   public Optional<ModInfo> modInfo(String modId) {
      return ModList.get().getModContainerById(modId).map((c) -> convert(c.getModInfo()));
   }

   public List<ModInfo> allMods() {
      return ModList.get().getMods().stream().map(NeoForgePlatform::convert).toList();
   }

   public Optional<Path> modFile(String modId, String path) {
      try {
         IModFileInfo file = ModList.get().getModFileById(modId);
         if (file == null) {
            return Optional.empty();
         } else {
            Path p = file.getFile().findResource(new String[]{path});
            return Files.isRegularFile(p, new LinkOption[0]) ? Optional.of(p) : Optional.empty();
         }
      } catch (Exception var5) {
         return Optional.empty();
      }
   }

   private static ModInfo convert(IModInfo info) {
      List<String> authors = (List)info.getConfig().getConfigElement(new String[]{"authors"}).map((o) -> List.of(String.valueOf(o).split(",\\s*"))).orElse(List.of());
      Optional<Path> icon = info.getLogoFile().flatMap((logo) -> {
         try {
            Path p = info.getOwningFile().getFile().findResource(new String[]{logo});
            return Files.isRegularFile(p, new LinkOption[0]) ? Optional.of(p) : Optional.empty();
         } catch (Exception var3) {
            return Optional.empty();
         }
      });
      return new ModInfo(info.getModId(), info.getDisplayName(), info.getVersion().toString(), info.getDescription(), authors, icon);
   }

   public Path configDir() {
      return FMLPaths.CONFIGDIR.get();
   }

   public Path gameDir() {
      return FMLPaths.GAMEDIR.get();
   }

   public boolean isClient() {
      return FMLEnvironment.dist.isClient();
   }

   public boolean isDedicatedServer() {
      return FMLEnvironment.dist.isDedicatedServer();
   }

   public boolean isDevelopmentEnvironment() {
      return !FMLLoader.isProduction();
   }

   public Optional<Function<Screen, Screen>> otherModConfigScreen(String modId) {
      return !this.isClient() ? Optional.empty() : ModList.get().getModContainerById(modId).flatMap(NeoForgeConfigScreens::factoryFor);
   }

   public void openUri(URI uri) {
      Util.getPlatform().openUri(uri);
   }

   static final class NeoForgeConfigScreens {
      static Optional<Function<Screen, Screen>> factoryFor(ModContainer container) {
         return container.getCustomExtension(IConfigScreenFactory.class).map((f) -> (parent) -> f.createScreen(container, parent));
      }
   }
}
